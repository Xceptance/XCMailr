# Proposal

## Why

When sorting mailboxes by expiration in ascending order (`sort=expires&dir=asc`), unlimited mailboxes (`ts_Active = 0`) are currently displayed first because `0` is treated as a numerical minimum rather than infinite future ($+\infty$). This breaks user expectations when looking for soonest-expiring mailboxes and hides urgent deadlines. Additionally, users lack a quick way in the dashboard filter toolbar to isolate temporary expiring mailboxes from permanent unlimited mailboxes.

Implementing "Perspective A (Pure Infinity)" for expiration sorting and expanding the status filter options resolves this:
1. `Expires ASC` presents soonest-expiring mailboxes first and places unlimited mailboxes at the very end ($+\infty$).
2. `Expires DESC` presents unlimited mailboxes first ($+\infty$) followed by longest-lasting finite mailboxes down to soonest-expiring.
3. The dashboard status filter dropdown is extended with "Expiring Only" and "Unlimited Only" options, enabling one-click isolation of temporary vs. permanent mailboxes without UI clutter.

## What Changes

- **Pure Infinity Expiration Sorting (`Perspective A`)**:
  - Update `MailboxWebController.resolveSort("expires", dir)` or query ordering so that `ts_Active = 0` is treated as infinite future ($+\infty$).
  - When `dir=asc`: orders finite timestamps ascending (`ts_Active ASC`) with unlimited mailboxes (`ts_Active = 0`) placed at the end.
  - When `dir=desc`: orders unlimited mailboxes (`ts_Active = 0`) at the top, followed by finite timestamps descending (`ts_Active DESC`).
- **Extended Filter Options in Web Console**:
  - Extend the mailbox status filter with:
    - `EXPIRING` ("Expiring Only"): matches active mailboxes with finite expiration (`expired = false AND ts_Active > 0`).
    - `UNLIMITED` ("Unlimited Only"): matches active permanent mailboxes (`ts_Active = 0`).
  - Update `mailboxRepository` query methods / controller filtering logic to support these new filter tokens alongside existing `ALL`, `ACTIVE`, and `EXPIRED`.
  - Update `xcmailr-webapp/src/main/resources/templates/mailboxes/index.html` filter select options.
- **Testing Pyramid Coverage**:
  - Unit tests verifying `resolveSort` and repository query behaviors.
  - Web slice tests in `MailboxSortingWebTest` and `MailboxWebControllerTest` covering happy path ASC/DESC ordering, filter selection preservation, edge cases (empty results, unknown filter fallback to `ALL`), and malformed direction fallbacks.
  - Enforce `final` modifiers throughout test and production code, full Javadocs, and explanatory code comments.

## Capabilities

### Modified Capabilities
- `web-console-ui`: Update `Dynamic Mailbox Dashboard` requirement to specify Perspective A expiration sorting (unlimited at end for ASC, unlimited at top for DESC) and the extended status filter dropdown options (`EXPIRING` and `UNLIMITED`).

## Impact

- **Backend**:
  - `MailboxWebController`: Update `resolveSort` for `"expires"` and status filter parsing in `listMailboxes`.
  - `MailboxRepository`: Add query methods or criteria for `EXPIRING` and `UNLIMITED` mailbox filtering.
- **Frontend**:
  - `templates/mailboxes/index.html`: Add `<option value="EXPIRING">Expiring Only</option>` and `<option value="UNLIMITED">Unlimited Only</option>` to `#statusFilterSelect`.
- **Tests**:
  - Update `MailboxSortingIntegrationTest` assertions where `ts_Active = 0` was previously expected first on ASC.
  - Add comprehensive tests across unit, repository, and controller layers with `final` modifiers and thorough Javadocs.

