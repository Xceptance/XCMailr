# Proposal

## Why

Code review feedback indicated that copyright headers across XCMailr lack the year range ("no year in copyright"), and requested alignment with how XLT manages copyright notices. Restoring the inception-to-current year range (`2013-2026`) establishes consistent legal attribution with XLT, updates Maven build configuration to support root and submodule license verification, and aligns all source and theme files with organizational standards.

## What Changes

- Update `<copyright>` property in root `pom.xml` to include the year range: `Copyright (c) ${project.inceptionYear}-2026 ${project.organization.name}`.
- Modernize `license-maven-plugin` configuration in root `pom.xml` to use `<licenseSets>` with `${maven.multiModuleProjectDirectory}/license-header.txt`, ensuring `mvn license:check` and `mvn license:format` execute cleanly across reactor root and submodules.
- Update copyright header comments across all 132 `.java` files in `xcmailr-client`, `xcmailr-webapp`, and `xcmailr-load-test-suite` from `Copyright (c) Xceptance Software Technologies GmbH` to `Copyright (c) 2013-2026 Xceptance Software Technologies GmbH`.
- Update copyright header comments in theme CSS files (`emerald-theme.css`, `xceptance-theme.css`) to reflect the `2013-2026` year range.
- Preserve all existing class-level, method-level Javadocs, architectural comments, and method parameter/variable constraints intact.

## Capabilities

### New Capabilities
None. This change affects only licensing header notices, theme comments, and Maven plugin configuration; runtime system capabilities are unchanged.

### Modified Capabilities
None. No behavioral requirements or system specifications are modified (`skip_specs: true`).

## Impact

- **Affected files**:
  - `pom.xml` (root POM property `<copyright>` and `license-maven-plugin` configuration)
  - Java source files across `xcmailr-client`, `xcmailr-webapp`, and `xcmailr-load-test-suite` (132 files)
  - Theme CSS files in `xcmailr-webapp/src/main/resources/static/css/` (`emerald-theme.css`, `xceptance-theme.css`)
- **APIs & Runtime Behavior**: None. No public or internal APIs, database entities, or application logic are altered.
- **Dependencies**: No dependency changes.
