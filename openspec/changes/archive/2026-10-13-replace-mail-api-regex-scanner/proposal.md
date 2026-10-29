# Proposal

## Why

GitHub Advanced Security / CodeQL flagged a High-severity alert (`Polynomial regular expression used on uncontrolled data`, CWE-1333) on `MailApiController.java` line 399:
```java
if (NESTED_QUANTIFIER_PATTERN.matcher(regex).find())
```
The heuristic regular expression `\([^)]*[*+{][^)]*\)[*+{]` intended to detect ReDoS constructs in client-supplied search filters is itself susceptible to polynomial ($O(n^2)$) backtracking when evaluated against unclosed sequences of parentheses (e.g. `((((((...`). To eliminate this vulnerability and pass security review cleanly, we must replace the regex-based check with a deterministic, linear-time $\mathcal{O}(n)$ character scanner.

## What Changes

- **Replace Regex Heuristic with Iterative Scanner**: Remove `NESTED_QUANTIFIER_PATTERN` and implement an iterative, single-pass character scanner method `containsNestedQuantifier(final String regex)` in `MailApiController`.
- **Linear-Time Guarantee**: The scanner iterates through the input string in $\mathcal{O}(n)$ time without recursion, backtracking, or regex engine invocation, strictly bounded by `MAX_REGEX_LENGTH` (255 characters).
- **Escape and Character Class Awareness**: The scanner correctly recognizes escape sequences (`\(`) and character classes (`[...]`), ensuring nested repetitions are detected accurately without false positives or polynomial degradation.
- **Strict Code Style**: Enforce `final` on all parameters and local variables, and provide comprehensive Javadoc and explanatory inline code comments.
- **Testing Pyramid Coverage**: Expand unit and boundary test suites in `MailApiRegexUnitTest` and integration tests in `MailApiIT` to verify happy paths, error cases (ReDoS patterns, unclosed parentheses), and special edge cases (repeated unclosed parentheses `((((...`, escaped quantifiers, character classes).

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `mailbox-management-api`: Refine regular expression ReDoS pre-validation requirements to mandate deterministic, linear-time scanning for nested quantifier patterns without polynomial regex evaluation.

## Impact

- **Affected Code**: `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/restapi/MailApiController.java`
- **Tests**: `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/restapi/MailApiRegexUnitTest.java`, `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/restapi/MailApiIT.java`
- **Security & CI**: Resolves CodeQL alert CWE-1333 (`java/polynomial-redos`) completely on `MailApiController`.
- **API Compatibility**: Fully backward compatible; existing valid queries continue to work identical to before, and ReDoS patterns continue to be rejected with HTTP 400 Bad Request.

