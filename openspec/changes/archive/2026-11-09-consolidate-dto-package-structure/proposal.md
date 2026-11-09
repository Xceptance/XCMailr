# Proposal

## Why

With the recent package modernization and server-side validation changes, data transfer objects in `xcmailr-webapp` are split across two separate packages:
- Web MVC form backing beans (`RegistrationForm`, `ProfileUpdateForm`) reside in `com.xceptance.xcmailr.dto`.
- REST API request, response, and error payloads (`MailboxData`, `MailData`, `AttachmentData`, `ApiError`, `ApiErrors`) reside in `com.xceptance.xcmailr.controllers.restapi`.

This creates an architectural asymmetry where web controllers delegate their input models to a dedicated `dto` package, while REST controllers keep their data representations co-located in their controller package. Consolidating all DTOs into a unified, flat `com.xceptance.xcmailr.dto` package restores package-by-layer architectural consistency, leaves controller packages containing strictly controller and advice classes, and eliminates confusion about where boundary data representations belong.

## What Changes

- **Move REST Data Models**: Relocate `MailboxData.java`, `MailData.java`, and `AttachmentData.java` from `com.xceptance.xcmailr.controllers.restapi` to `com.xceptance.xcmailr.dto`.
- **Move REST Error Models**: Relocate `ApiError.java` and `ApiErrors.java` from `com.xceptance.xcmailr.controllers.restapi` to `com.xceptance.xcmailr.dto`.
- **Update Controller References**: Update package declarations and import statements across `MailboxApiController`, `MailApiController`, and `RestValidationExceptionHandler`.
- **Update Test References**: Update package declarations and import statements across integration and unit test suites (`MailboxApiIT`, `MailApiIT`, `MailboxApiControllerValidationTest`).
- **Code Hygiene**: Ensure all moved classes follow project conventions including complete Javadoc, informative inline code comments, and `final` modifiers on method parameters and local variables.

## Capabilities

### New Capabilities

None. This is an internal package structure refactoring with no new user-facing or spec-level behaviors.

### Modified Capabilities

None. System behavior and HTTP REST API contracts remain identical (`skip_specs: true`).

## Impact

- **Affected Code**: `xcmailr-webapp` package structure (`com.xceptance.xcmailr.controllers.restapi` and `com.xceptance.xcmailr.dto`), REST controllers, and associated test suites.
- **REST API Contract**: Zero impact. JSON field names, serialization formats, validation error responses, and HTTP status codes remain 100% unchanged.
- **External Client Library (`xcmailr-client`)**: Zero impact. `xcmailr-client` defines its own client models (`xcmailr.client.Mailbox`, `xcmailr.client.Mail`, etc.) and does not depend on `xcmailr-webapp` classes.
