# Spec Delta

## MODIFIED Requirements

### Requirement: Admin Management Console
The system SHALL provide administrative interfaces for managing user accounts, reviewing transaction logs, analyzing traffic metrics, and maintaining the domain whitelist.

#### Scenario: Domain Whitelist Configuration
- **WHEN** an administrator adds or removes an allowed domain via the admin console
- **THEN** the system updates the registered domain table and enforces the change on the inbound SMTP listener immediately

#### Scenario: Paged Transaction Auditing
- **WHEN** an administrator inspects the mail transaction log
- **THEN** the system displays paginated records with filtering by timestamp, status, and address

#### Scenario: Default Domain Whitelist Seeding on Startup
- **WHEN** the application starts up against a fresh or migrated legacy database
- **THEN** the system ensures the administrator's default domain is present in the registered domain whitelist if absent, ensuring that mailbox creation domain options are available
