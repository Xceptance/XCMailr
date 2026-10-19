# Proposal: Adopt Standard HTMX 4 Explicit Inheritance

## Why

After upgrading to HTMX 4.0.0, enabling `implicitInheritance: true` caused child elements (such as `<tr>` rows in `mail-list.html` and `all-mail-list.html`) to inadvertently inherit `hx-trigger="mailListChanged from:body"` from their ancestor container `<div id="mail-list-container">`. This prevented HTMX from attaching default `click` listeners to email rows, breaking the inline email inspection in `#mail-viewer-pane`. Reverting to HTMX 4's standard explicit inheritance model eliminates accidental trigger leakage and ensures robust, predictable hypermedia interactions across all UI views.

## What Changes

- **Remove `implicitInheritance: true`**: Delete the `implicitInheritance: true` property from `<meta name="htmx-config">` and inline `<script>` configuration in `layout/base.html`, letting HTMX 4 follow its standard explicit inheritance architecture while keeping `noSwap: [204, 304, '4xx', '5xx']`.
- **Add explicit `hx-trigger="click"` to email table rows**: Explicitly annotate `<tr class="mail-item-row" ... hx-trigger="click">` in both `mailboxes/fragments/mail-list.html` and `mailboxes/fragments/all-mail-list.html` for clarity and defensive robustness.
- **Update client contract test**: Remove the assertion requiring `implicitInheritance: true` in `HtmxCsrfClientContractTest.java`.
- **Add email preview slice test**: Add a controller slice test in `MailWebControllerTest.java` verifying that `GET /mailboxes/{id}/mails/{mailId}` with `HX-Request: true` returns the email detail fragment (`mailboxes/fragments/mail-detail :: detail`).
- **Add browser-level click-to-preview contract test**: Extend `HtmxCsrfClientContractTest.java` using HtmlUnit to load the email list, click `#mail-row-{mailId}`, and assert that `#mail-viewer-pane` is dynamically updated with the message detail.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Update hypermedia client configuration requirements to adopt standard explicit inheritance and formalize email selection and preview swap requirements.

## Impact

- **Affected Templates**: `layout/base.html`, `mailboxes/fragments/mail-list.html`, `mailboxes/fragments/all-mail-list.html`.
- **Affected Tests**: `HtmxCsrfClientContractTest.java`, `MailWebControllerTest.java`.
- **Runtime Dependencies**: No dependency version changes (`org.webjars.npm:htmx.org:4.0.0` is retained).
- **Breaking Changes**: None. All templates in the project already declare target `hx-*` attributes explicitly without relying on inherited attributes.

