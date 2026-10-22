# Design

## Context

See `proposal.md` for background and motivation.

In the modernized Spring Boot 4 application, `ProfileController` handles personal details, password updates, and API token management. While administrators can delete other users via `AdminWebController.deleteUser(...)`, self-service deletion is not yet exposed to end users. 

XCMailr stores received emails (`Mail`) associated with mailboxes (`MBox`) via a foreign key `mailbox_id` (without automated JPA cascade relationships), and `MBox` entities reference `User` via `usr_id`. Deleting a user therefore requires cascading deletion across `mail`, `mailboxes`, and `users` tables within a single database transaction.

## Goals / Non-Goals

**Goals:**
- Provide a clear, accessible "Danger Zone / Delete Account" card in `profile.html` adhering to Option A (right column, beneath the REST API Token card).
- Implement a modal dialog requiring the user to re-enter their current password before confirming deletion.
- Enforce business logic in `ProfileController`: password verification, sole administrator deletion prevention, transactional cascade deletion, session and security context invalidation, and redirect to `/login?accountDeleted`.
- Add `countByAdminTrueAndActiveTrue()` query in `UserRepository`.
- Display a confirmation alert on `login.html` when redirected with `?accountDeleted`.
- Adhere strictly to the testing pyramid with automated unit and integration tests covering happy paths, error paths, and special security edge cases so manual verification is never needed.
- Use `final` keywords throughout all new Java code, parameters, and local variables.
- Provide comprehensive Javadoc and inline explanatory comments.

**Non-Goals:**
- Providing self-service account deactivation / temporary suspension (only permanent deletion is supported).
- Soft deletion / archiving of user data (data minimization requires immediate permanent removal).

## Decisions

### Decision 1: Two-Column UI Card Placement (Option A)
- **Choice**: Place the "Danger Zone / Delete Account" card in the right column of `profile.html` beneath the "REST API Token" card.
- **Rationale**: Keeps the profile view compact and balanced (Left column: Personal Info + Change Password; Right column: API Token + Danger Zone) without pushing critical settings down on large viewports.
- **Alternative considered**: Full-width bottom section (Option B), which caused excessive vertical scrolling and unbalanced whitespace on widescreen viewports.

### Decision 2: Re-Authentication via Password Challenge Modal
- **Choice**: Trigger a Bootstrap 5 modal (`#deleteAccountModal`) containing a password field (`currentPassword`) and submit button (`btn-danger`).
- **Rationale**: Account deletion is catastrophic and irreversible. Requiring re-entry of the current password prevents unauthorized deletion from unattended browser sessions or cross-site clickjacking attacks.
- **Alternative considered**: Simple browser `confirm()` popup, which cannot collect password re-authentication and is vulnerable to accidental taps.

### Decision 3: Sole Administrator Protection
- **Choice**: Add `countByAdminTrueAndActiveTrue()` to `UserRepository`. In `ProfileController`, check:
  ```java
  if (user.isAdmin() && userRepository.countByAdminTrueAndActiveTrue() <= 1)
  {
      model.addAttribute("errorMessage", "Your account cannot be deleted because it is the last administrator account. At least one active administrator account must exist.");
      populateProfileModel(model, user);
      return "user/profile";
  }
  ```
- **Rationale**: Prevents accidental administrative lockout of the system. Matches the legacy system's `user.isLastAdmin()` safeguard while accounting for active status.
- **Alternative considered**: Allowing deletion and leaving the system without admins, which requires manual database intervention to restore access.

### Decision 4: Transactional Cascade Data Cleanup
- **Choice**: Perform deletion in `ProfileController.deleteAccount(...)` annotated with `@Transactional`:
  1. Retrieve all mailboxes owned by the user (`mailboxRepository.findByUsr(user, Pageable.unpaged())`).
  2. Delete all emails associated with each mailbox (`mailRepository.deleteByMailbox(box.getId())`).
  3. Delete each mailbox (`mailboxRepository.delete(box)`).
  4. Delete the user (`userRepository.delete(user)`).
- **Rationale**: Reuses the proven deletion strategy from `AdminWebController.deleteUser(...)` and ensures zero orphan records remain in the database.
- **Alternative considered**: DB-level foreign key cascade deletion, which would require schema migration scripts and changes to JPA relationship mappings.

### Decision 5: Session Termination and Spring Security Context Cleanup
- **Choice**: After successful database deletion, invoke:
  ```java
  SecurityContextHolder.clearContext();
  final HttpSession session = request.getSession(false);
  if (session != null)
  {
      session.invalidate();
  }
  ```
  Followed by redirecting to `/login?accountDeleted`.
- **Rationale**: Guarantees that any cached authentication tokens, session cookies, or CSRF tokens in the active HTTP session are completely revoked immediately.

### Decision 6: Testing Pyramid Architecture
- **Unit / Repository Tests**: Verify `countByAdminTrueAndActiveTrue()` across various admin and user account states in `UserRepositoryTest` or focused test cases.
- **Web MVC Integration Tests (`ProfileControllerTest`)**:
  - `testDeleteAccountHappyPath`: Normal user deletes account with correct password -> mailboxes and emails are removed, user deleted, session invalidated, redirected to `/login?accountDeleted`.
  - `testDeleteAccountAdminWithMultipleAdmins`: Admin deletes account when a second admin exists -> successfully deleted.
  - `testDeleteAccountSoleAdminBlocked`: Sole admin attempts deletion -> rejected with error, user and mailboxes retained.
  - `testDeleteAccountWrongPassword`: Incorrect password submitted -> rejected with error, user and mailboxes retained.
  - `testDeleteAccountBlankPassword`: Blank password submitted -> rejected with error.
  - `testDeleteAccountUnauthenticated`: Unauthenticated request to `/profile/delete` -> redirected to `/login`.
  - `testDeleteAccountCsrfMissing`: Request missing CSRF token -> rejected with HTTP 403 Forbidden.
  - `testDeleteAccountCsrfInvalid`: Request with invalid/forged CSRF token -> rejected with HTTP 403 Forbidden.


## Risks / Trade-offs

- **[Risk]** Accidental deletion of mailboxes and emails without recovery option.  
  → **Mitigation**: Prominent danger-styled alert in modal dialog, explicit confirmation button, and mandatory password challenge.
- **[Risk]** Session persistence after account deletion if only context is cleared.  
  → **Mitigation**: Explicitly call `session.invalidate()` and `SecurityContextHolder.clearContext()` on the active `HttpServletRequest`.
- **[Risk]** Concurrency race during admin self-deletion when two admins delete simultaneously.  
  → **Mitigation**: `@Transactional` method with JPA count query ensures consistent database read state.

