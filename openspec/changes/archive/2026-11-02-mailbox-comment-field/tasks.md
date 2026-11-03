# Tasks

## 1. Database Migration & Domain Entity Layer

- [x] 1.1 Create idempotent Flyway migration `V4__Add_Mailbox_Comment.sql` adding nullable `comment VARCHAR(255)` column using `IF NOT EXISTS` and verify it runs cleanly against the database schema
- [x] 1.2 Update `models/MBox.java` to add `comment` property with `@Size(max = 255)`, Javadoc, and `final` getters/setters normalizing blank strings to `null`
- [x] 1.3 Update `MailboxRepository.java` to expand search queries (`findByUsrAndSearch`, `findByUsrAndExpiredAndSearch`, `findExpiringByUsrAndSearch`, `findUnlimitedByUsrAndSearch`) with case-insensitive `b.comment` matching
- [x] 1.4 Add repository tests in `MailboxRepositoryTest.java` verifying migration execution, persistence of comments with special characters and German umlauts (`ä`, `ö`, `ü`, `ß`), case-insensitive substring search by comment, and SQL injection safety with malicious payloads (e.g. `' OR '1'='1`)

## 2. Web Console Modals, Dashboard & Inboxes

- [x] 2.1 Add localized comment keys and placeholders to `messages.properties` and `messages_de.properties`
- [x] 2.2 Update `modal-new.html` and `modal-edit.html` to provide optional comment text inputs with localized placeholders and length constraints
- [x] 2.3 Update `MailboxWebController.java` (`createMailbox`, `editMailboxModal`, `updateMailbox`) to bind and persist `comment` using `final` parameters
- [x] 2.4 Update `mailbox-table.html` to display the comment as a subtitle underneath the address link in the Address column
- [x] 2.5 Update `MailWebController.java` and `all-mail-list.html` to provide and render recipient mailbox comments alongside recipient badges in the `/mails` unified inbox
- [x] 2.6 Update `mails.html` to display the mailbox comment tag in the mailbox inbox header
- [x] 2.7 Add Web controller tests in `MailboxWebControllerTest.java` verifying creation with and without comment, editing/clearing comments, subtitle HTML rendering, umlaut handling, and search bar SQL injection payload safety

## 3. REST API & Client Library Compatibility

- [x] 3.1 Update `controllers/restapi/MailboxData.java` to add optional `public String comment;` field and Javadoc
- [x] 3.2 Update `MailboxApiController.java` to set and return `comment` on POST creation and PUT update operations using `final` parameters
- [x] 3.3 Update `xcmailr-client`'s `xcmailr/client/Mailbox.java` to add `public String comment;` with Javadoc
- [x] 3.4 Add REST API tests verifying mailbox creation and update with comment, UTF-8 umlaut preservation, SQL injection payload safety in comment values, and backward compatibility when `comment` is omitted from payloads

## 4. Full Verification & Regression Testing

- [x] 4.1 Run the full Maven test suite (`mvn clean test`) across `xcmailr-webapp` and `xcmailr-client` to verify all existing and new tests pass with zero regressions
