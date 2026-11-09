# Design

## Context

See [proposal.md](proposal.md) for background and motivation.

Currently, `xcmailr-webapp` contains two packages holding data transfer representations:
1. `com.xceptance.xcmailr.dto`:
   - `RegistrationForm.java`
   - `ProfileUpdateForm.java`
2. `com.xceptance.xcmailr.controllers.restapi`:
   - `MailboxData.java`
   - `MailData.java`
   - `AttachmentData.java`
   - `ApiError.java`
   - `ApiErrors.java`

This design addresses the relocation and consolidation of the 5 REST data transfer objects into `com.xceptance.xcmailr.dto`.

## Goals / Non-Goals

**Goals:**
- Move all REST request, response, and error representation classes (`MailboxData`, `MailData`, `AttachmentData`, `ApiError`, `ApiErrors`) into `com.xceptance.xcmailr.dto`.
- Update all package declarations and import statements in affected controllers and tests.
- Leave `com.xceptance.xcmailr.controllers.restapi` containing strictly controller endpoints (`MailApiController`, `MailboxApiController`) and exception handlers (`RestValidationExceptionHandler`).
- Preserve 100% backward compatibility of REST API JSON serialization, field constraints, and error response structures.
- Ensure all moved classes adhere to project standards: comprehensive Javadoc, inline comments, and `final` modifiers.

**Non-Goals:**
- Creating subpackages such as `dto.web` or `dto.api` (Option 1 was explicitly selected by the user for a flat, cohesive package).
- Modifying `xcmailr-client` or its client-side domain models.
- Altering any JSON field mappings, validation logic, or endpoint route signatures.

## Decisions

### Decision 1: Consolidate into a flat `com.xceptance.xcmailr.dto` package
- **Choice**: Move all 5 REST data classes directly into `com.xceptance.xcmailr.dto`.
- **Rationale**: With a total of 7 DTO classes across `xcmailr-webapp`, a flat package is clean, concise, and matches the project's top-level layer conventions (`models`, `repositories`, `services`, `controllers`, `dto`, `config`, `security`).
- **Alternative Considered**: Sub-packaging by protocol (`com.xceptance.xcmailr.dto.web` and `com.xceptance.xcmailr.dto.api`). Rejected in favor of Option 1 to avoid premature over-abstraction and unnecessary directory nesting for just 7 files.

### Decision 2: Direct relocation without backward-compatibility stubs
- **Choice**: Move the classes directly and update all internal imports.
- **Rationale**: `xcmailr-webapp` is an executable application JAR, not an exported library. The client library (`xcmailr-client`) is completely decoupled and uses its own models (`xcmailr.client.Mailbox`, `xcmailr.client.Mail`, etc.). No external project imports `com.xceptance.xcmailr.controllers.restapi.MailboxData`.

## Risks / Trade-offs

- **[Risk] Missed import references in test files or controllers**:
  - *Mitigation*: Run a comprehensive codebase search (`grep`) across `xcmailr-webapp/src/` for any references to `com.xceptance.xcmailr.controllers.restapi.(MailboxData|MailData|AttachmentData|ApiError|ApiErrors)`.
- **[Risk] Regressions in JSON serialization or validation**:
  - *Mitigation*: Run the full reactor test suite (`mvn clean test`), including `MailboxApiIT`, `MailApiIT`, and `MailboxApiControllerValidationTest`.
