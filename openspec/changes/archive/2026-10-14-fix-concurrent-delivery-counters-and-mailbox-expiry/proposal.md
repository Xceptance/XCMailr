# Proposal

## Why

Under concurrent load testing, incoming SMTP deliveries to the same mailbox recipient fail with JPA `ObjectOptimisticLockingFailureException` (leading to SMTP `421 Mail system failure` and socket aborts) because `MailDeliveryService` mutates and persists the full `MBox` entity (`mailboxRepository.save(mbox)`) to increment `suppressions` or `forwards`. Furthermore, mailboxes created via `POST /api/v1/mailboxes` with `deactivationTime: 0` (or omitted) are mistakenly marked as immediately expired (`expired = true`) because the API controller evaluates `deactivationTime <= System.currentTimeMillis()`, misinterpreting the domain convention where `0` denotes an unlimited, non-expiring mailbox.

Eliminating these locking collisions with atomic database increments and correcting the `deactivationTime: 0` semantics will allow high-throughput concurrent email ingestion without socket drops, correctly support permanent mailboxes, and ensure our documentation and load tests operate accurately.

## What Changes

- **Atomic Counter Increments**: Introduce direct atomic JPQL/SQL update queries (`incrementSuppressions` and `incrementForwards`) in `MailboxRepository` and call them from `MailDeliveryService` instead of loading, mutating, and calling `save(mbox)`. This bypasses JPA entity `@Version` checks and prevents concurrent optimistic locking conflicts.
- **Permanent Mailbox Handling (`deactivationTime: 0`)**: Update `MailboxApiController` creation and update logic so that a `deactivationTime` of `0` (or omitted) is treated as permanent / never expiring (`expired = false`). Expiration is only evaluated when `deactivationTime > 0`.
- **Load Testing Documentation & Guidance**: Update `docs/load-testing-guide.md` with explicit details on mailbox creation payloads, the `/api/v1/mailboxes` endpoint, and guidelines on `deactivationTime` semantics (unlimited vs future epoch timestamp).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `mailbox-management-api`: Clarify and enforce that creating or updating a mailbox with `deactivationTime: 0` (or omitted) establishes an active, permanent mailbox (`expired = false`).
- `inbound-smtp-engine`: Require atomic counter increments for mailbox suppressions and forwards during email delivery to prevent concurrency collisions and SMTP session drops under load.

## Impact

- **Code Impact**:
  - `com.xceptance.xcmailr.repositories.MailboxRepository`: Add `@Modifying` atomic update queries.
  - `com.xceptance.xcmailr.services.MailDeliveryService`: Replace entity mutation + `save()` with atomic repository counter methods.
  - `com.xceptance.xcmailr.controllers.restapi.MailboxApiController`: Correct `deactivationTime <= System.currentTimeMillis()` checks to consider `deactivationTime > 0`.
  - `docs/load-testing-guide.md`: Update mailbox creation command examples and explain expiration timestamp options.
- **API Impact**: Backward compatible bug fix for REST API consumers; sending `deactivationTime: 0` or omitting it now properly creates an active mailbox instead of an expired one.
- **Database Impact**: Direct SQL updates on `MBox.suppressions` and `MBox.forwards` by primary key `id`.

