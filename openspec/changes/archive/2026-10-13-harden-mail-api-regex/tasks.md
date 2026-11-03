# Tasks

## 1. Controller Hardening & Validation Pipeline

- [x] 1.1 Define `MAX_REGEX_LENGTH` (255) and `NESTED_QUANTIFIER_PATTERN` in `MailApiController.java` with complete Javadoc, inline comments, and `final` modifier. Verify with `mvn test-compile`.
- [x] 1.2 Update `compileRegex()` in `MailApiController.java` to implement the multi-stage validation pipeline (blank check, length <= 255 check, nested quantifier check, syntax exception check) with `// codeql[java/regex-injection]` suppression comment, full Javadoc, and `final` modifiers on all parameters and local variables. Verify with `mvn test-compile`.

## 2. Unit Tests (Pyramid Base: Fast Isolated Tests)

- [x] 2.1 Create unit test class covering happy paths (standard wildcards, token matching, character classes, blank/empty inputs returning null). Verify with `mvn test -Dtest=MailApiRegexUnitTest`.
- [x] 2.2 Add unit test cases for error conditions (length > 255 throwing `RegexValidationException`, nested quantifiers like `(a+)+` and `(x*)*` throwing `RegexValidationException`, syntax errors throwing `RegexValidationException`). Verify with `mvn test -Dtest=MailApiRegexUnitTest`.
- [x] 2.3 Add unit test cases for special cases (exact 255-character valid regex boundary, multiline and dot-all flag matching behavior). Verify with `mvn test -Dtest=MailApiRegexUnitTest`.

## 3. Integration Tests (Pyramid Middle: HTTP API Contracts)

- [x] 3.1 Add integration tests in `MailApiIT.java` for error scenarios (query parameter exceeding 255 characters returning 400 Bad Request, query parameter with nested quantifier returning 400 Bad Request with field name). Verify with `mvn test -Dtest=MailApiIT`.
- [x] 3.2 Add integration tests in `MailApiIT.java` for happy paths and special cases (exact 255-character pattern returning 200 OK, blank/omitted regex parameter returning 200 OK without filtering error). Verify with `mvn test -Dtest=MailApiIT`.

## 4. Verification

- [x] 4.1 Run full webapp test suite to verify all unit and integration tests pass without regressions. Verify with `mvn test`.

