# Design: Responsive Unified All Mails View

## Context

The backend implementation for the unified inbox view in [`MailWebController.java`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/MailWebController.java) is already complete:
- `@GetMapping("/mails")` queries all emails across all mailboxes owned by the authenticated user in descending chronological order.
- It provides a `mailboxAddressMap` (`Map<Long, String>`) linking mailbox IDs to their full email addresses.
- It routes full page requests to `mailboxes/all-mails` and HTMX partial requests to `mailboxes/fragments/all-mail-list :: mailList`.

However, the template files `mailboxes/all-mails.html` and `mailboxes/fragments/all-mail-list.html` were empty 0-byte files, resulting in an empty HTTP 200 response (a blank white screen) in user browsers.

## Test Gap Root Cause Analysis

### Why Was This Not Caught by Automated Tests?

When inspecting the existing test in [`MailWebControllerTest.java`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailWebControllerTest.java):
```java
@Test
@DisplayName("GET /mails should list all emails across mailboxes for authenticated user")
public void testListAllMailsFullPage() throws Exception
{
    mockMvc.perform(get("/mails")
           .with(user("alice@xcmailr.test").roles("USER")))
           .andExpect(status().isOk())
           .andExpect(view().name("mailboxes/all-mails"))
           .andExpect(model().attributeExists("mails"))
           .andExpect(model().attributeExists("mailboxAddressMap"));
}
```

This test passed completely with zero failures for three structural reasons:
1. **View Name Verification Only**: MockMvc only checked that the controller returned the string `"mailboxes/all-mails"`. It did not check what was rendered.
2. **0-Byte Template Permissiveness in Thymeleaf**: Thymeleaf located the file `templates/mailboxes/all-mails.html` on the classpath. Because the file was completely empty (0 bytes), the template engine successfully parsed 0 characters, generated a response body of length 0 (`""`), and returned HTTP status 200 OK.
3. **Absence of Content & DOM Assertions**: In contrast to [`WebNavigationSecurityTest.java`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/test/java/com/xceptance/xcmailr/security/WebNavigationSecurityTest.java) which validates actual rendered strings via `content().string(containsString(...))`, `MailWebControllerTest` did not assert that the response body was non-empty or that key HTML elements (e.g., table tags, subject text, recipient badges, preview pane) were present.
4. **Missing Test Pyramid Layers**: The test suite lacked edge case tests (empty inboxes, multi-mailbox badge resolution, missing metadata fallbacks) and security/error tests (unauthenticated redirection, cross-user privacy isolation, method restrictions).

---

## Test Pyramid Strategy

To prevent similar regressions and establish rigorous quality gates, testing for this capability is structured across the test pyramid:

```
                  / \
                 /   \
                / E2E \       <-- SMTP delivery + Web inbox roundtrip
               /-------\
              / Slice & \     <-- Template syntax & fragment integration
             / Templates \
            /-------------\
           /   MockMvc     \  <-- Happy Path, Edge Cases, Security & Errors
          /   Controller    \
         /-------------------\
```

### Layer 1: Controller Slice Tests (`MailWebControllerTest.java`)

All controller tests will perform explicit assertions on the rendered response body using Hamcrest matchers (`content().string(...)`).

#### 1. Happy Path Cases
- **`testListAllMailsFullPage_RendersCompleteHtmlAndContent`**:
  - Validates HTTP 200, view name `"mailboxes/all-mails"`, and model attributes.
  - Asserts non-empty response body (`not(emptyOrNullString())`).
  - Asserts inclusion of page title/breadcrumb ("All Mails"), recipient mailbox badge (`inbox@xcmailr.test`), sender (`newsletter@sender.com`), subject (`Weekly Digest`), and preview pane container (`#mail-viewer-pane`).
- **`testListAllMailsFragment_RendersFragmentOnly`**:
  - Sends `HX-Request: true`.
  - Asserts view name `"mailboxes/fragments/all-mail-list :: mailList"`.
  - Asserts rendered rows contain email data and HTMX attributes (`hx-get`, `hx-target="#mail-viewer-pane"`).
  - Asserts that full page wrapper tags (`<html>`, `<head>`, `navbar`) are strictly absent.
- **`testListAllMails_MultipleMailboxes_RendersDistinctRecipientBadges`**:
  - Seeds two mailboxes for Alice (`alice-work@xcmailr.test` and `alice-temp@xcmailr.test`) with respective emails.
  - Asserts that both distinct recipient badges appear in the rendered list, matching each message accurately.

#### 2. Edge & Special Cases
- **`testListAllMails_EmptyInbox_RendersInformativeNotice`**:
  - Seeds a user with zero received emails.
  - Asserts that the response does not render broken table headers or blank cards, but instead displays a friendly empty-state notice (e.g. "No messages received yet").
- **`testListAllMails_MissingMailboxInMap_GracefulFallback`**:
  - Simulates an email referencing an unrecognized mailbox ID (e.g. deleted mailbox).
  - Asserts that the template handles null safely via `${mailboxAddressMap[m.mailbox.id] ?: 'Unknown Mailbox'}` without throwing a SpEL evaluation exception.
- **`testListAllMails_ResponsiveMarkupStructure`**:
  - Verifies presence of Bootstrap responsive grid classes (`col-12`, `col-lg-5`, `col-lg-7`), touch-friendly padding, and metadata sublines.

#### 3. Error & Security Cases
- **`testListAllMails_Unauthenticated_RedirectsToLogin`**:
  - Requests `/mails` without authentication.
  - Asserts HTTP 302 Redirection to `/**/login`.
- **`testListAllMails_UserIsolation_DoesNotExposeOtherUserEmails`**:
  - Seeds private emails and mailboxes for User Bob (`bob@xcmailr.test`).
  - Alice requests `/mails`.
  - Asserts that Bob's email subject, sender, and mailbox address are completely absent from both the HTML content and model attributes.
- **`testListAllMails_DisallowedHttpMethods_Rejected`**:
  - Submits POST, PUT, DELETE requests to `/mails`.
  - Asserts HTTP 405 Method Not Allowed.

### Layer 2: Template Rendering & Syntax Integrity

- Thymeleaf template compilation and rendering is validated as part of Spring Boot test execution.
- Any syntax errors, malformed tags, unclosed attributes, or SpEL expression failures will fail the build immediately.

### Layer 3: End-to-End System Roundtrip

- The local test procedure (sending SMTP via `curl` to port 25000 and observing the email in `/mails`) represents the end-to-end delivery path.
- Verifies integration between SubEthaSMTP inbound receiver, `MailDeliveryService`, `MailRepository`, and web presentation.

---

## UI & Responsive Design Decisions

### 1. Split-Pane Layout Parity with Single Mailbox Inbox

Structure `all-mails.html` to mirror [`mailboxes/mails.html`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/resources/templates/mailboxes/mails.html):
- Left pane (`col-lg-5`): Message list card with inbox count, refresh trigger, and message rows.
- Right pane (`col-lg-7`): `#mail-viewer-pane` housing the preview card, defaulting to an empty selection prompt and dynamically populating via HTMX when an email row is clicked.

### 2. Mobile Responsive Layout Hierarchy (< 768px)

```
Desktop (>= 992px):
+---------------------------------------------------------------------------------+
| Header: All Mails (5 messages across 2 mailboxes)           [ ↻ Refresh ]       |
+----------------------------------------+----------------------------------------+
| Message List (col-lg-5)                | Preview Pane (col-lg-7)                |
| - 09:12 sender@a.com -> box1@...       | [ Sanitized Email HTML Content ]       |
| - 08:30 sender@b.com -> box2@...       | [ Attachments & Headers ]              |
+----------------------------------------+----------------------------------------+

Mobile (< 992px):
+---------------------------------------------------------------------------------+
| Header: All Mails                                           [ ↻ Refresh ]       |
+---------------------------------------------------------------------------------+
| Message List (col-12)                                                           |
| +-----------------------------------------------------------------------------+ |
| | sender@a.com                                                                | |
| | Subject: Welcome to the service                                             | |
| | [To: box1@...] • 09:12                                          [Open] [Del]| |
| +-----------------------------------------------------------------------------+ |
| Preview Pane (col-12, stacked underneath)                                       |
| [ Select an email to view / rendered preview ]                                 |
+---------------------------------------------------------------------------------+
```

- **Stacking**: Below 992px, layout switches from split-pane to single vertical column (`col-12`).
- **Subline Metadata**: Rather than wide horizontal table columns, email entries use vertical flex sublines (`To: <badge>`, timestamp, actions) to prevent horizontal scrolling on screens as narrow as 320px.
- **Touch Targets**: All clickable action buttons and email item triggers have a minimum touch target size of 44x44px.

### 3. Recipient Mailbox Indication & Null Safety

- For each message, render `${mailboxAddressMap[m.mailbox.id] ?: 'Unknown Mailbox'}` with badge styling (`badge bg-light text-dark border`).
- Clicking an email row triggers `hx-get="@{'/mailboxes/' + ${m.mailbox.id} + '/mails/' + ${m.id}}"` targeting `#mail-viewer-pane`.

---

## Risks / Trade-offs

- **[Risk] High email count rendering time**: If a user has hundreds of emails, rendering all rows in one HTML document could increase payload size.
  - *Mitigation*: The current architecture is scoped to personal temporary mailboxes. Standard Thymeleaf rendering handles hundreds of rows in under 20ms. Pagination can be introduced as a separate enhancement if users exceed normal testing thresholds.
- **[Risk] Missing destination mailbox in map**: If a mailbox was hard-deleted but orphaned emails remain.
  - *Mitigation*: In the template, use null-safe expression `${mailboxAddressMap[m.mailbox.id] ?: 'Unknown Mailbox'}` to prevent template evaluation exceptions.
