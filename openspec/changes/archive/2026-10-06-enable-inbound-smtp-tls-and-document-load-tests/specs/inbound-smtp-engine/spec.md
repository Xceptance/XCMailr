# Spec Delta

## ADDED Requirements

### Requirement: Inbound SMTP TLS Keystore and Certificate Management
The system SHALL support loading server TLS certificates from configurable PKCS12 or JKS keystores to authenticate the server during STARTTLS connection negotiation.

#### Scenario: Default Bundled Development Keystore
- **WHEN** inbound SMTP TLS is enabled and no external keystore path is configured
- **THEN** the system loads the bundled development keystore from the classpath and completes the STARTTLS handshake

#### Scenario: External Production Keystore Configuration
- **WHEN** an external keystore file path and password are configured
- **THEN** the system loads the specified server certificate and presents it to connecting SMTP clients during STARTTLS negotiation

#### Scenario: Opportunistic Plaintext Delivery
- **WHEN** inbound SMTP TLS is enabled with opportunistic mode (`require-tls = false`) and an unencrypted client initiates a plaintext session
- **THEN** the system completes the session and processes incoming mail without requiring a STARTTLS upgrade

#### Scenario: Mandatory TLS Enforcement
- **WHEN** inbound SMTP TLS is configured as mandatory (`require-tls = true`) and an unencrypted client attempts mail transaction commands without upgrading via STARTTLS
- **THEN** the system rejects commands requiring encryption with an SMTP 530 error until STARTTLS is negotiated

#### Scenario: Invalid or Missing Keystore Handling
- **WHEN** inbound SMTP TLS is enabled but the configured keystore path is invalid or the password is wrong
- **THEN** the system logs a descriptive diagnostic error and aborts startup or prevents invalid TLS advertising
