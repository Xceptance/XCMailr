# Proposal: Support UTF-8 and Internationalized Email Headers

## Why

When receiving emails via modern SMTP clients (such as `curl`, modern mail user agents, or MTAs) that communicate with SubEthaSMTP's advertised `250-SMTPUTF8` and `250-8BITMIME` extensions, email headers often transmit raw 8-bit UTF-8 octets instead of legacy RFC 2047 ASCII-encoded words. By default, Angus Mail / Jakarta Mail's `LineInputStream` assumes ISO-8859-1 when `mail.mime.allowutf8` is not explicitly enabled.

This causes mojibake (e.g. `Subject: Docker Smoke Test ü` being parsed and stored as `Docker Smoke Test Ã¼`) across the mailbox view, email details, raw headers viewer, attachment filenames, and forwarded outbound emails. Enabling UTF-8 header decoding and international character handling ensures non-ASCII email metadata is parsed, persisted, rendered, and forwarded accurately.

## What Changes

- Enable Jakarta Mail UTF-8 MIME properties globally during application startup (`mail.mime.allowutf8=true`, `mail.mime.charset=UTF-8`, `mail.mime.decodefilename=true`, `mail.mime.decodetext.strict=false`).
- Ensure `JakartaMimeMessageParser.createMimeMessage(...)` and `OutboundMailService.createSession()` explicitly configure these session properties for resilient, defense-in-depth decoding.
- Accurately decode raw 8-bit UTF-8 and RFC 2047 encoded subject headers and sender display names in `MailDeliveryService` and web/REST controllers.
- Support international attachment filenames encoded via raw UTF-8 and RFC 2231/RFC 2047 parameter encoding.
- Ensure outbound email forwarding retains valid UTF-8 headers when relaying inbound emails.
- Implement automated testing following the testing pyramid across all tiers:
  - **Unit level**: Comprehensive happy path (ASCII, Latin-1 supplement, non-Latin scripts, multi-byte emojis, RFC 2047 Base64/Q-encoding, RFC 2231 attachments) and error/edge cases (missing headers, truncated UTF-8 sequences, malformed RFC 2047 tokens, long folded headers).
  - **Integration level**: `MailDeliveryService` database persistence and outbound forwarding under real Spring context.
  - **End-to-End & Presentation level**: Web UI rendering and attachment downloads via MockMvc and real SMTP socket delivery.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `inbound-smtp-engine`: Update requirements to specify support for raw UTF-8 and RFC 2047/RFC 2231 internationalized header decoding, subject and sender extraction, attachment filename handling, error resilience for malformed headers, and outbound forwarding.

## Impact

- **Affected Components**:
  - `XcmailrApplication`: Bootstrap initialization of Jakarta Mail system properties.
  - `JakartaMimeMessageParser`: Session configuration with UTF-8 MIME properties, attachment name decoding, and header extraction.
  - `OutboundMailService`: Outbound session configuration with UTF-8 properties.
  - `MailDeliveryService`: Inbound message metadata extraction for raw UTF-8 and encoded words.
  - `MailWebController`: Web view rendering of UTF-8 headers, subjects, and attachment downloads.
- **Database**: No schema changes required; H2 and PostgreSQL schemas already store character data as UTF-8 and raw messages as binary blobs.
- **APIs & Web UI**: Backward-compatible bugfix; replaces mojibake strings with proper Unicode representations.
