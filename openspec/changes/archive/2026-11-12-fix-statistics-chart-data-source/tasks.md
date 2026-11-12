# Tasks

## 1. Repository Traffic Aggregation Queries

- [x] 1.1 Define traffic aggregation projection interfaces and add `findDailyTrafficStatsSince(Date startDate)` and `findQuarterHourTrafficStatsSince(Date startDate)` query methods to `MailStatisticsRepository.java`
- [x] 1.2 Add repository integration tests in `MailStatisticsRepositoryTest.java` verifying that daily and quarter-hour queries accurately sum drops, forwards, and stores across domains and verify with `mvn test -Dtest=MailStatisticsRepositoryTest`

## 2. Email Statistics Service Modernization

- [x] 2.1 Refactor `EmailStatisticsService.java` constructor to inject `MailStatisticsRepository` and implement `getLast7DaysChartData(long now)` using `findDailyTrafficStatsSince` with zero-fill continuity
- [x] 2.2 Implement `getLast24HoursChartData(long now)` in `EmailStatisticsService.java` using `findQuarterHourTrafficStatsSince` by mapping 15-minute quarter-hour intervals into 24 continuous hourly buckets
- [x] 2.3 Update unit tests in `EmailStatisticsServiceTest.java` to validate 24-hour and 7-day bucket distribution, zero-filling, and category counts, verifying with `mvn test -Dtest=EmailStatisticsServiceTest`

## 3. End-to-End Consistency and Test Suite Verification

- [x] 3.1 Update WebMvc tests in `AdminWebControllerTest.java` to verify `/admin/statistics` populates `chart24h` and `chart7d` model attributes consistently with `MAIL_STATISTICS` domain statistics, verifying with `mvn test -Dtest=AdminWebControllerTest`
- [x] 3.2 Execute the full Maven test suite across all modules (`mvn clean test`) to confirm zero regressions and verify all tests pass
