# Design

## Context

See [proposal.md](proposal.md) for motivation and problem statement.

Currently, the admin statistics dashboard (`/admin/statistics`) displays two tables for sender domains:
1. `todaySenderDomains`: Sender domains active today (`LocalDate.now()`).
2. `weekSenderDomains`: Sender domains active over the last 7 days (`LocalDate.now().minusDays(6)`).

The existing JPQL query `findDomainStatisticsSince` groups solely by `ms.key.fromDomain`. In the 7-day view, this aggregates all traffic over the 7-day window into single cumulative totals per domain, completely losing the date on which traffic occurred. Furthermore, the table headers are static `<th>` elements without interactive sorting capability. In the legacy application, both tables were sortable in-browser using `tablesorter.js`.

## Goals / Non-Goals

**Goals:**
- Provide daily date and weekday visibility for 7-day sender domain traffic (`Datum / Tag`), grouped by `(ms.key.date, ms.key.fromDomain)`.
- Restore interactive table sorting across all columns in both sender domain tables (24 hours and 7 days) without page reload.
- Ensure case-insensitive alphabetical sorting for the `Absenderdomain` column, matching user management and mailbox table conventions.
- Ensure accurate chronological sorting for the `Datum / Tag` column and numeric sorting for count columns.
- Retain clean modern visual styling with Bootstrap Chevron indicators (`bi-chevron-expand`, `bi-chevron-up`, `bi-chevron-down`).

**Non-Goals:**
- Full-page server-side pagination for statistics tables (the sender domain tables represent summarized top traffic datasets within constrained time windows).
- Re-rendering or reloading Chart.js diagrams during table sorting (sorting is purely client-side DOM table reordering).
- Altering the 24-hour table schema (today's table represents current day traffic and remains 5 columns without redundant same-day date repeating).

## Decisions

### 1. DTO Model Evolution: Optional Date Field in `DomainStatisticsDto`
- **Choice**: Extend the existing Java record `DomainStatisticsDto` to `(Date date, String domain, long dropCount, long forwardCount, long storeCount)` and provide an overloaded 4-parameter constructor `DomainStatisticsDto(domain, dropCount, forwardCount, storeCount)` that delegates to `this(null, domain, dropCount, forwardCount, storeCount)`.
- **Rationale**: Preserves 100% binary and source compatibility with existing 24-hour aggregations, unit tests, and repository queries, while cleanly accommodating the date field for daily breakdowns.
- **Alternatives Considered**: Creating a new distinct record class `DailyDomainStatisticsDto`. Rejected because the data structure is identical except for the optional date, and using a single record reduces duplication in templates and DTO hierarchies.

### 2. Repository Query: `findDailyDomainStatisticsSince`
- **Choice**: Add a dedicated repository query to `MailStatisticsRepository`:
  ```sql
  SELECT new com.xceptance.xcmailr.dto.DomainStatisticsDto(
      ms.key.date,
      ms.key.fromDomain,
      COALESCE(SUM(ms.dropCount), 0L),
      COALESCE(SUM(ms.forwardCount), 0L),
      COALESCE(SUM(ms.storeCount), 0L))
  FROM MailStatistics ms
  WHERE ms.key.date >= :startDate
  GROUP BY ms.key.date, ms.key.fromDomain
  ORDER BY ms.key.date DESC, (COALESCE(SUM(ms.dropCount), 0L) + COALESCE(SUM(ms.forwardCount), 0L) + COALESCE(SUM(ms.storeCount), 0L)) DESC
  ```
- **Rationale**: Utilizing `ms.key.date` in the `GROUP BY` and `SELECT` projection groups the 96 quarter-hour slots of each day per domain. Leveraging the composite primary key index (`date, quarterHour, fromDomain, targetDomain`) ensures fast index range scans.
- **Alternatives Considered**: In-memory grouping in the controller or service. Rejected because database grouping is significantly faster, reduces network overhead, and offloads aggregation to PostgreSQL/H2.

### 3. Client-Side Sorting Engine (Vanilla JS, Zero Dependencies)
- **Choice**: Implement a compact, modular vanilla JavaScript table sorting function attached to tables marked `.table-sortable`.
- **Behavior**:
  - Headers contain a button/clickable link with `data-sort-col` and `data-sort-type` (`date`, `string`, `number`).
  - Active header displays `bi-chevron-up` (ASC) or `bi-chevron-down` (DESC); inactive headers display `bi-chevron-expand` (`text-body-secondary opacity-50`).
  - **Case-Insensitive String Sorting**: `valA.toLowerCase().localeCompare(valB.toLowerCase())`, preventing uppercase letters from sorting ahead of lowercase letters.
  - **Numeric Sorting**: Evaluates `data-sort-value` or parses text content as integers with fallback to 0.
  - **Date Sorting**: Compares ISO date strings (`YYYY-MM-DD`) via `data-sort-value` to guarantee chronological accuracy regardless of localized text representations.
- **Rationale**: Immediate response time, zero network round-trips, zero impact on Chart.js canvas state, and zero third-party dependencies (eliminating the legacy jQuery tablesorter requirement).

### 4. Template Formatting & Localization
- **Choice**: Localize table column headers and sort titles in `messages.properties` and `messages_de.properties`. Format dates using Thymeleaf `#dates.format(d.date, ...)` or standard localized date pattern, with `data-sort-value="${#dates.format(d.date, 'yyyy-MM-dd')}"` on the `<td>`.

## Risks / Trade-offs

- **[Risk] Multiple rows for the same domain across the 7-day period**: A domain active on 3 separate days will have 3 rows in the 7-day table.
  - **Mitigation**: This is the intended behavior of a daily breakdown. Sorting by `Absenderdomain` groups all occurrences of the same domain together, while sorting by `Datum` groups traffic by day.
- **[Risk] Date format variations across browser locales interfering with string sort**: Localized date strings (e.g. `11.11.2026` vs `11/11/2026`) might sort lexicographically incorrectly if parsed as raw text.
  - **Mitigation**: Always emit a machine-readable ISO date attribute (`data-sort-value="2026-11-11"`) on the `<td>` and have the sort script prioritize `dataset.sortValue`.
