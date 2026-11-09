# Proposal: Configurable UI Theming

## Why

XCMailr currently hardcodes its corporate theme stylesheet (`/css/xceptance-theme.css`) directly within the master Thymeleaf layout (`layout/base.html`). Organizations, internal teams, and developers who wish to deploy XCMailr with alternative visual identities or showcase different color themes cannot switch stylesheets without manually modifying and recompiling the application template.

Providing a configurable UI theming mechanism allows operators to switch the active theme via standard Spring Boot configuration (`application.yml` and environment variables), demonstrates the flexibility of the modernized frontend architecture to the development team, and allows custom corporate themes to be added simply by dropping a new CSS file onto the classpath without touching Java code or templates.

## What Changes

- **Spring Boot Configuration Property**: Introduce `xcmailr.ui.theme: ${XCMAILR_UI_THEME:xceptance}` in `application.yml` with clear inline documentation of its usage.
- **Dynamic Theme Resolution & Security Sanitization**: Implement a safe theme resolver bean that:
  - Sanitizes the theme name against a safe slug regex pattern (`^[a-zA-Z0-9_-]+$`) to prevent path traversal or injection.
  - Verifies whether `/static/css/{theme}-theme.css` exists on the classpath using Spring's `ResourceLoader`.
  - Automatically falls back to the default `xceptance` theme with a logged warning if the requested stylesheet does not exist, preventing broken unstyled HTML.
- **Master Template Dynamic Linking**: Update `layout/base.html` to dynamically link the active theme stylesheet resolved from configuration.
- **Alternative Demo Theme (`emerald`)**: Introduce `src/main/resources/static/css/emerald-theme.css` featuring a modern developer-tool aesthetic (mint/emerald green `#059669`, dark mode `#10b981`, Inter/system typography), providing a striking, professional visual contrast against the default corporate navy blue while honoring the same CSS custom properties and light/dark color mode toggles.
- **Documentation**: Document the `xcmailr.ui.theme` configuration property, environment variable overrides (`XCMAILR_UI_THEME`), and instructions for adding custom theme stylesheets in `README.md`.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Add requirements and scenarios for configurable web UI theme selection, dynamic stylesheet resolution, fallback behavior for missing/invalid theme names, and out-of-the-box support for the alternative `emerald` theme.

## Impact

- **Configuration**: New configuration property `xcmailr.ui.theme` (and environment variable `XCMAILR_UI_THEME`).
- **Templates**: `xcmailr-webapp/src/main/resources/templates/layout/base.html` dynamically resolves the theme stylesheet.
- **Static Assets**: New stylesheet `xcmailr-webapp/src/main/resources/static/css/emerald-theme.css`.
- **Java**: New helper component/advice to safely resolve and verify theme existence on the classpath.
- **Documentation**: Updated `application.yml` and `README.md`.
- **Backward Compatibility**: Fully backward-compatible; defaults to `xceptance` with no change in visual appearance when unconfigured.
