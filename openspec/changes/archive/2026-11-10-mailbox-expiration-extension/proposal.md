# Proposal

## Why

Mailbox expiration cannot be modified once a mailbox is created because the Edit Mailbox modal lacks expiration controls, and the dashboard table only offers a single `+1d` quick action button. Developers and QA engineers testing long-running workflows are forced to re-extend validity every single day; providing direct extension presets (`+1d`, `+7d`, `+30d`, `Unlimited`) in both the edit modal and the table quick actions eliminates this repetitive chore and prevents test disruption.

## What Changes

- **Edit Mailbox Modal Extension Selector**: Add an "Extend Validity By" segmented radio button pill group to `modal-edit.html` with presets for `Keep current` (0h, default), `+1d` (24h), `+7d` (168h), `+30d` (720h), and `Unlimited` (-1h / permanent), utilizing existing localization key `mailbox.modal.edit.extendBy`.
- **Table Row Quick Action Extension Toolbar**: Expand the desktop row action group in `mailbox-table.html` to provide direct 1-click buttons for all validity extension presets: `+1d`, `+7d`, `+30d`, and `∞` (unlimited, using Bootstrap icon `bi-infinity`), while consolidating all options in the row's kebab menu (`⋮`) for mobile viewports.
- **Controller Extension Handling**:
  - Update `updateMailbox` (`POST /mailboxes/{id}/edit`) in `MailboxWebController.java` to accept `extendHours` (defaulting to 0) and adjust `ts_Active` accordingly (or set to 0 for unlimited, clearing any expired state).
  - Update `extendMailbox` (`POST /mailboxes/{id}/extend`) in `MailboxWebController.java` to support setting unlimited duration when `hours = -1`.
- **Localization**: Ensure English (`messages.properties`) and German (`messages_de.properties`) bundles have all required labels, button tooltips, and accessibility ARIA descriptions for new table actions and modal controls.
- **Comprehensive Testing Pyramid**: Add Layer 1 unit tests in `MailboxWebControllerTest.java` and Layer 2 web slice integration tests in `MailboxWebTest.java` covering happy paths, error cases (non-existent mailbox, cross-user unauthorized access, invalid hours), and special edge cases (extending already expired mailboxes, extending active mailboxes, converting unlimited mailboxes to timed, and converting timed mailboxes to unlimited).

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `web-console-ui`: Update the "Dynamic Mailbox Dashboard" requirement to specify mailbox expiration extension capabilities both in the Edit Mailbox modal dialog and as zero-click quick action buttons on the mailbox dashboard table rows.

## Impact

- **Affected Code**:
  - `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/modal-edit.html`
  - `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/MailboxWebController.java`
  - `xcmailr-webapp/src/main/resources/conf/messages.properties` and `messages_de.properties`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxWebControllerTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxWebTest.java`
- **APIs**:
  - `POST /mailboxes/{id}/edit` accepts new optional `extendHours` parameter (default 0).
  - `POST /mailboxes/{id}/extend` supports `hours = -1` for setting unlimited validity.
- **Dependencies**: None. Uses existing Bootstrap 5 and Bootstrap Icons 1.11.3 (`bi-infinity`).
