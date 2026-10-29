# Design

## Context

The mailbox creation modal (`modal-new.html`) currently uses a standard HTML `<select>` element to specify the active duration (`durationHours`). In modern browser environments, particularly macOS WebKit and Chromium, native dropdowns display an OS-level popup menu. When users navigate or hover over options (such as "30 Days") without an explicit click commitment, clicking outside or directly clicking the modal's "Create Mailbox" submit button cancels the popup menu rather than selecting the hovered item. The form is then submitted with the pre-selected default value (`2 Hours`).

Additionally, in `MailboxWebController.java`, if an inline validation error occurs (such as an address collision or invalid character set), the controller repopulates `address` and `selectedDomain` into the Spring MVC model, but omits `durationHours` and `forwardEmails`. Consequently, any validation error silently wipes the user's custom duration selection, reverting it to the default.

See `proposal.md` for background motivation and `specs/web-console-ui/spec.md` for behavioral requirements.

## Goals / Non-Goals

**Goals:**
- Replace the native `<select>` dropdown with a responsive segmented radio button pill group (`btn-group` + `btn-check`) in `modal-new.html`.
- Provide immediate, single-click duration selection with unequivocal visual active state indicators (`1h`, `2h`, `1d`, `7d`, `30d`, `Unlimited`).
- Retain `selectedDuration` and `forwardEmails` in the model across all validation error paths in `MailboxWebController.java`.
- Align default parameter resolution to 2 hours (`defaultValue = "2"`) across controller methods and templates.
- Implement exhaustive automated test coverage across the testing pyramid (Unit, Controller Integration, Template Contract) covering happy paths, error scenarios, and special edge cases, applying `final` modifiers universally.

**Non-Goals:**
- Custom free-form date/time picker inputs (preset durations cover all standard operational temporary mailbox lifecycles).
- Changing REST API endpoints or modifying the underlying database schema (`mailboxes.ts_active` remains unchanged).
- Changing the edit modal duration extension behavior (separate workflow).

## Decisions

### 1. Segmented Radio Button Pills over Native Dropdown
- **Decision**: Render duration presets using Bootstrap 5 `.btn-group` with `.btn-check` radio inputs (`name="durationHours"`).
- **Rationale**:
  - Eliminates native OS popup menu occlusion and double-click dismiss capture issues on macOS.
  - Active selection is highlighted instantaneously with Bootstrap button styling (`btn-outline-secondary` or subtle primary active states).
  - Maintains standard HTML form serialization: the checked radio button's value is submitted as `durationHours`, requiring no custom JavaScript handlers or HTMX plugins.
- **Alternatives Considered**:
  - *Keep `<select>` with custom JS*: Prone to race conditions and inconsistent cross-browser focus management.
  - *HTML5 `<input type="datetime-local">`*: Requires date/time math from the user, inconsistent browser styling, and excessive complexity for simple temporary disposable addresses.

### 2. Standardized Presets and Labels
- **Decision**: Define the 6 duration options:
  1. `1` -> `1h` (1 Hour)
  2. `2` -> `2h` (2 Hours, default)
  3. `24` -> `1d` (1 Day / 24 Hours)
  4. `168` -> `7d` (1 Week / 7 Days)
  5. `720` -> `30d` (30 Days / 720 Hours)
  6. `0` -> `Unlimited` (No expiration, `ts_Active = 0`)
- **Rationale**: Compact labels (`1h`, `2h`, `1d`, `7d`, `30d`, `Unlimited`) fit cleanly in a single responsive row or wrapped two-column grid on mobile dialogs while remaining self-explanatory.

### 3. Model Attribute Retention on Validation Error Re-renders
- **Decision**: In `MailboxWebController.java`, include `selectedDuration` (int) and `forwardEmails` (boolean) in `model.addAttribute(...)` across all error branches (`invalidAddress`, `invalidDomain`, `mailboxExists`), as well as initializing them in `GET /mailboxes/new`.
- **Rationale**: Guarantees that user selections are never wiped when an error alert is shown.
- **Alternatives Considered**:
  - *Relying on HTMX form state caching*: Unreliable when the target is swapped with a new fragment.

### 4. Testing Pyramid Strategy
- **Decision**: Structure automated tests into 3 distinct tiers:
  1. **Unit Tier** (`MailboxValidationUnitTest.java`): Pure unit tests for timestamp arithmetic, duration mapping, overflow protection, and fallback evaluation.
  2. **Web / Integration Tier** (`MailboxWebControllerTest.java`): MockMvc controller tests for each preset (happy paths including `720` and `0`), validation error flows verifying model attribute retention, and edge cases.
  3. **Contract / Template Tier** (`MailboxCreationModalContractTest.java`): View assertions ensuring all 6 radio buttons exist, have proper labels and accessibility attributes, and correctly reflect model-driven checked states.
- **Standard**: All test methods, local variables, parameters, and helper classes MUST use `final` modifiers.

```
       / \
      /   \      Template Contract Tests (Modal markup, radio inputs, dynamic checks)
     / ----\
    /       \    Controller MockMvc Integration Tests (Presets, Errors, Model retention)
   / --------\
  /           \  Unit Tests (Duration arithmetic, overflow bounds, entity logic)
 / ------------\
```

## Risks / Trade-offs

- **[Risk]** Button group wrapping awkwardly on narrow mobile viewports (< 360px).
  - *Mitigation*: Use `flex-wrap gap-1` and `flex-fill` on pill labels, ensuring clean two-row or three-row wrapping on very small screens without horizontal scroll.
- **[Risk]** Form submission with missing `durationHours` if radio buttons are unselected via DOM manipulation.
  - *Mitigation*: Controller declares `@RequestParam(value = "durationHours", defaultValue = "2")`, safely falling back to 2 hours if omitted.
- **[Risk]** Arithmetic integer overflow for astronomical duration values.
  - *Mitigation*: Ensure `TimeUnit.HOURS.toMillis((long) durationHours)` is computed with 64-bit `long` arithmetic, with unit tests guarding against overflow.

## Migration Plan

- No database migrations or schema adjustments required.
- Deployment is completely zero-downtime and backward compatible with existing mailbox records.

