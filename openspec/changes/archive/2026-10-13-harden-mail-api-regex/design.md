# Design

## Context

`MailApiController` in `xcmailr-webapp` provides `GET /api/v1/mails` with query parameters (`from`, `subject`, `textContent`, `htmlContent`, `mailHeader`) compiled via `compileRegex()`. 

Clients such as `xcmailr-client` intentionally supply regular expressions to match dynamic email tokens in automated test suites. However, CodeQL flags `Pattern.compile` as `java/regex-injection` because arbitrary user input can contain catastrophic backtracking expressions (e.g., `(a+)+$`).

## Goals / Non-Goals

**Goals:**
- Provide structural pre-checks before pattern compilation: enforce length boundaries (<= 255 characters) and reject nested quantifier patterns prone to ReDoS.
- Return structured HTTP 400 Bad Request errors identifying the invalid parameter name and reason.
- Add CodeQL alert suppression (`// codeql[java/regex-injection]`) directly on the `Pattern.compile` statement.
- Maintain 100% backward compatibility for all legitimate test automation search expressions.
- Adhere to project code style: declare `final` on all method parameters and local variables, and provide comprehensive Javadoc and inline comments.
- Build comprehensive test coverage across the testing pyramid (happy paths, error cases, and special cases).

**Non-Goals:**
- Replacing Java's native regex engine with external linear-time engines (e.g., RE2/J).
- Altering the search functionality or removing regex support in favor of substring-only search (`Pattern.quote`).
- Changing web console mail filtering (which is database-driven).

## Decisions

### 1. In-Code CodeQL Suppression Over Literal Escaping (`Pattern.quote`)
- **Decision**: Add `// codeql[java/regex-injection]` directly above `Pattern.compile` instead of escaping expressions with `Pattern.quote()`.
- **Rationale**: `Pattern.quote()` escapes all regex metacharacters, effectively converting regex queries into literal string searches. This would break `xcmailr-client` and all existing test suites that use wildcards, character sets, or digit matchers (e.g. `\d{6}`). The in-code suppression documents the intentional API capability while satisfying CodeQL scanning.
- **Alternatives Considered**:
  - `Pattern.quote()`: Breaks API contract and client test suites.
  - GitHub UI alert dismissal: Transient and lost across repository forks or CI rebuilds.

### 2. Multi-Stage Regex Validation Pipeline
- **Decision**: Validate expressions sequentially in `compileRegex()`:
  1. **Null/Empty check**: Return `null` immediately.
  2. **Length limit**: If `regex.length() > MAX_REGEX_LENGTH (255)`, throw `RegexValidationException`.
  3. **ReDoS structural heuristic**: If the regex contains nested repetition (e.g., matching `NESTED_QUANTIFIER_PATTERN`), throw `RegexValidationException`.
  4. **Syntax compilation**: Call `Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL)` inside try-catch block for `PatternSyntaxException`.
- **Rationale**: Static heuristics catch dangerous backtracking patterns before the regex engine compiles them, preventing any CPU strain with minimal latency.
- **Alternatives Considered**:
  - Runtime execution timeout wrapper (`TimeoutCharSequence`): More complex to maintain and only intercepts during matching, whereas static rejection prevents compilation and execution upfront.

### 3. ReDoS Nested Quantifier Heuristic Pattern
- **Decision**: Use a pre-compiled `static final Pattern NESTED_QUANTIFIER_PATTERN = Pattern.compile("\\([^)]*[*+{][^)]*\\)[*+{]");` to detect nested repetition constructs (such as `(a+)+`, `(x*)*`, `([0-9]+)+`, etc.).
- **Rationale**: Exponential catastrophic backtracking requires a quantified repetition applied over an already quantified group. Detecting this syntax catches malicious or accidental ReDoS patterns while allowing un-nested repetitions and standard character classes.

### 4. Strict Testing Pyramid Strategy
- **Decision**: Add comprehensive tests across three levels:
  - **Happy Paths**: Typical test patterns (`.*Verification Code: \d+.*`, domain patterns, plain text and HTML body queries) matching emails as expected.
  - **Error Cases**: Expressions exceeding 255 chars, nested quantifier patterns (`(a+)+`, `(x*)*`), and syntactically malformed patterns (`[unclosed`).
  - **Special Cases**: Boundary lengths (exactly 255 chars), empty/whitespace strings, and multi-line/dot-all flag interactions.

## Risks / Trade-offs

- **[Risk]** Valid regexes containing benign nested groups might be rejected by the heuristic.  
  → **Mitigation**: Legitimate email search filters are almost exclusively linear expressions (e.g., `Order #\d+`, `.*token=[a-z0-9]+.*`). The heuristic specifically checks for repeated groups with outer quantifiers; standard grouped patterns without outer quantifiers (e.g., `(foo|bar)`) remain fully valid.
- **[Risk]** Subtle polynomial ReDoS without nested quantifiers.  
  → **Mitigation**: The 255-character maximum length restriction bounds the search space, preventing polynomial backtracking from causing noticeable CPU stalls.

