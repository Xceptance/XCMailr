# Design

## Context

Under concurrent load testing, sending emails to the same recipient mailbox (such as static address `jw@xcmailr.test` in the `SendMail` scenario) results in high rates of `ObjectOptimisticLockingFailureException` in `MailDeliveryService`. This occurs because `MailDeliveryService` increments delivery counters by updating the in-memory entity and calling `mailboxRepository.save(mbox)`:

```java
mbox.increaseSuppressions();
mailboxRepository.save(mbox);
```

Because `MBox` declares a JPA `@Version` column (`version`), concurrent updates on the same mailbox row collide on the version check, rolling back the transaction and propagating an unhandled exception to SubEthaSMTP. SubEthaSMTP then returns SMTP status `421 4.3.0 Mail system failure` and terminates the socket connection.

Although atomic queries `MailboxRepository.incrementSuppressions(id)` and `MailboxRepository.incrementForwards(id)` are already defined on `MailboxRepository`, `MailDeliveryService` has not been wired to use them.

Additionally, `MailboxApiController` evaluates expiration during mailbox creation and update as:
```java
mailbox.setExpired(mailboxData.deactivationTime <= System.currentTimeMillis());
```
In XCMailr's domain model, `deactivationTime == 0` denotes a permanent mailbox that never expires (as reflected in `MBox.isExpiredByTimestamp()`, which only expires if `ts_Active != 0`). However, because `0 <= System.currentTimeMillis()` evaluates to `true`, any mailbox created with `deactivationTime: 0` (or with `deactivationTime` omitted from JSON) is mistakenly marked as expired immediately upon creation.

## Goals / Non-Goals

**Goals:**
- Replace entity mutation and `mailboxRepository.save(mbox)` in `MailDeliveryService` with direct atomic repository calls (`incrementSuppressions(id)` and `incrementForwards(id)`).
- Ensure incoming email deliveries to the same mailbox recipient under high concurrency never fail due to optimistic locking or version collisions.
- Correct `MailboxApiController` so that `deactivationTime: 0` (or omitted) creates/maintains an active mailbox (`expired = false`).
- Update `docs/load-testing-guide.md` with explicit details on mailbox creation payloads, endpoints, and expiration options.
- Add unit/integration tests verifying the `deactivationTime: 0` behavior and concurrent delivery counter updates.

**Non-Goals:**
- Removing JPA `@Version` optimistic locking from the `MBox` entity entirely (it remains useful for user web UI and REST CRUD operations).
- Modifying SMTP connection management or listener thread pools in SubEthaSMTP.

## Decisions

### Decision 1: Use Existing Atomic JPQL Queries in MailDeliveryService
Instead of calling `mbox.increaseSuppressions(); mailboxRepository.save(mbox);`, `MailDeliveryService` will invoke:
```java
mailboxRepository.incrementSuppressions(mbox.getId());
```
and for forwards:
```java
mailboxRepository.incrementForwards(mbox.getId());
```
*Rationale*: `MailboxRepository` already contains `@Modifying @Transactional @Query` methods that execute direct database `UPDATE MBox b SET b.suppressions = b.suppressions + 1 WHERE b.id = :id`. This bypasses Hibernate dirty-checking and `@Version` increments, allowing arbitrarily many concurrent delivery transactions to increment the counters safely at the database level.

*Alternatives Considered*:
- *Retry loops with backoff around `save()`*: Adds latency, consumes thread resources, and will still fail under sustained 100+ req/sec loads.
- *Pessimistic write locking (`PESSIMISTIC_WRITE`)*: Forces sequential lock acquisition on the mailbox row, reducing delivery concurrency and risking lock timeouts under load.

### Decision 2: Guard Mailbox Expiration Calculation on `deactivationTime > 0`
In `MailboxApiController`, define helper logic for computing the expired flag:
```java
final boolean isExpired = mailboxData.deactivationTime > 0 && mailboxData.deactivationTime <= System.currentTimeMillis();
```
Apply this logic to:
1. `POST /api/v1/mailboxes` (creating a new custom/random mailbox)
2. `POST /api/v1/mailboxes` (reactivating an existing mailbox owned by the user)
3. `PUT /api/v1/mailboxes/{mailboxAddress}` (updating mailbox attributes)

*Rationale*: XCMailr's domain design establishes that a timestamp of `0` means "unlimited / no expiration". Only timestamps greater than 0 represent explicit expiration deadlines. This aligns `MailboxApiController` with `MBox.isExpiredByTimestamp()` and allows default JSON payloads (`{"address":"user@domain.com"}`) to produce active mailboxes.

*Alternatives Considered*:
- *Changing `deactivationTime` to nullable `Long`*: Adds complexity and potential null pointer issues across existing clients. Sticking to `long` with `0` representing permanent matches the existing domain model.

### Decision 3: Clarify Documentation and Pre-Test Setup
In `docs/load-testing-guide.md`:
- Document that `{"address":"jw@xcmailr.test"}` creates a permanent, non-expiring mailbox because `deactivationTime` defaults to `0`.
- Document how to create temporary mailboxes by specifying a future epoch millisecond timestamp if desired.

## Risks / Trade-offs

- **[Risk] Entity in-memory state desynchronization**: After calling `incrementSuppressions(mbox.getId())`, the local in-memory `mbox` object's `suppressions` counter is not updated.
  - *Mitigation*: In `MailDeliveryService.deliver()`, execution returns immediately after recording the transaction and incrementing the counter; `mbox` is never persisted or read again in that request scope.
- **[Risk] Negative or invalid deactivation timestamps**: A client might send a negative value for `deactivationTime`.
  - *Mitigation*: The condition `deactivationTime > 0 && deactivationTime <= System.currentTimeMillis()` ensures negative values are not considered past expiration. Additional input validation can reject negative timestamps if needed.

