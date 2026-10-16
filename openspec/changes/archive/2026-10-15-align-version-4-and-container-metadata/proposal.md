# Proposal: Align Version 4.0.0 and Container Metadata

## Why

XCMailr has undergone a comprehensive modernization leap to Java 25, Spring Boot 4.1.1, Tomcat 11, HTMX 2, and Docker containerization. However, the repository's Maven POMs still declare legacy version 3.1.0, and the automated GitHub Actions workflow publishes container images tagged only with branch names, commit hashes, or `:latest` without connecting to the project version. Furthermore, the published GitHub Container Registry (GHCR) package lacks visible documentation, project naming in Docker Compose, and explicit OCI metadata, making it difficult for team members and external testers to identify the containerized application and its exposed ports.

Aligning the application version to 4.0.0 across all POMs and configuring the CI/CD pipeline to extract and tag images with this version establishes a single source of truth and clear visibility for local and cloud deployments.

## What Changes

- **Major Version Bump to 4.0.0**: Update `<version>` from `3.1.0` to `4.0.0` across parent `pom.xml` and all module POMs (`xcmailr-webapp`, `xcmailr-client`, `xcmailr-load-test-suite`).
- **Automated Version-Driven Container Tagging**: Update `.github/workflows/build-container.yml` to extract the Maven project version and automatically publish container tags for `4.0.0`, `4.0`, `latest`, and Git SHA.
- **Enriched Container OCI Metadata**: Configure GitHub Actions container labels to preserve the authentic subtitle (`"A small mailer for us but a big helper for testing"`) while linking directly to test deployment documentation.
- **Docker Compose Stack Naming**: Add top-level `name: xcmailr-test-environment` to `docker-compose.test.yml` for distinct grouping in Docker Desktop and CLI tooling.
- **Documentation & Quickstart Alignment**: Update `readme.md` and deployment guides to reflect version `4.0.0` with a prominent container quickstart snippet detailing ports `8080` and `25000`/`2525`.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `containerized-deployment`: Update requirements to enforce version-driven image tagging from `pom.xml`, explicit OCI metadata preservation, and Docker Compose stack naming.

## Impact

- **Build & Packaging**: Maven build produces `xcmailr-webapp-4.0.0.jar` and `xcmailr-client-4.0.0.jar`.
- **CI/CD Pipeline**: `.github/workflows/build-container.yml` dynamically evaluates project version before publishing to `ghcr.io`.
- **Deployment & Test Assets**: `docker-compose.test.yml` and documentation consistently reference version 4.0.0 and canonical naming.

