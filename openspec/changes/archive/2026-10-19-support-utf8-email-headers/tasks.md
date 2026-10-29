# Tasks

## 1. Global & Session MIME Configuration and Unit Tests (Base of Pyramid)

- [x] 1.1 Configure Jakarta Mail system properties (`mail.mime.allowutf8`, `mail.mime.charset`, `mail.mime.decodefilename`, `mail.mime.decodetext.strict`) in `XcmailrApplication` bootstrap and verify properties are applied on startup.
- [x] 1.2 Implement helper method in `JakartaMimeMessageParser` to create default UTF-8 session properties, update `createMimeMessage` to use it, and update `OutboundMailService.createSession()` with UTF-8 MIME properties.
- [x] 1.3 Create unit test suite `JakartaMimeMessageParserTest` verifying happy path cases: raw 8-bit UTF-8 headers (umlauts, accents, non-Latin scripts, emojis), RFC 2047 Base64/Quoted-Printable encoded words, sender display names, and attachment filenames.
- [x] 1.4 Expand `JakartaMimeMessageParserTest` with error and edge cases: missing/empty subjects, missing sender, truncated UTF-8 sequences, malformed RFC 2047 tokens, long folded headers (CRLF + whitespace continuations), and attachment filenames with path traversal attempts.

## 2. Inbound Processing & Service Integration Tests (Middle of Pyramid)

- [x] 2.1 Update `MailDeliveryService` subject and originator extraction to decode raw 8-bit UTF-8 headers and internationalized address display names using `toUnicodeString()`.
- [x] 2.2 Add integration tests in `MailDeliveryServiceTest` verifying happy path: inbound SMTP delivery of raw UTF-8 messages (`Docker Smoke Test ü`) persists properly decoded strings in H2 database and forwards to outbound recipients with intact UTF-8 headers.
- [x] 2.3 Add integration tests in `MailDeliveryServiceTest` verifying error and edge cases: emails with truncated/malformed UTF-8 header octets do not fail delivery, raw message payloads are preserved, and existing mailbox status controls remain unaffected.

## 3. End-to-End & Presentation Tests (Top of Pyramid)

- [x] 3.1 Update `MailWebControllerTest` with MockMvc tests verifying happy path: email list table, detail preview modal, and raw headers view properly render unmangled UTF-8 characters without escaping issues.
- [x] 3.2 Add tests in `MailWebControllerTest` for attachment download endpoint verifying HTTP `Content-Disposition` header formatting for UTF-8 filenames and error handling for out-of-bounds attachment index.
- [x] 3.3 Add or update end-to-end SMTP delivery test in `SmtpDeliveryIT` sending raw 8-bit UTF-8 email over a live socket connection and verifying end-to-end database persistence.
- [x] 3.4 Run the full Maven test suite (`mvn clean test`) to verify zero regressions across the entire application.
