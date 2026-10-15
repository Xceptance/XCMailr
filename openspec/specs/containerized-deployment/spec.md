# Containerized Deployment Specification

## Purpose

Provides standardized container packaging, secure unprivileged runtime execution, automated image publishing, and deployment workflows for testing and production environments.

## Requirements

### Requirement: Multi-stage container packaging
The project SHALL provide a multi-stage container build definition that builds the application executable with Maven and OpenJDK 25, then packages it into a slim JRE runtime image.

#### Scenario: Build container image from source
- **WHEN** `docker build` is executed from the repository root
- **THEN** the build succeeds and produces a minimal runtime image containing only the executable JAR and runtime dependencies.

### Requirement: Non-root and secure runtime execution
The container image SHALL run under a dedicated, unprivileged non-root user and support volume mounts for persistent data storage and logs.

#### Scenario: Startup as unprivileged user
- **WHEN** the container is launched without privileged flags
- **THEN** the application initializes successfully under an unprivileged user account and writes database files to the mounted data volume.

### Requirement: Environment-driven port and host configuration
The containerized application SHALL dynamically configure web HTTP and inbound SMTP listening ports from environment variables, allowing deployment on non-standard ports to satisfy cloud security constraints.

#### Scenario: Custom inbound SMTP port
- **WHEN** the container is started with `MBOX_PORT=2525`
- **THEN** the inbound SubEthaSMTP service binds and accepts email on port 2525.

#### Scenario: Outbound email forwarding toggle
- **WHEN** the container is started without `SMTP_HOST` defined
- **THEN** outbound mail forwarding remains disabled and received emails are retained locally without external dispatch errors.

#### Scenario: Outbound email forwarding enabled
- **WHEN** the container is started with `SMTP_HOST`, `SMTP_PORT`, and credentials defined
- **THEN** outbound mail forwarding transmits copies of forwarded emails to the specified relay host.

### Requirement: Test environment composition with mock mail relay
The project SHALL provide a test composition definition (`docker-compose.test.yml`) combining XCMailr and Mailpit on an internal bridge network, providing an out-of-the-box test environment with outbound email inspection and an explicit project name `xcmailr-test-environment`.

#### Scenario: Launch test environment with Docker Compose
- **WHEN** `docker compose -f docker-compose.test.yml up -d` is executed
- **THEN** XCMailr is started with outbound SMTP configured to point to Mailpit on port 1025, Mailpit's web interface is published on port 8025, and the stack is grouped under project name `xcmailr-test-environment`.

#### Scenario: Forwarded email captured in Mailpit
- **WHEN** an email is received for a mailbox configured with forwarding enabled
- **THEN** XCMailr forwards the message to Mailpit (`mailpit:1025`) and the forwarded message is viewable in Mailpit's web UI (`http://<host>:8025`).

#### Scenario: High-volume load test with forwarding disabled
- **WHEN** load test traffic targets mailboxes configured with `"forward": false`
- **THEN** XCMailr processes inbound traffic without dispatching outbound emails, leaving Mailpit idle with zero overhead during load testing.

### Requirement: Automated container publishing
The repository SHALL provide an automated continuous integration workflow that compiles, tests, and publishes versioned and latest container images to GitHub Container Registry (`ghcr.io`), dynamically extracting the semantic project version from Maven `pom.xml` and attaching explicit OCI annotations preserving the authentic project subtitle.

#### Scenario: Build and publish on branch push or workflow dispatch
- **WHEN** changes are pushed to an active development branch or the workflow is triggered manually
- **THEN** the GitHub Actions workflow extracts the Maven version from `pom.xml`, builds the multi-stage image, and publishes tags for `<version>`, `<major>.<minor>`, `latest`, and Git SHA to `ghcr.io`.

#### Scenario: Container OCI metadata and tagline
- **WHEN** the container image is published to GitHub Container Registry
- **THEN** the image metadata contains OCI labels with title "XCMailr", description "A small mailer for us but a big helper for testing", vendor "Xceptance Software Technologies GmbH", and documentation URL.

### Requirement: Project version consistency
The project SHALL define version 4.0.0 consistently across all Maven modules, container packaging, and root documentation.

#### Scenario: Consistent version across POMs and documentation
- **WHEN** inspecting parent `pom.xml`, module POMs, and root `readme.md`
- **THEN** the declared version is `4.0.0`.

### Requirement: Cloud test deployment documentation
The project SHALL provide documentation detailing how to deploy the container to an ephemeral cloud VM, configure custom ports and firewall rules, execute load tests from a remote host, and cleanly terminate the VM.

#### Scenario: Deploying and testing on cloud VM
- **WHEN** a developer or tester consults the cloud test deployment guide
- **THEN** they find exact commands to launch the container via `docker run` or `docker compose -f docker-compose.test.yml`, map ports (including Mailpit port 8025), pre-create test mailboxes, configure XLT `project.properties`, and inspect forwarded emails.

