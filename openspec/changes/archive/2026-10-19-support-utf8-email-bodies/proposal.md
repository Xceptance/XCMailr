# Proposal

## Why

When incoming emails are received without an explicit `Content-Type` header (such as plain text smoke tests sent via `curl` or raw SMTP scripts) or with a `Content-Type` lacking a `charset` parameter, Jakarta Mail's underlying `text_plain` and `text_html` content handlers default to `us-ascii` / `iso-8859-1`. This hardcoded fallback ignores system configuration (`mail.mime.charset=UTF-8`), causing raw 8-bit UTF-8 payload bytes (e.g. `0xC3 0xBC` for `ü`) to be decoded into mojibake (`Ã¼`) in both the Web Console preview and REST API responses.

## What Changes

- Update `JakartaMimeMessageParser` to inspect the raw decoded payload byte stream (`part.getInputStream()`) for `text/plain` and `text/html` body parts.
- When no explicit charset is declared in `Content-Type` (or when declared as `us-ascii` while containing 8-bit bytes), decode the body using strict UTF-8 first.
- If strict UTF-8 decoding fails (malformed input), fall back gracefully to `ISO-8859-1` to preserve backward compatibility for legacy non-UTF-8 messages.
- If an explicit non-ASCII charset is declared (e.g. `ISO-8859-1`, `Windows-1252`, `Shift_JIS`), decode using the declared charset.
- Support both plain text and HTML bodies with transfer encodings (`quoted-printable`, `base64`, `7bit`, `8bit`).
- Add comprehensive automated unit and integration tests across the testing pyramid (happy paths, edge cases, error cases, and end-to-end SMTP-to-UI preview).

## Capabilities

### Modified Capabilities
- `inbound-smtp-engine`: Update email MIME extraction requirements to specify resilient UTF-8 body decoding and fallback for emails lacking explicit charset parameters or Content-Type headers.
- `web-console-ui`: Ensure plain text and HTML email inspection preview accurately displays UTF-8 body content without mojibake when headers omit charset specifications.

## Impact

- Affected components:
  - `com.xceptance.xcmailr.util.JakartaMimeMessageParser`: Body extraction logic for `text/plain` and `text/html` parts.
  - `com.xceptance.xcmailr.controllers.MailWebController`: Consumes parsed plain and HTML content for web preview.
  - `controllers.restapi.MailData` and `com.xceptance.xcmailr.controllers.restapi.MailApiController`: Consumes parsed plain and HTML content for REST API queries.
- Dependencies: Jakarta Mail / Angus Mail `Part.getInputStream()`.
- Backward compatibility: Fully preserved for emails with explicit `charset` headers and legacy ISO-8859-1 bodies.

