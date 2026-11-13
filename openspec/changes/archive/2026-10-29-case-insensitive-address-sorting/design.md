# Design: Case-Insensitive Mailbox Address Sorting

## Context

In `MailboxWebController.java`, mailbox column sorting is mapped via `resolveSort(final String sort, final String dir)` to a Spring Data `Sort` object. The address column is mapped as:

```java
case "address" -> Sort.by(direction, "address").and(Sort.by(direction, "domain"));
```

By default, Spring Data generates `ORDER BY b.address ASC, b.domain ASC` without case normalization. In relational databases (such as H2, MariaDB, and PostgreSQL under default C/binary collations), strings sort by ASCII code points where uppercase characters (`A-Z`, ASCII 65–90) precede lowercase characters (`a-z`, ASCII 97–122). As a result, addresses starting with uppercase letters (e.g. `UCoMfSLVUDQudxjJ@xcmailr.test`) sort ahead of lowercase addresses (e.g. `aaaa@xcmailr.test`), violating user expectations of natural alphabetical order.

## Goals / Non-Goals

**Goals:**
- Implement natural, case-insensitive sorting for mailbox addresses and domains.
- Use idiomatic Spring Data JPA `Sort.Order.ignoreCase()`.
- Ensure uniform case-insensitive behavior across both ascending (`asc`) and descending (`desc`) sort directions.
- Maintain full compatibility with all `MailboxRepository` query methods (`findByUsr`, `findByUsrAndSearch`, `findByUsrAndExpired`, `findExpiringByUsr`, `findUnlimitedByUsr`).
- Expand the automated testing pyramid (unit, web slice, and integration tests) to explicitly test mixed-case addresses.
- Preserve strict Java standards: `final` on all variables/parameters, comprehensive Javadoc, and explanatory code comments.

**Non-Goals:**
- Schema modifications or column collation migrations in Flyway.
- Modifying stored address casing (preserves exact casing as entered or supplied via API).
- Changing search filtering logic (which already applies `LOWER(...)` in JPQL).

## Decisions

### 1. Use Spring Data `Sort.Order.ignoreCase()`
*Decision:* In `MailboxWebController.resolveSort`, replace the case-sensitive `Sort.by(...)` with explicit case-insensitive `Sort.Order` definitions:

```java
case "address" -> Sort.by(
    Sort.Order.by("address").with(direction).ignoreCase(),
    Sort.Order.by("domain").with(direction).ignoreCase()
);
```

*Rationale:* 
- Spring Data JPA's query generator inspects `Sort.Order.isIgnoreCase()`. When `true`, it delegates to JPA criteria / Hibernate to wrap properties with the standard ANSI SQL `LOWER()` function (e.g. `ORDER BY LOWER(b.address) ASC, LOWER(b.domain) ASC`).
- Avoids fragile string-based JPQL functions in `JpaSort.unsafe(...)` and preserves property path validation against the `MBox` entity.
- Handles composite sorting cleanly (local address part first, then domain, both case-insensitive).

*Alternatives Considered:*
- `JpaSort.unsafe("LOWER(address)")`: Functions, but bypasses Spring Data's property validation and is unnecessary when `Sort.Order.ignoreCase()` is natively supported.
- Altering database column collation in Flyway: Intrusive, database-vendor specific, and risks breaking case-sensitive index lookups or unique constraints in legacy migrations.

### 2. Update and Broaden Testing Pyramid Coverage
*Decision:* Expand test coverage across all three testing tiers:
- **Tier 1 (Unit - `MailboxWebControllerTest`):**
  - Verify that `resolveSort("address", "asc")` and `resolveSort("address", "desc")` produce a `Sort` instance whose `Sort.Order` for both "address" and "domain" have `isIgnoreCase() == true` and match the requested direction.
- **Tier 2 (Web Slice - `MailboxSortingWebTest`):**
  - Verify table fragment rendering with `sort=address` passes correct model attributes and direction toggles.
- **Tier 3 (Integration - `MailboxSortingIntegrationTest`):**
  - Update `testSorting_SpecialCase_CaseInsensitiveAddressOrdering` with true mixed-case addresses (e.g. `"alpha"`, `"Bravo"`, `"charlie"`, `"DELTA"`, `"UCoMfSLV..."`).
  - Verify that on ASC, `"Bravo"` sorts strictly between `"alpha"` and `"charlie"`, and `"UCoMfSLV..."` sorts after `"DELTA"` rather than before `"alpha"`.
  - Verify that on DESC, reverse alphabetical order is strictly preserved.

## Risks / Trade-offs

- **[Risk]** Sorting with `LOWER(address)` could cause table scans on large tables without function-based indexes.
  - **Mitigation:** In XCMailr, queries are always filtered by tenant (`usr_id = :usr`) and paginated (page size 15). The number of active mailboxes per tenant is bounded, making in-memory or index-assisted sorting well within sub-millisecond execution times.
- **[Risk]** Interaction with custom `@Query` repository methods.
  - **Mitigation:** Spring Data JPA's dynamic sort handler natively appends `ORDER BY LOWER(alias.property)` to JPQL queries that take a `Pageable`. Existing repository queries already use alias `b`, ensuring seamless integration.

