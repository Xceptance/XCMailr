# Design: Version 4.0.0 and Container Metadata Alignment

## Context

See `proposal.md` for motivation. XCMailr's modernized Java 25 / Spring Boot architecture requires establishing `4.0.0` as the authoritative version across build tooling, documentation, and container registry artifacts.

## Goals / Non-Goals

**Goals:**
- Single source of truth for version 4.0.0 across parent and module Maven POMs.
- Automated extraction of Maven project version during GitHub Actions CI/CD to generate `4.0.0`, `4.0`, `latest`, and commit SHA container tags.
- Explicit OCI metadata annotations preserving the authentic project subtitle (`"A small mailer for us but a big helper for testing"`).
- Explicit top-level project naming (`name: xcmailr-test-environment`) in `docker-compose.test.yml`.
- Prominent Container Quickstart in `readme.md` detailing ports `8080` (Web UI/API) and `25000`/`2525` (Inbound SMTP).

**Non-Goals:**
- Modifying Java application runtime code or database schemas.
- Altering the multi-stage Docker build layers or unprivileged user configuration.
- Renaming the canonical GHCR package (`ghcr.io/xceptance/xcmailr`).

## Decisions

### Decision 1: Version Extraction Strategy in GitHub Actions
- **Approach**: In `.github/workflows/build-container.yml`, add an extraction step before `docker/metadata-action` that evaluates the Maven version from `pom.xml`:
  ```bash
  VERSION=$(grep -m 1 '<version>' pom.xml | sed -E 's/.*<version>(.*)<\/version>/\1/')
  MAJOR_MINOR=$(echo "$VERSION" | cut -d. -f1,2)
  echo "version=$VERSION" >> $GITHUB_OUTPUT
  echo "major_minor=$MAJOR_MINOR" >> $GITHUB_OUTPUT
  ```
- **Rationale**: Direct extraction from `pom.xml` keeps Maven as the single source of truth without requiring extra Maven downloads or dependencies.
- **Alternatives Considered**:
  - *Hardcoding `4.0.0` in workflow yaml*: High maintenance overhead and risk of version drift.
  - *Relying only on `git tag v*`*: Pushes to active branches (`modernizeXCMailr`, `develop`) would continue producing only commit hashes and `latest` without semver tags.

### Decision 2: OCI Metadata and Subtitle Preservation
- **Approach**: Configure explicit OCI labels in `docker/metadata-action`:
  ```yaml
  labels: |
    org.opencontainers.image.title=XCMailr
    org.opencontainers.image.description=A small mailer for us but a big helper for testing
    org.opencontainers.image.vendor=Xceptance Software Technologies GmbH
    org.opencontainers.image.documentation=https://github.com/Xceptance/XCMailr#container-quickstart
  ```
- **Rationale**: Overrides uninformative or stale defaults while preserving the authentic, historic XCMailr subtitle that the team values.

### Decision 3: Docker Compose Stack Naming
- **Approach**: Add top-level `name: xcmailr-test-environment` to `docker-compose.test.yml`.
- **Rationale**: Docker Desktop and `docker compose ps` group containers under this explicit project name rather than guessing the host working directory name.

## Risks / Trade-offs

- **[Risk] Module POM version desynchronization** → **Mitigation**: Update root `pom.xml` and all module POMs (`xcmailr-webapp`, `xcmailr-client`, `xcmailr-load-test-suite`) simultaneously, then run `mvn validate` to verify dependency consistency.
- **[Risk] Existing documentation referencing `3.1.0`** → **Mitigation**: Search and update all occurrences in `readme.md` and deployment guides to `4.0.0`.

