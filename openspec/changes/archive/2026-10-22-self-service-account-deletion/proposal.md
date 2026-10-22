# Proposal

## Why

In the legacy XCMailr platform, users had the ability to delete their user accounts and all associated data directly from the profile management page. During the modernization to Spring Boot and Thymeleaf, this self-service deletion option was omitted, leaving only administrative account deletion in the admin console. 

Restoring self-service account deletion allows users to exercise complete control over their accounts and data (supporting data minimization and privacy rights), with guardrails preventing the sole remaining system administrator from deleting their account and locking out administrative access.

## What Changes

- **Profile UI ("Danger Zone" Card - Option A)**: Add a dedicated "Delete Account" card in the right column of `profile.html` beneath the REST API Token section, containing a clear warning and an "I want to delete my account" action button.
- **Confirmation Modal**: Clicking the deletion button opens a Bootstrap 5 confirmation modal requiring the user to confirm intent and re-enter their current password before submitting.
- **Self-Service Deletion Endpoint**: Implement `POST /profile/delete` in `ProfileController`:
  - Re-authenticates the user by verifying their current password against BCrypt hash.
  - Enforces administrator protection: blocks deletion if the authenticated user is the last active system administrator.
  - Performs transactional cascade deletion of all mailboxes and stored emails belonging to the user, followed by the user record itself.
  - Invalidates the active HTTP session and clears the Spring Security context.
  - Redirects to `/login?accountDeleted`.
- **Repository Support**: Add `countByAdminTrueAndActiveTrue()` (or `countByAdminTrue()`) to `UserRepository` to verify whether an administrator attempting deletion is the sole remaining admin.
- **Login Confirmation Banner**: Update `login.html` to render a confirmation banner when accessed with `?accountDeleted`, giving the user clear confirmation that their account and data have been removed.
- **Comprehensive Automated Testing**: Implement automated tests following the testing pyramid:
  - Unit/service level tests for repository queries and admin checks.
  - Controller integration tests using `MockMvc` covering happy path (successful self-deletion, cascading deletion of mailboxes and emails, session invalidation), error cases (incorrect password, empty password, last admin rejection), and unauthenticated access prevention.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Enhance the `User Authentication and Self-Service` requirement to specify self-service account deletion, password verification, last-administrator protection, transactional cascade deletion of mailboxes and emails, and session termination with login feedback.

## Impact

- **Affected Files**:
  - `xcmailr-webapp/src/main/resources/templates/user/profile.html`
  - `xcmailr-webapp/src/main/resources/templates/auth/login.html`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/ProfileController.java`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/repositories/UserRepository.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/ProfileControllerTest.java`
- **Database**: No schema changes required; operates on existing `users`, `mailboxes`, and `mail` tables.
- **Security**: Endpoint is secured with standard CSRF protection, password re-authentication, and session invalidation upon completion.

