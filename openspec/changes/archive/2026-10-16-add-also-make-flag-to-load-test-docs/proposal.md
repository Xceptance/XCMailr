# Proposal

## Why

When building or executing the load test suite via Maven using project-isolation flags (`-pl xcmailr-load-test-suite`), Maven fails to resolve the local reactor dependency `com.xceptance:xcmailr-client:4.0.0` with `DependencyResolutionException` because `xcmailr-client` version 4.0.0 is not published to Maven Central. Without the `--also-make` (`-am`) flag, Maven excludes upstream reactor dependencies from the build lifecycle. Updating the documentation ensures smooth out-of-the-box local and CI execution for testers.

## What Changes

- Add `-am` (`--also-make`) to the build and packaging command in `docs/load-testing-guide.md`:
  `mvn clean test-compile package -pl xcmailr-load-test-suite -am`
- Add `-am` to the troubleshooting classpath conflict rebuild snippet in `docs/load-testing-guide.md`:
  `mvn clean package -pl xcmailr-load-test-suite -am`
- Add `-am` to the standalone test execution commands across all three scenarios (`SendMail`, `CreateAndDeleteMailbox`, `SendAndCheckMail`) in `xcmailr-load-test-suite/README.md`:
  `mvn test -pl xcmailr-load-test-suite -am ...`

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None. (This is a documentation-only fix; `skip_specs: true` is configured in `.openspec.yaml`.)

## Impact

- **Documentation**:
  - `docs/load-testing-guide.md`
  - `xcmailr-load-test-suite/README.md`
- **Code & Runtime**:
  - Zero application code, test code, or dependency POM modifications.

