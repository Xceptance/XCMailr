# Tasks

## 1. Core Parser Implementation and Unit Tests

- [x] 1.1 Implement resilient text extraction helper in `JakartaMimeMessageParser` that reads decoded payload bytes via `part.getInputStream()`, detects declared charsets, applies strict UTF-8 decoding with fallback to `ISO-8859-1`, and handles safety fallbacks
- [x] 1.2 Add unit tests for Happy Paths in `JakartaMimeMessageParserTest` verifying raw 8-bit UTF-8 text without `Content-Type` (German umlauts, French accents, CJK, Cyrillic, Arabic, 4-byte emojis), `text/plain` without `charset`, `text/html` without `charset`, and transfer encodings (`quoted-printable`, `base64`)
- [x] 1.3 Add unit tests for Legacy Compatibility and Special Cases in `JakartaMimeMessageParserTest` verifying explicit `charset=ISO-8859-1` retention, empty bodies, and whitespace-only bodies
- [x] 1.4 Add unit tests for Error and Fallback Cases in `JakartaMimeMessageParserTest` verifying malformed multi-byte UTF-8 sequences in untyped bodies fall back gracefully to `ISO-8859-1` without throwing exceptions, and unsupported charset names fall back safely

## 2. Web Controller and REST API Integration Tests

- [x] 2.1 Add integration tests in `MailWebControllerTest` verifying asynchronous mail detail preview (`/mailboxes/{boxId}/mails/{mailId}`) renders untyped UTF-8 plain text and HTML bodies accurately without mojibake
- [x] 2.2 Add integration tests in `MailApiIT` verifying REST API email retrieval (`MailData`) preserves untyped UTF-8 plain text and HTML content without mojibake

## 3. End-to-End SMTP Roundtrip and Full Regression Verification

- [x] 3.1 Add end-to-end SMTP roundtrip test in `ClientSmtpRoundtripIT` transmitting an email without `Content-Type` header containing UTF-8 characters and validating end-to-end receipt
- [x] 3.2 Execute the full automated test suite with `mvn clean test` and verify all tests across all modules pass cleanly

