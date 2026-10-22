# Design

## Context

The repository is structured as a Maven multi-module reactor:
- `xcmailr` (root POM)
- `xcmailr-webapp`
- `xcmailr-client`
- `xcmailr-load-test-suite`

The `xcmailr-load-test-suite` module depends on `xcmailr-client` at version `${project.version}` (`4.0.0`). Because version `4.0.0` is an unpublished local artifact, invoking Maven with `-pl xcmailr-load-test-suite` without `--also-make` (`-am`) causes Maven to look for `xcmailr-client:4.0.0` in Maven Central, resulting in `DependencyResolutionException`.

## Goals / Non-Goals

**Goals:**
- Update all copy-pasteable Maven CLI snippets in `docs/load-testing-guide.md` and `xcmailr-load-test-suite/README.md` that use `-pl xcmailr-load-test-suite` to include `-am`.
- Ensure a clean repository checkout can compile, package, and execute the test scenarios without manual prerequisite installations.

**Non-Goals:**
- Modify Maven POMs, dependency scopes, or plugin configurations.
- Require developers to pre-install artifacts into `~/.m2/repository`.
- Change any Java source code or runtime configurations.

## Decisions

### Decision 1: Use `-am` (`--also-make`) rather than requiring a pre-installation step
- **Rationale**: `-am` instructs Maven's reactor engine to detect that `xcmailr-load-test-suite` depends on `xcmailr-client` and compile `xcmailr-client` as part of the execution plan. It is completely self-contained and idempotent.
- **Alternative considered**: Documenting a prerequisite step like `mvn install -pl xcmailr-client -DskipTests`. Rejected because it introduces extra manual steps, pollutes local `~/.m2/repository`, and is prone to being missed by testers.

### Decision 2: Update both `docs/load-testing-guide.md` and `xcmailr-load-test-suite/README.md`
- **Rationale**: Developers run load tests both from the root walkthrough guide and from the module-level README. Aligning both documentation sources prevents discrepancies.

## Risks / Trade-offs

- **[Risk] Incremental build overhead**: Including `-am` adds upstream modules to the Maven execution plan.
  - **Mitigation**: `xcmailr-client` is lightweight (14 Java source files) and compiles in < 1 second. When classes are already up to date, Maven skips recompilation.

