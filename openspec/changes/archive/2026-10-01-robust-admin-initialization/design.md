# Design

## Context

See `proposal.md` for the motivation.

Currently, `AdminUserInitializer` runs during startup and inspects the database for the configured administrator email (`xcmailr.admin.address`). If the user exists, it previously verified whether the database password hash matched the configured password and updated the entity if it differed. In production or shared environments, this risked overwriting legitimate password changes made by administrators via the web console. Furthermore, database queries should only be performed when administrator credentials are explicitly configured.

## Goals / Non-Goals

**Goals:**
- Guard against invalid or omitted configuration: if the administrator email or password is null, blank, or unconfigured, exit immediately without querying the database or attempting any entity operations.
- Guarantee non-destructive startup: if valid administrator credentials are configured and the administrative user already exists in the database, perform **zero** mutations or updates.
- Support fresh deployments and disaster recovery: if valid administrator credentials are configured and the administrative user is missing (new installation or accidental deletion), bootstrap the account with the configured credentials.
- Provide zero-friction execution: require no special Spring profiles (such as `dev` or `prod`) or command-line VM options to run safely in any environment.
- Enforce the testing pyramid with comprehensive test coverage:
  - Unit tests: validation of the credential presence guard (verifying no repository interactions when credentials are missing/blank), absence checking, and domain extraction.
  - Component/Repository tests: verifying database persistence, hashing with `PasswordEncoder`, and non-mutation of existing records.
  - Integration tests: verifying Spring Security form authentication against bootstrapped and preserved accounts.
- Enforce code standards: apply `final` on all method parameters, local variables, and immutable fields; write comprehensive Javadoc and clear explanatory code comments.
- Document administrator lifecycle in `README.md`: clearly explain how admin accounts are bootstrapped, configured via environment variables, secured in production, and recovered in disaster scenarios.

**Non-Goals:**
- Interactive web-based setup wizard (unnecessary complexity for containerized/headless deployments).
- Custom CLI administrative command runner (environment variable configuration meets Twelve-Factor requirements).

## Decisions

### Decision 1: Credential Configuration Guard First (Must Both Be Non-Blank)
- **Choice**: Validate credentials using `StringUtils.hasText(adminMail) && StringUtils.hasText(adminPassword)`. If either is `null`, empty (`""`), or whitespace-only (`"   "`), skip initialization immediately without executing any queries against `UserRepository` or `DomainRepository`.
- **Rationale**: In containerized environments, unset environment variables (e.g. `ADMIN_PASSWORD=""` or `${ADMIN_PASSWORD:}`) resolve to empty strings rather than `null`. Requiring both fields to be strictly non-blank prevents accidental creation of accounts with empty passwords, eliminates database overhead, and lets operators disable automatic bootstrapping simply by leaving `xcmailr.admin.password` blank.

### Decision 2: Idempotent Absence Check Only When Configured (Twelve-Factor Pattern)
- **Choice**: If and only if both the administrator email and password are provided and non-blank, the initializer queries `userRepository.findByMailIgnoreCase(normalizedMail)`. If an account is present, it logs a message at `DEBUG` level and immediately returns without touching any attributes. If absent, it creates the administrator account with BCrypt password hashing, `admin = true`, and `active = true`.
- **Rationale**: Matches the original Ninja framework behavior and modern containerized software standards (Grafana, Keycloak, SonarQube). Ensures safety on existing databases while allowing seamless bootstrapping and recovery after accidental deletion.
- **Alternatives Considered**:
  - *Table count check (`userRepository.count() == 0`)*: Rejected because if the admin is deleted by accident while other users exist, recovery is impossible without manual SQL access.
  - *Profile-based gating (`@Profile("dev")`)*: Rejected because it requires operators and developers to manage profile flags and breaks fresh production deployments without explicit flags.

### Decision 3: Warn on Default Credentials During Creation
- **Choice**: If a new administrator account is being created and the password equals the default placeholder `"1234"`, emit a prominent `WARN` log instructing the operator to change the password immediately.
- **Rationale**: Balances out-of-the-box local developer convenience with production security awareness.

## README Documentation Specification

The `README.md` must be updated with an "Administrator Account Management" section detailing:
1. **Initial Setup**:
   - Explanation that on a fresh database, the system will automatically create the primary administrator account if `ADMIN_ADDRESS` and `ADMIN_PASSWORD` are configured.
   - Default credentials for local development (`admin@xcmailr.test` / `1234`).
2. **Non-Destructive Behavior**:
   - Explicit guarantee that once an administrator account exists, subsequent restarts will **never** alter or overwrite its password or privileges, even if the environment variables remain configured.
   - Instructing operators to use the web console profile page to update passwords after initial setup.
3. **Production Hardening**:
   - Guidance for production deployments: either set strong credentials in `ADMIN_ADDRESS` and `ADMIN_PASSWORD` prior to initial startup, or unset `ADMIN_PASSWORD` once initialized so no auto-bootstrap logic executes.
4. **Disaster Recovery (Accidental Deletion)**:
   - Step-by-step instructions: if the administrator account is ever deleted or access is lost, restart the container/service with valid `ADMIN_ADDRESS` and `ADMIN_PASSWORD` environment variables to automatically recreate the admin account.

## Risks / Trade-offs

- **[Risk] Operators expecting password synchronization via config file** → **Mitigation**: Document in the `README` that credentials for existing accounts must be updated via the web UI or database, preventing inadvertent credential resets.
- **[Risk] Case sensitivity discrepancies in email addresses** → **Mitigation**: Normalize administrator addresses using `.trim().toLowerCase()` and query via `findByMailIgnoreCase`.

## Testing Pyramid Strategy

1. **Unit Tests (`AdminUserInitializerUnitTest`)**:
   - Fast, isolated tests mocking `UserRepository`, `DomainRepository`, and `PasswordEncoder`.
   - Error cases: null address, blank address, blank password (verify `userRepository.findByMailIgnoreCase` is never called via `verifyNoInteractions`).
   - Happy path: missing user with configured credentials triggers save with encoded password.
   - Special cases: existing user with different password triggers no save/mutation.
2. **Component Tests (`AdminUserInitializerTest`)**:
   - Spring Boot integration test with active H2 test database.
   - Verify initial bootstrapping, domain whitelist insertion, and idempotency across repeated runs.
   - Verify password hash verification via `passwordEncoder.matches`.
3. **Integration & Security Tests (`WebAuthControllerTest`)**:
   - Verify end-to-end form login with newly bootstrapped admin credentials.
   - Verify form login when an existing user's password has been customized via UI.

