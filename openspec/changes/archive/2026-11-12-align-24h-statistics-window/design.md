# Design

## Context
See `proposal.md` for motivation and background.

Currently, `AdminWebController.showStatistics` populates the "Todays (last 24 hours) sender domains" table using `mailStatisticsRepository.findDomainStatisticsSince(today)` with `today = Date.valueOf(LocalDate.now())`. This queries only records where `ms.key.date >= today`, capturing traffic from midnight (00:00:00) to the current time. Meanwhile, `EmailStatisticsService.getLast24HoursChartData()` computes 24 continuous hourly buckets ending at the current hour, spanning yesterday afternoon to today. This mismatch causes the 24-hour domain table to omit yesterday's traffic and report significantly lower totals than the activity chart.

## Goals / Non-Goals

**Goals:**
- Provide 100% metric parity between the 24-hour activity chart total and the sum of domain traffic in the 24-hour sender domains table.
- Query `MAIL_STATISTICS` across the exact sliding 24-hour interval matching `EmailStatisticsService`: from `(currentHour + 1)` of yesterday through `currentHour` of today.
- Maintain existing `DomainStatisticsDto` data structures, table layout, client-side sorting, and English/German internationalization.
- Support comprehensive unit and integration test verification.

**Non-Goals:**
- Modifying the 7-day activity chart or 7-day domain table (both already operate consistently on 7-day calendar date ranges).
- Modifying database schemas or table indexes in `MAIL_STATISTICS`.
- Altering Chart.js configurations, themes, or responsive styling.

## Decisions

### 1. Hourly-Aligned Sliding 24-Hour Boundary Interval
`EmailStatisticsService.getLast24HoursChartData(now)` truncates the current timestamp to the current hour (`nowZdt = Instant.ofEpochMilli(now).atZone(zoneId).truncatedTo(ChronoUnit.HOURS)`), creating 24 hourly buckets:
- Bucket 0 starts at `yesterday (currentHour + 1):00` (e.g. 15:00 if current hour is 14:00).
- Bucket 23 ends at `today (currentHour + 1):00` (exclusive).

Each hour consists of 4 quarter-hour intervals (`0..3`). Therefore:
- The starting quarter-hour on `yesterday` is `startQuarterHour = (currentHour + 1) * 4`.
- On `yesterday`, valid intervals satisfy `ms.key.quarterHour >= startQuarterHour` (quarters `startQuarterHour` through `95`).
- On `today`, valid intervals satisfy `ms.key.quarterHour < startQuarterHour` (quarters `0` through `startQuarterHour - 1`).

**Boundary Edge Cases:**
- `currentHour = 23`: `startQuarterHour = 24 * 4 = 96`.
  - Yesterday: `quarterHour >= 96` matches 0 records.
  - Today: `quarterHour < 96` matches all 96 quarters of today (`0..95`).
- `currentHour = 0`: `startQuarterHour = 1 * 4 = 4`.
  - Yesterday: `quarterHour >= 4` matches quarters `4..95` (01:00 to 23:59).
  - Today: `quarterHour < 4` matches quarters `0..3` (00:00 to 00:59).
  - Total quarters = 92 + 4 = 96 quarters = 24 hours.

*Alternatives considered:*
- Filtering by minute-level quarter slot `(hour * 4) + (minute / 15)`: While granular, this would decouple the table from the 24-hour chart, which aggregates by full hour buckets ending at `nowHour`. Using `startQuarterHour = (currentHour + 1) * 4` guarantees that every interval displayed in the chart is included in the table.

### 2. Repository Query in `MailStatisticsRepository`
Introduce `findSliding24HoursDomainStatistics`:
```java
@Query("SELECT new com.xceptance.xcmailr.dto.DomainStatisticsDto(" +
       "ms.key.fromDomain, " +
       "COALESCE(SUM(ms.dropCount), 0L), " +
       "COALESCE(SUM(ms.forwardCount), 0L), " +
       "COALESCE(SUM(ms.storeCount), 0L)) " +
       "FROM MailStatistics ms " +
       "WHERE (ms.key.date = :yesterday AND ms.key.quarterHour >= :startQuarterHour) " +
       "   OR (ms.key.date = :today AND ms.key.quarterHour < :startQuarterHour) " +
       "GROUP BY ms.key.fromDomain " +
       "ORDER BY (COALESCE(SUM(ms.dropCount), 0L) + COALESCE(SUM(ms.forwardCount), 0L) + COALESCE(SUM(ms.storeCount), 0L)) DESC")
List<DomainStatisticsDto> findSliding24HoursDomainStatistics(
    @Param("yesterday") final Date yesterday,
    @Param("today") final Date today,
    @Param("startQuarterHour") final int startQuarterHour);
```
This returns `List<DomainStatisticsDto>` sorted by total volume descending, matching the signature expected by the view.

### 3. Controller Integration in `AdminWebController`
In `AdminWebController.showStatistics`:
```java
final ZonedDateTime now = ZonedDateTime.now();
final Date today = Date.valueOf(now.toLocalDate());
final Date yesterday = Date.valueOf(now.toLocalDate().minusDays(1));
final int startQuarterHour = (now.getHour() + 1) * 4;

final List<DomainStatisticsDto> todaySenderDomains =
    mailStatisticsRepository.findSliding24HoursDomainStatistics(yesterday, today, startQuarterHour);
```
The model attribute `todaySenderDomains` remains unchanged, preserving view template compatibility.

## Risks / Trade-offs

- **[Risk] Time zone discrepancies between application runtime and database server**
  → *Mitigation*: Compute `yesterday`, `today`, and `startQuarterHour` explicitly in application code using `ZonedDateTime.now()` (or configured system zone) and pass typed parameters (`Date`, `int`) into JPQL, avoiding database dialect-specific time functions.

- **[Risk] Null sums on empty statistics datasets**
  → *Mitigation*: Retain `COALESCE(SUM(...), 0L)` in JPQL to guarantee zero counts instead of null values when no records match.

## Migration Plan
1. Backward-compatible code-only update; no schema changes or data migrations required.
2. Verified via repository and controller integration tests.
