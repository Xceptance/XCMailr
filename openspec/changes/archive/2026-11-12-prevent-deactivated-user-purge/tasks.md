# Tasks

## 1. Eliminate Automated User Purge

- [x] 1.1 Remove `purgeExpiredInactiveUsers` call from `runMaintenanceTasks()` and remove method `purgeExpiredInactiveUsers` in `ExpirationScheduledService.java`, verifying code compiles without warnings
- [x] 1.2 Remove `findExpiredInactiveUsers` query method from `UserRepository.java`, verifying no remaining invocations exist across the codebase

## 2. Admin Controller Token Hygiene

- [x] 2.1 Update `toggleUserActive` in `AdminWebController.java` to defensively clear `confirmation` and `ts_confirm` when an administrator activates an inactive account, verifying parameter and variable `final` modifiers and Javadoc

## 3. Test Coverage and Verification

- [x] 3.1 Add integration test in `ExpirationScheduledServiceTest.java` verifying that running `runMaintenanceTasks()` preserves both inactive and unconfirmed user accounts with expired confirmation timestamps
- [x] 3.2 Add test in `AdminWebControllerTest.java` verifying that administrative user activation clears lingering confirmation tokens and timestamps
- [x] 3.3 Run test suite via `mvn test -Dtest=ExpirationScheduledServiceTest,AdminWebControllerTest,WebAuthControllerTest` and verify all tests pass
