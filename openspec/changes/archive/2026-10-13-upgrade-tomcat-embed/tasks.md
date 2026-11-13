# Tasks

## 1. Dependency Configuration

- [x] 1.1 Add `<tomcat.version>11.0.25</tomcat.version>` property to root `pom.xml` and verify POM validity with `mvn validate`
- [x] 1.2 Run `mvn dependency:tree -Dincludes=org.apache.tomcat.embed` to verify all embedded Tomcat dependencies converge to version 11.0.25

## 2. Verification and Regression Testing

- [x] 2.1 Run unit tests in `xcmailr-webapp` with `mvn test -pl xcmailr-webapp` to verify web context and controllers initialize without error
- [x] 2.2 Run full test suite and integration tests with `mvn verify` (or `mvn test`) to confirm end-to-end API, mail processing, and security filter chains operate cleanly under Tomcat 11.0.25

