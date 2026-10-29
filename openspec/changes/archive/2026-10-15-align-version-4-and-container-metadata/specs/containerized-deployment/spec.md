# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Project version consistency
The project SHALL define version 4.0.0 consistently across all Maven modules, container packaging, and root documentation.

#### Scenario: Consistent version across POMs and documentation
- **WHEN** inspecting parent `pom.xml`, module POMs, and root `readme.md`
- **THEN** the declared version is `4.0.0`.

