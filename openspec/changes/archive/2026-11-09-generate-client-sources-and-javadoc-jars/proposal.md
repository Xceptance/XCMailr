# Proposal

## Why

During modernizing XCMailr to Java 25, the Maven build configuration for the `xcmailr-client` library was unintentionally altered in commit `b6aad76` by adding `<source.skip>true</source.skip>` and removing the `<executions>` blocks that bound `maven-source-plugin` and `maven-javadoc-plugin` to the `package` phase. Consequently, executing `mvn package` on `xcmailr-client` (or during reactor builds) generates only the primary binary artifact `xcmailr-client-4.0.0.jar`, omitting both `xcmailr-client-4.0.0-sources.jar` and `xcmailr-client-4.0.0-javadoc.jar`.

`xcmailr-client` is a reusable, standalone client library consumed by test frameworks and external integration suites, and configured for publication to OSSRH / Maven Central. Publishing to Maven Central requires both sources and javadoc JARs, and IDE users rely on them for source navigation and API documentation. Restoring automatic packaging of these artifacts and resolving latent Javadoc warnings ensures seamless developer adoption and compliance with publication standards.

## What Changes

- **Restore Sources JAR Packaging**: Remove `<source.skip>true</source.skip>` from `<properties>` in `xcmailr-client/pom.xml` and add an execution for `maven-source-plugin` using goal `jar-no-fork` bound to phase `package`.
- **Restore Javadoc JAR Packaging**: Add an execution for `maven-javadoc-plugin` using goal `jar` bound to phase `package` in `xcmailr-client/pom.xml`.
- **Clean Up Javadoc Warnings**: Address missing `@param`, `@return`, `@throws`, and constructor comments across the 14 source files in `xcmailr-client` to produce clean, warning-free API documentation.
- **Enforce Code Conventions**: Ensure all modified source files adhere to project standards: complete Javadoc, informative inline comments, and `final` modifiers on parameters and local variables.

## Capabilities

### New Capabilities

None. This change alters build packaging and code documentation for `xcmailr-client` without introducing new system capabilities (`skip_specs: true`).

### Modified Capabilities

None. System behavior and REST API interactions are unchanged (`skip_specs: true`).

## Impact

- **Affected Code**: `xcmailr-client/pom.xml` and Javadoc annotations in `xcmailr-client/src/main/java/xcmailr/client/**/*.java`.
- **Build Output**: Running `mvn package -pl xcmailr-client` will produce three JAR artifacts in `xcmailr-client/target/`:
  - `xcmailr-client-4.0.0.jar`
  - `xcmailr-client-4.0.0-sources.jar`
  - `xcmailr-client-4.0.0-javadoc.jar`
- **Other Modules**: Zero impact on `xcmailr-webapp` or `xcmailr-load-test-suite`.
- **API Compatibility**: 100% backward compatible; no public method signatures or behaviors are altered.
