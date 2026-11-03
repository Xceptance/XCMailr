# Proposal

## Why

XCMailr's inbound SMTP server advertises STARTTLS by default (`xcmailr.mbox.enable-tls: true`), but does not configure an `SSLContext` or KeyStore for the embedded SubEthaSMTP engine. As a result, when TLS-capable clients (such as external MTAs in production, or the existing `xcmailr-load-test-suite` running with default `smtp.requireStartTls=true`) initiate a `STARTTLS` handshake, Java's TLS engine fails with `javax.net.ssl.SSLHandshakeException: (handshake_failure)` because no server certificate is available.

Furthermore, there is currently no documented or configurable mechanism for system operators to provide real production certificates (such as Let's Encrypt or corporate CA certificates) for inbound SMTP, the development and test profile configurations (`application.yml`, `application-dev.yml`, `application-test.yml`) do not declare SSL settings or environment variable bindings for `enable-tls` and `require-tls`, and the manual execution workflow for the 3 load test suite scenarios (`SendMail`, `CreateAndDeleteMailbox`, `SendAndCheckMail`) is not fully documented.

This change enables inbound SMTP TLS with configurable KeyStore support, aligns environment profiles (`application.yml`, `application-dev.yml`, `application-test.yml`), bundles a default development keystore for seamless local development, establishes a comprehensive Test Pyramid (unit tests, integration tests for happy path and error/special cases, and end-to-end verification), provides production TLS deployment documentation, and documents load test suite execution without modifying any code or configuration in `xcmailr-load-test-suite`.

## What Changes

- **Inbound SMTP Keystore Configuration**: Add `xcmailr.mbox.ssl` configuration properties (`key-store`, `key-store-password`, `key-store-type`, `key-alias`) to `XcmailrProperties` and align `application.yml`, `application-dev.yml`, and `application-test.yml`, supporting environment variable overrides (`MBOX_SSL_KEY_STORE`, `MBOX_SSL_KEY_STORE_PASSWORD`, `MBOX_ENABLE_TLS`, `MBOX_REQUIRE_TLS`, etc.).
- **Bundled Development Keystore**: Add a self-signed PKCS12 development keystore (`classpath:keystore.p12`) to `xcmailr-webapp/src/main/resources/` for immediate out-of-the-box local development and automated testing.
- **SSLContext Integration in SmtpServerService**: Configure `SmtpServerService` to construct an `SSLContext` from the configured KeyStore using a dedicated `SmtpSslContextFactory` and supply it to SubEthaSMTP via `.startTlsSocketFactory(sslContext)` whenever `enable-tls` is active.
- **Code Standards**: Maintain immutability with `final` modifiers on all parameters, local variables, and class fields, comprehensive Javadoc on public and protected components, and detailed explanatory code comments.
- **Test Pyramid Verification**:
  - *Unit Tests*: Verify `SslProperties` binding/defaults and isolate `SSLContext` construction logic in `SmtpSslContextFactoryTest` across happy paths, missing keystore errors, wrong password errors, and disabled TLS scenarios.
  - *Integration Tests*: Multi-scenario `SmtpStartTlsIT` covering encrypted STARTTLS delivery (happy path), plaintext backwards compatibility under opportunistic TLS (special case), and command rejection when mandatory TLS is enforced (error/special case).
  - *End-to-End System Tests*: Verify the untouched `xcmailr-load-test-suite` tests (`SendMail`, `CreateAndDeleteMailbox`, `SendAndCheckMail`) succeed against the running server.
- **Production TLS Documentation**: Update root `README.md` with an end-to-end production deployment guide covering both Web HTTPS (reverse proxy vs. embedded `server.ssl.*`), Inbound SMTP STARTTLS (converting Let's Encrypt / PEM certificates to PKCS12 via OpenSSL), and configuring `require-tls` via environment variables (`MBOX_REQUIRE_TLS=true`).
- **Load Test Suite Execution Guide**: Document prerequisites (API token generation) and copy-pasteable execution commands for all three load test scenarios (`SendMail`, `CreateAndDeleteMailbox`, `SendAndCheckMail`) in `xcmailr-load-test-suite/README.md`.
- **Zero Modifications to Load Test Suite**: Keep `xcmailr-load-test-suite` Java source files and properties completely untouched, ensuring full compatibility with existing load test assets.

## Capabilities

### Modified Capabilities
- `inbound-smtp-engine`: Adds configurable TLS keystore management and certificate-backed STARTTLS session negotiation for inbound SMTP connections.

## Impact

- **Affected Code**: `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/config/XcmailrProperties.java`, `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/services/SmtpServerService.java`, new factory `com.xceptance.xcmailr.services.SmtpSslContextFactory`, configuration files (`application.yml`, `application-dev.yml`, `application-test.yml`).
- **New Resources**: `xcmailr-webapp/src/main/resources/keystore.p12` (bundled development keystore).
- **Tests Added**: Unit tests (`XcmailrPropertiesTest`, `SmtpSslContextFactoryTest`) and integration tests (`SmtpStartTlsIT`).
- **Documentation**: `README.md` (root production TLS guide), `xcmailr-load-test-suite/README.md` (load testing execution).
- **Compatibility**: 100% backward compatible. Plaintext SMTP remains fully supported when `require-tls` is false.

