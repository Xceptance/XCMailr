# Spec Delta

## MODIFIED Requirements

### Requirement: Internationalized Email Forwarding
The system SHALL preserve UTF-8 headers, international characters, and message bodies when forwarding incoming emails to target destination addresses, ensuring untyped single-part messages declare UTF-8 character encoding while preserving explicit legacy charsets and multipart structures.

#### Scenario: Forward Email with UTF-8 Subject and Headers
- **WHEN** an incoming email with UTF-8 subject or headers is delivered to a mailbox configured for email forwarding
- **THEN** the system forwards the email via outbound SMTP preserving valid UTF-8 encoding in the forwarded message headers

#### Scenario: Forward Email with International Sender Display Name
- **WHEN** an incoming email with non-ASCII characters in the sender display name is forwarded
- **THEN** the system formats and emits the outbound `From` header preserving the Unicode display name

#### Scenario: Forward Untyped Plain Text Email Body
- **WHEN** an incoming email contains an untyped plain text body without a `Content-Type` header and is forwarded
- **THEN** the outbound forwarded message declares `Content-Type: text/plain; charset=UTF-8` and `MIME-Version: 1.0` so receiving MTAs and email clients decode body characters accurately

#### Scenario: Forward Email Body with Missing Charset Parameter
- **WHEN** an incoming single-part text email contains `Content-Type: text/plain` or `text/html` lacking an explicit `charset` parameter
- **THEN** the outbound forwarded message appends `; charset=UTF-8` to the Content-Type header

#### Scenario: Forward Email Body with Explicit Charset Preservation
- **WHEN** an incoming email specifies an explicit character set (such as `ISO-8859-1` or `UTF-8`) in its Content-Type header
- **THEN** the outbound forwarded message preserves the original Content-Type header and charset declaration unmodified

#### Scenario: Forward Multipart Email Preservation
- **WHEN** an incoming email has a `multipart/*` content type
- **THEN** the outbound forwarded message preserves the top-level multipart Content-Type header, boundary parameter, and nested body parts without alteration

