# Proposal

## Why
Navigating to the unified inbox (`/mails`) or mailbox mail listing (`/mailboxes/{boxId}/mails`) currently executes unpaginated entity queries (`mailRepository.findByMailboxInOrderByReceiveTimeDesc` and `findByMailboxOrderByReceiveTimeAsc`) that load full `Mail` entities into memory. Because `Mail` contains an eager `@Lob byte[] message` column holding complete raw MIME payloads, Hibernate clones each byte array for dirty-checking snapshots. In high-volume environments (such as load tests generating thousands of emails under `admin@xcmailr.test`), this immediately exhausts the JVM heap (`java.lang.OutOfMemoryError: Java heap space`), corrupts H2's MVStore, and causes a cascading HTTP 500 error that breaks the application until restarted.

## What Changes
- **Spring Data JPA Projection (`MailSummaryProjection`)**: Introduce a lightweight interface projection selecting only listing fields (`id`, `mailbox`, `sender`, `subject`, `receiveTime`), completely excluding the heavy `@Lob byte[] message` BLOB from SQL `SELECT` queries for all list views.
- **Database-Level Pagination in `MailRepository`**: Add paginated queries (`Page<MailSummaryProjection> findSummaryByMailboxIn(...)` and `Page<MailSummaryProjection> findSummaryByMailbox(...)`) utilizing Spring Data `Pageable` to translate requests into native SQL `LIMIT` and `OFFSET` clauses.
- **Controller Pagination in `MailWebController`**: Update `/mails` and `/mailboxes/{boxId}/mails` to accept `page` (default 0) and `size` (default 25) parameters, passing `Pageable` sorted by `receiveTime` descending, and exposing the `Page<MailSummaryProjection>` to templates.
- **UI Pagination Controls**: Update Thymeleaf templates (`all-mail-list.html` and `mail-list.html`) with pagination bars (previous, next, page numbers, item count summary) driven by HTMX (`hx-get`, `hx-target`, `hx-push-url`) matching the UX patterns of `mailbox-list.html`.
- **Defensive Parameter Normalization & SQL Injection Immunity**:
  - Sanitize and clamp pagination inputs (`page >= 0`, `size` clamped to [1, 100]).
  - Ensure zero SQL injection risk: controller parameters (`page`, `size`, `boxId`) are strongly-typed (`int`, `long`), sort properties are strictly hardcoded at compile time (never accepted from request parameters), and repository queries rely exclusively on parameterized JDBC bind variables (`PreparedStatement`).
- **Strict Multi-Tenant Data Isolation**:
  - Enforce that users can only view emails belonging to their own mailboxes: `/mails` strictly queries mailboxes owned by the authenticated `Principal`, and `/mailboxes/{boxId}/mails` verifies mailbox ownership via `getMailboxForUser`, rejecting cross-user access with HTTP 403 Forbidden.
- **Comprehensive Testing Pyramid**: Add unit tests, repository slice tests (`@DataJpaTest`), MVC controller slice tests (`@WebMvcTest`), and integration tests covering happy paths, edge cases (out-of-range pages, empty inboxes), security tests (multi-tenant isolation, unauthorized mailbox access, SQL injection payload rejection), and memory safety verification.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Update `Unified Cross-Mailbox Inbox View` and `Mailbox Email Inspection View` requirements to require server-side database-level pagination, lightweight projection querying excluding `@Lob byte[] message`, pagination navigation controls, robust handling of page boundaries, strict multi-tenant isolation, and explicit protection against SQL injection attempts.

## Impact
- **Controllers & Repositories**: `MailWebController`, `MailRepository`, and `models/Mail`.
- **DTOs / Projections**: New `MailSummaryProjection`.
- **Templates**: `mailboxes/all-mails.html`, `mailboxes/fragments/all-mail-list.html`, `mailboxes/mails.html`, and `mailboxes/fragments/mail-list.html`.
- **API / Backward Compatibility**: No breaking changes to REST API (`/api/v1/mails` remains separate and already accepts query parameters). Internal web endpoints support `page` and `size` query parameters with sensible defaults.
- **Security & Multi-Tenancy**: Guaranteed cross-tenant isolation and complete immunity against SQL injection via strongly-typed parameters, compile-time sorting, and parameterized queries.
- **Performance & Stability**: Drastically reduces JVM heap allocations from $O(N)$ with full MIME deep copies to $O(1)$ constant memory per request (~a few KB), permanently preventing heap exhaustion during web mail listing.
