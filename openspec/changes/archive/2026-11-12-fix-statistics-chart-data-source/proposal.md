# Proposal

## Why

The administrative statistics dashboard displays a severe discrepancy between email activity charts and the domain breakdown tables: on high-volume systems or production database imports, the 7-day and 24-hour tables report hundreds of thousands of processed emails (e.g., 600,000+ over 7 days, 17,000+ on a single day), while the charts directly above them only display ~150–250 emails per day (totaling ~1,300 over 7 days, 181 over 24 hours).

This occurs because the tables query persistent, aggregated metrics in `MAIL_STATISTICS`, whereas the chart service (`EmailStatisticsService`) queries raw rows in `mailtransactions` via `MailTransactionRepository`. In production (and by legacy design), silent email drops (`status 100`) were never recorded as individual transaction audit log rows, and transaction logs are subject to automated retention purging (`max-age-hours: 168`). Re-pointing chart generation to `MAIL_STATISTICS` restores 100% data consistency between the charts and tables and dramatically speeds up chart rendering.

## What Changes

- Modify `EmailStatisticsService` to derive 24-hour (hourly) and 7-day (daily) chart datasets from `MailStatisticsRepository` instead of `MailTransactionRepository`.
- Add repository query methods to `MailStatisticsRepository` to aggregate drop, forward, and store counts grouped by calendar date (for the 7-day view) and by date and quarter-hour (for the 24-hour sliding view).
- Ensure hourly and daily buckets maintain zero-fill continuity for periods without traffic.
- Maintain existing Chart.js contracts, labels, and three-series structure (`forwarded`, `stored`, `dropped`).
- Update unit and WebMvc integration tests to verify chart data consistency with `MAIL_STATISTICS` aggregates.

## Capabilities

### Modified Capabilities
- `web-console-ui`: Update the Admin Management Console requirement to mandate that the 24-hour and 7-day activity charts are backed by aggregated metrics from `MAIL_STATISTICS`, guaranteeing parity with the domain statistics tables.

## Impact

- Affected code: `EmailStatisticsService.java`, `MailStatisticsRepository.java`, `AdminWebController.java`, and associated test suites (`EmailStatisticsServiceTest.java`, `AdminWebControllerTest.java`, `MailStatisticsRepositoryTest.java`).
- Database & Schema: No schema changes or Flyway migrations needed; utilizes existing `MAIL_STATISTICS` composite key (`DATE`, `QUARTER_HOUR`, `FROM_DOMAIN`, `TARGET_DOMAIN`) and counts (`DROP_COUNT`, `FORWARD_COUNT`, `STORE_COUNT`).
- API/UI Contract: Fully backward-compatible; Chart DTO and Thymeleaf/Chart.js front-end templates remain unchanged.
