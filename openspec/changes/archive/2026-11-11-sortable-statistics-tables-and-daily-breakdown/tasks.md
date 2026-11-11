# Tasks

## 1. DTO and Repository Layer for Daily Domain Breakdowns

- [x] 1.1 Extend `DomainStatisticsDto` to optionally hold a `Date date` field with an overloaded constructor maintaining backwards compatibility, and verify with unit tests in `DomainStatisticsDtoTest`
- [x] 1.2 Add `findDailyDomainStatisticsSince(Date startDate)` query method to `MailStatisticsRepository` grouping by `ms.key.date, ms.key.fromDomain` and ordering by date DESC and total volume DESC, applying `final` modifiers and Javadoc
- [x] 1.3 Add repository tests in `MailStatisticsRepositoryTest` verifying that daily domain traffic across multiple dates is grouped per day and domain with accurate drop, forward, and store counts, and verify tests pass with `mvn test -pl xcmailr-webapp -Dtest=MailStatisticsRepositoryTest`

## 2. Controller and Localization Layer

- [x] 2.1 Update `AdminWebController.java` to fetch `weekSenderDomains` using `findDailyDomainStatisticsSince(weekAgo)` and supply it to the model, applying `final` modifiers and Javadoc
- [x] 2.2 Add localized message keys in `messages.properties` and `messages_de.properties` for the `Datum / Tag` column header (`admin.stats.domains.column.date`) and sort tooltips
- [x] 2.3 Add controller web integration tests in `AdminWebControllerTest` verifying `weekSenderDomains` contains date information in the model, and verify tests pass with `mvn test -pl xcmailr-webapp -Dtest=AdminWebControllerTest`

## 3. Web Console Template and Client-Side Interactive Sorting

- [x] 3.1 Update the 7-day sender domain table in `statistics.html` to include the `Datum / Tag` column with localized weekday and date formatting, `data-sort-value` ISO date attribute, and updated empty state colspan
- [x] 3.2 Implement client-side vanilla JavaScript table sorting on `.table-sortable` supporting case-insensitive natural string comparison (`toLowerCase().localeCompare()`), chronological date ordering, numeric count ordering, and directional Chevron icons (`bi-chevron-expand`, `bi-chevron-up`, `bi-chevron-down`)
- [x] 3.3 Enable client-side sorting across all columns in the 24-hour sender domain table with initial sort on total volume descending
- [x] 3.4 Add template and web integration assertions verifying sortable header markup, chevron icons, and formatted date output in English and German locales, verifying tests pass with `mvn test -pl xcmailr-webapp -Dtest=AdminWebControllerTest`

## 4. Full Verification and Specification Validation

- [x] 4.1 Run full project test suite (`mvn clean test -pl xcmailr-webapp`) ensuring 0 failures and 0 errors across all unit, controller, and repository tests
- [x] 4.2 Validate OpenSpec specification consistency with `openspec validate "sortable-statistics-tables-and-daily-breakdown"`
