# Design

## Context

See `proposal.md` for motivation and `specs/web-console-ui/spec.md` for modified requirement specifications. The admin statistics view (`/admin/statistics`) is served by `AdminWebController.showStatistics()` and rendered by `statistics.html` using Chart.js 4.6.1. The view currently displays 4 top metric cards, a 24-hour hourly activity line chart, a 24-hour sender domain table, a 7-day daily activity line chart, and a 7-day sender domain table.

## Goals / Non-Goals

**Goals:**
- Provide exact metric counts with granular breakdowns: distinguish administrators from standard users, and report truly active mailboxes (`expired = false`) separately from expired mailboxes.
- Surface the aggregate transaction delivery status distribution (`statusSummary`) already loaded by `AdminWebController`.
- Implement a clear, modern section hierarchy (Variante A) utilizing standard Bootstrap icons (`bi-clock-history`, `bi-calendar-week`) and subtle, monochrome pill badges without colorful clutter.
- Modernize the 7-day activity chart by transforming it from a continuous spline line chart into a daily stacked bar chart (`type: 'bar', stacked: true`), displaying period totals in the card header.
- Maintain full compatibility with Bootstrap light/dark themes and enforce strict Java code hygiene (`final` modifiers, Javadoc, testing pyramid).

**Non-Goals:**
- Tabbed navigation (Variante B was evaluated and dismissed in favor of linear section grouping).
- Modifying underlying database schema or adding new database tables.
- Replacing the client-side Chart.js library or introducing external dependencies.

## Decisions

### Decision 1: Daily Stacked Bar Chart for 7-Day Activity
- **Choice**: Use Chart.js stacked bar chart (`type: 'bar'`, `scales.x.stacked: true`, `scales.y.stacked: true`) for the 7-day activity chart (`#chart-last-7d`), while retaining the hourly line chart for the 24-hour view.
- **Rationale**: Daily email traffic consists of discrete calendar-day totals. Line charts with cubic spline interpolation (`tension: 0.3`) create confusing visual artifacts where inactive days draw flat baseline lines (green and red) that overlap along $y = 0$. A stacked bar chart represents zero days as empty ticks and stacks stored (blue), forwarded (green), and dropped (red) segments cleanly within each day's column.
- **Alternatives Considered**:
  - *Keep line chart with adjusted tension or disabled fill*: Still draws flat lines along $y = 0$ across empty days, failing to resolve the "two lines" confusion.
  - *Grouped bar chart (3 side-by-side bars per day)*: Triples the number of bars on small screens, causing visual clutter. Stacked bars maintain a single column per day.

### Decision 2: Clean Section Hierarchy with Modern Monochrome Badges
- **Choice**: Separate the 24-hour and 7-day sections using structured section header rows featuring a subtle top border (`border-top pt-4 mt-5`), standard icons (`bi-clock-history`, `bi-calendar-week`), and neutral monochrome pill badges (`badge rounded-pill bg-body-secondary text-secondary-emphasis border`).
- **Rationale**: Meets user requirements for a calm, professional dashboard without loud or distracting colors. Theme tokens (`bg-body-secondary`, `text-secondary-emphasis`) adapt seamlessly to both light and dark modes.
- **Alternatives Considered**:
  - *Loud, colorful badge banners (e.g. primary blue / warning yellow)*: Rejected per user instruction ("I do not want a colorful one, make it modern").

### Decision 3: Granular Count Queries in Repositories
- **Choice**: Introduce `countByAdmin(boolean admin)` in `UserRepository` and `countByExpired(boolean expired)` in `MailboxRepository`.
- **Rationale**: Spring Data JPA generates optimized `SELECT COUNT(*)` queries executed directly in the database engine, avoiding loading entity instances into memory.
- **Alternatives Considered**:
  - *Fetching all users/mailboxes and filtering in Java*: Inefficient and risks memory exhaustion as dataset sizes scale.

### Decision 4: Surfacing Transaction Delivery Status Breakdown
- **Choice**: Render the `statusSummary` list already retrieved by `AdminWebController.showStatistics()` as a compact badge row beneath the top metric cards.
- **Rationale**: `AdminWebController` already calls `transactionRepository.getStatusCounts()` and passes `statusSummary` to the model. The localization keys (`admin.stats.status.300`, `250`, `200`, `100`, etc.) already exist in `messages.properties` and `messages_de.properties`.

## Risks / Trade-offs

- **[Risk] Chart.js configuration mismatch between bar and line chart during theme changes** → **Mitigation**: Update `updateChartThemes()` in `statistics.html` to handle both `type: 'line'` (24h) and `type: 'bar'` (7d) chart instances, ensuring grid lines, borders, tick colors, and tooltips update uniformly.
- **[Risk] Inactive or empty transaction states** → **Mitigation**: Render a localized empty alert (`admin.stats.status.empty`) when `statusSummary` is empty, matching the behavior in `admin/transactions.html`.
- **[Risk] Chart responsive canvas resizing** → **Mitigation**: Maintain `maintainAspectRatio: false` within responsive `chart-container` wrappers (`height: 260px`).
