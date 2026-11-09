# Design

## Context

See `proposal.md` for motivation. Currently, the root `pom.xml` defines `<copyright>Copyright (c) ${project.organization.name}</copyright>`, and all 132 `.java` source files and 2 theme `.css` files have yearless copyright headers (`Copyright (c) Xceptance Software Technologies GmbH`). Additionally, `license-maven-plugin:4.6` in the root `pom.xml` specifies `<header>../license-header.txt</header>`, which fails when running `mvn license:check` from the root directory.

## Goals / Non-Goals

**Goals:**
- Align XCMailr's copyright notice format with XLT: `Copyright (c) 2013-2026 Xceptance Software Technologies GmbH`.
- Define `<copyright>Copyright (c) ${project.inceptionYear}-2026 ${project.organization.name}</copyright>` in the root `pom.xml`.
- Modernize the `license-maven-plugin` configuration using `<licenseSets>` with `${maven.multiModuleProjectDirectory}/license-header.txt`, ensuring `mvn license:check` and `mvn license:format` function properly across the entire Maven reactor and submodules.
- Update all 132 `.java` files across `xcmailr-client`, `xcmailr-webapp`, and `xcmailr-load-test-suite` as well as theme CSS files to the `2013-2026` copyright notice.
- Ensure all automated unit and integration tests pass cleanly and license checks pass with zero violations.

**Non-Goals:**
- Modifying third-party or generated assets (such as test output HTML reports or third-party web assets).
- Changing any application runtime logic, APIs, or database structures.

## Decisions

### Decision 1: Format copyright year range using `${project.inceptionYear}-2026`
- **Choice**: Set `<copyright>Copyright (c) ${project.inceptionYear}-2026 ${project.organization.name}</copyright>` in the root `pom.xml`.
- **Rationale**: Replicates the pattern used in XLT (`https://github.com/Xceptance/XLT`), fulfilling the reviewer's requirement to include the year range and maintain organizational consistency.
- **Alternatives Considered**:
  - *Yearless copyright*: Previously used, but explicitly flagged in code review.
  - *Single year (2026)*: Omits the inception year (`2013`), which represents the origin of the project and does not match XLT's convention (`2005-2026`).

### Decision 2: Modernize `license-maven-plugin` with `<licenseSets>` and `${maven.multiModuleProjectDirectory}`
- **Choice**:
  ```xml
  <licenseSets>
      <licenseSet>
          <header>${maven.multiModuleProjectDirectory}/license-header.txt</header>
          <properties>
              <copyright>${copyright}</copyright>
          </properties>
          <includes>
              <include>**/*.java</include>
          </includes>
      </licenseSet>
  </licenseSets>
  ```
- **Rationale**: In multi-module Maven projects, `${maven.multiModuleProjectDirectory}` resolves to the workspace root regardless of whether Maven is invoked from the repository root or within a submodule directory (e.g. `xcmailr-client`). The modern `<licenseSets>` element replaces deprecated direct configuration.
- **Alternatives Considered**:
  - *Keep `../license-header.txt`*: Fails when running commands from the root directory because `../` refers to the parent of the root.
  - *Copying `license-header.txt` to every submodule*: Creates redundant files prone to divergence.

### Decision 3: Update source files and CSS files
- **Choice**: Update the copyright header across all Java source files (132 files) and theme CSS files (`emerald-theme.css`, `xceptance-theme.css`).
- **Rationale**: Ensures every source file in the repository carries the exact notice validated by `license-maven-plugin`.

## Risks / Trade-offs

- **[Risk]** Disruption of existing class or method Javadocs.
  - *Mitigation*: License headers precede the `package` statement and are distinct from class Javadocs (`/** ... */`). All file updates will preserve package statements, imports, and Javadocs without alteration.
- **[Risk]** License check failures on build.
  - *Mitigation*: Validate with `mvn license:check` across all modules before concluding implementation.
