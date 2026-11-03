# Design: Standardize Yearless Copyright Headers

## Context

Source files throughout XCMailr contain differing copyright notice formats (`2013-2023`, `2020`, `2013-2026`). In addition, the root `pom.xml` contains a hardcoded copyright property (`<copyright>Copyright (c) ${project.inceptionYear}-2023 ${project.organization.name}</copyright>`) with `license-maven-plugin` disabled by default (`<license.skipFormat>true</license.skipFormat>`).

See `proposal.md` for motivation and rationale.

## Goals / Non-Goals

**Goals:**
- Standardize all source file copyright headers across `xcmailr-webapp`, `xcmailr-client`, and `xcmailr-load-test-suite` to the yearless format: `Copyright (c) Xceptance Software Technologies GmbH`.
- Align `license-header.txt` and root `pom.xml` properties with the yearless convention.
- Ensure all existing Javadoc comments (`/** ... */`), inline code explanations, and Apache License 2.0 boilerplate terms remain completely intact.
- Verify full project compilation and test suite pass post-update.

**Non-Goals:**
- Do not modify the full text of `license.md` (Apache License 2.0).
- Do not touch external third-party attribution comments (e.g., framework comments in `messages.properties` or test resources like `test_for_upload.txt`).
- Do not make any code, logic, or architectural changes.

## Decisions

### Decision 1: Yearless Header Notice Format
- **Choice**:
  ```java
  /*
   * Copyright (c) Xceptance Software Technologies GmbH
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *     http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
  ```
- **Rationale**: Complies with Apache 2.0 attribution recommendations without requiring annual file modification. Matches modern conventions adopted by major open-source ecosystems.
- **Alternatives Considered**:
  - *Keep year range (`2013-2026`)*: Requires repetitive git churn and manual/automated updates every January.
  - *Inception year only (`Copyright (c) 2013`)*: Static, but suggests no work has been done since 2013.

### Decision 2: Update Build Metadata & Configuration
- **Choice**:
  - Update root `pom.xml`: `<copyright>Copyright (c) ${project.organization.name}</copyright>`.
  - Keep `<inceptionYear>2013</inceptionYear>` in `pom.xml` as informational project metadata.
  - Update `license-header.txt` to align with `${copyright}`.
- **Rationale**: Keeps project metadata accurate and ensures any automated license tooling generates the correct header.

### Decision 3: Safe Batch Application and Verification
- **Choice**: Update headers across target source files (`.java` and relevant `.css`), checking diffs to guarantee that leading license comments are updated while adjacent class Javadoc blocks (`/** ... */`) are never touched or stripped.
- **Rationale**: Automated transformations risk stripping Javadoc if regex boundaries are greedy. Verifying `git diff` boundaries and compiling ensures zero collateral damage.

## Risks / Trade-offs

- **[Risk] Unintended removal or truncation of class Javadoc comments**:
  - *Mitigation*: Strictly delimit the replacement to the leading file comment block (`/* Copyright (c) ... */`) prior to package statements; never touch `/** ... */` Javadoc blocks.
- **[Risk] Large git diff across modules**:
  - *Mitigation*: Isolate all header updates into this dedicated change with clean commits, making review straightforward.

## Migration Plan

1. Update `license-header.txt` and root `pom.xml`.
2. Apply yearless header replacement across all Java files and `xceptance-theme.css`.
3. Review `git diff` to verify only copyright notice lines were changed.
4. Run `mvn clean test` to ensure compile and test integrity.

