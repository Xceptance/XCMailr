# Design

## Context

The global Thymeleaf base layout (`base.html`) renders a corporate footer that currently displays only `&copy; Xceptance Software Technologies GmbH. All rights reserved.`.

In earlier modernization steps, hardcoded copyright years (`2013-2026`) and internal runtime metadata (`XCMailr Modernized • Java 25 • Spring Boot 4.x • HTMX`) were removed. However, omitting the application version and copyright year left the application without build traceability and standard corporate attribution.

The application already uses Spring MVC `@ControllerAdvice` to expose global model attributes across all views (e.g. `ThemeResolver` provides `activeTheme`).

See `proposal.md` for motivation and `specs/web-console-ui/spec.md` for the behavior specification.

## Goals / Non-Goals

**Goals:**
- Provide dynamic, maintenance-free copyright year range calculation (`2013-${currentYear}`) without requiring manual annual edits.
- Expose application version to all web views using standard Spring Boot `BuildProperties` with a resilient fallback for test and development profiles.
- Render a balanced single-line corporate footer with localized copyright and version labels and a distinct CSS class (`xcmailr-version`).
- Clean up dead Ninja `version.ftl.html` resource filtering from `xcmailr-webapp/pom.xml`.
- Update contract and web controller tests to assert the presence of the year range and version while preserving negative assertions against framework/runtime technology disclosure.

**Non-Goals:**
- Re-introducing internal technology stack disclosure (Java runtime, Spring Boot version, or HTMX library names).
- Modifying CSS styling, theme palette variables, or navigation layouts.
- Adding interactive or collapsible footer elements.

## Decisions

### Decision 1: Controller Advice for Dynamic Global View Attributes
- **Choice**: Create `AppInfoAdvice` annotated with `@ControllerAdvice` in `com.xceptance.xcmailr.config` exposing `@ModelAttribute("copyrightYear")` and `@ModelAttribute("appVersion")`.
- **Rationale**: Isolates global application metadata resolution in a dedicated, testable Spring component rather than cluttering individual web controllers or relying on hardcoded template strings.
- **Year Calculation**: `String.format("2013-%d", Year.now().getValue())`. This ensures the current year rolls over automatically at midnight on January 1st without rebuilds or commits.
- **Alternatives Considered**:
  - *Hardcoding `2013-2026` in `base.html`*: Reintroduces annual maintenance churn.
  - *Injecting year via JavaScript in the client*: Prone to flash of unstyled content (FOUC), requires client script execution, and complicates server-side contract testing.

### Decision 2: Spring Boot BuildProperties with Resilient Fallback
- **Choice**: Enable `<goal>build-info</goal>` in `spring-boot-maven-plugin` inside `xcmailr-webapp/pom.xml`. Inject `@Autowired(required = false) BuildProperties buildProperties` into `AppInfoAdvice`, falling back to `@Value("${xcmailr.app.version:4.0.0}")` and `Package.getImplementationVersion()`.
- **Rationale**: In packaged JARs and production deployments, `build-info.properties` accurately reflects the Maven project version (`${project.version}`). During lightweight unit tests or IDE runs where Maven packaging has not executed, the fallback ensures tests never fail with `NoSuchBeanDefinitionException` or null pointers.
- **Alternatives Considered**:
  - *Maven resource filtering in `application.yml`*: Prone to IDE discrepancies and delimiter conflicts.
  - *Reading `pom.properties` manually*: Requires custom classpath I/O logic that is redundant with Spring Boot's built-in `BuildProperties`.

### Decision 3: Single-Line Balanced Footer Layout
- **Choice**: Structure the footer markup in `base.html` as:
  ```html
  <footer class="footer-xceptance text-center">
      <div class="container">
          <p class="mb-0">
              &copy; <span th:text="${copyrightYear}">2013-2026</span> <a href="https://www.xceptance.com" target="_blank">Xceptance Software Technologies GmbH</a>.
              <span th:text="#{layout.footer.copyright}">All rights reserved.</span> &bull;
              <span th:text="#{layout.footer.version}">Version</span>: <span class="xcmailr-version" th:text="${appVersion}">4.0.0</span>
          </p>
      </div>
  </footer>
  ```
- **Rationale**: Clean, unobtrusive, responsive, and matches Option A selected during exploration. Adding `.xcmailr-version` allows automated tests and browser scrapers to target the version element cleanly.

### Decision 4: Invert Contract Test Assertions
- **Choice**: Update `BaseLayoutFooterContractTest` to positively assert `containsString("2013-")`, `containsString("Version:")`, and `containsString("xcmailr-version")` across public (`/login`), authenticated (`/mailboxes`), and error (`/error`) views. Retain negative assertions against `Java 25`, `Spring Boot`, and `XCMailr Modernized`.
- **Rationale**: Guarantees regression protection across all view categories without relaxing security information disclosure boundaries.

## Risks / Trade-offs

- **[Risk]** `BuildProperties` bean is missing in `BaseLayoutFooterContractTest` and unit tests if tests run before Maven package phase.
  - **Mitigation**: `AppInfoAdvice` uses optional autowiring (`@Autowired(required = false)`) with a default version fallback (`4.0.0`), guaranteeing consistent model attributes in all environments.
- **[Risk]** Text overflow on very narrow viewports (<320px).
  - **Mitigation**: Bootstrap's default paragraph wrapping inside `.container.text-center` breaks gracefully onto subsequent lines on small viewports without horizontal overflow.
