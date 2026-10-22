# Proposal

## Why

GitHub Socket Security alerts flag `org.apache.tomcat.embed:tomcat-embed-core` version `11.0.24` (pulled transitively via `spring-boot-dependencies:4.1.1`) with three Critical CVEs:
1. **GHSA-h3x4-894j-xpx5 / CVE-2026-68525** (Critical, CWE-863): Incorrect Authorization in Tomcat's container `FormAuthenticator`.
2. **CVE-2026-43512** (Critical, CWE-592): Authentication Bypass in Tomcat's `DigestAuthenticator`.
3. **CVE-2026-65182** (Critical, CWE-285): Improper Access Control / Path Ordering Bypass in servlet security constraints.

Although XCMailr's defense-in-depth architecture delegates all authentication and authorization to Spring Security filter chains rather than container authenticators, bundling vulnerable embedded JARs triggers automated CI/CD policy alerts and violates compliance standards. Upgrading embedded Tomcat to patched version `11.0.25` resolves all three Critical CVEs.

## What Changes

- Add `<tomcat.version>11.0.25</tomcat.version>` property override in the parent `pom.xml` to align all embedded Tomcat dependencies (`tomcat-embed-core`, `tomcat-embed-el`, `tomcat-embed-websocket`) with the patched release.
- Verify dependency convergence with `mvn dependency:tree` confirming `11.0.25` is active across modules.
- Verify full integration and unit test suites pass without regression under Tomcat `11.0.25`.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `security-hardening`: Add requirement for secure embedded servlet container baseline remediating known critical container vulnerabilities.

## Impact

- **Dependencies**: Upgrades `org.apache.tomcat.embed:tomcat-embed-core`, `tomcat-embed-el`, and `tomcat-embed-websocket` from `11.0.24` to `11.0.25`.
- **Runtime**: Zero API or UI breaking changes; existing Spring Security filter chains and endpoints continue normal operation.
- **Compliance & CI**: Clears Socket Security critical dependency alerts on pull requests.

