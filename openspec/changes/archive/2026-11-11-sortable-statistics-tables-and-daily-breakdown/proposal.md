# Proposal

## Why

In the modernized admin statistics view (`/admin/statistics`), administrators currently cannot sort the sender domain tables (today's 24 hours and the last 7 days), a feature that was previously present in the legacy application. Additionally, in the "Absenderdomains der letzten 7 Tage" table, counts were rolled up across the entire 7-day period without indicating the date or day of the week on which the traffic occurred. This makes it impossible for administrators to determine when specific domain activity took place or to sort entries chronologically, alphabetically, or by traffic volume.

## What Changes

- **7-Day Domain Activity Breakdown with Date & Weekday**:
  - Update the 7-day sender domain query to group by both `date` and `fromDomain`, preserving the specific calendar date of activity.
  - Add a localized `Datum / Tag` (Date / Day) column to the 7-day sender domain table displaying the formatted date and weekday (e.g., `Mi, 11.11.2026` in German, `Wed, 11/11/2026` in English) using an ISO sort attribute (`data-sort-value="yyyy-MM-dd"`).
  - Update the 24-hour table to maintain its focused 5-column layout for today's activity while enabling full column sorting.
- **Client-Side Interactive Table Sorting (Option A)**:
  - Implement a lightweight, zero-dependency vanilla JavaScript table sorter for both statistics tables (`todaySenderDomains` and `weekSenderDomains`).
  - Provide interactive column headers with Bootstrap Chevron indicator icons (`bi-chevron-expand`, `bi-chevron-up`, `bi-chevron-down`) matching the styling of user management and mailbox tables.
  - Implement **case-insensitive natural alphabetical sorting** for the Sender Domain column (using `toLowerCase()` and `localeCompare`), consistent with mailbox address and user email sorting.
  - Implement **chronological sorting** for the Date / Day column.
  - Implement **numeric sorting** for metric count columns (*Verworfen*, *Weitergeleitet*, *Gespeichert*, *Gesamt*).
  - Maintain an initial default sort order of `Datum DESC, Gesamt DESC` for the 7-day table and `Gesamt DESC` for the 24-hour table.
- **Localization and Accessibility**:
  - Add localized header labels and tooltips (`admin.stats.domains.column.date`, `admin.stats.domains.sort.*`) in English (`messages.properties`) and German (`messages_de.properties`).
  - Ensure ARIA labels and keyboard accessibility on sortable headers.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Update the `Admin Management Console` requirement to mandate client-side case-insensitive column sorting with directional visual chevrons across sender domain tables, and daily date/weekday breakdowns for 7-day domain activity.

## Impact

- **Backend**:
  - `DomainStatisticsDto.java`: Extend or overload to optionally include a `Date date` field.
  - `MailStatisticsRepository.java`: Add query method `findDailyDomainStatisticsSince(Date startDate)` grouping by `ms.key.date, ms.key.fromDomain`.
  - `AdminWebController.java`: Pass the daily domain records to the view for `weekSenderDomains`.
- **Frontend**:
  - `xcmailr-webapp/src/main/resources/templates/admin/statistics.html`: Add date column to the 7-day table, add sortable header markup with chevrons, and add client-side sort JavaScript.
  - `messages.properties` / `messages_de.properties`: Add localized table header and sort tooltip strings.
- **Testing**:
  - Controller and repository integration tests verifying daily grouping, date field mapping, and order.
  - Thymeleaf rendering tests verifying sort attributes, column headers, and localized date displays.
