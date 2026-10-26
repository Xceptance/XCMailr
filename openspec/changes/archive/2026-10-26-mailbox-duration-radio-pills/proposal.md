# Proposal

## Why

During mailbox creation, the active duration control currently uses a native HTML `<select>` dropdown with a default pre-selection of "2 Hours". In modern desktop browsers (specifically macOS WebKit and Chromium), hovering or navigating over dropdown options without an explicit commit click leaves the select element uncommitted; clicking outside or directly clicking the submit button dismisses the OS dropdown menu without updating the value, causing the form to submit the default 2-hour duration instead of the intended duration (such as 30 days). Furthermore, if any inline validation error occurs during creation (such as duplicate address or invalid characters), the controller re-renders the creation modal without retaining the submitted `durationHours`, silently resetting the user's choice back to 2 hours.

Replacing the native dropdown with a responsive segmented radio button pill group ensures instant, single-click selection with unambiguous active visual state, eliminates dropdown dismiss quirks, and preserving the user's chosen duration across validation failures prevents accidental data loss.

## What Changes

- **Segmented Radio Button Group for Active Duration**: Replace `<select id="durationSelect" name="durationHours">` in `modal-new.html` with a Bootstrap 5 `btn-group` containing visually distinct radio button pills (`btn-check` with labels styled as segmented buttons):
  - 1 Hour (`1`)
  - 2 Hours (`2`, default checked)
  - 1 Day (`24`)
  - 1 Week (`168`)
  - 30 Days (`720`)
  - Unlimited (`0`)
- **Preserve Duration on Validation Re-renders**: Update `MailboxWebController.java` to retain `durationHours` in the Spring MVC model when validation errors occur, and update `modal-new.html` to dynamically check the corresponding radio pill based on the retained model attribute.
- **Testing Pyramid Quality Assurance**:
  - **Unit Tests**: Parameterized unit tests validating duration mapping, bounds, default fallbacks, and timestamp offset calculations.
  - **Integration / Web Tests**: Controller MockMvc tests covering happy paths (each preset duration, especially 30 days), validation error scenarios verifying duration retention, and edge cases (unlimited, negative/invalid duration inputs), with `final` modifiers across all test methods and assertions.
  - **Template Contract Tests**: Contract tests asserting the presence, accessibility attributes, and form parameter bindings of the radio button pill group.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Modifies `Requirement: Dynamic Mailbox Dashboard` to require segmented radio button pills for mailbox active duration selection and retention of duration selection on validation errors.

## Impact

- **UI Templates**: `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/modal-new.html`
- **Controllers**: `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/MailboxWebController.java`
- **Tests**:
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxWebControllerTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxValidationUnitTest.java`
  - New/updated template contract tests in `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/`
- **APIs**: No REST API changes; form submission parameter name `durationHours` remains identical and backward compatible.

