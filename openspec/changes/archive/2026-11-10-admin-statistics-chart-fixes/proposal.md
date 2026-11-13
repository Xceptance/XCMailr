# Proposal

## Why

Following the restoration of the 24-hour and 7-day activity charts in the administration email statistics dashboard, the chart canvases failed to render in client browsers (remaining blank white boxes). Investigation revealed:
1. A critical syntax error (`const systemPrefers dark = ...`) in `layout/base.html` that breaks early theme preference initialization and throws an `Uncaught SyntaxError` on all page loads.
2. Synchronous inline script execution of Chart.js initialization before the DOM and container layout bounding boxes have been computed.
3. Outdated Chart.js options (`tooltip.theme` and `scales.*.grid.borderColor`) that do not conform to Chart.js 4 specifications.

This change fixes these client-side defects to ensure that charts render reliably across desktop and mobile viewports with full theme support and resilience.

## What Changes

- **Fix syntax error in `layout/base.html`**: Correct `systemPrefers dark` to camelCase `systemPrefersDark` in the `<head>` theme initialization script.
- **Harden Chart.js initialization lifecycle**: Wrap chart instantiation in `document.addEventListener('DOMContentLoaded', ...)` (or execute immediately if `document.readyState !== 'loading'`), ensuring layout dimensions are fully calculated before rendering.
- **Modernize Chart.js 4 options**:
  - Replace obsolete `plugins.tooltip.theme` with explicit Chart.js 4 tooltip styling tokens (`backgroundColor`, `titleColor`, `bodyColor`, `borderColor`, `borderWidth`) synchronized with Bootstrap light and dark themes.
  - Update axis border lines from deprecated `scales.*.grid.borderColor` to `scales.*.border.color`.
- **Add fallback / diagnostic indicator**: Render an accessible fallback notice inside chart containers if Chart.js is blocked or unavailable in the client environment.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Ensure client-side theme initialization and Chart.js 4 activity charts initialize without syntax errors, adhere to DOM readiness lifecycle, and use standard Chart.js 4 theme tokens.

## Impact

- `layout/base.html`: Fixes syntax error in theme script in `<head>`.
- `admin/statistics.html`: Refactors chart initialization lifecycle and Chart.js 4 configuration options.
- No backend API, controller, or database schema changes.
