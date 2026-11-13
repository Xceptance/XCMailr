# Design

## Context

The mailbox dashboard renders mailbox rows inside a Bootstrap responsive table wrapper (`<div class="table-responsive">`) in `mailbox-table.html`. By default, Bootstrap 5 dropdown menus are positioned with Popper using `position: absolute` within the parent `.dropdown` container.

Per CSS specifications, an element with `overflow-x: auto` forces `overflow-y` to compute to `auto` or `hidden`, creating an overflow clipping boundary. When the mailbox table contains only a few rows (e.g., 1–2 rows), the dropdown menu extends beyond the bottom edge of `.table-responsive`, causing the bottom menu items (such as "Reset Counters", divider, and "Delete Mailbox") to be clipped or require awkward vertical scrolling inside the table container.

See `proposal.md` for full background and user impact.

## Goals / Non-Goals

**Goals:**
- Ensure the mailbox action dropdown menu renders fully visible across all viewports and table heights without clipping.
- Use native Bootstrap 5 / Popper data attributes to keep the solution purely declarative without custom JavaScript listeners.
- Maintain full compatibility with HTMX partial table swaps and Thymeleaf template rendering.

**Non-Goals:**
- Removing `.table-responsive` from the table layout (horizontal scrolling remains necessary on narrow screens when table content exceeds viewport width).
- Adding artificial `min-height` or empty space to tables.
- Refactoring modal dialogs or navbar dropdowns that do not suffer from container clipping.

## Decisions

### Decision 1: Use Declarative Bootstrap 5 Popper Configuration (`strategy: fixed` and `boundary: viewport`)

We add `data-bs-boundary="viewport"` and `data-bs-popper-config='{"strategy":"fixed"}'` to the dropdown toggle button in `mailbox-table.html`.

**Rationale:**
Bootstrap 5 integrates Popper v2. Configuring `strategy: "fixed"` instructs Popper to calculate coordinates using `position: fixed` relative to the viewport instead of `position: absolute` relative to the offset parent. Because `position: fixed` elements are placed relative to the viewport (or nearest transformed ancestor), they break out of the `overflow: auto` clipping context of `.table-responsive`.
Setting `data-bs-boundary="viewport"` ensures Popper's collision detection, flipping, and overflow boundary checks calculate against the viewport window.

**Alternatives Considered:**
- *Alternative A: CSS `min-height` or bottom padding on `.table-responsive`.*
  Rejected because it introduces artificial empty space when few or no mailboxes exist, and does not dynamically accommodate varying screen heights.
- *Alternative B: Dynamic DOM Teleportation via Custom JavaScript (e.g. moving `.dropdown-menu` to `document.body` on `show.bs.dropdown`).*
  Rejected because moving DOM nodes breaks HTMX event bubbling, complicates target resolution, and requires lifecycle management when HTMX replaces table fragments.
- *Alternative C: Overriding `overflow: visible` on `.table-responsive`.*
  Rejected because it eliminates horizontal scrolling on mobile viewports, breaking the responsive layout.

### Decision 2: Thymeleaf-Safe Attribute Formatting

We format the attribute as:
```html
data-bs-boundary="viewport"
data-bs-popper-config='{"strategy":"fixed"}'
```
Thymeleaf parses standard HTML5 attributes with single-quoted JSON without escaping conflicts, ensuring valid HTML5 output on the client.

## Risks / Trade-offs

- **[Risk]** Positioning synchronization during fast scroll while dropdown is open.
  → **Mitigation**: Bootstrap 5 automatically recalculates Popper positioning on scroll, and any outside click or interaction immediately closes the dropdown menu as standard UX behavior.
- **[Risk]** HTMX partial table updates replacing the dropdown while open.
  → **Mitigation**: Actions inside the dropdown (editing, deleting, toggling, extending) either trigger Bootstrap modal opens or fire HTMX requests that replace the table upon action completion, naturally resetting the dropdown.

