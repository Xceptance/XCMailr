# Proposal

## Why

When an authenticated user updates their preferred language in the profile settings (for example, switching from German to English or vice versa), the web console continues to render localized UI components (such as pagination indicators and action messages) in the stale language captured at initial login. This happens because `UserPreferenceLocaleResolver` reads the snapshot `User` inside `UserPrincipal` before querying the database, while `ProfileController` persists profile updates to the database without synchronizing the in-session `UserPrincipal`.

## What Changes

- Modify `UserPreferenceLocaleResolver.resolveLocale` to query `userRepository.findByMailIgnoreCase(...)` first whenever `userRepository != null`, making the database the authoritative source of truth across requests and sessions while preserving fallback to `UserPrincipal` when `userRepository` is not configured (e.g., in standalone unit tests).
- Update `ProfileController.updateProfile` to synchronize the in-session `UserPrincipal`'s user fields (`setForename`, `setSurname`, `setLanguage`) upon a successful profile update, maintaining in-memory session consistency without requiring the user to log out and log back in.
- Enforce OWASP security principles: prevent broken access control by scoping user lookups strictly to the authenticated `Authentication.getName()` identity, prevent privilege escalation by restricting session updates strictly to non-security profile fields (`forename`, `surname`, `language`), and ensure injection resilience when resolving language tags.
- Implement comprehensive test suites following the testing pyramid: unit tests for `UserPreferenceLocaleResolverTest` and WebMvc integration tests for `ProfileControllerTest` and `MailboxWebControllerTest`, thoroughly covering happy paths, error scenarios, and special/security cases.
- Enforce strict code standards throughout all modified production and test code: `final` keyword on all parameters, local variables, and fields, along with complete Javadoc documentation and descriptive inline comments.

## Capabilities

### New Capabilities

### Modified Capabilities
- `web-console-ui`: Update `User Authentication and Self-Service` requirement to mandate that profile language updates immediately take effect across subsequent web views and partial HTMX fragment requests in the active session without requiring re-authentication, while upholding OWASP tenant isolation and privilege escalation defenses.

## Impact

- Affected Code:
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/config/UserPreferenceLocaleResolver.java`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/ProfileController.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/config/UserPreferenceLocaleResolverTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/ProfileControllerTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxWebControllerTest.java`
- APIs / Systems: No external API or database schema changes; internal Spring MVC locale resolution and session state synchronization.
