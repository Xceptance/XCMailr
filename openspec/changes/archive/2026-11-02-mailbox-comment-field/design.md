# Design

## Context

See `proposal.md` for background and user motivation.

XCMailr manages relational schema updates via Flyway with Hibernate `ddl-auto: validate` enabled. Virtual test email addresses are represented by the `MBox` JPA entity (`mailboxes` table). The web console renders views via Thymeleaf and HTMX with localized message property bundles (`messages.properties`, `messages_de.properties`). Programmatic integrations use Spring MVC REST controllers (`MailboxApiController`) and the Java client library (`xcmailr-client`).

## Goals / Non-Goals

**Goals:**
- Provide a robust, idempotent database migration (`V4__Add_Mailbox_Comment.sql`) adding a nullable `comment VARCHAR(255)` column that safely executes across fresh and existing environments.
- Support full UTF-8 character encoding, guaranteeing seamless storage, retrieval, display, and search for special characters and German umlauts (e.g. `ä`, `ö`, `ü`, `ß`).
- Render non-blank comments as a clean subtitle under the address in the mailbox dashboard table (`mailbox-table.html`), preserving the 7-column responsive layout without visual clutter.
- Allow entering, updating, and clearing comments in the mailbox creation (`modal-new.html`) and edit (`modal-edit.html`) modal dialogs.
- Enable case-insensitive search by comment substring across all mailbox repository search queries (`findByUsrAndSearch`, `findByUsrAndExpiredAndSearch`, `findExpiringByUsrAndSearch`, `findUnlimitedByUsrAndSearch`).
- Display the mailbox comment alongside recipient badges in the unified cross-mailbox inbox view (`all-mail-list.html`) and in the single mailbox inbox header (`mails.html`).
- Ensure complete backward compatibility for the REST API (`MailboxData`) and Java client library (`Mailbox`), allowing callers omitting `comment` to proceed without error.
- Enforce strict engineering standards: explicit `final` modifiers on method parameters and local variables, thorough Javadoc and inline comments, and comprehensive test pyramid coverage (unit, repository, integration, and controller tests) without overengineering.

**Non-Goals:**
- Rich text or Markdown formatting in comments: comments are stored as plain text and strictly HTML-escaped by Thymeleaf to prevent Cross-Site Scripting (XSS).
- Unlimited length text notes: comments are capped at 255 characters to ensure fast indexing, predictable UI layout, and efficient memory usage.
- Modifying plain text address export endpoints (`/mailboxes/export/*`): export files remain one plain email address per line for script compatibility.

## Decisions

### 1. Database DDL Idempotency (`V4__Add_Mailbox_Comment.sql`)
- **Decision:** Use `ALTER TABLE mailboxes ADD COLUMN IF NOT EXISTS comment VARCHAR(255);`.
- **Rationale:** Aligns with existing Flyway migration patterns in `V1` and `V3` (which use `IF NOT EXISTS` and `IF EXISTS`). In environments or CI containers where schemas may have been pre-initialized or partially upgraded, idempotent DDL prevents startup migration failure while allowing Hibernate `validate` mode to succeed cleanly.
- **Alternatives Considered:** Standard `ALTER TABLE ... ADD COLUMN comment` without `IF NOT EXISTS` fails if the column was previously applied.

### 2. UI Layout in Mailbox Dashboard Table (`mailbox-table.html`)
- **Decision:** Place the comment as a subtitle underneath the address link in the Address column.
- **Rationale:** The dashboard table has a carefully balanced 7-column responsive structure (Checkbox, Address, Status, Expires, Forwards, Suppressed, Actions). Adding an 8th column causes horizontal scrolling and forces aggressive column suppression on tablets and mobile screens. Rendering the comment as a subtitle directly pairs the project context with the address while keeping the layout compact.
- **Alternatives Considered:**
  - Dedicated column: Adds unnecessary horizontal sprawl.
  - Inline badge next to address: Causes wrapping issues for long email addresses on narrower screens.

### 3. Search Query Extension in `MailboxRepository`
- **Decision:** Augment all user search JPQL queries with:
  ```sql
  OR (b.comment IS NOT NULL AND LOWER(b.comment) LIKE LOWER(CONCAT('%', :search, '%')))
  ```
- **Rationale:** Fulfills the core user requirement: when users remember a project name or test scenario (e.g. `Projekt Überweisung` or `Checkout`), typing it into the search box instantly filters the mailbox table.
- **Alternatives Considered:** Separate search input for comments only. Rejected because a single unified search field provides superior user experience without complicating the toolbar.

### 4. Recipient Comment Display in Unified Inbox (`MailWebController` & `all-mail-list.html`)
- **Decision:** Provide a `mailboxCommentMap` (`Map<Long, String>`) from `MailWebController` to `all-mail-list.html` mapping mailbox IDs to non-blank comments.
- **Rationale:** The `/mails` unified inbox query utilizes `MailSummaryProjection` to deliberately avoid loading full message BLOBs into memory. Passing a lightweight map of user mailbox comments preserves this high-performance memory projection while giving the template immediate access to recipient comments for badge rendering.
- **Alternatives Considered:** Eagerly joining `MBox` inside the mail query. Rejected to prevent N+1 query overhead and memory bloating.

### 5. REST API DTO and Client Backward Compatibility
- **Decision:** Add `public String comment;` as an optional field in `MailboxData` and `xcmailr-client`'s `Mailbox`.
- **Rationale:** Jackson automatically handles missing JSON fields on deserialization. Existing clients submitting `{ "address": "...", "deactivationTime": 0, "forwardEnabled": true }` continue to parse and deserialize without error. New clients can provide and inspect `"comment"`.
- **Alternatives Considered:** Creating a separate `/api/v2` endpoint. Unnecessary churn for a non-breaking additive field.

### 6. SQL Injection Prevention & Parameterized Query Architecture
- **Decision:** Bind all search inputs, path variables, form parameters, and REST payloads strictly via named JPA query parameters (`@Param("search")`) and JDBC `PreparedStatement` parameters. Never use dynamic string concatenation or manual query interpolation.
- **Rationale:** Spring Data JPA compiles parameterized JPQL queries into parameterized JDBC prepared statements with `?` placeholders. The underlying database engine and JDBC driver treat parameter values strictly as string literal data, completely neutralizing SQL meta-characters, quote escapes, and injection attacks (such as `' OR '1'='1`, `'; DROP TABLE mailboxes; --`, or `UNION SELECT`).
- **Alternatives Considered:** Building dynamic CriteriaBuilder queries with string concatenation. Rejected as unnecessarily complex and prone to injection flaws.

## Risks / Trade-offs

- **[Risk: SQL injection via search bar, web forms, or REST payloads]** → **Mitigation:** Complete reliance on parameterized JPQL queries with named parameters (`:search`) and Hibernate/JDBC `PreparedStatement` bindings; zero dynamic SQL concatenation; verified by automated security test cases in repository and controller suites submitting classic injection payloads.
- **[Risk: Malicious content or script injection via comment]** → **Mitigation:** Thymeleaf's `th:text` automatically XML/HTML-escapes all dynamic content. Server-side `@Size(max = 255)` prevents unbounded payload attacks.
- **[Risk: Umlaut / special character collation differences in search]** → **Mitigation:** Explicit `LOWER(...)` normalization in JPQL queries with UTF-8 database encoding; verified by integration tests specifically querying with mixed-case umlauts (`ä`, `ö`, `ü`, `ß`).
- **[Risk: Whitespace-only comments creating empty visual subtitle lines]** → **Mitigation:** Entity setter normalizes empty or whitespace-only strings to `null`, and Thymeleaf templates check `${box.comment != null and !box.comment.isBlank()}` before rendering.

