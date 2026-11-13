# Design

## Context
See [proposal.md](proposal.md) for motivation and problem background.

Currently, [`MailWebController.java`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/MailWebController.java) provides two listing endpoints:
1. `GET /mails`: Cross-mailbox unified inbox for the authenticated user.
2. `GET /mailboxes/{boxId}/mails`: Single mailbox inbox.

Both endpoints execute unpaginated entity queries (`mailRepository.findByMailboxInOrderByReceiveTimeDesc(mailboxIds)` and `findByMailboxOrderByReceiveTimeAsc(boxId)`). The entity [`Mail.java`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/java/models/Mail.java) contains an eager `@Lob byte[] message` column holding complete raw MIME payloads. When loading lists of emails, Hibernate loads every byte array into memory and deep-copies each array in `PrimitiveByteArrayJavaType` for dirty checking. Under load test volumes (e.g. thousands of emails generated under `admin@xcmailr.test`), this exhausts the JVM heap (`-Xmx512m`), causes H2 to close the database, and triggers HTTP 500 errors.

In contrast, [`MailboxWebController.java`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/MailboxWebController.java) and [`AdminWebController.java`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/AdminWebController.java) already use Spring Data `Pageable` and HTMX pagination controls.

## Goals / Non-Goals

**Goals:**
- **Push Pagination Down to Database**: Pass Spring Data `Pageable` (`page`, `size`, `sort`) to `MailRepository`, generating SQL `LIMIT` and `OFFSET` clauses so only the requested page of emails is read from disk.
- **Lightweight Projection**: Introduce an interface projection (`MailSummaryProjection`) that selects only `id`, `mailbox`, `sender`, `subject`, and `receiveTime`, completely omitting `@Lob byte[] message` from the SQL `SELECT` query and memory footprint for list views.
- **HTMX Pagination Controls**: Implement accessible pagination bars in `all-mail-list.html` and `mail-list.html` with page size options, page numbers, next/previous buttons, and total item indicators matching the UI patterns of `mailbox-list.html`.
- **Defensive Parameter Clamping**: Normalize inputs in `MailWebController` (`page >= 0`, `size` clamped to [1, 100], defaulting to 25) to protect against negative offsets or excessive page sizes.
- **Strict Multi-Tenant Isolation**: Ensure users can never view, inspect, or count emails belonging to other users.
- **Complete SQL Injection Immunity**: Ensure all input parameters are strongly typed, validated, and bound via JDBC `PreparedStatement` parameters, with hardcoded compile-time sorting to prevent dynamic SQL injection.
- **Strict Code Quality & Pyramid Testing**: Use `final` variables and parameters everywhere, provide comprehensive Javadoc and code comments, and implement unit, repository slice, web slice, and integration tests covering happy paths, error conditions, edge cases, multi-tenant isolation, and SQL injection prevention.

**Non-Goals:**
- Modifying the REST API (`/api/v1/mails`), which operates independently with its own filtering and JSON serialization.
- Modifying the single email view (`/mailboxes/{boxId}/mails/{mailId}`) or raw `.eml` download (`/raw`), which already fetch single records on demand and legitimately require the full `byte[] message` payload.
- Altering the database schema or Flyway migrations (entity structure and table columns remain unchanged).

## Decisions

### 1. Spring Data JPA Interface Projection over Class DTO
- **Choice**: Interface-based projection `MailSummaryProjection`.
- **Interface Definition**:
  ```java
  package com.xceptance.xcmailr.repositories.projections;

  /**
   * Lightweight projection for email list views, omitting the heavy raw message BLOB.
   */
  public interface MailSummaryProjection {
      Long getId();
      Long getMailbox();
      String getSender();
      String getSubject();
      long getReceiveTime();
  }
  ```
- **Rationale**: Spring Data JPA natively generates optimized SQL `SELECT` queries targeting only the getters declared in the projection interface. Hibernate completely bypasses the `@Lob byte[] message` column, meaning zero raw MIME bytes are read from disk or cloned into the JVM heap. Thymeleaf property access expressions (`${mail.subject}`, `${mail.sender}`, `${mail.receiveTime}`) continue to resolve seamlessly.
- **Alternative Considered**: JPQL constructor expressions (`SELECT new MailSummaryDto(...)`). While functional, class DTOs require explicit constructor maintenance and custom JPQL queries, whereas Spring Data interface projections work out of the box with query derivation and pagination.

### 2. Repository Method Signatures in `MailRepository`
- **Choice**: Add paginated summary query methods:
  ```java
  Page<MailSummaryProjection> findSummaryByMailboxIn(Collection<Long> mailboxIds, Pageable pageable);
  Page<MailSummaryProjection> findSummaryByMailbox(Long mailboxId, Pageable pageable);
  ```
- **Rationale**: Using explicit query derivation or `@Query` returning `Page<MailSummaryProjection>` provides built-in total count queries and pagination metadata (`totalPages`, `totalElements`, `number`, `size`).
- **Default Sorting**: Descending order by `receiveTime` (most recent emails first).

### 3. Controller Architecture & Parameter Normalization
- **Choice**:
  ```java
  @GetMapping("/mails")
  public String listAllMails(@RequestParam(value = "page", defaultValue = "0") final int page,
                             @RequestParam(value = "size", defaultValue = "25") final int size,
                             @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
                             final Principal principal,
                             final Model model)
  ```
- **Normalization**:
  ```java
  final int normalizedPage = Math.max(0, page);
  final int normalizedSize = Math.min(Math.max(1, size), 100);
  final Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "receiveTime"));
  ```
- **Zero-Mailbox Handling**: If the user owns no mailboxes (`mailboxIds.isEmpty()`), return `Page.empty(pageable)` immediately without querying the database.

### 4. UI Hypermedia & HTMX Pagination Fragment
- **Choice**:
  The pagination controls in `all-mail-list.html` and `mail-list.html` render a standard Bootstrap 5 pagination component:
  - First / Previous page links with disabled state on `page.first`.
  - Numbered page items with `active` highlight on `page.number`.
  - Next / Last page links with disabled state on `page.last`.
  - HTMX attributes: `hx-get="/mails?page=..."`, `hx-target="#mail-list-container"`, `hx-push-url="true"`.
  - When rendered in full page mode, the full page shell is returned; when `HX-Request: true` is present, only `all-mail-list :: mailList` or `mail-list :: mailList` is returned.

### 5. Multi-Tenant Data Isolation Enforcement
- **Unified Inbox (`/mails`)**:
  - The authenticated user is resolved from `userRepository.findByMailIgnoreCase(principal.getName())`.
  - The system queries only mailboxes owned by that specific user: `mailboxRepository.findByUsr(user)`.
  - The query passes solely those mailbox IDs: `mailRepository.findSummaryByMailboxIn(mailboxIds, pageable)`.
  - Under no circumstances does the query include mailbox IDs belonging to other users. If the user owns no mailboxes, `Page.empty(pageable)` is returned directly without issuing a database query.
- **Single Mailbox Inbox (`/mailboxes/{boxId}/mails`)**:
  - The mailbox ownership is validated using `getMailboxForUser(boxId, user)`:
    ```java
    final MBox mailbox = mailboxRepository.findById(mailboxId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mailbox not found"));
    if (mailbox.getUsr() == null || mailbox.getUsr().getId() != user.getId()) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to mailbox");
    }
    ```
  - If User A attempts to request `/mailboxes/{boxId}/mails` where `boxId` belongs to User B, an HTTP 403 Forbidden exception is immediately thrown. No email records, counts, or metadata are returned.

### 6. SQL Injection Immunity
- **Strongly-Typed Controller Parameters**:
  - `page` is `@RequestParam(value = "page", defaultValue = "0") final int page`.
  - `size` is `@RequestParam(value = "size", defaultValue = "25") final int size`.
  - `boxId` is `@PathVariable("boxId") final long boxId`.
  - Spring MVC's `DataBinder` parses these parameters strictly as primitive numeric types. Any attempt to supply SQL injection payloads (e.g. `page=1' OR '1'='1`, `boxId=1; DROP TABLE mail`) causes a `MethodArgumentTypeMismatchException` resulting in HTTP 400 Bad Request before any controller code or SQL queries can execute.
- **Compile-Time Static Sorting**:
  - SQL injection often targets dynamic `ORDER BY` clauses when frameworks expose sorting to user input (e.g. `Sort.by(request.getParameter("sort"))`).
  - In `MailWebController`, the sort property and direction are **statically hardcoded**:
    ```java
    Sort.by(Sort.Direction.DESC, "receiveTime")
    ```
  - The controller does not accept a `sort` parameter. Any user-supplied `sort` query parameter is completely ignored by Spring MVC, eliminating dynamic ORDER BY injection.
- **JPA Parameterized Queries (`PreparedStatement`)**:
  - All database queries executed by Spring Data JPA rely on JDBC `PreparedStatement` bind parameters (`?` or `:mailboxIds`). User or mailbox identifiers are passed as typed parameters, never concatenated into SQL or JPQL strings.

### 7. Testing Pyramid Strategy
```
               /\
              /  \      Integration Tests (WebErrorHandlingTest, ClientSmtpRoundtripIT)
             /----\
            /      \    Web Slice Tests (MailWebControllerTest via @WebMvcTest)
           /--------\
          /          \  Repository Slice Tests (MailRepositoryTest via @DataJpaTest)
         /------------\
        /              \ Unit Tests (Parameter validation, projection mapping)
       +----------------+
```
- **Unit Tests**:
  - Verification of pagination parameter clamping, edge case bounds (`page = -5`, `page = 999999`, `size = 0`, `size = 500`).
  - Empty mailbox ID list behavior.
- **Repository Slice Tests (`@DataJpaTest`)**:
  - Populate test emails with large simulated payloads.
  - Verify that `findSummaryByMailboxIn` and `findSummaryByMailbox` return paginated results with correct counts and sorting.
  - Verify that only requested page sizes (e.g. 5 out of 20 emails) are returned.
  - **Multi-Tenant Data Isolation Test**:
    - Seed User A with Mailbox 1 (emails M1, M2) and User B with Mailbox 2 (email M3).
    - Query `findSummaryByMailboxIn` with User A's mailbox IDs.
    - Assert that results contain ONLY M1 and M2, with total elements = 2, and zero occurrences of M3.
    - Query `findSummaryByMailbox` with Mailbox 1 ID and assert M3 is never returned.
  - **Parameterized Query Verification**:
    - Verify queries execute via parameterized binding without dynamic string concatenation.
- **Web Slice Tests (`@WebMvcTest(MailWebController.class)`)**:
  - Verify full page load `GET /mails` includes page model and pagination attributes.
  - Verify HTMX partial request `GET /mails` with `HX-Request: true` returns the partial fragment.
  - Verify pagination link URLs and parameter passing (`page=1&size=10`).
  - Verify edge cases: user with 0 mailboxes, mailbox with 0 mails, out-of-range page index.
  - **Multi-Tenant Isolation Tests**:
    - Authenticate as User A. Request `GET /mails`. Verify only User A's mailboxes are queried and only User A's emails appear in the model.
    - Authenticate as User A. Request `GET /mailboxes/{user2BoxId}/mails`. Verify response status is HTTP 403 Forbidden, and verify repository mail queries are never invoked.
  - **SQL Injection Prevention Tests**:
    - `GET /mails?page=1' OR '1'='1` -> verify HTTP 400 Bad Request.
    - `GET /mails?size=25; DROP TABLE mail;--` -> verify HTTP 400 Bad Request.
    - `GET /mailboxes/1' OR '1'='1/mails` -> verify HTTP 400 Bad Request.
    - `GET /mails?sort=receiveTime; DROP TABLE mail` -> verify sort parameter is ignored and request succeeds safely with status 200 using static sort.
  - Verify security cases: unauthenticated access redirects to `/login` (302).
- **Integration Tests**:
  - Verify end-to-end flow with real database context, ensuring no `OutOfMemoryError` occurs when querying accounts with large message volumes, and confirming tenant isolation holds through the entire filter and controller stack.

## Risks / Trade-offs

- **[Risk] Projection Compatibility with Existing Templates**: If templates access properties not declared in `MailSummaryProjection`, a runtime SpEL error could occur.
  - **Mitigation**: The templates only access `mail.id`, `mail.mailbox`, `mail.sender`, `mail.subject`, and `mail.receiveTime`. All are explicitly declared in `MailSummaryProjection`.
- **[Risk] Count Query Overhead for Large Tables**: Spring Data `Page<T>` executes a `SELECT COUNT(*)` query alongside the `SELECT ... LIMIT ...` query.
  - **Mitigation**: Mail count queries are scoped to the user's mailboxes (`WHERE mailbox IN (...)`). With indexes on `mailbox` and `receiveTime` in the H2 schema, count queries take single-digit milliseconds even with tens of thousands of rows.
- **[Risk] Out-of-Range Page Request**: A user navigating directly to `page=999` on an inbox with only 2 pages.
  - **Mitigation**: Spring Data returns an empty `Page` with `totalElements` and `totalPages` intact. The UI handles empty content gracefully without error.
