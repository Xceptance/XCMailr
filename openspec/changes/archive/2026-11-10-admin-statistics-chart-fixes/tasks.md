# Tasks

## 1. Base Layout Theme Initialization Fix

- [x] 1.1 Fix identifier syntax error in `layout/base.html` line 22 (`systemPrefers dark` to `systemPrefersDark`) and verify no syntax error occurs during page evaluation.

## 2. Admin Statistics Chart Modernization and Fallback

- [x] 2.1 Refactor chart initialization lifecycle in `statistics.html` with a DOM readiness wrapper (`document.readyState === 'loading' ? DOMContentLoaded : immediate`) and add accessible fallback placeholders inside chart containers.
- [x] 2.2 Modernize Chart.js 4 configuration in `statistics.html` by replacing `plugins.tooltip.theme` with explicit Chart.js 4 tooltip theme tokens and updating scale border properties to `scales.*.border.color`.

## 3. Integration Verification & Testing

- [x] 3.1 Update and run WebMvc integration tests in `AdminWebControllerTest` to verify chart container, fallback markup, and script assertions pass via `mvn test -pl xcmailr-webapp -Dtest=AdminWebControllerTest`.
- [x] 3.2 Verify chart rendering and console logs via automated test to confirm canvas elements render with valid dimensions and no JavaScript warnings or errors.
