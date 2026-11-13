# Tasks

## 1. Project Version Alignment to 4.0.0

- [x] 1.1 Update root `pom.xml` version to `4.0.0` and update parent version references in `xcmailr-webapp/pom.xml`, `xcmailr-client/pom.xml`, and `xcmailr-load-test-suite/pom.xml`, verifying consistency with `mvn validate`
- [x] 1.2 Update all version occurrences from `3.1.0` to `4.0.0` across `readme.md` and `docs/production-deployment-guide.md`, verifying with `grep -rn "3.1.0" .` that no stale version strings remain

## 2. CI/CD Container Tagging and OCI Metadata

- [x] 2.1 Update `.github/workflows/build-container.yml` to extract the Maven POM version and generate container tags for `<version>`, `<major>.<minor>`, `latest`, and short Git SHA
- [x] 2.2 Configure explicit OCI metadata labels in `.github/workflows/build-container.yml` preserving the authentic subtitle `"A small mailer for us but a big helper for testing"`, vendor, and cloud deployment guide documentation link

## 3. Docker Compose Stack Naming and Quickstart Documentation

- [x] 3.1 Add top-level `name: xcmailr-test-environment` to `docker-compose.test.yml` and verify validity with `docker compose -f docker-compose.test.yml config`
- [x] 3.2 Add a prominent Container Quickstart section near the top of `readme.md` detailing ports `8080` and `25000`/`2525`, default credentials/tokens, and links to deployment guides

