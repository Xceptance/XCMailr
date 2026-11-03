# Design

## Context

See `proposal.md` for motivation.

When an incoming email lacks a `Content-Type` header (such as `curl` plain-text test payloads) or specifies `Content-Type: text/plain` or `text/html` without a `charset` parameter, Jakarta Mail defaults to `text/plain; charset=us-ascii` per RFC 2045 §5.2. Angus Mail's content handler (`org.eclipse.angus.mail.handlers.text_plain`) hardcodes a fallback from `null` charset to `us-ascii` / `iso-8859-1`, reading raw 8-bit UTF-8 bytes into ISO-8859-1 chars and producing mojibake (e.g. `0xC3 0xBC` becomes `\u00C3\u00BC`, rendered as `Ã¼`).

Both the web console UI (`MailWebController`) and the REST API (`MailApiController` via `MailData`) extract email text bodies using `JakartaMimeMessageParser`.

## Goals / Non-Goals

**Goals:**
- Enable `JakartaMimeMessageParser` to extract plain text and HTML bodies with correct UTF-8 decoding when `Content-Type` or `charset` parameters are omitted.
- Maintain full compatibility with emails declaring explicit legacy charsets (e.g. `ISO-8859-1`, `Windows-1252`, `Shift_JIS`).
- Maintain resilient fallback to `ISO-8859-1` when untyped bodies contain malformed or non-UTF-8 bytes.
- Preserve support for all standard MIME transfer encodings (`quoted-printable`, `base64`, `7bit`, `8bit`).
- Follow the testing pyramid with automated unit, error/edge case, and integration roundtrip tests.

**Non-Goals:**
- Modifying Angus Mail or Jakarta Mail third-party library binaries.
- Overriding the global Java Activation Framework `CommandMap` system-wide.
- Modifying how raw email bytes are stored in the `mail` database table (raw message fidelity is preserved as-is).
- Modifying outbound SMTP email forwarding payload transformation.

## Decisions

### Decision 1: Direct Stream Decoding in `JakartaMimeMessageParser`
Instead of calling `(String) part.getContent()` for `text/plain` and `text/html` parts, read the decoded payload stream using `part.getInputStream()`.
- **Rationale**: `part.getInputStream()` automatically transparently decodes MIME transfer encodings (`quoted-printable`, `base64`, `7bit`, `8bit`) and returns the underlying payload bytes. This allows `JakartaMimeMessageParser` to control character decoding without Angus Mail's hardcoded `us-ascii` fallback intervening.
- **Alternatives Considered**:
  - *Post-hoc Mojibake Repair*: Converting the mojibake string back to ISO-8859-1 bytes and re-decoding as UTF-8. Rejected because it is heuristic, fragile, and risks corrupting deliberate text containing Latin-1 characters.
  - *Custom Activation Framework CommandMap*: Registering custom `DataContentHandler` classes in the Java activation registry. Rejected because it alters global JVM activation state, is difficult to isolate across test boundaries, and couples to internal handler APIs.

### Decision 2: Charset Resolution Hierarchy
Implement the following resolution order for text parts:
1. **Explicit Charset Check**: Extract the `Content-Type` header from `part.getHeader("Content-Type")`. If present, parse with `jakarta.mail.internet.ContentType` to extract the `charset` parameter.
2. **Explicit Non-ASCII Charset**: If `charset` is present, not blank, and not case-insensitively `"us-ascii"`:
   - Attempt decoding with `Charset.forName(charset)`.
   - If the charset is unsupported or decoding fails, fall back to step 3.
3. **Untyped / Default Charset Handling**: If `charset` is missing, blank, or `"us-ascii"`:
   - Attempt strict UTF-8 decoding using `StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)`.
   - If UTF-8 decoding succeeds, return the UTF-8 string.
   - If UTF-8 decoding fails with `CharacterCodingException` (indicating legacy 8-bit Latin-1 / Windows-1252 bytes that violate UTF-8 grammar), fall back cleanly to `StandardCharsets.ISO_8859_1`.
4. **Safety Net**: If stream reading fails unexpectedly, fall back to `(String) part.getContent()`.

```text
+-------------------------------------------------------------+
|                Email Part (text/*)                          |
+------------------------------+------------------------------+
                               |
                               v
                Explicit charset parameter present?
               /                                   \
             YES (not us-ascii)                     NO (or us-ascii)
              |                                            |
              v                                            v
+-----------------------------+             +-------------------------------+
| Decode with declared        |             | Read part.getInputStream()    |
| Charset.forName(charset)    |             +---------------+---------------+
+-----------------------------+                             |
                                                            v
                                                   Strict UTF-8 Decoder
                                                  /                    \
                                              SUCCESS                MALFORMED
                                                |                        |
                                                v                        v
                                          Return UTF-8           Fallback ISO-8859-1
```

### Decision 3: Testing Pyramid Architecture
- **Unit Tests (`JakartaMimeMessageParserTest`)**:
  - Happy paths: untyped email without `Content-Type` containing German umlauts, French accents, non-Latin scripts (Cyrillic, CJK, Arabic), and 4-byte emojis.
  - Happy paths: `Content-Type: text/plain` and `text/html` without `charset` parameter.
  - Happy paths: Transfer encodings (`base64` and `quoted-printable`) without explicit `charset`.
  - Legacy compatibility: `Content-Type: text/plain; charset=ISO-8859-1` with genuine Latin-1 bytes decoded as ISO-8859-1.
  - Error and edge cases: malformed multi-byte UTF-8 sequences in untyped body fall back gracefully to ISO-8859-1 without throwing exceptions; empty body; whitespace-only body.
- **Integration Tests (`ClientSmtpRoundtripIT` & `MailWebControllerTest`)**:
  - Deliver untyped raw UTF-8 email via SMTP and verify retrieval via REST API and Web Console preview fragment `/mailboxes/{id}/mails/{id}` without mojibake.

## Risks / Trade-offs

- **[Risk] Memory usage for large email bodies** → *Mitigation*: Incoming email payloads are strictly enforced against `mbox.maxSize` (configured via `properties.getMbox().getMaxSize()`) before MIME parsing, ensuring stream reading cannot cause memory exhaustion.
- **[Risk] Accidental UTF-8 misinterpretation of legacy Latin-1 text** → *Mitigation*: If an email explicitly declares `charset=iso-8859-1`, that declared charset is honored directly without UTF-8 heuristics. For untyped messages, valid multi-byte UTF-8 byte sequences almost never occur by chance in natural Latin-1 text.

