# Tasks

## 1. Repository & Data Access Layer

- [x] 1.1 Add `countByAdminTrueAndActiveTrue()` to `UserRepository` with Javadoc and `final` modifiers, and verify compilation
- [x] 1.2 Add unit/repository tests verifying `countByAdminTrueAndActiveTrue()` correctly distinguishes active admins from inactive admins and standard users

## 2. Backend Self-Service Deletion Controller

- [x] 2.1 Inject `MailboxRepository` and `MailRepository` into `ProfileController` with Javadoc and `final` constructor parameters
- [x] 2.2 Implement `POST /profile/delete` in `ProfileController` with `@Transactional`, validating current password, enforcing sole administrator protection, cascading mailbox and email removal, invalidating session and security context, and redirecting to `/login?accountDeleted`
- [x] 2.3 Add comprehensive Javadoc, inline logic explanations, and ensure `final` is used on all parameters and local variables in `ProfileController`


## 3. Web Views & Confirmation Modal (Option A)

- [x] 3.1 Update `profile.html` to add a "Danger Zone / Delete Account" card in the right column beneath the REST API Token section, with warning callout and modal launch button
- [x] 3.2 Add the Bootstrap 5 `#deleteAccountModal` to `profile.html` with CSRF protection, password confirmation field (`currentPassword`), cancel action, and danger submit button
- [x] 3.3 Update `login.html` to render a confirmation banner when `param.accountDeleted` is present


## 4. Comprehensive Automated Test Suite (Testing Pyramid)

- [x] 4.1 Implement happy path integration test in `ProfileControllerTest` verifying normal user account deletion, cascade removal of mailboxes and emails, session invalidation, and redirect to `/login?accountDeleted`
- [x] 4.2 Implement happy path integration test in `ProfileControllerTest` verifying administrator self-deletion when another active administrator exists
- [x] 4.3 Implement error case integration tests in `ProfileControllerTest` verifying rejection and error message display on incorrect password and blank password
- [x] 4.4 Implement special case integration test in `ProfileControllerTest` verifying sole administrator deletion prevention and data retention
- [x] 4.5 Implement security integration tests in `ProfileControllerTest` verifying unauthenticated access redirection, missing CSRF token rejection (HTTP 403), and invalid CSRF token rejection (HTTP 403)
- [x] 4.6 Run the full automated test suite (`mvn test`) and verify all tests pass without errors

