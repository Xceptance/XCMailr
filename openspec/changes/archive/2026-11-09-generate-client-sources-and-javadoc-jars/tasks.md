# Tasks

## 1. Maven Build Configuration

- [x] 1.1 Remove `<source.skip>true</source.skip>` from `<properties>` in `xcmailr-client/pom.xml` and verify via `mvn help:effective-pom -pl xcmailr-client` that `source.skip` is false or unset.
- [x] 1.2 Add execution `attach-sources` (goal `jar-no-fork`, phase `package`) to `maven-source-plugin` in `xcmailr-client/pom.xml` and verify `mvn package -pl xcmailr-client -DskipTests` generates `xcmailr-client/target/xcmailr-client-4.0.0-sources.jar`.
- [x] 1.3 Add execution `attach-javadocs` (goal `jar`, phase `package`) to `maven-javadoc-plugin` in `xcmailr-client/pom.xml` and verify `mvn package -pl xcmailr-client -DskipTests` generates `xcmailr-client/target/xcmailr-client-4.0.0-javadoc.jar`.

## 2. Javadoc Quality and Remediation

- [x] 2.1 Remediate missing Javadoc tags and constructor comments in client model classes (`MailFilterOptions.java`, `Attachment.java`, and `Mail.java`) and verify doclint improvements via `mvn javadoc:javadoc -pl xcmailr-client`.
- [x] 2.2 Remediate missing Javadoc tags and comments in client implementation classes (`RestApiClient.java`, `AbstractApiImpl.java`, and `Utils.java`) and verify `mvn javadoc:javadoc -pl xcmailr-client` emits zero warnings.

## 3. Build Verification and Integration Checks

- [x] 3.1 Execute `mvn clean package -pl xcmailr-client` and verify all three JAR artifacts (`xcmailr-client-4.0.0.jar`, `xcmailr-client-4.0.0-sources.jar`, and `xcmailr-client-4.0.0-javadoc.jar`) are created in `xcmailr-client/target/`.
- [x] 3.2 Inspect the contents of `xcmailr-client-4.0.0-sources.jar` using `jar -tf` and verify all client Java source files are packaged.
- [x] 3.3 Inspect the contents of `xcmailr-client-4.0.0-javadoc.jar` using `jar -tf` and verify generated HTML documentation files are packaged.
- [x] 3.4 Execute full reactor build `mvn clean test` across all modules to verify zero regressions.
