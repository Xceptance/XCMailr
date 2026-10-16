# Design

## Context

See `proposal.md` for background and motivation.

Currently, [`.github/workflows/build-container.yml`](file:///Users/rbaumgarten/checkouts/XCMailr/.github/workflows/build-container.yml) runs on `ubuntu-latest` (an x86_64 runner) and uses `docker/setup-buildx-action@v3` and `docker/build-push-action@v6` without specifying target platforms. Consequently, Docker Buildx builds solely for the runner's native architecture (`linux/amd64`).

The base images used in [`Dockerfile`](file:///Users/rbaumgarten/checkouts/XCMailr/Dockerfile) (`eclipse-temurin:25-jdk` and `eclipse-temurin:25-jre`) are already multi-architecture official images with published layers for both `linux/amd64` and `linux/arm64`.

## Goals / Non-Goals

**Goals:**
- Configure the GitHub Actions workflow to build and publish multi-platform container images for both `linux/amd64` and `linux/arm64`.
- Publish an OCI Multi-Platform Manifest List (fat manifest) under `ghcr.io/xceptance/xcmailr:<tag>`.
- Allow Apple Silicon Macs, Windows on ARM (via WSL2), and 64-bit ARM Linux servers to pull and run the container natively without requiring `--platform linux/amd64` flags.
- Update project documentation to reflect multi-architecture support.

**Non-Goals:**
- Building native Windows containers (`windows/amd64`). Developers and CI on Windows run Linux containers via WSL 2, which natively executes `linux/amd64` (and `linux/arm64` on ARM Windows).
- Setting up dedicated ARM runners in GitHub Actions; QEMU emulation is sufficient for the build step.

## Decisions

### Decision 1: Install QEMU via `docker/setup-qemu-action@v3`
- **Choice**: Add `docker/setup-qemu-action@v3` prior to `docker/setup-buildx-action@v3` in `.github/workflows/build-container.yml`.
- **Rationale**: `setup-qemu-action` registers binfmt handlers for QEMU, enabling Buildx to execute ARM64 instructions during container build steps on an x86 runner.
- **Alternatives Considered**: 
  - *Dedicated ARM64 runners*: Requires custom runner infrastructure or GitHub paid ARM runners; unnecessary given the lightweight Java build.

### Decision 2: Specify `platforms: linux/amd64,linux/arm64` in `docker/build-push-action@v6`
- **Choice**: Pass `platforms: linux/amd64,linux/arm64` to `docker/build-push-action@v6`.
- **Rationale**: Generates and pushes both architecture layers along with an OCI multi-platform manifest list for all tagged releases (`latest`, semantic versions, Git SHA).
- **Alternatives Considered**: 
  - *Building separate image tags (e.g. `:latest-arm64`)*: Clunky for users; requires manual image selection. Multi-arch manifest lists allow automatic client detection.

### Decision 3: Document platform support and verification
- **Choice**: Mention supported architectures in `readme.md` and `docs/cloud-test-deployment-guide.md`.
- **Rationale**: Informs developers and testers that Docker on Apple Silicon or ARM cloud hosts works out of the box.

## Risks / Trade-offs

- **[Build Time Increase via QEMU]** $\rightarrow$ *Mitigation*: QEMU emulation can be slower for CPU-intensive compilation steps. However, XCMailr's Maven build compiles in under 2 minutes, and Docker Buildx leverages GitHub Actions layer caching (`type=gha`) to avoid rebuilding unchanged layers across CI runs.

