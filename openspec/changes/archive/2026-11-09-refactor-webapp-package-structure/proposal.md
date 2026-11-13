# Proposal: Modernize xcmailr-webapp Package Structure

## Why

During the migration from the legacy Ninja framework to Spring Boot, several packages in `xcmailr-webapp` were retained at the root package level (`models`, `controllers/restapi`, and `etc`) to minimize initial migration churn. This legacy layout causes architectural inconsistency, forces manual string-based package scanning configurations (`scanBasePackages`, `@EntityScan`, `@EnableJpaRepositories`) in `XcmailrApplication`, and disconnects REST API DTOs from their respective controllers.

Relocating these legacy packages into `com.xceptance.xcmailr.*` establishes a standard, cohesive Spring Boot package architecture, removes scanning workarounds, and completes the internal modernization of `xcmailr-webapp`.

## What Changes

- **Relocate JPA Entities**: Move all entity and mapping classes from top-level `models` (`AbstractEntity`, `Domain`, `Mail`, `MailStatistics`, `MailStatisticsKey`, `MailTransaction`, `MBox`, `Status`, `User`) to `com.xceptance.xcmailr.models`.
- **Relocate REST DTOs and Errors**: Move REST DTOs and API error classes (`MailData`, `MailboxData`, `AttachmentData`, `ApiError`, `ApiErrors`) from top-level `controllers.restapi` and `controllers.restapi.util` into `com.xceptance.xcmailr.controllers.restapi`, co-locating them with `MailApiController` and `MailboxApiController`.
- **Relocate Utilities**: Move `HelperUtils` and `SizeLimitExceededException` from top-level `etc` to `com.xceptance.xcmailr.util`, and relocate `HelperUtilsTest` from test package `etc` to `com.xceptance.xcmailr.util`.
- **Clean Up Spring Boot Bootstrapping**: Remove manual string array scanning arguments (`scanBasePackages = {"controllers", "services"}`, `@EntityScan("models")`, etc.) in `XcmailrApplication.java`, relying on standard Spring Boot hierarchical scanning from `com.xceptance.xcmailr`.
- **Update Imports**: Update all `import models.*`, `import etc.*`, and `import controllers.restapi.*` statements across `xcmailr-webapp` main and test sources.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None. (This change is an internal structural refactoring with `skip_specs: true`; all functional requirements, database schemas, and REST contracts remain identical).

## Impact

- **Affected Code**: `xcmailr-webapp` source and test classes (~17 files moved, ~40 consumer files updated).
- **APIs & Contracts**: None. JSON shapes, HTTP response codes, and REST endpoints remain 100% backward-compatible.
- **Persistence & Database**: None. Database tables, column definitions, and Flyway SQL migrations (V1–V4) are completely unaffected.
- **Templates**: None. Thymeleaf templates do not reference fully-qualified class names.
- **Client Library**: None. `xcmailr-client` is fully decoupled and maintains its own `xcmailr.client.*` packages.
