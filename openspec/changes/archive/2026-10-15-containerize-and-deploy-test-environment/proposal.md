# Proposal

## Why

Local endurance load tests pushed the test machine's CPU to 98.5%, proving that running both the load generator (XLT) and the application on the same host limits the maximum achievable test pressure. To benchmark XCMailr under high load and establish a reusable test deployment mechanism for future development tasks, the application must be packaged as an immutable, production-grade container image and deployed seamlessly to an ephemeral cloud test environment.

## What Changes

- Add a production-ready multi-stage `Dockerfile` targeting Eclipse Temurin 25 JRE with non-root user execution, read-only root compatibility, and persistent data volume mounts.
- Add a test environment composition file (`docker-compose.test.yml`) combining XCMailr and Mailpit (`axllent/mailpit:latest`) on an isolated bridge network, providing an out-of-the-box test environment with outbound email inspection (Web UI on port 8025) alongside XCMailr (ports 8080 and 25000/2525).
- Add a GitHub Actions workflow (`.github/workflows/build-container.yml`) that builds and publishes the container image to GitHub Container Registry (`ghcr.io`) on push and manual trigger.
- Add comprehensive cloud deployment documentation (`docs/cloud-test-deployment-guide.md`) detailing:
  - Ephemeral Google Cloud VM setup and 1-command deployment using either standalone `docker run` or `docker compose -f docker-compose.test.yml up -d` with Mailpit.
  - Custom inbound SMTP port configuration (`MBOX_PORT`) to bypass cloud port 25 restrictions.
  - Integration with the XLT load test suite (`xcmailr.host` and `smtp.port` configuration).
  - Outbound email forwarding verification via Mailpit web interface or real production relay.
  - Tear-down instructions to eliminate lingering cloud costs.
- Update `readme.md` to reference the new containerized deployment option and link to the cloud test deployment guide.

## Capabilities

### New Capabilities
- `containerized-deployment`: Defines container packaging, runtime isolation, dynamic port bindings, health check integration, Mailpit mock relay test composition, and deployment automation for testing and production environments.

### Modified Capabilities
None. (Existing application logic, REST endpoints, and SMTP protocols remain backward-compatible).

## Impact

- **Build & Packaging**: Introduces root-level `Dockerfile`, `.dockerignore`, and `docker-compose.test.yml`.
- **CI/CD**: Introduces `.github/workflows/build-container.yml`.
- **Documentation**: Adds `docs/cloud-test-deployment-guide.md` and updates `readme.md`.
- **Runtime**: Zero breaking changes to existing JAR execution or Maven development workflows.

