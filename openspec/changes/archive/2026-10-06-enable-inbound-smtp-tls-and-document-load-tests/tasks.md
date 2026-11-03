# Tasks

## 1. Keystore Asset & Configuration Setup Across Profiles

- [x] 1.1 Generate a self-signed PKCS12 development keystore at `xcmailr-webapp/src/main/resources/keystore.p12` with RSA key pair (CN=localhost, SAN=dns:localhost,dns:xcmailr.test, 10-year validity, password `topsecret`) and verify entry details using `keytool -list -v -keystore xcmailr-webapp/src/main/resources/keystore.p12 -storepass topsecret`.
- [x] 1.2 Add nested `SslProperties` (`keyStore`, `keyStorePassword`, `keyStoreType`, `keyAlias`) with full Javadoc, `final` parameters and fields, and defaults to `MboxProperties` in `XcmailrProperties.java`. Verify with `./mvnw test-compile -pl xcmailr-webapp`.
- [x] 1.3 Align configuration across environment profiles (`application.yml`, `application-dev.yml`, `application-test.yml`), adding `xcmailr.mbox.ssl` definitions with environment variable bindings (`MBOX_SSL_KEY_STORE`, `MBOX_SSL_KEY_STORE_PASSWORD`, `MBOX_ENABLE_TLS`, `MBOX_REQUIRE_TLS`, etc.) and ephemeral port preservation for tests. Verify with `./mvnw test-compile -pl xcmailr-webapp`.

## 2. Unit Testing (Test Pyramid Base)

- [x] 2.1 Implement `SmtpSslContextFactory.java` in `com.xceptance.xcmailr.services` to encapsulate `KeyStore` loading via Spring's `ResourceLoader` and `SSLContext` creation, using `final` modifiers, full Javadoc, and clean exception handling with detailed code comments. Verify compilation with `./mvnw test-compile -pl xcmailr-webapp`.
- [x] 2.2 Create unit tests in `XcmailrPropertiesTest.java` verifying `SslProperties` default values, custom setter behavior, and property binding using `final` variables. Verify with `./mvnw test -pl xcmailr-webapp -Dtest=XcmailrPropertiesTest`.
- [x] 2.3 Create unit tests in `SmtpSslContextFactoryTest.java` covering the happy path (valid PKCS12), error path (missing keystore resource), error path (wrong password), and special case (disabled TLS returns null), with full Javadoc and `final` modifiers. Verify with `./mvnw test -pl xcmailr-webapp -Dtest=SmtpSslContextFactoryTest`.

## 3. SubEtha Integration & Integration Testing (Test Pyramid Middle)

- [x] 3.1 Update `SmtpServerService.java` to inject `SmtpSslContextFactory`, construct the `SSLContext` when `enable-tls` is true, and configure `.startTlsSocketFactory(sslContext)` on SubEtha's `SMTPServer.Builder`, applying `final` modifiers and explanatory code comments. Verify with `./mvnw test-compile -pl xcmailr-webapp`.
- [x] 3.2 Implement integration test `SmtpStartTlsIT.java` with full Javadoc, `final` variables, and tests covering:
  - Happy path: client issues `STARTTLS`, completes TLS handshake, and delivers encrypted email.
  - Special case: client connects without `STARTTLS` and delivers in plaintext under opportunistic TLS (`require-tls=false`).
  - Error/special case: mandatory TLS (`require-tls=true`) rejects commands with SMTP 530 until `STARTTLS` is negotiated.
  Verify with `./mvnw test -pl xcmailr-webapp -Dtest=SmtpStartTlsIT`.

## 4. Documentation & E2E Verification (Test Pyramid Peak)

- [x] 4.1 Update root `README.md` with a comprehensive **Production Deployment & SSL/TLS Configuration** section explaining Web HTTPS (reverse proxy vs. embedded `server.ssl.*`), Inbound SMTP TLS (`xcmailr.mbox.ssl.*`), configuring `require-tls` via environment variables (`MBOX_REQUIRE_TLS=true`), converting Let's Encrypt PEM files to PKCS12 via `openssl pkcs12 -export`, and Linux port 25 binding considerations. Verify markdown formatting.
- [x] 4.2 Update `xcmailr-load-test-suite/README.md` with complete documentation for running each test scenario (`SendMail`, `CreateAndDeleteMailbox`, `SendAndCheckMail`), detailing required parameters, API token creation, and report generation in the XLT results browser. Verify documentation clarity and accuracy.
- [x] 4.3 Execute all three load test suite scenarios (`SendMail`, `CreateAndDeleteMailbox`, `SendAndCheckMail`) against the modernized server to verify end-to-end execution with `-Dsmtp.requireStartTls=true` while keeping `xcmailr-load-test-suite` code and configuration 100% untouched.

