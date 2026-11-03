# Tasks

## 1. Linear Scanner Implementation in MailApiController

- [x] 1.1 Implement the deterministic linear-time character scanner `containsNestedQuantifier(final String regex)` with strict `final` modifier usage, Javadoc, and detailed inline comments in `MailApiController`. Verify with compilation.
- [x] 1.2 Remove constant `NESTED_QUANTIFIER_PATTERN` and update `compileRegex` to use `containsNestedQuantifier` in step 3 of regex validation. Verify with compilation.

## 2. Comprehensive Test Pyramid Coverage

- [x] 2.1 Update and extend unit tests in `MailApiRegexUnitTest` covering happy paths (safe groups, literal search terms), error conditions (nested quantifiers `(a+)+`, `(x*)*`, `([0-9]+)+`, `(a{2,}){2,}`, `((a+))+`), and verify they pass.
- [x] 2.2 Add unit tests for special and boundary cases: repeated unclosed parentheses `((((...` (verifying linear time execution and unclosed parenthesis syntax error rejection), escaped parentheses `\\(a+\\)`, and character classes `[(+)]+` in `MailApiRegexUnitTest`. Verify all unit tests pass.

## 3. Integration & Build Verification

- [x] 3.1 Run end-to-end integration tests in `MailApiIT` and execute full Maven test suite (`mvn clean test`). Verify all 169+ tests pass.
- [x] 3.2 Verify code quality, documentation completeness (Javadoc, comments), and absence of any CodeQL / compiler warnings.

