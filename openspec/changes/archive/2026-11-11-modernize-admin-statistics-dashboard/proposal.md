# Proposal

## Why

The administrative statistics dashboard (`/admin/statistics`) aggregates key system metrics, traffic charts, and domain statistics, but user experience and data interpretation are hampered by several issues:
1. The 7-day activity chart renders zero and low-frequency values as overlapping lines on a 0–20,000 scale, causing users to see confusing lines without clear attribution.
2. The 24-hour and 7-day sections lack visual separation, resulting in a continuous stack of four visually identical white cards.
3. Top-level metric cards lack granular breakdowns: "Registered Users" does not distinguish between administrators and standard users, while "Active Mailboxes" mistakenly queries `count()`, including expired mailboxes.
4. Transaction status distribution data (`statusSummary`) is fetched by `AdminWebController` but never displayed in the view template.

Modernizing this dashboard addresses visual ambiguity, corrects metric reporting, and presents email activity clearly to operators.

## What Changes

- **Visual Section Hierarchy (Variante A)**:
  - Introduce clean, modern, non-colorful section dividers separating the 24-Hour and 7-Day activity windows.
  - Employ standard Bootstrap icons (`bi-clock-history`, `bi-calendar-week`) and subtle, monochrome pill badges (`rounded-pill bg-body-secondary text-secondary-emphasis border`) compatible with both light and dark themes.
- **7-Day Activity Chart Modernization (Daily Stacked Bar Chart)**:
  - Transform the 7-day activity chart from a continuous spline line chart into a stacked daily bar chart (`type: 'bar', stacked: true`).
  - Eliminate flat zero-line visual artifacts on inactive days while clearly displaying daily ratios of stored (blue), forwarded (green), and dropped (red) messages.
  - Add period total indicators in the card header for immediate clarity.
- **Accurate Metrics & Granular Breakdowns**:
  - **Registered Users**: Display total user count with subtitle breakdown of administrators and standard users (`countByAdmin(true)` vs `countByAdmin(false)`).
  - **Active Mailboxes**: Correct the query to count non-expired mailboxes (`countByExpiredFalse()`) with total and expired counts displayed in the subtitle.
  - **Transaction Delivery Status Overview**: Render a subtle status distribution summary badge group (Stored 250, Forwarded 300, Inactive Mailbox 200, Unknown Mailbox 100, Relay/Error codes) utilizing existing localization keys.
- **Code Hygiene & Testing**:
  - Enforce `final` modifiers, comprehensive Javadoc, code comments, and automated test pyramid coverage (unit, service, controller, and repository layers).

## Capabilities

### New Capabilities
<!-- None: This change enhances existing console capabilities. -->

### Modified Capabilities
- `web-console-ui`: Update the Admin Management Console requirement to specify daily stacked bar charts for 7-day traffic, metric breakdowns (admin/standard users, active/expired mailboxes), transaction status distribution display, period totals, and structured section layout with modern monochrome badges.

## Impact

- **Controllers & Services**: `AdminWebController` passes granular user counts (`adminCount`, `regularCount`) and mailbox counts (`activeMailboxCount`, `expiredMailboxCount`) to the model.
- **Repositories**: `UserRepository` (`countByAdmin`) and `MailboxRepository` (`countByExpired`).
- **Templates**: `statistics.html` updated with modern section headers, status distribution badges, updated Quick Stats cards, and Chart.js bar chart initialization for 7 days.
- **Localization**: Verify and augment `conf/messages.properties` and `conf/messages_de.properties` for user/mailbox breakdown labels and section badges.
- **APIs & Database**: No database schema migrations required; uses existing tables (`users`, `mailboxes`, `mailtransactions`, `MAIL_STATISTICS`).
