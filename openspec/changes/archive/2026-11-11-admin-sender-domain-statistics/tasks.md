# Tasks

## 1. Database Migration and Entity Updates

- [x] 1.1 Create idempotent Flyway migration `xcmailr-webapp/src/main/resources/db/migration/V5__Add_Store_Count_To_Mail_Statistics.sql` adding column `STORE_COUNT INT DEFAULT 0` using `ALTER TABLE MAIL_STATISTICS ADD COLUMN IF NOT EXISTS STORE_COUNT INT DEFAULT 0;` and verify application starts and Flyway migration succeeds idempotently
- [x] 1.2 Update `MailStatistics.java` entity with `final` modifiers, comprehensive Javadoc, and `storeCount` column mapping with getters, setters, updated `hashCode`/`equals`, and `toString`
- [x] 1.3 Add status code `250` documentation to `MailTransaction.java` and create immutable Java record `DomainStatisticsDto.java` (`domain`, `dropCount`, `forwardCount`, `storeCount`) with Javadoc and `final` components
- [x] 1.4 Create unit test `MailStatisticsTest.java` verifying happy path property access, special case equality with different store counts, and null-safe key handling

## 2. Asynchronous Transaction Buffer and Ingestion

- [x] 2.1 Implement `MailTransactionQueueService.java` encapsulating `ConcurrentLinkedQueue<MailTransaction>` with non-blocking `enqueue`, `drainAll`, and `size` methods, applying `final` on all parameters and local variables with descriptive Javadoc and inline comments
- [x] 2.2 Create unit test `MailTransactionQueueServiceTest.java` following the testing pyramid covering happy path enqueue/drain FIFO retrieval, error case null handling without NPE, and multi-threaded concurrency special cases
- [x] 2.3 Update `MailDeliveryService.java` to inject `MailTransactionQueueService`, record status `250` (*"Mail received and stored in inbox"*) when incoming emails are stored without forwarding (`!mbox.isForwardEmails()`), and route all transaction records through `enqueue` rather than direct synchronous JPA repository saves
- [x] 2.4 Update `MailDeliveryServiceTest.java` and `SmtpDeliveryIT.java` to verify happy path status 250 enqueueing for stored messages, error case drop status enqueueing (100, 200, 600), and special case non-blocking delivery under concurrent requests

## 3. Scheduled Batch Aggregation Engine and Retention

- [x] 3.1 Update `ExpirationScheduledService.aggregateAndCleanTransactions` to drain queued transactions from `MailTransactionQueueService`, batch persist to `mailtransactions` via `saveAll()`, aggregate drop, forward, and store counts exactly once into `MAIL_STATISTICS`, and add `@PreDestroy` graceful shutdown flush hook, applying `final` modifiers and full Javadoc
- [x] 3.2 Implement unit tests in `ExpirationScheduledServiceTest` verifying the mathematical quarter-hour calculation `(hour * 4) + (minute / 15)` producing slot indices `0..95`, including boundary conditions (`00:00:00.000` -> 0, `00:14:59.999` -> 0, `00:15:00.000` -> 1, `23:59:59.999` -> 95) and midnight date transitions
- [x] 3.3 Implement integration tests in `ExpirationScheduledServiceTest` verifying happy path same-slot aggregation: multiple incoming transactions within the same 15-minute slot increment a single `MAIL_STATISTICS` row with accurate `dropCount`, `forwardCount`, and `storeCount`
- [x] 3.4 Implement integration tests in `ExpirationScheduledServiceTest` verifying happy path consecutive-slot row insertion: transactions occurring in consecutive 15-minute intervals (e.g. 10:14 in slot 40 vs 10:16 in slot 41) create separate rows with sequential `quarterHour` values
- [x] 3.5 Implement integration tests in `ExpirationScheduledServiceTest` verifying special case multi-domain partitioning: transactions from different sender or recipient domains in the identical 15-minute slot create distinct rows partitioned by `(fromDomain, targetDomain)`
- [x] 3.6 Implement integration tests in `ExpirationScheduledServiceTest` verifying special case multi-batch consecutive runs: a second scheduled run processing transactions in the same 15-minute slot discovers the pre-existing row via `findById(key)` and updates its counts without primary key conflicts
- [x] 3.7 Implement integration tests in `ExpirationScheduledServiceTest` verifying special case idempotency and shutdown flush: consecutive executions with an empty queue leave counts completely unchanged without multiplying, and `@PreDestroy` flushes buffered transactions before context close
- [x] 3.8 Implement integration tests in `ExpirationScheduledServiceTest` verifying error case malformed domain handling: transactions with null, blank, or malformed email addresses (lacking `@`) are saved to `mailtransactions` for auditing but safely bypassed for `MAIL_STATISTICS` aggregation without throwing `NullPointerException`

## 4. Sender Domain Statistics and Chart Backend

- [x] 4.1 Add `findDomainStatisticsSince` JPQL aggregation query to `MailStatisticsRepository.java` to aggregate drop, forward, and store counts grouped by sender domain ordered by total volume descending, applying `final` on query parameters
- [x] 4.2 Create repository integration test `MailStatisticsRepositoryTest.java` covering happy path domain aggregation, error/boundary case with no records before start date, and special case handling of zero counts via `COALESCE` without returning nulls
- [x] 4.3 Update `ChartDataDto.java` record to include the `stored` count series, and update `EmailStatisticsService.java` to bucket status 250 into the `stored` series for both 24-hour and 7-day chart calculations, declaring `final` throughout and adding descriptive Javadoc
- [x] 4.4 Update `EmailStatisticsServiceTest.java` covering happy path three-series bucketing (300, 250, dropped), boundary case timestamps outside the window, and special case continuous zero-fill on intervals with zero traffic

## 5. Admin Web Console and UI Presentation

- [x] 5.1 Update `AdminWebController.showStatistics` to query today's (last 24 hours) and last 7 days' sender domain statistics and bind `todaySenderDomains` and `weekSenderDomains` to the Thymeleaf model, declaring all parameters and local variables as `final` with complete Javadoc
- [x] 5.2 Update `admin/statistics.html` to add the Stored (status 250, primary blue) dataset to the 24-hour and 7-day Chart.js line charts, remove the raw "Transaktions-Zustellungsübersicht" table, and render the two sender domain breakdown tables with localized headers and empty state handling
- [x] 5.3 Add status 250 badge (`Stored (250)`) to `admin/transactions.html` and `admin/fragments/transaction-table.html`, and update `messages.properties` and `messages_de.properties` with localized strings for stored messages and domain table columns
- [x] 5.4 Update `AdminWebControllerTest.java` covering happy path model attributes for `todaySenderDomains`, `weekSenderDomains`, and 3-series chart data, as well as special case empty domain collections rendering localized empty state messages

## 6. Verification and Integration Checks

- [x] 6.1 Run full test suite `./mvnw clean test` to ensure all existing and new unit and integration tests pass without error
- [x] 6.2 Verify that end-to-end SMTP delivery, queue draining, `MAIL_STATISTICS` persistence, and the admin statistics dashboard render as expected with accurate counts and domain tables
