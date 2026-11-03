# Tasks

## 1. Repository Layer Extensions & Slice Tests (Tier 2)

- [x] 1.1 Add `findExpiringByUsr` and `findExpiringByUsrAndSearch` queries to `MailboxRepository` with `final` parameters, comprehensive Javadoc, and code comments, and verify with Tier 2 repository slice tests verifying filtering of active finite mailboxes (`expired = false AND ts_Active > 0`).
- [x] 1.2 Add `findUnlimitedByUsr` and `findUnlimitedByUsrAndSearch` queries to `MailboxRepository` with `final` parameters, comprehensive Javadoc, and code comments, and verify with Tier 2 repository slice tests verifying filtering of active permanent mailboxes (`expired = false AND ts_Active = 0`).

## 2. Controller Sorting & Status Filter Logic with Unit Tests (Tier 1)

- [x] 2.1 Add `resolveStatus(final String status)` helper in `MailboxWebController` to whitelist and map `ALL`, `ACTIVE`, `EXPIRED`, `EXPIRING`, and `UNLIMITED` with safe fallback to `ALL`, using `final` modifiers, full Javadoc, and code comments, and verify with Tier 1 unit tests covering happy paths, case insensitivity, whitespace, null/empty values, and invalid/malicious strings.
- [x] 2.2 Update `resolveSort(final String sort, final String dir)` in `MailboxWebController` to implement Perspective A ($+\infty$) expiration sorting via conditional `JpaSort.unsafe` expressions (`CASE WHEN ts_Active = 0 THEN 1 ELSE 0 END ASC, ts_Active ASC` for asc; `CASE WHEN ts_Active = 0 THEN 0 ELSE 1 END ASC, ts_Active DESC` for desc) using `final` modifiers, Javadoc, and code comments, and verify with Tier 1 unit tests.
- [x] 2.3 Update `listMailboxes` in `MailboxWebController` to branch on `EXPIRING` and `UNLIMITED` status filters, routing to corresponding repository methods while preserving search queries, model attributes, and pageable sort order, and verify via controller mock tests.

## 3. Frontend Dashboard Template & Parameter Binding

- [x] 3.1 Update the status filter select element in `templates/mailboxes/index.html` to add `EXPIRING` ("Expiring Only") and `UNLIMITED` ("Unlimited Only") options while preserving existing `ALL`, `ACTIVE`, and `EXPIRED` options and maintaining mobile responsiveness, and verify template rendering and HTMX parameter serialization.

## 4. End-to-End Testing Pyramid & Integration Verification (Tier 3)

- [x] 4.1 Update existing `testSorting_SpecialCase_PermanentUnlimitedMailboxes_ExpiresSort` in `MailboxSortingIntegrationTest` to assert Perspective A behavior: `Expires ASC` puts unlimited mailboxes at the end ($+\infty$) and `Expires DESC` puts unlimited mailboxes at the top ($+\infty$).
- [x] 4.2 Add Tier 3 integration tests in `MailboxSortingIntegrationTest` for happy paths: status=EXPIRING, status=UNLIMITED, and combined filter/sort expressions.
- [x] 4.3 Add Tier 3 integration tests in `MailboxSortingIntegrationTest` for error and special cases: invalid status fallback, invalid sort fallback, empty account, and all-unlimited accounts.
- [x] 4.4 Run full project verification via `mvn clean test -pl xcmailr-webapp` to verify zero regressions, zero test failures, and full test suite success.
