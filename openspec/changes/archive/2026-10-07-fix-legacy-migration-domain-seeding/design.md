# Design

## Context

See `proposal.md` for background and motivation.

Currently, `AdminUserInitializer.java` implements Spring Boot's `ApplicationRunner` at `@Order(10)`. Its `run` method performs four sequential steps:
1. Validates configured administrator email and password.
2. Checks if the administrator account already exists. If found, it optionally sets the initial API token, logs a debug message, and executes `return;`.
3. Creates the missing administrator account if absent.
4. Calls `seedDefaultDomain(normalizedMail)`.

Because Step 2 executes `return;`, Step 4 is never reached when migrating an existing database with a pre-existing administrator. `seedDefaultDomain()` already contains an idempotent existence check (`!domainRepository.existsByDomainnameIgnoreCase(domainPart)`), making it completely safe to run regardless of whether the administrator account was newly created or already present.

## Goals / Non-Goals

**Goals:**
- Ensure the administrator's email domain (e.g., `xcmailr.test`) is seeded into `register_domains` on startup if absent, even when the administrator account already exists in the database.
- Keep the check strictly within application startup (`ApplicationRunner`), introducing zero overhead to HTTP requests, mailbox operations, or SMTP traffic.
- Update `docs/legacy-migration-local-test-guide.md` to accurately document the Flyway logs for both initial migration cutover and subsequent restarts.

**Non-Goals:**
- Dynamically merging `application.yml` domains in `MailboxWebController` (the database table `register_domains` remains the authoritative source of truth for the web UI whitelist).
- Hardcoding domain insertions in SQL / Flyway migrations (production environments use environment-specific domain configurations).

## Decisions

### Decision 1: Relocate `seedDefaultDomain` Execution Before Returning

In `AdminUserInitializer.java`, `seedDefaultDomain(normalizedMail)` will be invoked before returning in Step 2:

```java
if (existingUserOpt.isPresent())
{
    final User existing = existingUserOpt.get();
    if (!StringUtils.hasText(existing.getApiToken()) && StringUtils.hasText(adminApiToken))
    {
        existing.setApiToken(adminApiToken.trim());
        existing.setApiTokenCreationTimestamp(System.currentTimeMillis());
        userRepository.save(existing);
        LOG.info("Configured initial API token for existing administrator account '{}'", normalizedMail);
    }
    LOG.debug("Administrator account '{}' already exists. Leaving credentials and permissions untouched.", normalizedMail);
    seedDefaultDomain(normalizedMail);
    return;
}
```

*Rationale*:
- Reuses existing, tested logic in `seedDefaultDomain`.
- Safe and idempotent: if the domain already exists, `existsByDomainnameIgnoreCase` returns `true` and skips inserting.
- Guarantees that migrated databases immediately have the admin's domain available in the web UI dropdown without manual intervention.

*Alternatives Considered*:
- *Hardcoding in Flyway migration script*: Rejected because production environments may operate under different domain names than `xcmailr.test`.
- *Querying `xcmailr.mbox.domain-list` dynamically in `MailboxWebController`*: Rejected because it bypasses the admin whitelist management model and creates split authority between the database and configuration files.

### Decision 2: Update Migration Runbook to Detail First-Time vs Subsequent Flyway Logs

In `docs/legacy-migration-local-test-guide.md`, Checkpoint 1 will clearly distinguish between:
1. **Initial Migration Cutover**:
   - `Schema history table "PUBLIC"."flyway_schema_history" does not exist yet`
   - `Creating Schema History table ... with baseline ... Successfully baselined schema with version: 0`
   - `Migrating schema "PUBLIC" to version "1 - Initial Setup"`, `"2 - ..."`, `"3 - ..."`
   - `Successfully applied 3 migrations to schema "PUBLIC", now at version v3`
2. **Subsequent Application Restarts**:
   - `Current version of schema "PUBLIC": 3`
   - `Schema "PUBLIC" is up to date. No migration necessary.`

## Risks / Trade-offs

- **[Risk] Extra query on application startup** → *Mitigation*: The check runs once at boot in `ApplicationRunner` (`existsByDomainnameIgnoreCase`). The `domainname` column is indexed/unique, taking sub-millisecond execution time and having 0% impact on runtime web requests.
- **[Risk] Unexpected domain whitelist addition in production** → *Mitigation*: It strictly seeds the domain from `xcmailr.admin.address`, which by definition is the system operator's domain and already trusted.
