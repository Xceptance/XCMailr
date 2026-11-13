# Design: Modernize xcmailr-webapp Package Structure

## Context

See `proposal.md` for motivation and background.

Currently, `xcmailr-webapp` retains three legacy top-level package roots left over from the Ninja framework migration:
1. `models/`: JPA entities and mapping classes (`User`, `MBox`, `Mail`, `Domain`, `MailTransaction`, `MailStatistics`, `MailStatisticsKey`, `Status`, `AbstractEntity`).
2. `controllers/restapi/`: REST DTOs and API errors (`MailData`, `MailboxData`, `AttachmentData`, `util/ApiError`, `util/ApiErrors`).
3. `etc/`: Utility classes (`HelperUtils`, `SizeLimitExceededException`), with `etc.HelperUtilsTest` in the test tree.

As a result, `XcmailrApplication.java` contains manual string array arguments for component scanning, entity scanning, and JPA repository scanning.

## Goals / Non-Goals

**Goals:**
- Move all JPA entities from top-level `models` to `com.xceptance.xcmailr.models`.
- Move all REST DTOs and errors from top-level `controllers.restapi` to `com.xceptance.xcmailr.controllers.restapi`.
- Move `HelperUtils` and `SizeLimitExceededException` from `etc` to `com.xceptance.xcmailr.util`, and test class to `com.xceptance.xcmailr.util`.
- Simplify `XcmailrApplication.java` to leverage default Spring Boot hierarchical package scanning.
- Update all affected imports across main and test sources while keeping all 681 tests passing.

**Non-Goals:**
- Modifying REST API endpoints, JSON request/response formats, or HTTP status codes.
- Altering Flyway SQL migrations or database table schemas.
- Modifying `xcmailr-client` or `xcmailr-load-test-suite` (both are decoupled from `xcmailr-webapp` internal packages).
- Adding server-side validation or updating copyright headers (deferred to separate, focused changes).

## Decisions

### Decision 1: Co-locate REST DTOs in `com.xceptance.xcmailr.controllers.restapi`
- **Choice**: Place `MailData`, `MailboxData`, `AttachmentData`, `ApiError`, and `ApiErrors` directly within `com.xceptance.xcmailr.controllers.restapi` alongside `MailApiController` and `MailboxApiController`.
- **Rationale**: These data transfer and error classes belong exclusively to the REST API HTTP boundary. Co-locating them maximizes feature cohesion and prevents polluting other layers. Furthermore, it cleanly isolates REST contracts from future Web UI form backing beans.
- **Alternatives Considered**: Creating a global `com.xceptance.xcmailr.dto` package. Rejected because it blurs technical boundaries and tends to become a general dumping ground for unrelated classes.

### Decision 2: Consolidate Utilities into `com.xceptance.xcmailr.util`
- **Choice**: Move `HelperUtils` and `SizeLimitExceededException` to `com.xceptance.xcmailr.util`, matching `JakartaMimeMessageParser`.
- **Rationale**: Removes the obsolete `etc` package and groups generic parsing/formatting utilities in one standard location.

### Decision 3: Standardize Spring Boot Bootstrapping
- **Choice**: Remove manual package list arguments from `XcmailrApplication.java`:
  - Change `@SpringBootApplication(scanBasePackages = {"com.xceptance.xcmailr", "controllers", "services"})` to `@SpringBootApplication`.
  - Remove `@EntityScan` and `@EnableJpaRepositories` string overrides, or retain standard defaults scoping to `com.xceptance.xcmailr.*`.
- **Rationale**: Since `XcmailrApplication` is located at `com.xceptance.xcmailr`, Spring Boot natively discovers all controllers, services, repositories, and entities residing in child packages.

## Risks / Trade-offs

- **[Risk] Missed import references causing runtime or compile failures**  
  *Mitigation*: Execute a full reactor build and verify all 681 tests pass (`mvn clean test`).
- **[Risk] JPA entity discovery regression**  
  *Mitigation*: Verify through `EntityMappingTest`, `RepositoryTests`, and `MailRepositoryTest` that Spring Boot and Hibernate properly register all entities in `com.xceptance.xcmailr.models`.
