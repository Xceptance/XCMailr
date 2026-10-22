# Tasks

## 1. Projection Interface Definition

- [x] 1.1 Create `MailSummaryProjection` interface in `com.xceptance.xcmailr.repositories.projections` defining getters for `id`, `mailbox`, `sender`, `subject`, and `receiveTime`, with comprehensive class-level and method-level Javadoc, and verify compilation via `./mvnw compile`.

## 2. Repository Layer Pagination & Projection

- [x] 2.1 Add paginated summary methods `Page<MailSummaryProjection> findSummaryByMailboxIn(Collection<Long> mailboxIds, Pageable pageable)` and `Page<MailSummaryProjection> findSummaryByMailbox(Long mailboxId, Pageable pageable)` to `MailRepository.java`, with full Javadoc and `final` parameter modifiers, and verify compilation via `./mvnw test-compile`.
- [x] 2.2 Add repository slice tests in `MailRepositoryTest.java` (`@DataJpaTest`) covering:
  - Happy path: paginated querying with sorting descending by `receiveTime`.
  - Projection optimization: verify queries return `MailSummaryProjection` instances without loading `@Lob byte[] message` into memory.
  - Multi-tenant data isolation: seed User A with Mailbox 1 (emails M1, M2) and User B with Mailbox 2 (email M3); verify `findSummaryByMailboxIn` with User A's mailbox IDs returns only M1 and M2 (totalElements = 2) and zero records of M3; verify `findSummaryByMailbox` for Mailbox 1 never returns M3.
  - SQL injection immunity: verify that repository queries use parameterized bindings for collection and single-id inputs.
  - Special and edge cases: empty mailbox list input, mailbox with zero received emails, page index beyond total pages (returns empty page with valid total elements and page count).
  - Verify all repository slice tests pass via `./mvnw test -Dtest=MailRepositoryTest`.

## 3. Controller Layer Pagination and Parameter Normalization

- [x] 3.1 Refactor `MailWebController.java` endpoints (`GET /mails` and `GET /mailboxes/{boxId}/mails`) to accept pagination parameters (`page`, `size`) and `HX-Request` header, apply defensive parameter normalization (`Math.max(0, page)`, `Math.min(Math.max(1, size), 100)`), short-circuit immediately on empty mailbox lists returning `Page.empty()`, enforce static compile-time sorting (`Sort.by(Sort.Direction.DESC, "receiveTime")`) to prevent SQL injection, apply `final` to all parameters and local variables, and add thorough Javadoc and inline code comments explaining projection and security safeguards.
- [x] 3.2 Update `MailWebController.java` to inject pagination model attributes (`mailPage`, `currentPage`, `totalPages`, `totalElements`, `pageSize`) and support dual rendering modes: full page layout for standard requests and HTMX partial fragment (`all-mail-list :: mailList` or `mail-list :: mailList`) when `HX-Request` is present.
- [x] 3.3 Add web slice tests in `MailWebControllerTest.java` (`@WebMvcTest`) covering the testing pyramid:
  - Happy path: full page load for `GET /mails` and `GET /mailboxes/{boxId}/mails` verifying status 200, view name, and pagination model attributes.
  - Happy path HTMX navigation: requests with `HX-Request: true` verifying fragment view returned (`all-mail-list :: mailList` or `mail-list :: mailList`).
  - Parameter normalization & bounds checking: negative page number clamped to 0, excessive page size clamped to 100, page index exceeding available pages returning empty page, user owning zero mailboxes returning empty page without invoking repository query, mailbox with zero emails.
  - Multi-tenant data isolation: authenticate as User A, request `GET /mails`, verify only User A's mailboxes and emails are resolved, and verify User B's emails/addresses are never queried or leaked into model/view.
  - Unauthorized mailbox access: authenticate as User A, request `GET /mailboxes/{user2BoxId}/mails`, verify response status is HTTP 403 Forbidden and repository mail queries are never invoked.
  - SQL injection prevention tests:
    - Test `GET /mails?page=1' OR '1'='1` verifying rejection with HTTP 400 Bad Request.
    - Test `GET /mails?size=25; DROP TABLE mail;--` verifying rejection with HTTP 400 Bad Request.
    - Test `GET /mailboxes/1' OR '1'='1/mails` verifying rejection with HTTP 400 Bad Request.
    - Test `GET /mails?sort=receiveTime; DROP TABLE mail` verifying sort parameter is ignored and request executes safely using static sort.
  - Error and security cases: unauthenticated access redirects to `/login` (HTTP 302), non-GET requests (e.g. POST) return HTTP 405 Method Not Allowed.
  - Verify all web slice tests pass via `./mvnw test -Dtest=MailWebControllerTest`.

## 4. UI Templates HTMX Pagination Controls

- [x] 4.1 Update `all-mail-list.html` to consume `mailPage.content`, display destination mailbox badges, render an accessible Bootstrap 5 HTMX pagination bar (First, Previous, page numbers with active highlight, Next, Last) targeting `#mail-list-container` with `hx-push-url="true"`, and display a user-friendly empty state indicator when `mailPage.totalElements == 0`.
- [x] 4.2 Update `mail-list.html` to consume `mailPage.content`, render matching Bootstrap 5 HTMX pagination controls targeting `#mail-list-container` with `hx-push-url="true"`, and display a user-friendly empty state indicator when `mailPage.totalElements == 0`.
- [x] 4.3 Add template rendering and web integration tests verifying the presence of HTMX pagination attributes (`hx-get`, `hx-target`), active/disabled page links at boundary conditions (first and last pages), and empty state text when zero emails exist, and verify via `./mvnw test -Dtest="MailWebController*,WebErrorHandlingTest"`.

## 5. End-to-End Verification & OpenSpec Validation

- [x] 5.1 Execute the entire test suite (`./mvnw clean test`) to verify zero regressions across unit, repository slice, web slice, and integration tests, explicitly verifying multi-tenant isolation, SQL injection prevention, and memory safety without `OutOfMemoryError`.
- [x] 5.2 Validate the OpenSpec change directory via `openspec validate paginate-and-optimize-mail-listing` and verify clean schema compliance.
