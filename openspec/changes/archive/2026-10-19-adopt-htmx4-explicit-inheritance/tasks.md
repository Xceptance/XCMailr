# Tasks: Adopt Standard HTMX 4 Explicit Inheritance

## 1. HTMX 4 Configuration & Template Annotations

- [x] 1.1 Remove `implicitInheritance: true` from `<meta name="htmx-config">` and inline `<script>` in `xcmailr-webapp/src/main/resources/templates/layout/base.html`, retaining `noSwap: [204, 304, '4xx', '5xx']`, and verify HTML syntax is valid
- [x] 1.2 Add explicit `hx-trigger="click"` attribute to `<tr class="mail-item-row">` in `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mail-list.html` and `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/all-mail-list.html`, and verify template formatting

## 2. Controller Slice Testing

- [x] 2.1 Add controller slice test in `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailWebControllerTest.java` verifying that `GET /mailboxes/{id}/mails/{mailId}` with `HX-Request: true` returns the email detail preview fragment (`mailboxes/fragments/mail-detail :: detail`) with status 200 OK and populated model, adhering to strict `final` usage, Javadoc, and `// GIVEN/WHEN/THEN` structure, and verify with `mvn test -Dtest=MailWebControllerTest`
- [x] 2.2 Add edge/error slice tests in `MailWebControllerTest.java` verifying 404 Not Found for non-existent mailId and access restriction for another user's mailbox with `HX-Request: true`, and verify with `mvn test -Dtest=MailWebControllerTest`

## 3. Client Contract & Interactive Browser Testing

- [x] 3.1 Update static contract test in `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/HtmxCsrfClientContractTest.java` to assert `<meta name="htmx-config">` contains `noSwap` and does not contain `implicitInheritance`, and verify with `mvn test -Dtest=HtmxCsrfClientContractTest`
- [x] 3.2 Add browser-level HtmlUnit test in `HtmxCsrfClientContractTest.java` for happy-path email selection: load `/mailboxes/{id}/mails`, click `#mail-row-{mailId}`, execute HTMX event loop, and assert `#mail-viewer-pane` loads email detail preview content, using `final`, Javadoc, and `// GIVEN/WHEN/THEN` structure, and verify with `mvn test -Dtest=HtmxCsrfClientContractTest`
- [x] 3.3 Add special-case / error-handling HtmlUnit tests in `HtmxCsrfClientContractTest.java` verifying error swap suppression (`noSwap`) preserves existing `#mail-viewer-pane` DOM structure during failed or disallowed requests, and verify with `mvn test -Dtest=HtmxCsrfClientContractTest`

## 4. Full Verification & Regression Safety

- [x] 4.1 Run the full test suite (`mvn clean test-compile test`) across all modules to verify zero regressions

