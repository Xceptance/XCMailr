# Data Compatibility & Safety Guarantees

This document details the architectural safeguards and guarantees ensuring that existing production data—including registered users, password credentials, mailboxes, received emails, domains, and audit transactions—remain completely intact, usable, and unharmed when migrating from legacy XCMailr to the modernized Spring Boot application.

---

## 1. Safety Architecture Overview

XCMailr enforces multi-layer data protection designed specifically for zero-data-loss, in-place database modernization:

```
+-----------------------------------------------------------------------------------+
|                            Database Safety Layers                                 |
+-----------------------------------------------------------------------------------+
|  1. Hibernate DDL Safety     -->  spring.jpa.hibernate.ddl-auto: validate         |
|                                   Strict validation only: NO create/alter/drop.   |
|                                                                                   |
|  2. Flyway Migration Match   -->  baseline-on-migrate: true, baseline-version: 0  |
|                                   Identical V1-V3 history; 0 new migrations run.  |
|                                                                                   |
|  3. 1:1 Entity Mappings      -->  Exact table names, column types & sequences     |
|                                   No renamed tables, dropped fields, or schema drift|
|                                                                                   |
|  4. Auth Hash Compatibility  -->  Spring Security BCrypt matches legacy jBCrypt   |
|                                   Existing users log in with original passwords.  |
|                                                                                   |
|  5. Non-Destructive Boot     -->  AdminUserInitializer skips existing accounts    |
|                                   No overwrite of admin passwords or permissions. |
|                                                                                   |
|  6. Retention Isolation      -->  Scheduled cleanups only touch expired artifacts |
|                                   Active registered users are NEVER deleted.      |
+-----------------------------------------------------------------------------------+
```

---

## 2. Zero DDL Mutation (`ddl-auto: validate`)

In [`xcmailr-webapp/src/main/resources/application.yml`](../xcmailr-webapp/src/main/resources/application.yml), Hibernate DDL management is explicitly configured to `validate`:

```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
```

### Why this guarantees safety:
- **No Schema Alterations**: Hibernate will **never** issue `CREATE TABLE`, `ALTER TABLE`, `DROP TABLE`, or `DROP CONSTRAINT` against the production database.
- **Fail-Safe Startup Verification**: During application initialization, Hibernate inspects the existing database catalog and validates that table names, column types, and foreign key definitions match the JPA entity classes. If any discrepancy or missing column is detected, Spring Boot aborts startup immediately with a `SchemaManagementException` rather than executing destructive alterations.

---

## 3. Flyway Migration Idempotency & Baseline Compatibility

The database schema in legacy XCMailr was managed via Flyway starting with version 1.0 through 3.0. The modernized application carries the exact same migration scripts in [`xcmailr-webapp/src/main/resources/db/migration/`](../xcmailr-webapp/src/main/resources/db/migration/):

- `V1__Initial_Setup.sql`: Defines tables (`users`, `mailboxes`, `mail`, `register_domains`, `mailtransactions`, `MAIL_STATISTICS`) and sequences.
- `V2__Change_Mail_Message_Type.sql`: Standardizes `mail.message` storage to `BLOB`.
- `V3__Remove_Mail2MailBox_Reference.sql`: Removes foreign key from `mail` to `mailboxes` to allow independent mailbox lifecycles.

### Flyway Configuration:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
```

### Protection on an existing production database:
1. **Existing Flyway History**: Production databases that already contain a `flyway_schema_history` table at version 3 will be recognized as up-to-date. Flyway executes **zero** SQL statements.
2. **Idempotent Statements**: All statements in `V1__Initial_Setup.sql` employ `IF NOT EXISTS` syntax (`CREATE TABLE IF NOT EXISTS ...`, `CREATE SEQUENCE IF NOT EXISTS ...`). Even if executed against an un-baselined existing database, no tables or existing rows are overwritten.

---

## 4. 1:1 Entity and Table Schema Alignment

All domain entities in `models.*` maintain strict 1:1 compatibility with the legacy database tables and columns:

| Entity | Target Table | Primary Columns Preserved | Notes |
| :--- | :--- | :--- | :--- |
| [`models.User`](../xcmailr-webapp/src/main/java/models/User.java) | `users` | `id`, `forename`, `surname`, `mail`, `passwd`, `admin`, `active`, `confirmation`, `ts_confirm`, `bad_pw_count`, `language`, `apitoken`, `api_token_creation_timestamp` | Full legacy account lifecycle preserved. |
| [`models.MBox`](../xcmailr-webapp/src/main/java/models/MBox.java) | `mailboxes` | `id`, `address`, `ts_active`, `expired`, `domain`, `forwards`, `suppressions`, `usr_id`, `forward_emails`, `version` | Optimistic locking (`version`) and user foreign key maintained. |
| [`models.Mail`](../xcmailr-webapp/src/main/java/models/Mail.java) | `mail` | `id`, `sender`, `subject`, `receive_time`, `message` (BLOB), `mailbox_id`, `uuid` | MIME payloads stored as raw byte arrays. |
| [`models.Domain`](../xcmailr-webapp/src/main/java/models/Domain.java) | `register_domains` | `id`, `domainname` | Allowed domain whitelist table. |
| [`models.MailTransaction`](../xcmailr-webapp/src/main/java/models/MailTransaction.java) | `mailtransactions` | `id`, `ts`, `status`, `sourceaddr`, `relayaddr`, `targetaddr` | Transaction history table. |
| [`models.MailStatistics`](../xcmailr-webapp/src/main/java/models/MailStatistics.java) | `MAIL_STATISTICS` | `date`, `QUARTER_HOUR`, `FROM_DOMAIN`, `TARGET_DOMAIN`, `DROP_COUNT`, `FORWARD_COUNT` | Aggregate metrics table. |

### Primary Key Sequences
Primary key generation uses the existing sequence objects in the database (`users_seq`, `mailboxes_seq`, `mail_seq`, `register_domains_seq`, `mailtransactions_seq`). The application picks up the current sequence values, ensuring no ID collisions with existing records.

---

## 5. Password Authentication Compatibility

Legacy XCMailr hashed passwords using standard `jBCrypt` with `$2a$` salt prefixes.

The modernized application configures Spring Security with [`BCryptPasswordEncoder`](../xcmailr-webapp/src/main/java/com/xceptance/xcmailr/security/SecurityConfig.java) and [`PasswordFirstAuthenticationProvider`](../xcmailr-webapp/src/main/java/com/xceptance/xcmailr/security/PasswordFirstAuthenticationProvider.java).

- **Direct Hash Match**: Spring's `BCryptPasswordEncoder` natively validates standard `$2a$`, `$2b$`, and `$2y$` hashes stored in `users.passwd`.
- **Zero Friction**: Existing users can log in immediately with their existing passwords. No password reset, migration script, or re-hashing step is required.

---

## 6. Non-Destructive Administrator Bootstrapping

The application includes an automated bootstrap component ([`AdminUserInitializer`](../xcmailr-webapp/src/main/java/com/xceptance/xcmailr/config/AdminUserInitializer.java)) to simplify fresh deployments.

On startup against an existing database, it enforces a strictly non-destructive policy:
```java
final Optional<User> existingUserOpt = userRepository.findByMailIgnoreCase(normalizedMail);
if (existingUserOpt.isPresent())
{
    LOG.debug("Administrator account '{}' already exists. Leaving credentials and permissions untouched.", normalizedMail);
    return;
}
```

- If an account with the configured administrator email exists, **the initializer does nothing**.
- It will **never** overwrite passwords, modify roles, or reset account attributes for existing users.

---

## 7. Background Cleanup & Retention Boundaries

The background maintenance scheduler ([`ExpirationScheduledService`](../xcmailr-webapp/src/main/java/com/xceptance/xcmailr/services/ExpirationScheduledService.java)) performs routine maintenance tasks with strict safety constraints:

| Task | Target Records | Boundary / Safety Rule |
| :--- | :--- | :--- |
| **Mailbox Expiration** | `mailboxes` | Only flags boxes where `ts_active <= now` and `expired == false`. Marks `expired = true` and `active = false`. No rows deleted. |
| **Mail Retention Purge** | `mail` | Only deletes emails where `receive_time < (now - retentionPeriod)`. Default retention is preserved from configuration. |
| **API Token Expiry** | `users` | Only clears `apitoken` where `api_token_creation_timestamp < (now - tokenExpirationDays)`. User account remains active. |
| **Unconfirmed User Purge** | `users` | **Only deletes unactivated signups**: `active = false AND confirmation IS NOT NULL AND ts_confirm < cutoff`. Active registered users (`active = true`) are **never** queried or deleted. |
| **Transaction Rollup** | `mailtransactions` | Aggregates entries older than `max-age-hours` into `MAIL_STATISTICS`. |

---

## Summary of Guarantees

1. Existing users, passwords, and permissions remain 100% valid and usable.
2. Existing mailboxes and their forwarding rules continue to function without modification.
3. The database schema cannot be altered by Hibernate on boot.
4. Existing sequences ensure uninterrupted unique ID generation.

