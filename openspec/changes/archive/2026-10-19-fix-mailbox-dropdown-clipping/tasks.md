# Tasks

## 1. Template Enhancements

- [x] 1.1 Update mailbox actions dropdown toggle in `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html` to add `data-bs-boundary="viewport"` and `data-bs-popper-config='{"strategy":"fixed"}'`, verifying the attributes are present in the template source.

## 2. Controller & Template Tests

- [x] 2.1 Add MockMvc assertions in `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxWebControllerTest.java` confirming that rendered mailbox rows output `data-bs-boundary="viewport"` and `data-bs-popper-config='{"strategy":"fixed"}'` on the dropdown toggle button, and verify by running `./mvnw test -Dtest=MailboxWebControllerTest`.

## 3. Verification

- [x] 3.1 Run the full webapp test suite via `./mvnw test -pl xcmailr-webapp` to verify that all web console tests pass without regressions.

