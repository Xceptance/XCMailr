# Tasks

## 1. Metric Accuracy & Repository Extensions

- [x] 1.1 Add `countByAdmin(final boolean admin)` to `UserRepository` and `countByExpired(final boolean expired)` to `MailboxRepository`, and verify with repository integration tests in `UserRepositoryTest` and `MailboxRepositoryTest`.
- [x] 1.2 Update `AdminWebController.showStatistics()` to query granular user counts (`adminCount`, `regularCount`), mailbox counts (`activeMailboxCount`, `expiredMailboxCount`), compute period totals for 24h and 7d traffic, and populate the model with `final` variables; verify with unit and slice tests in `AdminWebControllerTest`.
- [x] 1.3 Define and synchronize localization keys for user breakdowns, active/expired mailboxes, section titles, and period totals across `conf/messages.properties` and `conf/messages_de.properties`.

## 2. Dashboard Layout & Section Hierarchy (Variante A)

- [x] 2.1 Update Quick Stats cards in `statistics.html` to display total users with administrator/standard breakdown, true active mailboxes with total/expired subtitle, and render the transaction delivery status distribution badge row (`statusSummary`); verify via `AdminWebControllerTest`.
- [x] 2.2 Add structured section header rows separating 24-hour activity and 7-day activity in `statistics.html`, incorporating standard Bootstrap icons (`bi-clock-history`, `bi-calendar-week`), clean dividers (`border-top pt-4 mt-5`), and subtle monochrome pill badges (`rounded-pill bg-body-secondary text-secondary-emphasis border`).
- [x] 2.3 Add period total indicators in the 24-hour and 7-day activity card headers so operators immediately see overall email volume for each window.

## 3. 7-Day Daily Stacked Bar Chart

- [x] 3.1 Convert `#chart-last-7d` Chart.js configuration in `statistics.html` from a spline line chart to a stacked daily bar chart (`type: 'bar'`, `scales.x.stacked = true`, `scales.y.stacked = true`) with distinct blue (stored), green (forwarded), and red (dropped) bars, while keeping `#chart-last-24h` as an hourly line chart.
- [x] 3.2 Update `updateChartThemes()` in `statistics.html` to dynamically synchronize grid lines, borders, tick colors, and tooltips across both line and bar chart instances when switching between light and dark themes.

## 4. Verification and Code Hygiene

- [x] 4.1 Run the complete test suite (`mvn clean test -pl xcmailr-webapp`) verifying 0 test failures, regression-free controller execution, and test pyramid coverage.
- [x] 4.2 Verify code standards: enforce `final` modifiers on all new method arguments, local variables, and fields, complete Javadoc on public methods, and confirm no console or runtime warnings.
