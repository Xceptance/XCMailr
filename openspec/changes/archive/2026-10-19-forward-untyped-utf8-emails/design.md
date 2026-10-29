# Design

## Context

Incoming email delivery in `MailDeliveryService.deliver()` ingests raw message bytes and persists them unmodified into the `mail` table. When forwarding is activated on the destination mailbox, `MailDeliveryService` instantiates a new `MimeMessage` using `outboundMailService.createSession()` and the inbound `rawBytes`. It configures routing headers (`To`, `Sender`, `From`, `Reply-To`, `X-FORWARDED-FROM`, `X-Loop`, `Auto-Submitted`) and invokes `outboundMailService.send(forwardMsg)`.

For raw emails received without a `Content-Type` header or with `Content-Type: text/plain` omitting a `charset` parameter, `forwardMsg` relays the message without declaring a UTF-8 character encoding. Per RFC 2045 §5.2 and RFC 2046 §4.1.2, downstream MTAs and clients (such as Mailpit) fall back to `US-ASCII` / `ISO-8859-1`, corrupting multi-byte UTF-8 sequences (e.g. `üüü` rendered as `Ã¼Ã¼Ã¼`).

See `proposal.md` for the motivation and `specs/inbound-smtp-engine/spec.md` for requirement contracts.

## Goals / Non-Goals

**Goals:**
- Provide pragmatic, lightweight header normalization in `MailDeliveryService` immediately before outbound dispatch.
- Ensure untyped single-part messages declare `Content-Type: text/plain; charset=UTF-8` and `MIME-Version: 1.0`.
- Ensure single-part text messages lacking a `charset=` parameter have `; charset=UTF-8` appended.
- Preserve explicit charsets (e.g. `charset=ISO-8859-1`, `charset=UTF-8`) and multipart structures (`multipart/*`) completely untouched.
- Follow the testing pyramid with comprehensive happy path, error/edge case, and special case unit tests.
- Maintain code quality with complete Javadoc, descriptive code comments, and `final` modifiers on all variables and parameters.

**Non-Goals:**
- Unpacking, modifying, or reassembling multipart MIME trees (overengineering that risks corrupting binary attachments, boundaries, or signatures).
- Heuristic byte-by-byte charset detectors (in modern environments, untyped email bodies sent over modern systems are UTF-8).
- Modifying stored raw email bytes in the database (raw inbound payload remains pristine).

## Decisions

### Decision 1: Lightweight Top-Level MIME Header Normalization
Normalize headers directly on `forwardMsg` within `MailDeliveryService` prior to `outboundMailService.send(forwardMsg)`:
```java
// Ensure MIME-Version is present
if (forwardMsg.getHeader("MIME-Version") == null)
{
    forwardMsg.setHeader("MIME-Version", "1.0");
}

// Normalize untyped or missing-charset single-part messages
final String contentType = forwardMsg.getHeader("Content-Type", null);
if (contentType == null)
{
    forwardMsg.setHeader("Content-Type", "text/plain; charset=UTF-8");
}
else if (!contentType.toLowerCase().contains("multipart/") && !contentType.toLowerCase().contains("charset="))
{
    forwardMsg.setHeader("Content-Type", contentType + "; charset=UTF-8");
}
```
**Rationale**:
- Minimal touchpoint (< 15 lines of code) with zero runtime performance overhead.
- Preserves the message body byte stream bit-for-bit without re-serializing attachments or re-encoding characters.
- Non-intrusive: strictly checks for `multipart/` and existing `charset=`.

**Alternatives Considered**:
- *Raw byte/string manipulation before creating `MimeMessage`*: Parsing raw email bytes with string splitting or regex risks corrupting header folding (CRLF followed by whitespace) and message boundaries.
- *Recursive traversal and manipulation of multipart parts*: Complex, prone to breaking attachments or signatures, and unnecessary because tools capable of producing multipart emails already declare charsets.

### Decision 2: Test Pyramid Implementation in `MailDeliveryServiceTest`
Structure unit tests into distinct test groups reflecting the testing pyramid:
1. **Happy Path Tests**:
   - Forwarding an untyped single-part UTF-8 email (no `Content-Type` header, containing `üüü`). Verify outbound message has `Content-Type: text/plain; charset=UTF-8` and `MIME-Version: 1.0`.
   - Forwarding a single-part email with `Content-Type: text/plain` lacking `charset`. Verify outbound message has `Content-Type: text/plain; charset=UTF-8`.
   - Forwarding a single-part email with `Content-Type: text/html` lacking `charset`. Verify outbound message has `Content-Type: text/html; charset=UTF-8`.
2. **Error and Edge Case Tests**:
   - Forwarding when recipient mailbox is inactive or missing (verify forward is suppressed and no outbound message dispatched).
   - Inbound email with empty/whitespace-only body or minimal headers (verify normalization does not throw exceptions).
3. **Special Cases**:
   - Forwarding an email with explicit `Content-Type: text/plain; charset=ISO-8859-1` (verify charset is preserved unmodified).
   - Forwarding an email with explicit `Content-Type: text/plain; charset=UTF-8` (verify header is preserved unmodified).
   - Forwarding a `multipart/alternative` email (verify top-level multipart header and boundary are preserved without adding charset).

## Risks / Trade-offs

- **[Risk]**: A legacy client sends an untyped message encoded in ISO-8859-1.
  - **Mitigation**: Modern operating systems and MTAs default to UTF-8. In the rare case of legacy 8-bit text without headers, standard internet mail practice assumes UTF-8. Senders relying on legacy single-byte encodings should declare their charset parameter.
- **[Risk]**: Header mutation interfering with existing transport encoding.
  - **Mitigation**: Modifying `Content-Type` on the `MimeMessage` header table before calling `saveChanges()` / `send()` allows Jakarta Mail to manage transport streaming without re-encoding existing body bytes.

## Open Questions

None. The scope and implementation are fully confirmed.

