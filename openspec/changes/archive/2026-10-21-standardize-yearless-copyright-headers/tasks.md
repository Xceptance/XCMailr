# Tasks

## 1. Build Metadata & License Template Alignment

- [x] 1.1 Update root `pom.xml` property `<copyright>` to `Copyright (c) ${project.organization.name}` and verify `license-header.txt` matches the yearless template using `git diff pom.xml license-header.txt`.

## 2. Source File Header Standardization

- [x] 2.1 Update copyright headers across `xcmailr-client` Java sources to `Copyright (c) Xceptance Software Technologies GmbH` and verify with `git grep "Copyright (c)" xcmailr-client/`.
- [x] 2.2 Update copyright headers across `xcmailr-load-test-suite` Java sources to `Copyright (c) Xceptance Software Technologies GmbH` and verify with `git grep "Copyright (c)" xcmailr-load-test-suite/`.
- [x] 2.3 Update copyright headers across `xcmailr-webapp` main/test Java sources and `xceptance-theme.css` to `Copyright (c) Xceptance Software Technologies GmbH` and verify with `git grep "Copyright (c)" xcmailr-webapp/`.

## 3. Verification & Integrity

- [x] 3.1 Inspect git diff to verify that all class-level Javadocs (`/** ... */`), method comments, and Apache 2.0 license body text remain completely intact with zero unintended modifications.
- [x] 3.2 Execute `mvn clean test` across all modules to verify clean compilation and passing test suites.

