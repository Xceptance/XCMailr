-- Clear historical confirmation tokens and timestamps left over from legacy XCMailr
-- on all active user accounts and any already-deactivated user accounts that own mailboxes.
-- Genuinely unconfirmed registrations (active = FALSE with zero mailboxes) remain untouched.
UPDATE users
SET confirmation = NULL,
    ts_confirm = NULL
WHERE active = TRUE
   OR EXISTS (
       SELECT 1
       FROM mailboxes
       WHERE mailboxes.usr_id = users.id
   );
