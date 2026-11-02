# Proposal

## Why

Users create virtual test email addresses for diverse projects, automated test suites, or external services. Without a way to document purpose or context directly on the mailbox, users frequently lose track of which project or testing scenario relies on a given test address. Adding an optional comment/note field to mailboxes resolves this issue across the web console, search queries, unified inbox views, and REST API.

## What Changes

- Add an optional `comment` field (up to 255 characters, full UTF-8 support for special characters and umlauts like `ä`, `ö`, `ü`, `ß`) to virtual test email addresses (`MBox`).
- Introduce an idempotent database schema migration (`V4__Add_Mailbox_Comment.sql`) using `ALTER TABLE mailboxes ADD COLUMN IF NOT EXISTS comment VARCHAR(255);`.
- Update mailbox creation and edit dialogs (`modal-new.html`, `modal-edit.html`) to support entering, updating, and clearing comments with localized labels and placeholders.
- Display the mailbox comment as a clean subtitle underneath the address in the mailbox dashboard table (`mailbox-table.html`), preserving the responsive 7-column layout.
- Extend dashboard search queries in `MailboxRepository` to perform case-insensitive substring search over mailbox comments in addition to address and domain.
- Display the mailbox comment alongside recipient badges in the unified cross-mailbox inbox view (`all-mail-list.html` at `/mails`) and in the single mailbox inbox header (`mails.html`).
- Update the REST API DTO (`MailboxData`) and controller (`MailboxApiController`) to accept and return the optional `comment` field, preserving full backward compatibility for existing callers.
- Update `xcmailr-client` (`Mailbox.java`) to expose the `comment` field.
- Implement comprehensive unit, repository, integration, and controller tests following the testing pyramid covering happy paths, edge cases, special characters, and backward compatibility.

## Capabilities

### Modified Capabilities
- `web-console-ui`: Enhance mailbox dashboard, modals, search, and inbox views to support creating, editing, displaying, and filtering mailboxes by optional comments.
- `mailbox-management-api`: Enhance REST mailbox lifecycle endpoints to accept, store, and return optional comments while preserving backward compatibility.

## Impact

- Database schema: `mailboxes` table gets a new nullable `comment VARCHAR(255)` column via Flyway `V4`.
- Backend domain model: `models.MBox` entity gains `comment` property and validation.
- Spring Data JPA: `MailboxRepository` search queries are updated.
- Web controllers & views: `MailboxWebController`, `MailWebController`, Thymeleaf templates, and message property bundles.
- REST API & Client: `controllers.restapi.MailboxData`, `MailboxApiController`, and `xcmailr-client`'s `Mailbox.java`.
- No breaking changes; existing REST API clients and web workflows continue to function without modification.

