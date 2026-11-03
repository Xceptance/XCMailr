# Tasks

## 1. Localization Infrastructure & Unit Tests (Tier 1)

- [x] 1.1 Configure `spring.messages.basename: conf/messages` and `spring.messages.encoding: UTF-8` in `application.yml` and verify bundle detection with Spring context startup
- [x] 1.2 Implement `UserPreferenceLocaleResolver` in `com.xceptance.xcmailr.config` with `final` parameters/variables, Javadoc, and inline comments, registering it as the MVC `LocaleResolver` bean with English fallback
- [x] 1.3 Add Tier 1 unit tests in `UserPreferenceLocaleResolverTest` using `final` variables, Javadoc, and comments covering Happy Path (`"de"` -> `Locale.GERMAN`, `"en"` -> `Locale.ENGLISH`), Error/Fallback Cases (`null`, `""`, `"fr"` -> `Locale.ENGLISH`), and Special Cases (unauthenticated request delegating to `AcceptHeaderLocaleResolver`, `setLocale` no-op execution)
- [x] 1.4 Add `mailbox.table.showingRange` and `mailbox.table.showingSingle` keys to `conf/messages.properties` and `conf/messages_de.properties` with en-dash range formatting
- [x] 1.5 Add Tier 1 unit tests in `MessageSourceTest` using `final` variables, Javadoc, and comments verifying parameter interpolation for both English and German message bundles

## 2. Template Updates & Integration Tests (Tier 2)

- [x] 2.1 Update `mailbox-table.html` footer to replace the legacy page count with localized range expressions (`mailbox.table.showingRange` and `mailbox.table.showingSingle`) using page arithmetic, preserving HTMX pagination buttons and export utility links
- [x] 2.2 Add Tier 2 integration tests in `MailboxControllerTest` using `final` variables, Javadoc, and comments verifying Happy Path (Page 1 range `1–15 of 17`), Special Cases (Page 2 range `16–17 of 17`, Single item `Showing 1 of 1 mailbox`, German locale `Zeige 1–15 von 17 Postfächern` and `Zeige 1 von 1 Postfach`, HTMX partial swaps), and Edge Cases (empty dataset)

## 3. Verification & Code Quality (Tier 3)

- [x] 3.1 Audit implementation and test code to confirm strict compliance with coding standards: `final` on all method parameters and local variables, complete Javadoc, and explanatory inline comments
- [x] 3.2 Execute the full Maven test suite (`mvn test`) to ensure all Tier 1, 2, and 3 tests pass without regressions
- [x] 3.3 Validate OpenSpec change integrity and specifications with `openspec validate --specs`
