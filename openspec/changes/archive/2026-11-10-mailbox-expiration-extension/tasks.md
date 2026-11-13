# Tasks

## 1. Controller & Backend Extension Logic (Layer 1 Unit Testing)

- [x] 1.1 Update `MailboxWebController.java` `updateMailbox` (`POST /mailboxes/{id}/edit`) to accept `@RequestParam(value = "extendHours", defaultValue = "0") final int extendHours`, implement timestamp calculation for keep current (0), extension (>0), and unlimited (-1), enforce `final` on all parameters and local variables, and add comprehensive Javadoc and inline comments. Verify by compiling with `mvn test-compile -pl xcmailr-webapp`.
- [x] 1.2 Update `MailboxWebController.java` `extendMailbox` (`POST /mailboxes/{id}/extend`) to support setting unlimited duration when `hours = -1`, enforce `final` on all parameters and local variables, and add comprehensive Javadoc and inline comments. Verify by compiling with `mvn test-compile -pl xcmailr-webapp`.
- [x] 1.3 Add Layer 1 unit tests in `MailboxWebControllerTest.java` covering happy path tests (`updateMailbox_extendKeepCurrent`, `updateMailbox_extend24h`, `updateMailbox_extend168h`, `updateMailbox_extend720h`, `updateMailbox_extendUnlimited`, `extendMailbox_hours24`, `extendMailbox_hours168`, `extendMailbox_hours720`, `extendMailbox_unlimited`), error cases (non-existent mailbox ID, cross-tenant unauthorized access), and special edge cases (extending expired mailbox relative to `now`, extending active mailbox preserving remaining future duration, converting unlimited mailbox to timed). Verify all tests pass with `mvn test -pl xcmailr-webapp -Dtest=MailboxWebControllerTest`.

## 2. Web UI Templates & Localization

- [x] 2.1 Update `modal-edit.html` with the segmented radio button pill group for "Extend Validity By" (`extendHours = 0`, `24`, `168`, `720`, `-1`) matching `modal-new.html` styling and verify localized labels and default checked state.
- [x] 2.2 Update `mailbox-table.html` with direct 1-click extension buttons (`+1d`, `+7d`, `+30d`, `∞` using `bi-infinity`) for desktop viewports (`d-none d-md-inline-flex`) and add all extension options to the mobile kebab dropdown menu (`d-md-none`).
- [x] 2.3 Verify and update localization bundles (`messages.properties` and `messages_de.properties`) with all required button labels, tooltips (`mailbox.action.extend7dTooltip`, `mailbox.action.extend30dTooltip`, `mailbox.action.extendUnlimitedTooltip`, `mailbox.modal.edit.extendKeep`), and accessibility ARIA descriptions in English and German.

## 3. Web Slice Integration Testing (Layer 2) & Verification

- [x] 3.1 Add Layer 2 web slice integration tests in `MailboxWebTest.java` covering HTMX requests to `POST /mailboxes/{id}/edit` with `extendHours` and `POST /mailboxes/{id}/extend` with various durations (`24`, `168`, `720`, `-1`), verifying HTTP 200, `HX-Trigger: mailboxChanged` header, tenant isolation (cross-tenant access rejected), and CSRF token protection. Verify with `mvn test -pl xcmailr-webapp -Dtest=MailboxWebTest`.
- [x] 3.2 Add web slice test cases in `MailboxWebTest.java` verifying that the Edit Mailbox modal fragment (`GET /mailboxes/{id}/edit`) renders all duration radio buttons with correct default checked state and localized labels in both English and German. Verify with `mvn test -pl xcmailr-webapp -Dtest=MailboxWebTest`.
- [x] 3.3 Add web slice test cases in `MailboxWebTest.java` verifying that the mailbox table fragment (`GET /mailboxes`) renders the direct 1-click extension buttons with correct HTMX attributes and localized tooltips. Verify with `mvn test -pl xcmailr-webapp -Dtest=MailboxWebTest`.
- [x] 3.4 Execute full test suite (`mvn clean test`) across all reactor modules and verify all tests pass without errors.
