# Design

## Context

See `proposal.md` for motivation and background. XCMailr's web console relies on Spring Boot 4.x, Thymeleaf 3, and HTMX. The locale resolution pipeline (`UserPreferenceLocaleResolver`) resolves user language preferences dynamically (`en` vs `de`). Currently, however, view templates hardcode English string literals across layouts, forms, buttons, and tables, with only a small number of mailbox pagination strings localized.

## Goals / Non-Goals

**Goals:**
- Provide complete internationalization (English default and German) across all 18 Thymeleaf templates and fragments in `xcmailr-webapp`.
- Establish a clean, hierarchical dot-notation naming standard (`layout.*`, `auth.*`, `profile.*`, `mailbox.*`, `mail.*`, `admin.*`, `error.*`, `common.*`) in `conf/messages.properties` and `conf/messages_de.properties`.
- Enforce OWASP security standards by ensuring all dynamic localized messages and parameters use automatic HTML escaping (`th:text`, `th:placeholder`, `th:title`, `th:aria-label`), avoiding unescaped `th:utext`.
- Implement a comprehensive testing pyramid:
  - Unit tests validating translation bundle key parity and parameter placeholder alignment.
  - Integration and controller tests (`MockMvc`) verifying localized markup rendering for German and English across happy paths, validation/error conditions, and edge cases.
- Follow strict Java conventions: `final` on all local variables, parameters, and test constants, with clear Javadoc and explanatory comments.

**Non-Goals:**
- Introducing languages beyond English and German at this time.
- Modifying backend REST APIs (`/api/v1/...`) or database schemas.
- Deprecating or deleting existing legacy message keys that may be referenced by older components.

## Decisions

### 1. Hierarchical Dot-Notation Message Key Standard
- **Decision**: Adopt namespaced, lowercase dot-notation for all newly introduced keys:
  - `layout.nav.*`, `layout.footer.*`
  - `auth.login.*`, `auth.register.*`, `auth.password.*`
  - `profile.*`, `profile.token.*`, `profile.delete.*`
  - `mailbox.*`, `mailbox.modal.*`, `mailbox.batch.*`
  - `mail.*`, `mail.detail.*`
  - `admin.users.*`, `admin.domains.*`, `admin.transactions.*`, `admin.stats.*`
  - `common.button.*`, `common.alert.*`
- **Alternatives Considered**: 
  - *Reusing Ninja underscore keys (`mailbox_Header_Address`)*: Rejected because the legacy naming is inconsistent, mixed-case, and lacks uniform hierarchical structure.
  - *Using English phrases as keys*: Rejected because it leads to fragile templates whenever English phrasing is adjusted.

### 2. Thymeleaf Escaping & OWASP XSS Prevention
- **Decision**: All template references use standard Thymeleaf message resolution:
  - `th:text="#{key}"` or `th:text="#{key(${param})}"`
  - `th:placeholder="#{key}"`
  - `th:title="#{key}"`
  - `th:aria-label="#{key}"`
- **Security Rule**: Never use `th:utext` for localized messages or user input interpolation. Thymeleaf's default XML/HTML escaping prevents malicious payloads in user-supplied data (such as email addresses, user names, or query strings) from triggering script injection.

### 3. Testing Pyramid Strategy
- **Unit Level (`MessageBundleParityTest`)**:
  - Validates that every key in `messages.properties` has an exact match in `messages_de.properties`.
  - Verifies that parameter placeholder counts (e.g. `{0}`, `{1}`) match between English and German for every message.
  - Verifies that no keys have empty or whitespace-only translation values.
- **Controller & Integration Level (`WebConsoleLocalizationIntegrationTest`)**:
  - Happy Path Tests: Validates full HTML response rendering for authenticated German users across `/mailboxes`, `/mails`, `/profile`, and `/admin/**`, asserting German text in navigation, buttons, table headers, and badges.
  - Error Cases & Status Banners: Validates localized alerts on `/login` (`?error`, `?logout`, `?accountDeleted`), registration validation errors, and invalid token alerts.
  - Edge Cases & Missing Keys: Validates that fallback to default English occurs seamlessly without template rendering errors.

### 4. Code Standards
- All newly added Java test classes and test methods will use `final` modifiers for all variable declarations, method parameters, and constants.
- Full class-level and method-level Javadoc will document intent, preconditions, and assertions.

## Risks / Trade-offs

- **[Risk] Missing translation key at runtime causes raw `??key_de??` in view**:
  - *Mitigation*: The `MessageBundleParityTest` unit test executes during build and fails if any key is missing or blank in either bundle.
- **[Risk] Parameter count mismatch causes `IllegalArgumentException` during message formatting**:
  - *Mitigation*: The automated parity test parses and checks format tokens `{0}`, `{1}` across both properties files.
- **[Risk] Large template diff across 18 templates**:
  - *Mitigation*: Changes are grouped logically by template domain with automated controller tests ensuring no regressions in layout structure or HTMX attributes.

