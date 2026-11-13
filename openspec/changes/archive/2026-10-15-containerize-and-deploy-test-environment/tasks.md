# Tasks

## 1. Container Packaging

- [x] 1.1 Create `.dockerignore` to exclude local build artifacts (`target/`), `.git/`, IDE configurations, and temporary directories from build context and verify file presence.
- [x] 1.2 Create multi-stage `Dockerfile` using `eclipse-temurin:25-jdk` builder and `eclipse-temurin:25-jre` runtime with non-root user `xcmailr` (UID 10001), `/data` volume, and entrypoint running the fat JAR. Verify Dockerfile syntax and structure.
- [x] 1.3 Verify container configuration compatibility with environment variables (`PORT`, `MBOX_PORT`, `ADMIN_API_TOKEN`, `SPRING_DATASOURCE_URL`, `APP_URL`).
- [x] 1.4 Create `docker-compose.test.yml` defining the test environment stack with XCMailr and Mailpit (`axllent/mailpit:latest`), pre-configured outbound SMTP relay (`mailpit:1025`), published ports (8080, 25000/2525, 8025), and persistent volume mounts. Verify compose YAML syntax and service definitions.

## 2. CI/CD Pipeline Automation

- [x] 2.1 Create `.github/workflows/build-container.yml` triggered on push to `develop`/`modernizeXCMailr` and `workflow_dispatch`, building and publishing container images to GitHub Container Registry (`ghcr.io/xceptance/xcmailr`) using `GITHUB_TOKEN`. Verify workflow YAML syntax and step structure.

## 3. Documentation & Guides

- [x] 3.1 Create `docs/cloud-test-deployment-guide.md` with step-by-step instructions for launching an ephemeral Google Cloud VM, opening firewall ports, running the test stack via `docker run` or `docker compose -f docker-compose.test.yml up -d`, pre-creating `jw@xcmailr.test`, and cleanly terminating the VM.
- [x] 3.2 Add custom port configuration instructions (`MBOX_PORT=2525`) and XLT test suite alignment (`xcmailr.host` and `smtp.port` in `xcmailr-load-test-suite/config/project.properties`) to `docs/cloud-test-deployment-guide.md`.
- [x] 3.3 Add the dedicated section "Inspecting Forwarded & Outbound Emails with Mailpit" to `docs/cloud-test-deployment-guide.md` documenting port 8025 access, outbound SMTP environment variables, and verification curl commands.
- [x] 3.4 Update `readme.md` to add "Option 3: Run via Docker & Docker Compose" under Running the Application and add a reference to `docs/cloud-test-deployment-guide.md`.

## 4. Verification

- [x] 4.1 Validate OpenSpec change compliance by running `openspec validate containerize-and-deploy-test-environment --strict` and verifying zero validation errors.

