# Design

## Context

The application root `pom.xml` imports `org.springframework.boot:spring-boot-dependencies:4.1.1` in `<dependencyManagement>`, which defaults `<tomcat.version>` to `11.0.24`. See `proposal.md` for background and vulnerability motivation.

## Goals / Non-Goals

**Goals:**
- Cleanly upgrade all embedded Apache Tomcat dependencies (`tomcat-embed-core`, `tomcat-embed-el`, `tomcat-embed-websocket`) to `11.0.25`.
- Resolve the three Critical CVEs (CVE-2026-68525, CVE-2026-43512, CVE-2026-65182) flagged by Socket Security.
- Verify zero regression in existing unit, integration, and security filter tests.

**Non-Goals:**
- Upgrading Spring Boot or modifying any other managed dependency versions.
- Refactoring application-level authentication, session management, or Spring Security filter chains.

## Decisions

### Decision 1: Root POM Property and DependencyManagement Override

Define `<tomcat.version>11.0.25</tomcat.version>` in `<properties>` and declare `tomcat-embed-core`, `tomcat-embed-el`, and `tomcat-embed-websocket` referencing `${tomcat.version}` within `<dependencyManagement>` before the `spring-boot-dependencies` BOM import in the root `pom.xml`.

- **Rationale**: When `spring-boot-dependencies` is imported with `<scope>import</scope>` rather than used as a Maven `<parent>`, Maven resolves BOM properties from the BOM itself rather than inheriting POM properties. Pre-declaring the three `tomcat-embed-*` artifacts in `<dependencyManagement>` ensures precedence over the BOM and cleanly aligns all Tomcat dependencies to 11.0.25 across all modules.

## Risks / Trade-offs

- **[Risk: Semantic regression between Tomcat 11.0.24 and 11.0.25]** → **Mitigation**: 11.0.24 to 11.0.25 is a patch-level release primarily delivering bug and security fixes. Full integration test suite execution (`mvn test` including `MailApiIT` and web controller tests) will verify runtime stability.
- **[Risk: Transitive version mismatch or partial convergence]** → **Mitigation**: Run `mvn dependency:tree -Dincludes=org.apache.tomcat.embed` across the multi-module build to confirm that all embedded Tomcat dependencies resolve exclusively to `11.0.25`.

## Migration Plan

1. Add `<tomcat.version>11.0.25</tomcat.version>` to the parent `pom.xml`.
2. Inspect dependency tree to ensure complete convergence on `11.0.25`.
3. Execute the full test suite.
4. Rollback strategy: Remove `<tomcat.version>` property from parent `pom.xml` if any regressions are discovered.

