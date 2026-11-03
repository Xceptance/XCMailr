# Design: Support UTF-8 and Internationalized Email Headers

## Context

See [proposal.md](file:///Users/rbaumgarten/checkouts/XCMailr/openspec/changes/support-utf8-email-headers/proposal.md) for motivation.

SubEthaSMTP 7.2.2 advertises `250-SMTPUTF8` and `250-8BITMIME`. Modern SMTP clients (e.g. `curl`, MTAs, modern mail clients) take advantage of this and transmit headers containing international characters as raw 8-bit UTF-8 octets.

In Angus Mail 2.0.5 (the Jakarta Mail 2.1 implementation), `LineInputStream` defaults `allowutf8` to `false`. When reading header bytes with `allowutf8 == false`, raw 8-bit octets are converted into chars using ISO-8859-1 (Latin-1), turning UTF-8 byte sequences like `0xC3 0xBC` (`ü`) into `\u00C3\u00BC` (`Ã¼`).

## Goals / Non-Goals

**Goals:**
- Ensure incoming emails with raw UTF-8 headers (e.g. `Subject: ... ü`) are decoded accurately to Unicode Java Strings without mojibake.
- Support both raw 8-bit UTF-8 and RFC 2047 encoded-word headers (`=?UTF-8?B?...?=` / `=?UTF-8?Q?...?=`).
- Ensure sender display names containing non-ASCII characters (e.g. `From: "Jürgen" <sender@external.com>`) are decoded properly.
- Ensure attachment filenames containing UTF-8 characters (either raw or RFC 2231 encoded) are decoded properly.
- Ensure outbound email forwarding preserves valid UTF-8 headers and avoids re-encoding corrupted strings.
- Provide defense-in-depth configuration across both JVM-level system properties and Jakarta Mail `Session` properties.
- Establish an automated testing suite following the testing pyramid covering happy path, error handling, and special internationalization cases.

**Non-Goals:**
- Altering the database schema: the existing H2 and PostgreSQL schemas already store `subject` and `sender` as UTF-8 character columns and `message` as raw binary `bytea` / `blob`.
- Altering SMTP envelope validation: mailbox routing rules and email address syntactic constraints remain unchanged.

## Decisions

### 1. Defense-in-Depth Property Configuration (Global System Properties + Session Defaults)
- **Decision**: Configure the MIME properties both globally at application startup and explicitly on every `Session` created by `JakartaMimeMessageParser` and `OutboundMailService`.
- **Properties**:
  - `mail.mime.allowutf8 = "true"`: Instructs `LineInputStream` and `InternetHeaders` to decode raw 8-bit header bytes using UTF-8 instead of ISO-8859-1.
  - `mail.mime.charset = "UTF-8"`: Sets the default MIME charset to UTF-8.
  - `mail.mime.decodefilename = "true"`: Enables decoding of attachment filenames per RFC 2231 and RFC 2047.
  - `mail.mime.decodetext.strict = "false"`: Permissively decodes slightly malformed encoded words rather than failing or leaving them mangled.
- **Rationale**: Setting system properties in `XcmailrApplication` ensures any background, third-party, or transitive code calling Jakarta Mail defaults to UTF-8. Setting explicit properties on `Session.getInstance(...)` ensures unit tests, isolated services, or environments without global property initialization also behave consistently.
- **Alternatives Considered**:
  - *System properties only*: Fragile if unit tests run in isolation without bootstrapping the main application class.
  - *Session properties only*: Certain static utility methods in Jakarta Mail (`MimeUtility.decodeText`, etc.) consult `System.getProperty` directly if no active session is in thread context.

### 2. Sender and Subject Header Extraction
- **Decision**: In `MailDeliveryService`, extract `subject` and `originator` using `mimeMessage.getSubject()` and `InternetAddress.toUnicodeString()` (or decoded display name) while handling both raw UTF-8 and RFC 2047 encoded words.
- **Rationale**: `InternetAddress.toUnicodeString()` properly formats address display names with non-ASCII characters, preventing address formatting corruption.

### 3. Attachment Filename Handling
- **Decision**: In `JakartaMimeMessageParser`, verify that `part.getFileName()` returns decoded filenames and fallback to `MimeUtility.decodeText(fileName)` if needed when `mail.mime.decodefilename=true`.
- **Rationale**: Ensures filenames containing umlauts, accents, or Asian characters (e.g., `Prüfbericht.pdf`) are rendered and downloaded with the intended name.

## Testing Strategy (Testing Pyramid)

To guarantee robustness without regressions, testing is organized into three distinct tiers following the testing pyramid:

```
        / \
       /   \     E2E / Presentation Tier (Top)
      / Web \    - MockMvc Web UI rendering (UTF-8 subjects, detail preview, headers)
     / & REST\   - Attachment download endpoint verification
    /─────────\  - Real SMTP socket delivery (SubEthaSMTP listener)
   /           \
  / Integration \ Service Integration Tier (Middle)
 /    Tier       \ - MailDeliveryService persistence with H2 DB
/─────────────────\- OutboundMailService forwarding with UTF-8 preservation
/                   \
/     Unit Tier      \ Unit Tier (Base - fast, isolated, comprehensive)
/      (Parser &      \- Happy Path: Latin-1, non-Latin scripts, Emojis, RFC 2047 B/Q, RFC 2231
/      Properties)     \- Error Cases: Malformed UTF-8 octets, broken RFC 2047, missing headers
/───────────────────────\- Special Cases: Header folding, filename sanitization, mixed text
```

### 1. Unit Testing Tier (Base of Pyramid)
Fast, isolated, in-memory tests executing in `JakartaMimeMessageParserTest` and `OutboundMailServiceTest` without Spring context overhead:
- **Happy Path Cases**:
  - Raw 8-bit UTF-8 headers with Latin-1 supplement characters (`ä`, `ö`, `ü`, `ß`, `é`, `ñ`).
  - Raw 8-bit UTF-8 headers with non-Latin alphabets and scripts (Cyrillic `Тест`, Japanese `テスト`, Arabic).
  - 4-byte UTF-8 emojis (`🚀`, `🎉`, `📬`, `🇩🇪`).
  - RFC 2047 encoded words: Base64 (`=?UTF-8?B?...?=`) and Quoted-Printable (`=?UTF-8?Q?...?=`).
  - International sender display names in `From` header: raw UTF-8 (`"Jürgen Schmidt" <j@example.com>`) and RFC 2047.
  - Attachment filenames: raw UTF-8 (`prüfbericht.pdf`), RFC 2231 continuation parameters (`filename*0*=UTF-8''...`), and RFC 2047 encoded filenames.
- **Error Cases**:
  - Missing or empty `Subject` header (`Subject:` or omitted altogether) returns empty string / safe default.
  - Missing or malformed `From` header falls back to envelope sender without throwing unhandled exceptions.
  - Truncated or malformed UTF-8 byte sequences in header octets: decodes with safe fallback replacement characters without crashing.
  - Malformed RFC 2047 tokens (unclosed `=?`, invalid Base64 padding): handled permissively due to `mail.mime.decodetext.strict=false`.
- **Special Cases**:
  - RFC 5322 header folding: long UTF-8 subjects split across multiple lines with CRLF and continuation whitespace are parsed into a single unfolded Unicode string.
  - Filenames containing path traversal attempts (`../../evil.pdf`) or reserved shell characters are sanitized and safely resolved.
  - Mixed ASCII and multi-byte UTF-8 text with special MIME characters (quotes, semicolons, brackets).

### 2. Service Integration Tier (Middle of Pyramid)
Component-level integration tests in `MailDeliveryServiceTest` with in-memory H2 database:
- **Happy Path Cases**:
  - Full email ingestion with raw UTF-8 headers (`Subject: Docker Smoke Test ü`): message is parsed, saved via `MailRepository`, and verified via JPA entity query confirming exact Unicode character preservation.
  - Forwarding an inbound UTF-8 email to an external recipient: verifies `OutboundMailService` receives a properly configured `MimeMessage` with intact UTF-8 headers.
- **Error & Special Cases**:
  - Ingestion of emails with malformed UTF-8 byte sequences does not abort delivery or roll back transaction; raw message bytes remain intact in the database and metadata remains queryable.
  - Existing delivery controls (mailbox expiration, inactive user suppression, relay protection, loop prevention) continue functioning identically when messages contain UTF-8 content.

### 3. End-to-End & Presentation Tier (Top of Pyramid)
Web and socket-level verification:
- **Happy Path Cases**:
  - `MailWebControllerTest` (MockMvc): verifies that `/mailboxes/{id}/mails` renders unmangled UTF-8 subjects and sender names in the email list table HTML.
  - `MailWebControllerTest` (MockMvc): verifies that `/mailboxes/{id}/mails/{mailId}` renders the email preview modal with unescaped/unescaped UTF-8 subject, sender, HTML/plain body, and raw headers view.
  - Attachment download endpoint (`/mailboxes/{id}/mails/{mailId}/attachments/{index}`): verifies HTTP response headers contain properly quoted/formatted UTF-8 `Content-Disposition: attachment; filename="prüfbericht.pdf"`.
  - `SmtpDeliveryIT`: live socket delivery over TCP port sending raw UTF-8 bytes to SubEthaSMTP and verifying end-to-end database persistence.
- **Error & Special Cases**:
  - Emails with empty subject rendered as fallback indicator in web view.
  - Downloading attachment with invalid index returns HTTP 404 cleanly.

## Risks / Trade-offs

- **[Risk]** Inbound emails from legacy non-conformant clients sending raw 8-bit characters in ISO-8859-1 without advertising a charset.
  - **Mitigation**: Modern MTAs negotiating `SMTPUTF8` require UTF-8. If an invalid UTF-8 byte sequence is encountered, Angus Mail falls back gracefully.
- **[Risk]** Test pollution if system properties are mutated during individual test execution.
  - **Mitigation**: Set properties idempotently in application bootstrap, and configure default session properties explicitly on every `Session.getInstance(...)`.

## Migration Plan

1. No schema or database migration is required.
2. Deploy the modernized jar or container; newly received emails and outbound forwards immediately benefit from UTF-8 decoding.
