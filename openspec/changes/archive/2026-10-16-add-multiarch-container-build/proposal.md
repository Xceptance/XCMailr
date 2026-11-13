# Proposal

## Why

Currently, the GitHub Actions container publishing workflow builds container images exclusively for the runner's native architecture (`linux/amd64`). When developers and automated testing environments on ARM64 architectures (such as Apple Silicon macOS M1/M2/M3/M4, Windows on ARM, or AWS Graviton Linux instances) attempt to pull `ghcr.io/xceptance/xcmailr:latest`, Docker fails with `no matching manifest for linux/arm64/v8 in the manifest list entries` unless emulation is explicitly forced. Adding multi-architecture image packaging ensures that containers run natively and performantly across both x86_64 and ARM64 platforms out of the box.

## What Changes

- Configure QEMU emulation and Docker Buildx in `.github/workflows/build-container.yml` to support multi-platform builds.
- Configure `docker/build-push-action` to build and publish multi-arch manifest lists targeting `linux/amd64` and `linux/arm64`.
- Update container quickstart and deployment documentation to note out-of-the-box support for both AMD64 and ARM64 architectures.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `containerized-deployment`: Update the "Automated container publishing" requirement to specify that container publishing builds and publishes multi-architecture manifest lists supporting `linux/amd64` and `linux/arm64`.

## Impact

- **CI Workflow**: `.github/workflows/build-container.yml` execution time will slightly increase due to cross-platform building via QEMU for ARM64, while leveraging GitHub Actions layer caching.
- **Registries**: `ghcr.io/xceptance/xcmailr` manifests will now be OCI multi-platform manifest lists (fat manifests).
- **Client compatibility**: Full transparent native compatibility on Apple Silicon Macs, Windows x86/ARM via WSL2, and Linux AMD64/ARM64 servers without `--platform` workarounds.

