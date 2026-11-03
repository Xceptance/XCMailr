# Spec Delta

## MODIFIED Requirements

### Requirement: Automated container publishing
The repository SHALL provide an automated continuous integration workflow that compiles, tests, and publishes versioned and latest container images to GitHub Container Registry (`ghcr.io`) for both `linux/amd64` and `linux/arm64` CPU architectures as a multi-platform manifest list, dynamically extracting the semantic project version from Maven `pom.xml` and attaching explicit OCI annotations preserving the authentic project subtitle.

#### Scenario: Build and publish on branch push or workflow dispatch
- **WHEN** changes are pushed to an active development branch or the workflow is triggered manually
- **THEN** the GitHub Actions workflow extracts the Maven version from `pom.xml`, builds multi-platform images (`linux/amd64` and `linux/arm64`) using QEMU and Docker Buildx, and publishes multi-arch manifest lists for tags `<version>`, `<major>.<minor>`, `latest`, and Git SHA to `ghcr.io`.

#### Scenario: Container OCI metadata and tagline
- **WHEN** the container image is published to GitHub Container Registry
- **THEN** the image metadata contains OCI labels with title "XCMailr", description "A small mailer for us but a big helper for testing", vendor "Xceptance Software Technologies GmbH", and documentation URL.

#### Scenario: Multi-architecture manifest discovery
- **WHEN** a client pulls the container image on an ARM64 host (such as Apple Silicon macOS) or an AMD64 host (such as x86_64 Linux or Windows)
- **THEN** Docker resolves the host architecture automatically from the multi-platform manifest list without requiring explicit platform flags.

