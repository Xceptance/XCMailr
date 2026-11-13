# Design

## Context

See [proposal.md](proposal.md) for background and motivation.

Prior to modernization, `xcmailr-client/pom.xml` configured `maven-source-plugin` and `maven-javadoc-plugin` to build during the `package` lifecycle phase. During the Java 25 modernization in commit `b6aad76`, `<source.skip>true</source.skip>` was introduced and `<executions>` sections were removed, leaving `xcmailr-client` producing only the primary class JAR `xcmailr-client-4.0.0.jar`.

This technical design outlines restoring the build lifecycle bindings, configuring plugin options compatible with Java 25 and modern Maven practices, and remediating latent Javadoc warnings across `xcmailr-client`.

## Goals / Non-Goals

**Goals:**
- Automatically generate `xcmailr-client-4.0.0-sources.jar` and `xcmailr-client-4.0.0-javadoc.jar` when running `mvn package` or `mvn install` on `xcmailr-client` (or root reactor).
- Use standard, recommended Maven plugin goals (`jar-no-fork` for sources, `jar` for javadoc).
- Clean up all 27 Javadoc compiler warnings across `xcmailr-client` Java files.
- Preserve full backward compatibility of API contracts and method signatures.
- Adhere to coding standards: full Javadoc comments, inline explanatory comments, and `final` modifiers.

**Non-Goals:**
- Attaching sources or javadoc JARs to internal applications (`xcmailr-webapp` or `xcmailr-load-test-suite`).
- Modifying client API method signatures or external dependencies.

## Decisions

### Decision 1: Configure plugin executions directly in `xcmailr-client/pom.xml`
- **Choice**: Bind `attach-sources` and `attach-javadocs` directly within `xcmailr-client/pom.xml`.
- **Rationale**: `xcmailr-client` is the only published library in the XCMailr repository. `xcmailr-webapp` is a standalone executable Spring Boot service, and `xcmailr-load-test-suite` is an internal benchmark framework. Centralizing in root POM would require configuring opt-out flags in sibling modules. Localizing keeps changes minimal and self-contained.
- **Alternative Considered**: Adding execution blocks to parent `pom.xml`. Rejected to prevent cluttering sibling build configurations.

### Decision 2: Use `jar-no-fork` for `maven-source-plugin`
- **Choice**: Bind goal `jar-no-fork` under execution ID `attach-sources` to phase `package`.
- **Rationale**: The standard `jar` goal forks the Maven lifecycle and re-executes `generate-sources`, causing duplicate logging and overhead. The Maven documentation explicitly recommends `jar-no-fork` when binding to the build lifecycle.
- **Alternative Considered**: Goal `jar`. Rejected because of lifecycle re-forking.

### Decision 3: Fix latent Javadoc warnings rather than ignoring them
- **Choice**: Add missing `@param`, `@return`, and constructor Javadocs in `xcmailr-client` classes, while retaining `<failOnError>false</failOnError>` as a safety net in `pom.xml`.
- **Rationale**: Clean, warning-free documentation improves IDE code completion and provides quality documentation for API consumers.
- **Alternative Considered**: Leaving the warnings intact with `<quiet>true</quiet>`. Rejected because addressing them aligns with the project's quality standard.

## Risks / Trade-offs

- **[Risk] Javadoc build failure on future strict Java compilers**:
  - *Mitigation*: Remediation of all known doclint warnings in `xcmailr-client` and maintaining `<failOnError>false</failOnError>` in `maven-javadoc-plugin`.
- **[Risk] Build time increase for `xcmailr-client`**:
  - *Mitigation*: Tested execution time for sources and javadoc generation is under 2 seconds; zero impact on webapp compilation.
