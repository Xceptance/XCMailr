# Proposal

## Why

When incoming emails without a `Content-Type` header (such as raw messages submitted via `curl`, shell scripts, cron jobs, or monitoring alerts) or with `Content-Type: text/plain` omitting a `charset` parameter are forwarded to external destination addresses, receiving MTAs and email clients (like Mailpit) assume the default MIME character set (`US-ASCII` / `ISO-8859-1`). This causes multi-byte UTF-8 characters (e.g. `üüü`) to be decoded and rendered as mojibake (`Ã¼Ã¼Ã¼`). Ensuring forwarded untyped single-part messages explicitly declare `text/plain; charset=UTF-8` and `MIME-Version: 1.0` resolves this encoding corruption while preserving explicit legacy charsets and leaving multipart messages untouched.

## What Changes

- Normalize forwarded MIME messages in `MailDeliveryService` prior to outbound dispatch:
  - Ensure `MIME-Version: 1.0` is present.
  - If `Content-Type` is absent, set `Content-Type: text/plain; charset=UTF-8`.
  - If `Content-Type` is a single-part text MIME type (`text/*`) lacking a `charset=` parameter, append `; charset=UTF-8`.
  - Leave explicit charsets (e.g., `charset=ISO-8859-1`, `charset=UTF-8`) untouched.
  - Leave multipart messages (`multipart/*`) completely untouched to prevent altering boundary delimiters, nested parts, or attachments.
- Add comprehensive automated tests across the testing pyramid:
  - Unit tests in `MailDeliveryServiceTest` covering happy path (untyped UTF-8 bodies, missing charset parameter), error and edge cases (unparseable headers, missing recipient), and special cases (explicit ISO-8859-1 preservation, multipart message preservation, MIME-Version defaulting).
  - Add clean Javadoc, explanatory code comments, and declare `final` on variables and parameters wherever possible.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `inbound-smtp-engine`: Add requirements and scenarios under `Internationalized Email Forwarding` to guarantee proper UTF-8 charset declarations when forwarding untyped or missing-charset plain text messages, while preserving explicit charsets and multipart structures.

## Impact

- **Affected code**: `MailDeliveryService.java` (forwarding preparation step) and `MailDeliveryServiceTest.java`.
- **APIs and Dependencies**: No changes to public APIs, database schema, or third-party dependencies. Uses standard Jakarta Mail (`MimeMessage`) APIs.
- **Backwards Compatibility**: Fully backwards compatible; explicit charsets and multipart messages are preserved without modification.

