# Design

## Context

See [proposal.md](proposal.md) for background and motivation.
In commit `296f5ef`, Chart.js 4.6.1 was integrated to display 24h and 7d activity charts. However, client-side rendering failed due to an uncaught syntax error in `layout/base.html`, uncoordinated execution timing relative to DOM layout readiness, and outdated Chart.js options.

## Goals / Non-Goals

**Goals:**
- Eliminate the syntax error in `layout/base.html` line 22 (`systemPrefers dark` -> `systemPrefersDark`).
- Ensure chart instantiation waits for DOM readiness and computed element dimensions.
- Align all Chart.js options with Chart.js 4.x specifications (explicit tooltip color tokens, `border.color` on scales).
- Provide an accessible fallback message when Chart.js is not loaded or disabled.
- Maintain seamless dark/light theme switching without visual lag or console errors.

**Non-Goals:**
- Modifying backend SQL/JPA queries or `EmailStatisticsService` calculations (which are already verified and accurate).
- Altering transaction status categorization (statuses 100, 200, 400 remain mapped to "Dropped" in accordance with delivery architecture).

## Decisions

### 1. Fix Identifier Syntax Error in `layout/base.html`
- **Choice**: Replace `const systemPrefers dark = ...` with `const systemPrefersDark = ...` and update reference on line 23.
- **Rationale**: Valid JavaScript identifier syntax is required. A syntax error inside `<head>` terminates that script execution immediately.

### 2. Chart Lifecycle and DOM Readiness
- **Choice**: Encapsulate chart initialization in a function and invoke it via:
  ```javascript
  if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', initCharts);
  } else {
      initCharts();
  }
  ```
- **Rationale**: Ensures the `<canvas>` elements have attached to the DOM tree and their parent `.chart-container` elements (`height: 260px; width: 100%`) have evaluated layout bounding boxes before Chart.js reads `clientWidth`/`clientHeight`.
- **Alternative considered**: Executing inline at the end of `<body>`. Wrapping with `readyState` check is safer because `statistics.html` is injected via Thymeleaf layout replacement into `<main>`, which sits before the closing scripts.

### 3. Chart.js 4.x Options Alignment
- **Tooltip**: Replace invalid `plugins.tooltip.theme: theme.isDark ? 'dark' : 'light'` with standard Chart.js 4 tooltip styling:
  ```javascript
  tooltip: {
      backgroundColor: theme.isDark ? '#212529' : '#ffffff',
      titleColor: theme.isDark ? '#f8f9fa' : '#212529',
      bodyColor: theme.isDark ? '#e9ecef' : '#495057',
      borderColor: theme.borderColor,
      borderWidth: 1,
      padding: 10
  }
  ```
- **Axis Border**: Replace deprecated `scales.x.grid.borderColor` with standard Chart.js 4 scale border property:
  ```javascript
  scales: {
      x: {
          border: { color: theme.borderColor },
          grid: { color: theme.gridColor }
      },
      y: {
          border: { color: theme.borderColor },
          grid: { color: theme.gridColor }
      }
  }
  ```
- **Rationale**: Eliminates console warnings and guarantees predictable visual styling in both light and dark themes.

### 4. Degraded Fallback Handling
- **Choice**: Place an unobtrusive placeholder alert or message inside each chart container (`<div class="chart-fallback alert alert-light text-center small text-muted ...">`), which is hidden (`display: none`) upon successful `new Chart(...)` instantiation.
- **Rationale**: If a browser adblocker, script blocker, or network partition prevents `chart.umd.js` from loading, the administrator sees a clear explanatory indicator rather than a mysterious blank void.

## Risks / Trade-offs

- **[Risk]** Layout shift when fallback is hidden and canvas displays.
  → **Mitigation**: The container has explicit CSS `height: 260px; position: relative`. Fallback is absolutely centered within this container, so removing it causes zero reflow or layout shift.
- **[Risk]** Dynamic theme switching updates might lag on slow clients.
  → **Mitigation**: Reusing existing `MutationObserver` on `data-bs-theme` with `chart.update('none')` (no animation) keeps theme synchronization instantaneous (sub-millisecond).
