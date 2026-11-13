# Proposal: Admin Email Statistics Charts

## Why
The administrative statistics page (`/admin/statistics`) currently displays aggregate summary metric cards and a transaction delivery breakdown table, but lacks the visual time-series charts present in legacy XCMailr. Administrators cannot visually inspect email traffic patterns, peak load intervals, or forwarding-versus-drop distributions over the past 24 hours and past 7 days at a glance. Restoring these charts provides immediate operational visibility into email throughput and rejection trends.

## What Changes
- Restore two vertically stacked, full-width time-series line charts on `/admin/statistics`:
  - **Last 24 Hours Activity**: Displays forwarded and dropped email counts aggregated by hour across 24 continuous hourly buckets ending at the current hour.
  - **Last 7 Days Activity**: Displays forwarded and dropped email counts aggregated by day across 7 continuous daily buckets ending on the current day.
- Add Chart.js as a client-side charting dependency via WebJar (`org.webjars.npm:chart.js:4.6.1`) in `xcmailr-webapp/pom.xml`.
- Implement backend data aggregation in `AdminWebController` (supported by `MailTransactionRepository` / `MailStatisticsRepository`) that computes time series buckets and zero-fills periods without email traffic.
- Integrate Chart.js styling with the application's CSS theme tokens, dynamically synchronizing grid lines, tooltip backgrounds, and text colors across light and dark modes.
- Add internationalized chart titles, dataset legends, and axis labels in both English (`en`) and German (`de`).

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Add requirements and scenarios under `Admin Management Console` specifying the visual rendering, dual time-series data streams (forwarded vs. dropped), bucket resolutions (hourly for 24 hours, daily for 7 days), continuous zero-filling, and theme responsiveness for email traffic charts on `/admin/statistics`.

## Impact
- **Dependencies**: Add `org.webjars.npm:chart.js:4.6.1` to `xcmailr-webapp/pom.xml`.
- **Backend Code**:
  - `AdminWebController` (or a dedicated statistics service helper) preparing structured chart view model objects (timestamps/labels, forwarded counts, dropped counts).
  - Time-series aggregation logic leveraging existing `MailTransaction` records (7-day audit retention) or `MailStatistics` records with deterministic timezone handling (UTC / system default).
- **Frontend / Templates**:
  - `xcmailr-webapp/src/main/resources/templates/admin/statistics.html`: Add responsive card containers with `<canvas id="chart-last-24h">` and `<canvas id="chart-last-7d">`.
  - Chart initialization JavaScript leveraging WebJar Chart.js, rendering dual datasets with distinct accent colors, and observing Bootstrap theme mutations (`data-bs-theme`).
- **Localization**:
  - `messages.properties` and `messages_de.properties`: Add keys for chart headings, dataset names (`Forwarded`, `Dropped`), and axis labels.
- **Testing**:
  - Unit tests for time bucket calculations, boundary rounding, and zero-fill logic.
  - WebMvc slice and integration tests verifying chart data presence in the model and canvas elements in the rendered DOM.
