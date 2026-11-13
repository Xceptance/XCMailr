# Proposal

## Why
The 24-hour activity chart displays email traffic across a sliding 24-hour window (96 quarter-hour intervals ending at the current hour), but the accompanying "Todays (last 24 hours) sender domains" table aggregates data only from midnight (`today 00:00:00`), leading to large discrepancies between the chart total (e.g. 71,449 emails) and the table total (e.g. 17,805 emails). Aligning the table aggregation query to the identical sliding 24-hour window guarantees complete metric consistency and accurate domain visibility.

## What Changes
- Add repository query `findSliding24HoursDomainStatistics` to `MailStatisticsRepository` aggregating traffic across the sliding 24-hour interval: `(ms.key.date = :yesterday AND ms.key.quarterHour > :currentQuarter) OR (ms.key.date = :today AND ms.key.quarterHour <= :currentQuarter)`.
- Update `AdminWebController.java` to compute the current quarter-hour slot `currentQuarter = (now.getHour() * 4) + (now.getMinute() / 15)` and pass `yesterday`, `today`, and `currentQuarter` to populate `todaySenderDomains`.
- Retain existing UI layout, internationalization, and sorting behavior while ensuring the total emails in the 24-hour sender domains table matches the 24-hour activity chart total.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Clarify the `Admin Management Console` requirement and scenarios so that the 24-hour sender domains breakdown table aggregates email traffic over the exact sliding 24-hour window across the midnight date boundary matching the 24-hour activity chart.

## Impact
- **Backend Components**: `MailStatisticsRepository`, `AdminWebController`.
- **UI / Frontend**: `/admin/statistics` view (`admin/statistics.html`), where the 24-hour sender domains table now reflects full 24-hour historical traffic rather than only the current calendar day.
- **Tests**: `MailStatisticsRepositoryTest`, `AdminWebControllerTest`.
- **APIs / Data Model**: No database schema changes; uses existing `MAIL_STATISTICS` table and `MailStatistics` entity. No breaking API changes.
