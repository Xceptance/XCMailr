# Proposal

## Why

The current table pagination and page size selector controls use standard Bootstrap 5 `.pagination` styles with default saturated electric blue (`#0d6efd`) active background blocks and bright blue link text. Because pagination and page density selectors are secondary utility controls at the bottom of the table, this loud electric blue styling dominates the viewport and competes visually with the primary tabular data. Modern web interfaces (e.g. GitHub, Linear, Stripe) employ neutral, calm monochrome styling for secondary table controls to keep visual focus on data.

## What Changes

- Replace default electric blue `.pagination` styles with modern calm monochrome / dark charcoal styling tokens across both corporate (`xceptance-theme.css`) and developer (`emerald-theme.css`) themes.
- Unselected pagination numbers, navigation controls (`Previous`, `Next`), and size presets (`5`, `15`, `25`, `50`, `all`) will render with dark neutral body text (`var(--bs-body-color)`), white card background, subtle border (`var(--xc-border-color)`), and subtle soft hover wash (`var(--bs-tertiary-bg)`).
- Active page numbers and active page size presets will render with a confident dark charcoal background (`#212529` in light mode, `#334155` in dark mode) and crisp white text.
- Disabled items (`...`, disabled `Previous`/`Next`, and the `Display Count` label) will use secondary muted text (`var(--xc-text-muted)`) and subtle background.
- Update `web-console-ui` specification to define the neutral monochrome visual contract for table pagination and display count controls.
- Add and update web integration tests verifying modern CSS classes and theme tokens.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Update requirement for table pagination and display count selectors to mandate calm monochrome / dark charcoal styling tokens instead of saturated primary color fills.

## Impact

- Affected stylesheets: `xcmailr-webapp/src/main/resources/static/css/xceptance-theme.css`, `xcmailr-webapp/src/main/resources/static/css/emerald-theme.css`.
- Affected views: All 5 tabular views (`/mailboxes`, `/admin/users`, `/admin/transactions`, `/mailboxes/{id}/mails`, `/mails`) automatically inherit the modernized styling without template or controller refactoring.
- Testing: Web integration tests and theme verification tests.
