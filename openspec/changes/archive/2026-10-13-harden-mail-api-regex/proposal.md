# Proposal

## Why

The REST API endpoint for inspecting received emails (`GET /api/v1/mails`) allows client-provided regular expressions to filter sender, subject, body, and header fields. Passing untrusted, arbitrary regular expressions to Java's backtracking regex engine (`java.util.regex.Pattern.compile`) without size and complexity constraints triggers CodeQL security warnings (`java/regex-injection`) and leaves the application susceptible to Regular Expression Denial of Service (ReDoS) through catastrophic backtracking. 

Hardening the regex compilation with structural pre-checks (length limits and nested quantifier detection) protects application availability while preserving full backward compatibility for legitimate test automation filters and satisfying CodeQL analysis.

## What Changes

- **Regex Length Validation**: Enforce a maximum length limit of 255 characters on all query parameter regex strings (`from`, `subject`, `textContent`, `htmlContent`, `mailHeader`), returning HTTP 400 Bad Request when exceeded.
- **Catastrophic Backtracking Heuristic Guard**: Reject potentially exponential backtracking constructs (such as nested quantifiers like `(a+)+`, `(x*)*`, or `(a|aa)+`) with HTTP 400 Bad Request before pattern compilation.
- **CodeQL Alert Suppression**: Add inline CodeQL alert suppression comments (`// codeql[java/regex-injection]`) on the validated `Pattern.compile` statement documenting the intentional authenticated API search feature.
- **Comprehensive Test Pyramid Coverage**: Add unit and slice integration tests verifying happy path filtering, error rejection (syntax errors, length violations, ReDoS constructs), and special edge cases (null/empty filters, max length limits, multi-flag behavior).
- **Code Standards**: Use `final` modifiers on all parameters and local variables, and provide comprehensive code comments and Javadoc.

## Capabilities

### Modified Capabilities

- `mailbox-management-api`: Enhance `REST Mail Inspection and Attachment Download` requirement with regex validation constraints, rejection of oversized and dangerous ReDoS expressions, and validation error contracts.

## Impact

- **Affected Code**: `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/restapi/MailApiController.java` and related test suites (`MailApiIT.java` and new unit slice tests).
- **API Contract**: Fully backward-compatible for legitimate search patterns. Reject malicious or oversized regexes with structured HTTP 400 Bad Request.
- **Dependencies & Performance**: Zero new dependencies. Minimal CPU overhead during validation pre-checks.

