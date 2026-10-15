# Design

## Context

See proposal.md for motivation. XCMailr is a Spring Boot application running on Java 25 with virtual threads, featuring an embedded Tomcat web server (port 8080), SubEthaSMTP inbound server (default port 25000), embedded H2 or external relational database, and optional outbound SMTP relay for mail forwarding.

## Goals / Non-Goals

**Goals:**
- Provide a reproducible, production-grade multi-stage `Dockerfile` producing a minimal, secure container image.
- Automate building and publishing container images to GitHub Container Registry (`ghcr.io`) via GitHub Actions.
- Provide a multi-container test composition (`docker-compose.test.yml`) combining XCMailr and Mailpit (`axllent/mailpit:latest`) to inspect forwarded emails via web UI during functional/QA testing.
- Provide a concise, practical cloud test deployment guide (`docs/cloud-test-deployment-guide.md`) for running high-pressure load tests against an ephemeral cloud VM.
- Enable dynamic configuration of ports (especially custom inbound SMTP port `MBOX_PORT` like `2525` to bypass cloud port 25 restrictions) and database settings.
- Document an optional outbound mail forwarding configuration section (Mailpit mock and production relay).
- Reference Docker execution and the deployment guide in `readme.md`.

**Non-Goals:**
- Heavy Infrastructure-as-Code (Terraform, Pulumi) or Kubernetes Helm charts (out of scope for lightweight ephemeral testing).
- Complex VM lifecycle watchdogs or cron daemons on the test host; testing is explicitly ephemeral (launch VM, test, terminate VM).
- Modifying core application Java code or REST endpoints.

## Decisions

### Decision 1: Base Image Selection
- **Choice**: Multi-stage build using `eclipse-temurin:25-jdk` (or Maven with Temurin 25) for compiling, and `eclipse-temurin:25-jre` (Ubuntu/Debian slim base) for runtime.
- **Rationale**: Eclipse Temurin is the official OpenJDK distribution supporting Java 25 virtual threads with long-term stability and small image footprint (~250MB vs >700MB full JDK).
- **Alternative Considered**: Distroless or Alpine. Distroless makes troubleshooting during initial container testing difficult; Temurin slim strikes the optimal balance of small size and debuggability.

### Decision 2: Container Security & Storage Isolation
- **Choice**: Run the container process under a dedicated non-root user (`xcmailr`, UID 10001, GID 10001). Pre-create `/data` and `/app/log` with appropriate ownership.
- **Rationale**: Follows container security best practices and prevents privilege escalation.
- **Storage**: Default H2 database path configured to `/data/xcmailrDB` inside the container, mountable to a host volume (`-v /var/data/xcmailr:/data`).

### Decision 3: Ephemeral Cloud Test Workflow
- **Choice**: Adopt an on-demand, ephemeral VM model rather than a permanent scheduled instance.
- **Rationale**: The Docker container is 100% immutable and stateless for load testing. Developers or colleagues spin up a fresh VM, launch the container, run tests with XLT, and delete the VM immediately after testing. This eliminates continuous cloud costs and guarantees clean test state.

### Decision 4: Upstream Port 8080 and Custom SMTP Port 25000/2525
- **Choice**: Expose HTTP on port `8080` and inbound SMTP via environment variable `MBOX_PORT` (default `25000`).
- **Rationale**: Port `8080` allows direct HTTP access in ad-hoc test environments, and serves as the standard internal upstream port behind reverse proxies (`https://xcmailr.xceptance.de/`) in production with `APP_URL` configured. `MBOX_PORT` allows switching to ports like `2525` or `10025` to comply with cloud provider network policies.

### Decision 5: GitHub Actions Registry Publishing
- **Choice**: Use GitHub's native `GITHUB_TOKEN` with `packages: write` permissions to publish to `ghcr.io/xceptance/xcmailr`.
- **Rationale**: Requires zero manual secret management or credentials setup in GitHub repository settings.

### Decision 6: Multi-Container Test Environment with Mailpit
- **Choice**: Provide `docker-compose.test.yml` running XCMailr alongside Mailpit (`axllent/mailpit:latest`) on an isolated Docker bridge network. XCMailr connects outbound SMTP to `mailpit:1025` (`SMTP_HOST=mailpit`, `SMTP_PORT=1025`, `SMTP_AUTH=false`, `SMTP_TLS=false`), while Mailpit publishes its web viewer on port `8025`.
- **Rationale**:
  - *Full Feature Verification*: QA and developers can test mail forwarding and account activation emails through the Mailpit UI (`http://<host>:8025`) without external SMTP credentials or spam blacklist risks.
  - *Zero Load Test Overhead*: In load tests, mailboxes are created with `"forward": false`. XCMailr handles inbound traffic in memory/DB without dispatching outbound SMTP relay calls, so Mailpit remains idle (consuming ~15MB RAM, 0% CPU, zero disk writes).
  - *Consistency*: Mailpit is already adopted in `docker-compose.dev.yml` for local development, creating identical developer and testing workflows.

```
          [ Browser / Test Client / XLT Load Generator ]
                   |              |              |
          Port 8080|     Port 25000/2525|     Port 8025
          (Web/REST)        (Inbound SMTP)   (Mailpit UI)
                   |              |              |
  +----------------V--------------V--------------V----------------+
  | Cloud Test VM Host                                            |
  |                                                               |
  |   +------------------------------------+                      |
  |   | XCMailr Container                  |                      |
  |   |   - Web Console & REST API (:8080) |                      |
  |   |   - Inbound SubEthaSMTP (:25000)   |                      |
  |   +------------------+-----------------+                      |
  |                      | outbound relay                         |
  |      Docker Bridge   | (mailpit:1025)                         |
  |      Network         v                                        |
  |   +------------------------------------+                      |
  |   | Mailpit Container                  |                      |
  |   |   - Mock SMTP Server (:1025)       |                      |
  |   |   - Web Mailbox Viewer (:8025) <---+                      |
  |   +------------------------------------+                      |
  +---------------------------------------------------------------+
```

## Risks / Trade-offs

- **[Risk] Cloud Firewall Drops Inbound Traffic**  
  *Mitigation*: The deployment guide provides explicit `gcloud` and Linux firewall commands to open ports 8080 and the custom SMTP port.
- **[Risk] `SendMail` Load Test Dropping Emails on Fresh Instance**  
  *Mitigation*: The guide includes the pre-flight `curl` command to initialize `jw@xcmailr.test` with `deactivationTime: 0` before starting XLT tests.
- **[Risk] Container File Permission Errors with Volume Mounts**  
  *Mitigation*: The Dockerfile explicitly sets permissions on `/data` and documents the correct host volume permission setup.

