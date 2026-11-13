# Tasks

## 1. WebJar Dependency Setup

- [x] 1.1 Add `org.webjars.npm:chart.js:4.6.1` to `xcmailr-webapp/pom.xml` and verify `mvn dependency:resolve -pl xcmailr-webapp` succeeds

## 2. Backend Statistics Aggregation and DTOs

- [x] 2.1 Create immutable `ChartDataDto` record containing `labels`, `forwarded`, and `dropped` series in `com.xceptance.xcmailr.dto`
- [x] 2.2 Add transaction projection query in `MailTransactionRepository` for timestamped status entries since a given epoch cutoff
- [x] 2.3 Implement `EmailStatisticsService` with methods `getLast24HoursChartData(long now)` and `getLast7DaysChartData(long now)` featuring automatic zero-fill continuity, robust boundary clamping, comprehensive Javadocs, and `final` modifiers
- [x] 2.4 Create `EmailStatisticsServiceTest` unit tests covering happy path data aggregation, zero-fill continuity for empty intervals, boundary edge cases, and ensure all tests pass via `mvn test -Dtest=EmailStatisticsServiceTest -pl xcmailr-webapp`

## 3. Controller Integration and View Model Binding

- [x] 3.1 Update `AdminWebController.showStatistics` to inject `EmailStatisticsService` and populate `chart24h` and `chart7d` view model attributes
- [x] 3.2 Update `AdminWebControllerTest` (or create dedicated statistics slice test) verifying authenticated admin access, model attribute presence, and non-empty chart data payloads via `mvn test -Dtest=AdminWebControllerTest -pl xcmailr-webapp`

## 4. Frontend View and Chart.js Theme Synchronization

- [x] 4.1 Add localized message keys for chart card titles, dataset legends (`Forwarded`, `Dropped`), and axis descriptions in `messages.properties` and `messages_de.properties`
- [x] 4.2 Update `xcmailr-webapp/src/main/resources/templates/admin/statistics.html` to include Chart.js WebJar script and render two vertically stacked cards with responsive `<canvas id="chart-last-24h">` and `<canvas id="chart-last-7d">` containers
- [x] 4.3 Implement client-side Chart.js initialization script in `statistics.html` binding dual datasets with corporate accent colors, defensive load guards, and a `MutationObserver` on `data-bs-theme` for dynamic light/dark mode styling

## 5. Integration Verification and Quality Assurance

- [x] 5.1 Implement full WebMvc integration test verifying `/admin/statistics` renders HTML with both chart canvases, localized message labels, and well-formed embedded JSON datasets
- [x] 5.2 Execute complete test suite and verification build via `mvn clean verify -pl xcmailr-webapp` to ensure zero regressions
