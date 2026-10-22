# Proposal

## Why

Source files in XCMailr currently contain inconsistent copyright year ranges (`2013-2023`, `2020`, `2013-2026`) in their license headers. Maintaining yearly copyright ranges requires annual churn and review across the codebase without legal or functional necessity. Adopting yearless copyright notices standardizes licensing attribution to Xceptance Software Technologies GmbH, eliminates future annual maintenance overhead, and maintains full compliance with the Apache License 2.0 while keeping all Javadoc and code comments intact.

## What Changes

- Update `license-header.txt` to use the yearless copyright notice: `${copyright}` where `${copyright}` expands to `Copyright (c) ${project.organization.name}`.
- Update root `pom.xml` property `<copyright>` to `Copyright (c) ${project.organization.name}` (removing year ranges).
- Standardize all existing Java source files across `xcmailr-webapp` and `xcmailr-client` to the yearless header format: `Copyright (c) Xceptance Software Technologies GmbH`.
- Preserve all existing class-level and method-level Javadocs, architectural comments, and inline code explanations.
- Ensure the project builds cleanly and all automated tests continue to pass.

## Capabilities

### New Capabilities
None. This change affects only licensing header comments and build metadata; runtime capabilities are unchanged.

### Modified Capabilities
None. No behavioral requirements or system specifications are modified (`skip_specs: true`).

## Impact

- **Affected files**: Root `pom.xml`, `license-header.txt`, and Java source files under `xcmailr-webapp` and `xcmailr-client`.
- **APIs & Runtime Behavior**: None. No public or internal APIs, database entities, or application logic are altered.
- **Dependencies**: No dependency changes.

