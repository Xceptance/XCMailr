# Design

## Context

The global Thymeleaf base layout (`base.html`) renders a corporate footer containing both a copyright notice and a technology stack line (`XCMailr Modernized • Java 25 • Spring Boot 4.x • HTMX`). Message bundles (`messages.properties` and `messages_de.properties`) also hold corresponding `layout.footer.summary` entries. Contract and web controller tests assert the presence of this banner.

See `proposal.md` for motivation and `specs/web-console-ui/spec.md` for the updated requirement contract.

## Goals / Non-Goals

**Goals:**
- Eliminate internal framework and runtime version disclosure from all web console views.
- Clean up obsolete `layout.footer.summary` keys from English and German message properties.
- Enhance `BaseLayoutFooterContractTest` with negative assertions verifying tech stack strings are absent from rendered layouts.
- Update `AuthWebControllerTest` assertions to verify corporate brand notices without asserting the tech stack banner.

**Non-Goals:**
- Modifying copyright text, company link, or general footer CSS styles.
- Adding conditional dev-mode footer bars or environment indicators.
- Changing header navigation or other layout components.

## Decisions

### Decision 1: Complete Removal of Tech Stack Line
- **Choice**: Remove `<p class="small mb-0">` from `base.html` entirely.
- **Rationale**: Version and framework metadata provide no end-user utility and constitute an information disclosure surface. Developers already have build tools, runtime logs, and Spring Boot Actuator for stack inspection.
- **Alternatives Considered**:
  - *Render conditionally only in local dev*: Adds template logic and environment coupling to the base layout for zero operational value.
  - *Keep generic "XCMailr Modernized" without versions*: Still redundant and clutters the corporate brand footer.

### Decision 2: Remove Obsolete Localization Keys
- **Choice**: Delete `layout.footer.summary` from `messages.properties` and `messages_de.properties`.
- **Rationale**: Prevents dead translation keys from accumulating in message bundles.
- **Alternatives Considered**: Keeping unused keys for backwards compatibility (unnecessary since localization keys are internal to the web console application).

### Decision 3: Add Negative Regression Assertions to Contract Tests
- **Choice**: In `BaseLayoutFooterContractTest`, assert `not(containsString(...))` for `XCMailr Modernized`, `Java 25`, `Spring Boot`, and `HTMX` across public (`/login`), authenticated (`/mailboxes`), and error (`/error`) views.
- **Rationale**: Prevents accidental re-introduction of technology stack disclosures during future layout modifications.

## Risks / Trade-offs

- **[Risk]** Existing integration tests fail due to missing string assertions.
  - **Mitigation**: Grepped all occurrences across `src/test/`; identified `BaseLayoutFooterContractTest` and `AuthWebControllerTest`. These tests will be updated directly as part of this change.
- **[Risk]** Footer height or vertical alignment looks unbalanced with a single line.
  - **Mitigation**: The footer container `.footer-xceptance` already centers content with standard Bootstrap padding; reducing two paragraphs to one yields a cleaner, more standard layout.

