# Tasks

## 1. Mailbox Expiration Semantics (`deactivationTime: 0`)

- [x] 1.1 Update `MailboxApiController` creation, reactivation, and update logic to set `expired = (mailboxData.deactivationTime > 0 && mailboxData.deactivationTime <= System.currentTimeMillis())`, treating `0` as permanent (active), and verify compilation
- [x] 1.2 Add unit and controller tests in `MailboxApiControllerTest` verifying that creating and updating mailboxes with `deactivationTime: 0` (or omitted) yields an active mailbox with `expired = false` and `ts_Active = 0`, whereas future timestamps remain active and past timestamps become expired

## 2. Atomic Counter Increments in Mail Delivery

- [x] 2.1 Update `MailDeliveryService` to call `mailboxRepository.incrementSuppressions(mbox.getId())` instead of mutating `mbox.increaseSuppressions()` and calling `mailboxRepository.save(mbox)` when handling inactive/expired mailboxes or inactive owners
- [x] 2.2 Update `MailDeliveryService` to call `mailboxRepository.incrementForwards(mbox.getId())` instead of mutating `mbox.increaseForwards()` and calling `mailboxRepository.save(mbox)` when forwarding emails
- [x] 2.3 Add integration test verifying concurrent delivery invocations increment suppression and forward counters without throwing `ObjectOptimisticLockingFailureException`

## 3. Documentation & Load Verification

- [x] 3.1 Update `docs/load-testing-guide.md` to document the correct `/api/v1/mailboxes` endpoint, provide complete payload examples, clarify `deactivationTime: 0` (permanent) vs positive timestamps, and verify markdown links
- [x] 3.2 Run the complete project build (`mvn clean test`) to ensure all unit and integration tests pass

