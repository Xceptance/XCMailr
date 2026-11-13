# Design

## Context

XCMailr modernizes inbound SMTP using SubEthaSMTP 7.2.2 running inside `SmtpServerService`. While `xcmailr.mbox.enable-tls: true` is configured by default, SubEtha's builder was initialized without an explicit `SSLContext`. Consequently, SubEtha delegates socket creation to `SSLSocketFactory.getDefault()`, which in standard Java server mode has no KeyManager or X.509 certificate configured. 

When a client requests `STARTTLS` (including `xcmailr-load-test-suite` tests which specify `smtp.requireStartTls = true`), Java's TLS engine fails with `javax.net.ssl.SSLHandshakeException: (handshake_failure)`.

See `proposal.md` for motivation and background.

## Goals / Non-Goals

**Goals:**
- **Zero Modifications to Load Test Suite**: Keep `xcmailr-load-test-suite` code, classes, and properties 100% untouched while ensuring its tests succeed against the modernized server.
- **Configurable Inbound TLS Keystore**: Introduce `xcmailr.mbox.ssl` properties in `XcmailrProperties` and align `application.yml`, `application-dev.yml`, and `application-test.yml` across environments.
- **Bundled Development Keystore**: Package a pre-generated, valid PKCS12 keystore (`classpath:keystore.p12`) containing a self-signed certificate for `localhost` and `xcmailr.test` to enable instant out-of-the-box local testing.
- **SubEtha SSLContext Integration**: Wire an initialized `SSLContext` into `SMTPServer.Builder.startTlsSocketFactory(sslContext)` in `SmtpServerService`.
- **Test Pyramid Coverage**: Implement unit tests (factory, properties, error cases), integration tests (STARTTLS happy path, opportunistic plaintext, mandatory TLS rejection), and system E2E tests (load test suite).
- **Production TLS Documentation**: Provide clear instructions in root `README.md` for production certificates (converting Let's Encrypt PEM files to PKCS12 via OpenSSL, environment variables, reverse proxy vs. native HTTPS, and `require-tls` configuration).
- **Load Test Execution Documentation**: Fully document prerequisites and command lines for `SendMail`, `CreateAndDeleteMailbox`, and `SendAndCheckMail` in `xcmailr-load-test-suite/README.md`.
- **Code Standards**: Maintain immutability with `final` modifiers on all parameters, local variables, and class fields, comprehensive Javadoc on public and protected APIs, and clear explanatory code comments throughout.

**Non-Goals:**
- Modifying any Java source files or configuration in `xcmailr-load-test-suite`.
- Building an in-application ACME / Let's Encrypt renewal client (production deployments handle certificate renewal externally via Certbot or Kubernetes cert-manager).
- Changing database schemas or user authentication flows.

## Understanding Opportunistic TLS vs. Mandatory TLS & Backward Compatibility

In SMTP protocol standards (RFC 3207 and RFC 5321):
- **Opportunistic TLS (`enable-tls: true`, `require-tls: false`)**:
  - The server advertises the `STARTTLS` extension capability in its `EHLO` response.
  - If the client supports TLS, it upgrades the transport via `STARTTLS` before transmitting email data.
  - If the client is a legacy or non-TLS sender (such as existing test scripts, automated health checks, or legacy systems), the server still accepts the email in plaintext.
  - **Why Backward Compatibility Matters**: In XCMailr, existing integration tests (like `ClientSmtpRoundtripIT.java`) and existing client automations connect over plaintext SMTP. If the server made TLS mandatory by default, all existing plaintext clients would immediately fail with `530 5.7.0 Must issue a STARTTLS command first`. Keeping opportunistic TLS as the default ensures 100% backward compatibility.
- **Mandatory TLS (`enable-tls: true`, `require-tls: true`)**:
  - Enforces that no mail transaction commands (`MAIL FROM:`, `RCPT TO:`, `DATA`) are accepted until the connection has been upgraded with `STARTTLS`.
  - Useful for hardened, high-security production deployments.

Testing both behaviors in our test pyramid ensures that we verify STARTTLS encryption works without inadvertently breaking plaintext senders.

## How `require-tls` is Configured in Production

In production environments, `require-tls` controls whether encryption is mandatory for inbound SMTP traffic. The configuration hierarchy is supported natively via Spring Boot's environment binding:

### 1. Configuration Mechanisms
- **Environment Variable (Standard for Containers & systemd)**:
  ```bash
  export MBOX_REQUIRE_TLS=true
  ```
  or via Spring Boot relaxed binding:
  ```bash
  export XCMAILR_MBOX_REQUIRE_TLS=true
  ```
- **CLI / JVM System Property**:
  ```bash
  java -jar xcmailr-webapp.jar --xcmailr.mbox.require-tls=true
  ```
  or:
  ```bash
  java -Dxcmailr.mbox.require-tls=true -jar xcmailr-webapp.jar
  ```
- **External Configuration File (`application.yml` in config directory or profile `prod`)**:
  ```yaml
  xcmailr:
    mbox:
      require-tls: true
  ```

### 2. Operational Impact of `require-tls` in Production
- **Standard Internet Inbound MX (`require-tls: false`) [Recommended]**:
  - Public MX servers on the Internet should almost always run with `require-tls: false` (opportunistic TLS).
  - External mail servers (Gmail, Outlook, Yahoo, SendGrid) will automatically encrypt with STARTTLS.
  - Senders that do not support modern cipher suites or TLS will not bounce; their mail is still accepted.
- **Restricted / Compliance Inbound (`require-tls: true`) [Enforced]**:
  - Used when XCMailr runs in a private corporate network, PCI-DSS / HIPAA regulated enclave, or dedicated internal testing gateway where plaintext mail is strictly forbidden.
  - SubEtha responds to unencrypted `MAIL FROM:` or `RCPT TO:` commands with `530 5.7.0 Must issue a STARTTLS command first`, forcing senders to encrypt before any sender/recipient information is transmitted.

## Environment Configuration Alignment

XCMailr utilizes Spring Boot profile configuration files:
1. **`xcmailr-webapp/src/main/resources/application.yml` (Base)**:
   - Declares the full `xcmailr.mbox.ssl` configuration block with environment variable bindings:
     ```yaml
     xcmailr:
       mbox:
         host: ${MBOX_HOST:xcmailr.test}
         port: ${MBOX_PORT:25000}
         enable-tls: ${MBOX_ENABLE_TLS:true}
         require-tls: ${MBOX_REQUIRE_TLS:false}
         ssl:
           key-store: ${MBOX_SSL_KEY_STORE:classpath:keystore.p12}
           key-store-password: ${MBOX_SSL_KEY_STORE_PASSWORD:topsecret}
           key-store-type: ${MBOX_SSL_KEY_STORE_TYPE:PKCS12}
           key-alias: ${MBOX_SSL_KEY_ALIAS:}
     ```
2. **`xcmailr-webapp/src/main/resources/application-dev.yml` (Development)**:
   - Inherits `xcmailr.mbox.ssl` from `application.yml`.
   - Adds clarifying documentation indicating that local dev uses `classpath:keystore.p12` for inbound SMTP alongside local Mailpit for outbound SMTP.
3. **`xcmailr-webapp/src/main/resources/application-test.yml` (Integration Testing)**:
   - Explicitly sets `xcmailr.mbox.port: 0` (ephemeral port allocation to avoid port collisions during parallel or repeated test runs).
   - Inherits `xcmailr.mbox.ssl.key-store: classpath:keystore.p12` so all test slices can perform TLS handshakes locally without external file dependencies.

## Test Pyramid Strategy

To ensure high quality, reliability, and regression protection, tests are structured according to the classic Test Pyramid:

```
                          ▲
                         / \
                        /   \
                       / E2E \   <- Load Test Suite: SendMail, CreateAndDeleteMailbox, SendAndCheckMail
                      /───────\
                     /  Integ  \ <- SmtpStartTlsIT: Happy Path (TLS), Opportunistic (Plain), Mandatory TLS
                    /───────────\
                   /    Unit     \ <- SmtpSslContextFactoryTest, XcmailrPropertiesTest (Happy, Error, Edge)
                  /───────────────\
```

### 1. Unit Tests (Isolated, Fast, No Sockets)
- **`XcmailrPropertiesTest`**:
  - Validates default property values for `SslProperties` (`classpath:keystore.p12`, `topsecret`, `PKCS12`).
  - Verifies getters, setters, and builder semantics with `final` guarantees.
- **`SmtpSslContextFactoryTest`**:
  - Extracts SSL context creation into a dedicated component/factory (`SmtpSslContextFactory`) to enable isolated unit testing.
  - *Happy Path*: Verifies that a valid PKCS12 resource produces an initialized, non-null `SSLContext` with TLS protocol.
  - *Error Case - Missing Keystore*: Verifies that an invalid/non-existent keystore path throws an explicit exception with a clear diagnostic message.
  - *Error Case - Bad Password*: Verifies that an incorrect keystore password throws an `IOException` / `UnrecoverableKeyException`.
  - *Special Case - Disabled TLS*: Verifies that if `enable-tls` is false, no keystore loading occurs.

### 2. Integration Tests (Spring Context, Real Sockets on Ephemeral Ports)
- **`SmtpStartTlsIT`**:
  - Runs in a Spring Boot integration test slice on an ephemeral port (`port: 0`).
  - *Happy Path*: Client connects, issues `STARTTLS`, completes the TLS handshake with SubEtha using `TrustAllTrustManager`, and delivers an encrypted message. Verifies mail is delivered and recorded.
  - *Special Case (Opportunistic TLS)*: With `enable-tls=true` and `require-tls=false`, an unencrypted client sends an email directly in plaintext without `STARTTLS`. Verifies successful delivery and backwards compatibility.
  - *Error / Special Case (Mandatory TLS)*: With `require-tls=true`, an unencrypted client attempts `MAIL FROM:` or `RCPT TO:` without `STARTTLS`. Verifies SubEtha rejects the command with `530 5.7.0 Must issue a STARTTLS command first`. Upgrading with `STARTTLS` subsequently succeeds.

### 3. End-to-End System Tests (Running Application, Real Scenarios)
- **`xcmailr-load-test-suite`**:
  - Executed against the running server without modifying any load test code:
    - `SendMail`: Inbound SMTP transmission with `smtp.requireStartTls=true`.
    - `CreateAndDeleteMailbox`: REST API mailbox provisioning and teardown.
    - `SendAndCheckMail`: Full roundtrip (REST mailbox creation $\rightarrow$ SMTP STARTTLS delivery $\rightarrow$ REST message verification $\rightarrow$ REST mailbox cleanup).

## Decisions

### 1. Keystore Format: Industry-Standard PKCS12 (`.p12`)
- **Decision**: Use PKCS12 as the default keystore type (`key-store-type: PKCS12`).
- **Rationale**: PKCS12 is the Java standard (default since Java 9) and the cross-platform standard for bundling certificates and private keys. Standard OpenSSL CLI commands can export directly to PKCS12.
- **Alternatives considered**:
  - Legacy JKS: Deprecated/proprietary Java format, issues compiler/tool warnings in modern JDKs.
  - Raw PEM loading in Java: Requires manual parser or third-party libraries (BouncyCastle); PKCS12 is natively supported by `KeyStore.getInstance("PKCS12")`.

### 2. Spring `ResourceLoader` for Keystore Resolution
- **Decision**: Inject Spring's `ResourceLoader` into the SSL context factory to resolve `xcmailr.mbox.ssl.key-store`.
- **Rationale**: Seamlessly supports both `classpath:keystore.p12` (for embedded development/testing) and `file:/etc/ssl/smtp-keystore.p12` (for production environments).
- **Alternatives considered**: `FileInputStream` directly (fails for classpath resources inside packaged JARs).

### 3. SubEtha Integration via `startTlsSocketFactory(SSLContext)`
- **Decision**: Initialize an `SSLContext` with `KeyManagerFactory` and pass it to SubEtha's `SMTPServer.Builder.startTlsSocketFactory(sslContext)`.
- **Rationale**: SubEtha 7.2.2 natively accepts `SSLContext` and wraps incoming plain client sockets into server-mode `SSLSocket` instances using this context during `STARTTLS` negotiation.

### 4. Modular `SmtpSslContextFactory`
- **Decision**: Extract `SSLContext` creation into a dedicated service/factory class `SmtpSslContextFactory`.
- **Rationale**: Decouples TLS engine initialization and certificate validation from the lifecycle of the SubEtha server. Enables clean unit testing of keystore parsing and error handling without spawning network threads.

### 5. Code Quality & Modifiers
- **Decision**: Enforce `final` modifiers on all method arguments, local variables, and class fields. Provide comprehensive Javadoc on all public/protected classes and methods. Add detailed inline code comments explaining TLS initialization and exception handling.

## Risks / Trade-offs

- **[Risk]** Self-signed certificate used in production by mistake.
  - **Mitigation**: Document clearly in `application.yml` and `README.md` that `classpath:keystore.p12` is for local development and testing only, and provide exact commands for configuring real certificates in production.
- **[Risk]** Port 25 requires elevated privileges on Linux production hosts.
  - **Mitigation**: Document Linux port binding options in `README.md` (`setcap 'cap_net_bind_service=+ep'`, authbind, or iptables port forwarding from 25 to 25000).
- **[Risk]** Static initializer in `xcmailr-load-test-suite` logs warning if `xcmailr.apiToken` is omitted.
  - **Mitigation**: Document in `xcmailr-load-test-suite/README.md` that passing `-Dxcmailr.apiToken=<token>` avoids this cosmetic warning even for `SendMail`.

