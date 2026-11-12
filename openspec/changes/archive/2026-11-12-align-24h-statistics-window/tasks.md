# Tasks

## 1. Repository Query for Sliding 24-Hour Domain Statistics

- [x] 1.1 Add `findSliding24HoursDomainStatistics` to `MailStatisticsRepository` querying domain statistics across the yesterday/today boundary and verify query syntax and results in `MailStatisticsRepositoryTest`
- [x] 1.2 Add unit test cases in `MailStatisticsRepositoryTest` covering midnight transitions, boundary quarter-hour intervals, and multi-domain aggregations and verify all repository tests pass

## 2. Admin Controller Integration

- [x] 2.1 Update `AdminWebController.showStatistics` to calculate `yesterday`, `today`, and `startQuarterHour = (now.getHour() + 1) * 4` and pass them to `findSliding24HoursDomainStatistics`
- [x] 2.2 Update `AdminWebControllerTest` to verify that the sliding 24-hour domain statistics query is invoked with expected boundary parameters and populated into the view model
- [x] 2.3 Verify code hygiene (`final` modifiers on all parameters and local variables, complete Javadoc documentation) across modified controller and repository methods

## 3. End-to-End Verification

- [x] 3.1 Execute Maven test suite for `MailStatisticsRepositoryTest`, `AdminWebControllerTest`, and `EmailStatisticsServiceTest` and verify that chart volume and domain table aggregates align consistently
