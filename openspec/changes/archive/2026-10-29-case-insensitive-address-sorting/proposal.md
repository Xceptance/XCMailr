# Proposal: Case-Insensitive Mailbox Address Sorting

## Why

When sorting mailboxes by address on the dashboard table, the system currently applies raw database collation, resulting in binary ASCII code-point ordering where uppercase characters precede lowercase characters (ASCII 65–90 vs 97–122). For example, `UCoMfSLVUDQudxjJ@xcmailr.test` sorts before `aaaa@xcmailr.test` instead of appearing naturally under 'u'. Users expect natural, case-insensitive alphabetical sorting regardless of whether email addresses contain uppercase letters (such as addresses generated via API or automated test suites).

## What Changes

- Modify `MailboxWebController.resolveSort` so that sorting by `address` applies case-insensitive ordering (`ignoreCase()`) to both the local address part and the domain.
- Update the `web-console-ui` specification to explicitly define case-insensitive alphabetical ordering for address sorting across all pagination pages.
- Update test cases in `MailboxSortingIntegrationTest` to use realistic mixed-case addresses (e.g. `Alpha`, `bravo`, `CHARLIE`, `UCoMf...`) ensuring case-insensitive sorting is strictly enforced across ASC and DESC.
- Add unit tests in `MailboxWebControllerTest` verifying that the generated `Sort` contains case-insensitive `Sort.Order` definitions for `address` and `domain`.
- Follow strict code standards: `final` on all parameters and local variables, comprehensive Javadoc, and clear code comments.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Update the `Dynamic Mailbox Dashboard` requirement to explicitly require case-insensitive alphabetical sorting for the `Address` column.

## Impact

- `MailboxWebController.java`: `resolveSort` method updated to construct `Sort.by(new Sort.Order(direction, "address").ignoreCase()).and(Sort.by(new Sort.Order(direction, "domain").ignoreCase()))`.
- `web-console-ui/spec.md`: Delta spec clarifying case-insensitive address ordering.
- Automated tests: `MailboxSortingIntegrationTest.java` and `MailboxWebControllerTest.java` updated to validate case-insensitive sorting behavior.

