# Design

## Context

GitHub Advanced Security / CodeQL flagged a High-severity polynomial ReDoS alert (`java/polynomial-redos`, CWE-1333) on `MailApiController.java` line 399. The heuristic regular expression `\([^)]*[*+{][^)]*\)[*+{]` intended to catch ReDoS in user input backtracks quadratically ($O(n^2)$) on unclosed sequences of open parentheses (`((((((...`). See `proposal.md` for motivation.

## Goals / Non-Goals

**Goals:**
- Completely eliminate CodeQL CWE-1333 on `MailApiController` by removing `NESTED_QUANTIFIER_PATTERN` and replacing it with an iterative Java scanner.
- Guarantee deterministic $\mathcal{O}(n)$ execution time bounded by `MAX_REGEX_LENGTH` (255 characters) with zero backtracking.
- Accurately detect nested repetition constructs (such as `(a+)+`, `(x*)*`, `([0-9]+)+`, `(a{2,}){2,}`) while correctly permitting safe expressions (e.g. `(user|admin)@example.com`, `.*\\d{6}.*`).
- Correctly handle escape characters (`\\(`) and character classes (`[...]`).
- Maintain testing pyramid standards with happy path, error, and boundary/special case tests.
- Follow code style guidelines: `final` on all variables and parameters, explicit Javadoc, and detailed code comments.

**Non-Goals:**
- Building a full regular expression parser or compiler: the scanner only inspects structural repetition nesting. Full syntax validation is delegated to `Pattern.compile`.
- Modifying REST API contract or endpoints: error responses remain HTTP 400 Bad Request with identical payload format.

## Decisions

### Decision 1: Pure Java Linear Character Scanner vs Regex Refinement

- **Chosen Approach**: Implement `static boolean containsNestedQuantifier(final String regex)` as a single-pass character scanner loop.
- **Alternatives Considered**:
  - *Hardened Possessive Regex (`\\([^()]*+[*+{][^()]*+\\)[*+{]`)*: While possessive quantifiers prevent runtime backtracking, CodeQL's static analyzer frequently flags regexes operating on untrusted data if repetition constructs are present. Using pure Java code guarantees CodeQL cannot trigger regex ReDoS rules on this line.
  - *Inline CodeQL Suppression Comment*: Simply suppressing the alert leaves the $\mathcal{O}(n^2)$ backtracking behavior in production code. A deterministic scanner fixes the root cause.
- **Rationale**: A deterministic loop is mathematically immune to backtracking and completely removes the regex evaluation from the check.

### Decision 2: Scanner State Tracking

- **Mechanism**:
  - Use a boolean array `final boolean[] groupHasQuantifier = new boolean[MAX_REGEX_LENGTH + 1]` indexed by parenthesis depth `groupDepth`.
  - Maintain `inEscape` flag for backslash escaping (`\(`).
  - Maintain `inCharClass` flag for square brackets (`[...]`), where repetition symbols are literal characters.
  - When encountering an unescaped quantifier (`*`, `+`, `{`) while `groupDepth > 0`, mark `groupHasQuantifier[groupDepth] = true`.
  - When encountering an unescaped `)` while `groupDepth > 0`:
    - Record whether the closed group had a quantifier.
    - Propagate quantifier presence to the parent group (`groupDepth - 1`).
    - Inspect the immediate next non-whitespace character: if it is a quantifier (`*`, `+`, `{`), return `true` immediately.
  - Returns `false` upon completing the single pass if no nested quantifiers were found.

## Risks / Trade-offs

- **[Risk] Unclosed brackets or parentheses in malformed input**:
  → *Mitigation*: The scanner safely handles unclosed constructs without throwing exceptions or looping infinitely; any syntax errors are subsequently caught and reported by `Pattern.compile` in step 4 of `compileRegex`.
- **[Risk] False positives on escaped quantifiers or character classes**:
  → *Mitigation*: Scanner tracks `inEscape` and `inCharClass` state so `\+` or `[+]` do not flag safe groups.

## Testing Strategy

- **Unit Tests (`MailApiRegexUnitTest`)**:
  - *Happy paths*: Literal searches, token extractors, safe alternation groups `(user|admin)`.
  - *Error paths*: `(a+)+`, `(x*)*`, `([0-9]+)+`, `(a{2,}){2,}`, nested groups `((a+))+`.
  - *Special / Boundary cases*: 255-character inputs consisting of repeated unclosed parentheses `((((...` verifying linear time and syntax rejection; escaped parentheses `\\(a+\\)+`; character classes with quantifiers `[(+)]+`.
- **Integration Tests (`MailApiIT`)**:
  - End-to-end HTTP 400 responses on `/api/v1/mails` when query parameters contain nested quantifiers or malformed regex.

