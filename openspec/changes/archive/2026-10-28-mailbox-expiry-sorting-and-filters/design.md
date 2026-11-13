# Design

## Context

The mailbox dashboard in XCMailr supports server-side sorting across user mailboxes for columns such as `address`, `status`, `expires`, `forwards`, and `suppressions`, as well as status filtering (`ALL`, `ACTIVE`, `EXPIRED`).

Expiration for mailboxes is stored on the `models.MBox` JPA entity in field `ts_Active` (`long` primitive millisecond epoch timestamp). Permanent unlimited mailboxes use the sentinel value `0L`.

Previously, raw numerical ordering `ORDER BY ts_Active ASC` treated `0L` as the lowest numerical timestamp, causing permanent mailboxes to appear before expiring mailboxes when sorting by expiration ascending. In real-world usage, users sort by expiration ascending to identify mailboxes expiring soonest. Treating permanent mailboxes as already expired or expiring first violates user intuition.

Under **Perspective A (Pure Infinity: $+\infty$)**, unlimited mailboxes represent infinite lifespan:
- `Expires ASC`: Finite expiration timestamps sorted chronologically (expiring soonest first), with unlimited mailboxes (`ts_Active = 0`) positioned at the very end ($+\infty$).
- `Expires DESC`: Unlimited mailboxes (`ts_Active = 0`) positioned at the very top ($+\infty$), followed by finite timestamps in reverse chronological order (expiring furthest in the future down to soonest).

Additionally, users require more granular dashboard filtering to isolate expiring mailboxes from permanent mailboxes without sorting.

See `proposal.md` for motivation and background.

---

## Goals / Non-Goals

### Goals
- Implement Perspective A ($+\infty$) expiration sorting at the database query level via conditional JPQL/SQL `ORDER BY CASE ...` expressions via Spring Data `JpaSort.unsafe`.
- Add status filter options `EXPIRING` ("Expiring Only") and `UNLIMITED` ("Unlimited Only") to the dashboard filter dropdown, controller request mapping, and repository query layer.
- Preserve complete filter, sort, and search query state across table refreshes, column header clicks, pagination, and HTMX partial DOM updates.
- Provide a robust testing pyramid including Tier 1 unit tests, Tier 2 repository tests, and Tier 3 web/database integration tests covering happy paths, error cases, and special cases.
- Enforce clean code quality: apply `final` modifiers wherever possible, full Javadoc documentation on all classes and methods, and detailed code comments.

### Non-Goals
- Altering the database schema or modifying the `MBox.ts_Active` column type (remains `long` epoch ms with `0L` sentinel).
- Changing expiration sorting behavior for columns other than `expires`.
- Introducing client-side JavaScript sorting or breaking full-dataset server-side pagination.

---

## Decisions

### Decision 1: Perspective A SQL/JPQL Sorting via `JpaSort.unsafe`

**Options considered:**
1. *In-Memory Sorting in Controller*: Fetch all user mailboxes, sort using a Java `Comparator`, and slice in memory.
   - *Rejected*: Defeats database-level pagination, degrades performance on large datasets, and reintroduces Issue #91.
2. *Separate Database Column or View*: Add a computed database column or view mapping `0L` to `Long.MAX_VALUE`.
   - *Rejected*: Requires schema migration and adds entity synchronization complexity.
3. *Conditional SQL `CASE` Expression via `JpaSort.unsafe` (Selected)*:
   - For `Expires ASC`:
     ```java
     JpaSort.unsafe(Sort.Direction.ASC, "(CASE WHEN ts_Active = 0 THEN 1 ELSE 0 END)")
            .and(JpaSort.unsafe(Sort.Direction.ASC, "ts_Active"))
     ```
     Produces: `ORDER BY CASE WHEN m.ts_active = 0 THEN 1 ELSE 0 END ASC, m.ts_active ASC`.
     Finite timestamps have weight 0 and sort ascending (soonest first); unlimited (0) has weight 1 and sorts at the end ($+\infty$).
   - For `Expires DESC`:
     ```java
     JpaSort.unsafe(Sort.Direction.ASC, "(CASE WHEN ts_Active = 0 THEN 0 ELSE 1 END)")
            .and(JpaSort.unsafe(Sort.Direction.DESC, "ts_Active"))
     ```
     Produces: `ORDER BY CASE WHEN m.ts_active = 0 THEN 0 ELSE 1 END ASC, m.ts_active DESC`.
     Unlimited (0) has weight 0 and sorts at the very top ($+\infty$); finite timestamps have weight 1 and sort descending (latest first down to soonest).

**Rationale**:
Empirical verification confirmed that Spring Data JPA and Hibernate correctly parse this `JpaSort.unsafe` expression, qualify the entity attribute alias `ts_Active`, and execute the exact SQL query cleanly across both H2 and production relational databases without schema changes.

---

### Decision 2: Status Filter Partitioning & Repository Methods

**Status filter taxonomy:**
- `ALL`: All mailboxes belonging to the user (`findByUsr` / `findByUsrAndSearch`).
- `ACTIVE`: All active mailboxes (`expired = false`).
- `EXPIRED`: All deactivated or elapsed mailboxes (`expired = true`).
- `EXPIRING`: Active mailboxes with finite expiration deadline (`expired = false AND ts_Active > 0`).
- `UNLIMITED`: Active permanent mailboxes (`expired = false AND ts_Active = 0`).

Notice that `ACTIVE` is a disjoint union of `EXPIRING` and `UNLIMITED`:
$$\text{ACTIVE} = \text{EXPIRING} \cup \text{UNLIMITED}$$

**New Repository Methods in `MailboxRepository`:**
```java
@Query("SELECT b FROM MBox b WHERE b.usr = :usr AND b.expired = false AND b.ts_Active > 0")
Page<MBox> findExpiringByUsr(@Param("usr") final User usr, final Pageable pageable);

@Query("SELECT b FROM MBox b WHERE b.usr = :usr AND b.expired = false AND b.ts_Active > 0 AND (LOWER(b.address) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.domain) LIKE LOWER(CONCAT('%', :search, '%')))")
Page<MBox> findExpiringByUsrAndSearch(@Param("usr") final User usr, @Param("search") final String search, final Pageable pageable);

@Query("SELECT b FROM MBox b WHERE b.usr = :usr AND b.expired = false AND b.ts_Active = 0")
Page<MBox> findUnlimitedByUsr(@Param("usr") final User usr, final Pageable pageable);

@Query("SELECT b FROM MBox b WHERE b.usr = :usr AND b.expired = false AND b.ts_Active = 0 AND (LOWER(b.address) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.domain) LIKE LOWER(CONCAT('%', :search, '%')))")
Page<MBox> findUnlimitedByUsrAndSearch(@Param("usr") final User usr, @Param("search") final String search, final Pageable pageable);
```

**Controller Normalization via `resolveStatus`:**
A new package-private static helper `resolveStatus(final String status)` normalizes input against a strict whitelist:
```java
static String resolveStatus(final String status)
{
    if (status == null)
    {
        return "ALL";
    }
    final String cleanStatus = status.trim().toUpperCase();
    return switch (cleanStatus)
    {
        case "ACTIVE", "EXPIRED", "EXPIRING", "UNLIMITED" -> cleanStatus;
        default -> "ALL";
    };
}
```

---

### Decision 3: Frontend Template Dropdown & Parameter Preservation

In `templates/mailboxes/index.html`:
Extend `<select id="statusFilterSelect" name="status">`:
```html
<option value="ALL" th:selected="${status == 'ALL'}">All Statuses</option>
<option value="ACTIVE" th:selected="${status == 'ACTIVE'}">Active Only</option>
<option value="EXPIRED" th:selected="${status == 'EXPIRED'}">Expired Only</option>
<option value="EXPIRING" th:selected="${status == 'EXPIRING'}">Expiring Only</option>
<option value="UNLIMITED" th:selected="${status == 'UNLIMITED'}">Unlimited Only</option>
```

In `templates/mailboxes/fragments/mailbox-table.html`:
No markup changes are necessary because header sort links and pagination controls already serialize `status=${status}`, which dynamically preserves `EXPIRING` and `UNLIMITED`.

---

### Decision 4: Testing Pyramid Architecture

Follow the testing pyramid with zero coverage gaps:
1. **Tier 1 (Unit Tests)**:
   - File: `MailboxWebControllerTest` or dedicated unit test class.
   - Fast, Mockito/JUnit 5 tests verifying `resolveSortField`, `resolveSortDirection`, `resolveStatus`, and `resolveSort` under all permutations:
     - *Happy paths*: whitelisted values for column, direction, and status.
     - *Error cases*: null, empty string, whitespace, arbitrary strings, case variations, SQL injection strings.
     - *Special cases*: verify sorting criteria generated for `expires asc` and `expires desc`.
2. **Tier 2 (Repository Tests)**:
   - File: `MailboxRepositoryTest` or dedicated `@DataJpaTest`.
   - Verifies SQL generation and predicate behavior for `findExpiringByUsr`, `findExpiringByUsrAndSearch`, `findUnlimitedByUsr`, and `findUnlimitedByUsrAndSearch`.
   - Verifies correct filtering when dataset has mixed states (active expiring, active unlimited, expired finite, expired unlimited).
3. **Tier 3 (Integration Tests)**:
   - File: `MailboxSortingIntegrationTest`.
   - Update `testSorting_SpecialCase_PermanentUnlimitedMailboxes_ExpiresSort` to assert Perspective A:
     - `ascList.get(0)` is early expiring, `ascList.get(1)` is late expiring, `ascList.get(2)` is unlimited (`ts_Active = 0`).
     - `descList.get(0)` is unlimited (`ts_Active = 0`), `descList.get(1)` is late expiring, `descList.get(2)` is early expiring.
   - Add integration tests for:
     - `status=EXPIRING` filter execution.
     - `status=UNLIMITED` filter execution.
     - Combined `status=EXPIRING&sort=expires&dir=asc`.
     - Combined `status=ALL&sort=expires&dir=asc` with mixed mailboxes.
     - Fallback for unknown status `status=INVALID`.
     - Fallback for unknown sort `sort=INVALID`.

---

## Risks / Trade-offs

- **[Risk]** Dialect incompatibility with `CASE WHEN` in ORDER BY across different SQL databases.
  - **Mitigation**: Standard SQL-92 `CASE WHEN ... THEN ... ELSE ... END` in `ORDER BY` is supported across all major relational databases (H2, PostgreSQL, MySQL, MariaDB, Oracle). Verified empirically in H2 test runner.
- **[Risk]** Performance impact of conditional `ORDER BY` on very large datasets without an index on the CASE expression.
  - **Mitigation**: User mailboxes are already partitioned by `usr_id`. Typical per-user mailbox volume is bounded (tens to thousands). The database engine filters by `usr_id` using an index first before sorting the user slice.
- **[Risk]** Breaking existing test assertions expecting `0L` to sort first in ASC.
  - **Mitigation**: Intentionally update `MailboxSortingIntegrationTest` to assert Perspective A behavior as specified.

---

## Migration Plan

No database schema migrations are required. The changes are backward compatible:
- Unrecognized or legacy status filter values safely default to `ALL`.
- Deploying the update immediately activates Perspective A sorting and extended filter options.
- Rollback can be performed cleanly by reverting code commits without database rollback.

