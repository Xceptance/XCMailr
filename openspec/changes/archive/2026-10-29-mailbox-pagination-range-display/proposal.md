# Proposal: Localized Mailbox Item Range Pagination Display

## Why

On the mailbox dashboard ("My Email Addresses"), the current table footer displays pagination as `Page 1 of 2 (17 total mailboxes)` or simply `17 total mailboxes` when single-page. While the total number of mailboxes is visible, users cannot immediately discern which slice or range of mailboxes is currently being displayed (e.g., items 1 through 15) without doing mental math against the page size. Furthermore, the mailbox dashboard pagination text is hardcoded in English, ignoring the user's language preference (`en` vs `de`) configured in their account profile.

Addressing GitHub Issue #92 with an item range display (`Showing 1–15 of 17 mailboxes` / `Zeige 1–15 von 17 Postfächern`) provides intuitive, standard pagination feedback and ensures consistent localization across the modernized web application.

## What Changes

- **Localized Item Range Pagination Display**:
  - Replace the current footer text (`Page 1 of 2 (17 total mailboxes)` / `17 total mailboxes`) in `mailbox-table.html` with an item range indicator:
    - Multi-page dataset: `Showing {start}–{end} of {total} mailboxes` (e.g., `Showing 1–15 of 17 mailboxes` on page 1, `Showing 16–17 of 17 mailboxes` on page 2).
    - Single-page dataset (> 1 item): `Showing 1–{total} of {total} mailboxes` (e.g., `Showing 1–3 of 3 mailboxes`).
    - Single item dataset (= 1 item): `Showing 1 of 1 mailbox` (singular).
    - Empty dataset (= 0 items): Handled by the existing empty-state display; footer pagination is omitted or shows `0 mailboxes`.
  - Localize the range strings for German when the user's account profile language is set to `de` (e.g., `Zeige 1–15 von 17 Postfächern`, `Zeige 1 von 1 Postfach`).
- **Spring Boot MessageSource & User-Aware Locale Resolution**:
  - Configure Spring Boot's `MessageSource` in `application.yml` (`spring.messages.basename: conf/messages`) to load existing message bundles (`conf/messages.properties` and `conf/messages_de.properties`).
  - Introduce a user-preference `LocaleResolver` bean that inspects the authenticated `UserPrincipal`'s configured language (`user.getLanguage()`) and resolves `Locale.GERMAN` for `de` or `Locale.ENGLISH` as default, ensuring consistent localization across full page requests and asynchronous HTMX partial swaps.
  - Define new message keys in `conf/messages.properties` and `conf/messages_de.properties` for mailbox table pagination summaries.
- **Testing Pyramid & Code Quality Standards**:
  - Follow the testing pyramid with Tier 1 unit tests (`UserPreferenceLocaleResolverTest`, `MessageSourceTest`), Tier 2 MockMvc integration tests (`MailboxControllerTest`), and Tier 3 full suite validation.
  - Cover happy paths, error/fallback cases (null, empty, or invalid user language codes), and special cases (unauthenticated requests, single-item singular phrasing, and HTMX partial DOM updates).
  - Enforce strict Java style: `final` on all method parameters, local variables, and fields wherever possible; comprehensive Javadoc on all classes and methods; and explanatory inline comments.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `web-console-ui`: Modifies `Requirement: Dynamic Mailbox Dashboard` to require a localized item range pagination indicator below the mailbox table matching the authenticated user's profile language.

## Impact

- **Templates & UI**: [`mailbox-table.html`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html) footer markup updated to use parameterized Thymeleaf `#{...}` message expressions.
- **Configuration**:
  - [`application.yml`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/resources/application.yml) updated with `spring.messages.basename: conf/messages`.
  - Web MVC configuration updated to provide a user-aware `LocaleResolver`.
- **Localization Resources**: [`conf/messages.properties`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/resources/conf/messages.properties) and [`conf/messages_de.properties`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/resources/conf/messages_de.properties) updated with pagination text keys.
- **Backend APIs / Database**: No schema changes or query modifications; relies on Spring Data JPA `Page<MBox>` metadata already passed to the model.
