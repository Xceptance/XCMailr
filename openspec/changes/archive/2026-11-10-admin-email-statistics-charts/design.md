# Design: Admin Email Statistics Charts

## Context
See [proposal.md](proposal.md) for motivation and problem background.

Currently, the administration statistics endpoint (`GET /admin/statistics`) in `AdminWebController.java` queries basic summary totals (total users, active mailboxes, stored messages, total transactions) and a grouped status breakdown table via `MailTransactionRepository.getStatusCounts()`. The visual line charts displaying email traffic trends over the past 24 hours and 7 days present in legacy XCMailr were lost during the framework modernization.

The application uses Spring Boot 3.3.4, Thymeleaf 3, Bootstrap 5.3.3, and HTMX. Client-side libraries are bundled via WebJars (`org.webjars:bootstrap`, `org.webjars.npm:bootstrap-icons`, `org.webjars.npm:htmx.org`). Application styling supports light and dark modes controlled via the `data-bs-theme` attribute on the root `<html>` element.

## Goals / Non-Goals

**Goals:**
- Deliver two full-width, vertically stacked time-series line charts on `/admin/statistics`:
  1. **Last 24 Hours**: Hourly aggregation (24 continuous hourly buckets ending at the current hour).
  2. **Last 7 Days**: Daily aggregation (7 continuous daily buckets ending on the current day).
- Display dual time series per chart: Forwarded (`status == 300`) and Dropped (`status != 300`).
- Ensure continuous unbroken zero baseline by pre-allocating time buckets and zero-filling periods without activity.
- Integrate Chart.js 4.6.1 via WebJar (`org.webjars.npm:chart.js:4.6.1`) matching project WebJar conventions.
- Synchronize chart canvas styling (grid line colors, tick labels, legend text) dynamically with light/dark theme toggles.
- Fully internationalize chart titles, dataset legends, and axis descriptions in English (`en`) and German (`de`).

**Non-Goals:**
- Modifying underlying transaction audit retention policies (7 days / 168 hours).
- Introducing client-side charts on non-administrative user views.
- Complex chart export capabilities (e.g. PDF generation, SVG download).
- Real-time polling/WebSocket chart streaming (charts refresh on standard page navigation or reload).

## Decisions

### 1. Charting Library: Chart.js 4.6.1 WebJar
- **Choice**: `org.webjars.npm:chart.js:4.6.1`.
- **Rationale**:
  - Legacy XCMailr utilized Chart.js; restoring it maintains visual continuity.
  - Chart.js 4.x is lightweight (~65kB gzipped), dependency-free, responsive, canvas-rendered, and provides built-in tooltip interactions and smooth animations.
  - Matches the existing WebJar dependency pattern used for Bootstrap, Bootstrap Icons, and HTMX in `xcmailr-webapp/pom.xml`.
- **Alternatives Considered**:
  - *ApexCharts*: Feature-rich but heavier (~140kB gzipped) with unnecessary SVG overhead for simple line series.
  - *uPlot*: Extremely fast and lightweight (~12kB), but less cohesive out-of-the-box styling and tooltip layout compared to Bootstrap tokens.
  - *Apache ECharts*: Heavyweight (~300kB+), overkill for two dual-line graphs.
  - *Pure SVG templates*: Complex server-side coordinate math, lacks smooth client tooltips and canvas DPI scaling on high-density displays.

### 2. Layout & Responsiveness: Vertically Stacked Cards
- **Choice**: Stack the two charts vertically across the full container width between the quick metric cards and the transaction delivery breakdown table.
- **Rationale**:
  - Maximizes horizontal canvas width, ensuring time labels (24 hourly ticks and 7 date ticks) do not overlap or truncate.
  - Provides generous touch/hover hit-boxes for tooltips across desktop and mobile screens.
  - Matches user feedback and legacy layout ergonomics.
- **Alternatives Considered**:
  - *Side-by-side cards*: Cramps the 24-hour horizontal axis labels on medium and tablet screens (<1200px), requiring label rotation or skipping.

### 3. Backend Aggregation Architecture: Java Time-Bucket Projections with Zero-Filling
- **Choice**: Implement an `EmailStatisticsService` (or helper methods in `AdminWebController`) that queries timestamped transaction counts from `MailTransactionRepository` and aggregates them into continuous time buckets in Java.
- **Data Model Structure**:
  ```java
  public record ChartDataDto(
      List<String> labels,
      List<Long> forwarded,
      List<Long> dropped
  ) {}
  ```
- **Aggregation Logic**:
  - **24-Hour Window**: Calculates 24 hourly buckets from `(now - 23 hours)` truncated to the top of the hour up to `now`.
  - **7-Day Window**: Calculates 7 daily buckets from `(currentDate - 6 days)` up to `currentDate`.
  - Zero-fills all buckets upfront.
  - Queries `MailTransaction` records with `ts >= windowStart`.
  - Buckets transactions based on epoch millisecond offsets. If `status == 300`, increments `forwarded`; otherwise increments `dropped`.
- **Rationale**:
  - Computing buckets in Java ensures 100% database-agnostic behavior, avoiding vendor-specific SQL date functions (`date_trunc`, `formatdatetime`, `system_range`).
  - Guarantees zero-fill continuity even during quiet periods without email traffic.
  - Cleanly unit-testable using mock clocks and fixed timestamps.
- **Alternatives Considered**:
  - *Database-side GROUP BY SQL*: Requires dialect-specific SQL functions that differ between H2 (tests) and PostgreSQL/MySQL (production).
  - *MailStatistics table aggregation*: `MailStatistics` is only updated periodically by background jobs and had duplicate-accumulation risks. `MailTransaction` provides immediate real-time ground truth for the 7-day retention window.

### 4. Client-Side Rendering & Theme Synchronization
- **Choice**:
  - Include `<script th:src="@{/webjars/chart.js/4.6.1/dist/chart.umd.js}">` in `statistics.html`.
  - Pass chart DTOs into the model and serialize to JavaScript using Thymeleaf standard expression inlining (`/*[[${chart24h}]]*/`).
  - Initialize two `Chart` instances configured with smooth bezier tension (`0.3`), corporate colors (`#198754` success green for forwarded, `#dc3545` danger red for dropped).
  - Register a `MutationObserver` on `document.documentElement` watching `data-bs-theme`:
    - When `data-bs-theme` changes (light/dark toggle), reconfigure chart grid colors (`rgba(255, 255, 255, 0.1)` vs `rgba(0, 0, 0, 0.06)`), axis label colors, and tooltip background colors, then invoke `chart.update('none')`.
- **Rationale**:
  - Seamlessly integrates with the existing dark/light mode toggle in `base.html`.
  - Canvas charts dynamically adapt without page reloads.

## Risks / Trade-offs

- **[Risk] High Transaction Volume Performance** → Querying `MailTransaction` where `ts >= cutoff` could load many entities if traffic is high.
  - *Mitigation*: Use a lightweight projection query in `MailTransactionRepository` selecting only `ts` and `status` (`SELECT mt.ts, mt.status FROM MailTransaction mt WHERE mt.ts >= :startTs`), avoiding full entity persistence context instantiation.
- **[Risk] Timezone Skew Between Server and Client** → Server formats bucket labels (e.g. `14:00`, `2026-11-10`) in the server's local timezone.
  - *Mitigation*: Format labels consistently using `ZoneId.systemDefault()`, matching the application's transaction log timestamp display conventions.
- **[Risk] Missing WebJar Resource** → If the Chart.js WebJar path changes or fails to load, chart initialization could throw.
  - *Mitigation*: Guard initialization code with `if (typeof Chart !== 'undefined')` and log a diagnostic warning if unavailable.
