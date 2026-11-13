# Design

## Context

See `proposal.md` for background and user problem statement. Currently, `MBox` stores expiration in `ts_Active` as milliseconds since epoch, where `0L` signifies an unlimited (permanent) mailbox, and a positive `long` represents an expiration timestamp. The `expired` boolean flag tracks explicit deactivation or scheduled expiration.

In the web layer:
- `modal-edit.html` displays expiration as read-only text without input controls.
- `POST /mailboxes/{id}/edit` accepts only `comment`, `forwardEmails`, and `active`.
- `POST /mailboxes/{id}/extend` accepts an `hours` parameter (defaulting to 24), calculating `base = Math.max(now, mailbox.getTs_Active())` and adding `hours`.
- `mailbox-table.html` provides a desktop-only compact `+1d` button and a mobile kebab item for `+1d`.

## Goals / Non-Goals

**Goals:**
- Provide clear validity extension controls in `modal-edit.html` using segmented radio buttons (`Keep current`, `+1d`, `+7d`, `+30d`, `Unlimited`).
- Provide direct 1-click quick action extension buttons (`+1d`, `+7d`, `+30d`, `∞`) on each table row in `mailbox-table.html` for desktop/tablet viewports, with consolidated options in the kebab menu for mobile.
- Enhance `MailboxWebController.java` to handle extension parameters on both `edit` and `extend` endpoints, including converting between timed and unlimited states.
- Follow strict code hygiene: `final` on all declarations, parameters, local variables, and loop variables, accompanied by comprehensive Javadoc and inline comments.
- Follow the testing pyramid: Layer 1 unit tests (`MailboxWebControllerTest.java`) and Layer 2 web slice integration tests (`MailboxWebTest.java`), covering happy paths, error cases, and special edge cases.

**Non-Goals:**
- Free-form HTML5 datetime-local pickers: Presets prevent browser-to-server timezone discrepancies and match the established visual pattern of `modal-new.html`.
- Database schema changes: The existing `MBox.ts_Active` (`long`) and `expired` (`boolean`) fields already support finite timestamps and permanent zero values.
- Bulk validity extensions modification: Bulk actions continue to support the existing +24h batch workflow.

## Decisions

### 1. Segmented Radio Button Controls in Edit Modal
- In `modal-edit.html`, beneath `Current Expiration`, add a segmented button group (`btn-group w-100 flex-wrap`) under label key `mailbox.modal.edit.extendBy`:
  - `Keep current` (`extendHours = 0`, checked by default)
  - `+1d` (`extendHours = 24`)
  - `+7d` (`extendHours = 168`)
  - `+30d` (`extendHours = 720`)
  - `Unlimited` (`extendHours = -1`)
- *Rationale*: Reuses existing message bundle key `mailbox.modal.edit.extendBy`. Avoids accidental expiration changes when editing other fields (since `0` is default). Matches the visual language of `modal-new.html`.

### 2. Direct 1-Click Extension Buttons in Table Row Quick Actions
- In `mailbox-table.html`, expand the desktop action group (`btn-group btn-group-sm`) to include:
  - Toggle Active (`bi-power` / `bi-play-fill`)
  - `+1d` (`hx-post="/mailboxes/{id}/extend?hours=24"`)
  - `+7d` (`hx-post="/mailboxes/{id}/extend?hours=168"`)
  - `+30d` (`hx-post="/mailboxes/{id}/extend?hours=720"`)
  - `∞` (`hx-post="/mailboxes/{id}/extend?hours=-1"`, using `<i class="bi bi-infinity"></i>`)
  - Kebab menu (`⋮`)
- For mobile viewports (`d-md-none`), include all extension options (`+1d`, `+7d`, `+30d`, `Set Unlimited`) inside the kebab dropdown.
- *Rationale*: Solves the developer's everyday friction. Testing teams can extend mailboxes by 1 week, 1 month, or permanently with zero intermediate clicks or modal navigation.

### 3. Timestamp Calculation Logic in `MailboxWebController`
- In `updateMailbox` (`POST /mailboxes/{id}/edit`):
  - If `extendHours == 0`: keep existing `ts_Active`.
  - If `extendHours == -1`: set `ts_Active = 0L`, `expired = false`.
  - If `extendHours > 0`:
    - If `mailbox.getTs_Active() == 0L` (was unlimited) or `mailbox.isExpired()` or `mailbox.getTs_Active() < now`: `base = now`.
    - Else: `base = Math.max(now, mailbox.getTs_Active())`.
    - `mailbox.setTs_Active(base + TimeUnit.HOURS.toMillis(extendHours))`
    - `mailbox.setExpired(false)`
- In `extendMailbox` (`POST /mailboxes/{id}/extend`):
  - Support `hours == -1`: set `ts_Active = 0L`, `expired = false`.
  - Support `hours > 0`: same base calculation as above.
- *Rationale*: Guarantees that extending an already expired mailbox extends from the current wall-clock time (not from the past), extending an active mailbox adds time onto the remaining duration, and setting unlimited clears any expiration flag.

### 4. Comprehensive Testing Pyramid
- **Layer 1: Unit Tests (`MailboxWebControllerTest.java`)**:
  - `updateMailbox_extendKeepCurrent`: verifies `ts_Active` is untouched.
  - `updateMailbox_extendHours_activeMailbox`: verifies future expiration adds hours onto current `ts_Active`.
  - `updateMailbox_extendHours_expiredMailbox`: verifies expired mailbox base is `now`.
  - `updateMailbox_extendUnlimited`: verifies `ts_Active` is set to `0L` and `expired` set to `false`.
  - `extendMailbox_hours24`: verifies +24h extension.
  - `extendMailbox_hours168`: verifies +7d extension.
  - `extendMailbox_hours720`: verifies +30d extension.
  - `extendMailbox_unlimited`: verifies `hours = -1` sets `0L`.
  - `extendMailbox_notFound`: verifies 404 for unknown mailbox ID.
  - `extendMailbox_crossUser`: verifies 404 / access denied when user does not own the mailbox.
- **Layer 2: Web Slice Integration Tests (`MailboxWebTest.java`)**:
  - Verify HTMX partial responses and headers (`HX-Trigger: mailboxChanged`).
  - Verify CSRF enforcement on mutating POST endpoints.
  - Verify modal rendering includes all radio buttons with localized labels in English and German.
  - Verify table row rendering contains the new action buttons with proper attributes.

## Risks / Trade-offs

- **[Risk] Table row action buttons crowding on tablet viewports (768px - 991px)**
  → *Mitigation*: Use compact `btn-group-sm` and short button labels (`+1d`, `+7d`, `+30d`, `<i class="bi bi-infinity"></i>`). The entire button group consumes ~195px, which fits alongside standard table columns. On screens `< 768px`, all standalone buttons auto-collapse into the kebab menu.
- **[Risk] Inadvertently shortening an active mailbox when extending**
  → *Mitigation*: The formula `base = Math.max(now, mailbox.getTs_Active())` guarantees that remaining future time is preserved when adding days.
- **[Risk] Extending an already expired mailbox resulting in a timestamp still in the past**
  → *Mitigation*: Using `Math.max(now, ...)` ensures expired mailboxes are extended relative to the current timestamp and unexpired (`expired = false`).
- **[Risk] Discrepancy between UI labels and backend values**
  → *Mitigation*: Explicit unit and web slice tests verify that selecting each preset maps to exact hour constants (`24`, `168`, `720`, `-1`).
