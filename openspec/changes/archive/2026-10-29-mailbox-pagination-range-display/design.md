# Design: Localized Mailbox Item Range Pagination Display

## Context

On the mailbox dashboard (`/mailboxes`), pagination is handled using Spring Data JPA's `Page<MBox>`. Currently, [`mailbox-table.html`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html#L215-L232) renders:
- Multi-page: `Page <number+1> of <totalPages> (<totalElements> total mailboxes)`
- Single-page: `<totalElements> total mailboxes`

This string is hardcoded in English, ignoring the user's `User.language` preference (`"en"` vs `"de"`). Furthermore, displaying `Page 1 of 2` forces the user to calculate the visible slice mentally.

Existing resource bundles exist at [`conf/messages.properties`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/resources/conf/messages.properties) and [`conf/messages_de.properties`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/resources/conf/messages_de.properties), but `spring.messages.basename` was not configured in `application.yml` (defaulting to classpath root `messages`).

## Goals / Non-Goals

**Goals:**
- Provide a clear, localized item range indicator:
  - Multi-item: `Showing {start}–{end} of {total} mailboxes` (e.g., `Showing 1–15 of 17 mailboxes` on page 1, `Showing 16–17 of 17 mailboxes` on page 2).
  - Single-item: `Showing 1 of 1 mailbox`.
  - German translation when `user.language == "de"`: `Zeige {start}–{end} von {total} Postfächern` and `Zeige 1 von 1 Postfach`.
- Configure Spring Boot `MessageSource` to resolve `conf/messages` bundles with UTF-8 encoding.
- Implement an authenticated user-aware `LocaleResolver` that reads `UserPrincipal.getUser().getLanguage()` for both initial HTTP GET requests and subsequent HTMX asynchronous partial DOM swaps.
- Maintain existing HTMX pagination triggers, filter state, and export utility links.
- Adhere strictly to the testing pyramid with comprehensive happy path, error/fallback, and special case tests.
- Maintain strict code standards: `final` on all method parameters, local variables, and class fields wherever possible, full Javadoc, and explanatory inline comments.

**Non-Goals:**
- Mass migration of all 300+ legacy messages across every template in the application (this design establishes the reusable i18n foundation and applies it to the mailbox pagination feature).
- Changes to database entities, Flyway migrations, or JPA pagination queries.

## Decisions

### 1. Template Item Range Computation vs Controller Attributes
- **Decision**: Calculate the slice start and end directly in the Thymeleaf template from the existing `Page<MBox> page` object:
  - Start item: `page.number * page.size + 1`
  - End item: `page.number * page.size + page.numberOfElements`
  - Total items: `page.totalElements`
- **Rationale**: `Page<T>` already exposes `number`, `size`, `numberOfElements`, and `totalElements`. Computing the range in Thymeleaf avoids polluting controller models and keeps all display formatting concerns in the presentation layer.
- **Alternatives considered**:
  - *Compute in Controller / DTO*: Requires touching `MailboxController` endpoints and passing additional model attributes (`startItem`, `endItem`), creating unnecessary coupling.

### 2. User-Aware LocaleResolver
- **Decision**: Create `UserPreferenceLocaleResolver` implementing Spring's `org.springframework.web.servlet.LocaleResolver`:
  - Inspect `SecurityContextHolder.getContext().getAuthentication()`.
  - If the principal is an instance of `UserPrincipal`, check `user.getLanguage()`.
  - If `"de"`, return `Locale.GERMAN`; otherwise default to `Locale.ENGLISH`.
  - If unauthenticated, fall back to standard `AcceptHeaderLocaleResolver` logic using the incoming HTTP `Accept-Language` header.
- **Rationale**:
  - Automatically resolves the correct locale for both full-page requests (`/mailboxes`) and HTMX partial fragment requests (`HX-Request: true`).
  - No need for query parameters (`?lang=de`) or cookie synchronizers, as the user profile in the database is the source of truth for authenticated sessions.
- **Alternatives considered**:
  - *SessionLocaleResolver with LocaleChangeInterceptor*: Requires explicit requests to switch locale and session attribute management, which can desynchronize if the user updates their language in another tab or profile settings.
  - *Accept-Language header only*: Fails to honor the user's explicit profile setting when their operating system or browser default differs.

### 3. Resource Bundles & Message Keys
- **Decision**: Configure `spring.messages.basename: conf/messages` and `spring.messages.encoding: UTF-8` in `application.yml`.
  Define new keys:
  - In `conf/messages.properties`:
    - `mailbox.table.showingRange=Showing {0}–{1} of {2} mailboxes`
    - `mailbox.table.showingSingle=Showing 1 of 1 mailbox`
  - In `conf/messages_de.properties`:
    - `mailbox.table.showingRange=Zeige {0}–{1} von {2} Postfächern`
    - `mailbox.table.showingSingle=Zeige 1 von 1 Postfach`
- **Rationale**: Uses standard Java `MessageFormat` parameter placeholders (`{0}`, `{1}`, `{2}`) compatible with Thymeleaf `#{...}` syntax.

### 4. Range Separator Typography
- **Decision**: Use the en-dash (`–`, `\u2013`) without surrounding spaces between numbers (e.g., `1–15`), adhering to standard typographic style guides for numeric ranges.

## Testing Strategy (Testing Pyramid)

The implementation follows the Testing Pyramid across three distinct tiers, ensuring comprehensive verification of happy path, error/fallback, and special cases:

```
          / \
         /   \
        / E2E \       Tier 3: Full Context & Test Suite Verification
       /-------\
      / Integr. \     Tier 2: MockMvc Controller Slice Tests (Full Page & HTMX)
     /-----------\
    /  Unit Tests \   Tier 1: Isolated Component Tests (LocaleResolver & MessageSource)
   /---------------\
```

### Tier 1: Unit Tests (Fast, Isolated, Boundary Verification)
- **`UserPreferenceLocaleResolverTest`**:
  - **Happy Path (German)**: Authenticated `UserPrincipal` with `language = "de"` resolves `Locale.GERMAN`.
  - **Happy Path (English)**: Authenticated `UserPrincipal` with `language = "en"` resolves `Locale.ENGLISH`.
  - **Error / Fallback Case (Null/Empty)**: Authenticated user with `language = null` or `language = ""` safely falls back to `Locale.ENGLISH`.
  - **Error / Fallback Case (Unsupported Locale)**: Authenticated user with `language = "es"` or `language = "invalid"` safely falls back to `Locale.ENGLISH`.
  - **Special Case (Unauthenticated Request)**: Request without authentication or with `AnonymousAuthenticationToken` delegates to `AcceptHeaderLocaleResolver` (honoring request `Accept-Language: de` or defaulting to English).
  - **Special Case (No-op Mutator)**: Invoking `setLocale(...)` or `setLocaleContext(...)` executes safely without throwing unsupported operation exceptions.
- **`MessageSourceTest`**:
  - **Happy Path**: Resolves `mailbox.table.showingRange` with `{0}=1, {1}=15, {2}=17` producing `"Showing 1–15 of 17 mailboxes"` for `Locale.ENGLISH` and `"Zeige 1–15 von 17 Postfächern"` for `Locale.GERMAN`.
  - **Special Case (Singular)**: Resolves `mailbox.table.showingSingle` producing `"Showing 1 of 1 mailbox"` for `Locale.ENGLISH` and `"Zeige 1 von 1 Postfach"` for `Locale.GERMAN`.

### Tier 2: Integration & Slice Tests (MockMvc + Web Security + Thymeleaf Rendering)
- **`MailboxControllerTest`**:
  - **Happy Path (Full Page Range Display)**: Navigating to `/mailboxes` with 17 owned mailboxes on page 0 with size 15 renders HTML containing `"Showing 1–15 of 17 mailboxes"`.
  - **Happy Path (German Profile)**: Navigating to `/mailboxes` with authenticated user profile `language = "de"` renders HTML containing `"Zeige 1–15 von 17 Postfächern"`.
  - **Special Case (HTMX Partial Page 2)**: Issuing an HTMX GET request to `/mailboxes?page=1&size=15` with header `HX-Request: true` renders fragment containing `"Showing 16–17 of 17 mailboxes"`.
  - **Special Case (Singular Item)**: Navigating to `/mailboxes` with exactly 1 owned mailbox renders `"Showing 1 of 1 mailbox"` (or `"Zeige 1 von 1 Postfach"` for German user).
  - **Special Case (Multi-page Last Slice Arithmetic)**: Requesting the final page of a 32-item dataset with size 15 renders `"Showing 31–32 of 32 mailboxes"`.
  - **Edge Case (Zero Mailboxes)**: Navigating to `/mailboxes` with 0 mailboxes renders the empty-state container and does not display broken range arithmetic.

### Tier 3: End-to-End System Tests
- **Full Test Suite (`mvn test`)**: Validates entire application lifecycle, database migration stability, security filters, and cross-cutting components.

## Coding Standards

- **Strict `final` Keyword Usage**:
  - All method parameters, local variables, and class fields MUST be marked `final` wherever possible to guarantee immutability and prevent unintended state reassignment.
- **Comprehensive Javadoc**:
  - Every new class, configuration method, and test class/method MUST include complete Javadoc explaining purpose, parameters, return values, and edge conditions.
- **Explanatory Inline Comments**:
  - All key decision branches, fallback paths, and template arithmetic MUST be documented with concise inline comments explaining the rationale.

## Risks / Trade-offs

- **[Risk]** User language code is null, blank, or an unsupported value.
  - **Mitigation**: `UserPreferenceLocaleResolver` strictly defaults to `Locale.ENGLISH` for any value other than `"de"` or when `getLanguage()` is empty.
- **[Risk]** Empty mailbox list (`page.totalElements == 0`).
  - **Mitigation**: Handled cleanly: if `totalElements == 0`, the empty state fragment or block is rendered, and range indicators are omitted.
