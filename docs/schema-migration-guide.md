# Schema Extension & Database Migration Guide

This guide describes how to extend the database schema (adding columns, creating new tables, or adding indexes) and author Flyway migrations for new features in XCMailr while maintaining complete backwards compatibility and zero data loss.

---

## 1. How Migrations Work in XCMailr

XCMailr couples **Flyway** with **Hibernate `ddl-auto: validate`**. Database modifications follow a strict automated execution sequence during application boot:

```
                      Application Startup
                              │
                              ▼
        ┌───────────────────────────────────────────┐
        │ 1. Flyway Migration Engine                │
        │    - Scans classpath:db/migration         │
        │    - Checks flyway_schema_history table   │
        │    - Executes pending migrations in order │
        │    - Records checksum & execution status  │
        └─────────────────────┬─────────────────────┘
                              │
                              ▼
        ┌───────────────────────────────────────────┐
        │ 2. Hibernate Schema Validation            │
        │    - Reads ddl-auto: validate             │
        │    - Validates JPA entities against DB    │
        │    - Passes (new columns already exist!)  │
        └─────────────────────┬─────────────────────┘
                              │
                              ▼
        ┌───────────────────────────────────────────┐
        │ 3. Spring Boot Web & SMTP Services Start   │
        │    - Ready to serve traffic               │
        └───────────────────────────────────────────┘
```

> [!NOTE]
> Flyway always runs **before** Hibernate creates the `EntityManagerFactory`. The database schema is already updated by Flyway before Hibernate performs its validation check.

---

## 2. Migration File Conventions & Rules

All migration scripts are stored in:
```text
xcmailr-webapp/src/main/resources/db/migration/
```

### File Naming Pattern
```text
V<Version>__<Description>.sql
```
- **Prefix**: Uppercase `V`
- **Version**: Sequential integer or dot-separated version (e.g., `V4`, `V5`, `V5.1`).
- **Separator**: Exactly **two underscores** (`__`).
- **Description**: Concise title using underscores between words (e.g. `Add_Mailbox_Notes.sql`).
- **Extension**: `.sql`

### Current Migration History
| File | Purpose |
| :--- | :--- |
| `V1__Initial_Setup.sql` | Baseline schema: tables (`users`, `mailboxes`, `mail`, `register_domains`, etc.) and sequences. |
| `V2__Change_Mail_Message_Type.sql` | Upgraded `mail.message` storage to `BLOB`. |
| `V3__Remove_Mail2MailBox_Reference.sql` | Removed foreign key constraint from `mail` to `mailboxes`. |
| **`V4__<Your_Feature>.sql`** | **Next available migration version.** |

### Immutability Rule
Once a migration script has been merged or applied to any database (including local development databases):
- **Never edit or rename an existing migration file.** Flyway stores a cryptographic checksum in `flyway_schema_history` and will refuse to start if a previously executed migration file changes.
- Always create a new version (`V4`, `V5`, etc.) to apply corrections or additions.

---

## 3. Step-by-Step: Adding a New Column to an Existing Table

Suppose a new feature requires adding an optional user-defined `notes` field to the `mailboxes` table.

### Step 1: Author the Migration Script
Create `xcmailr-webapp/src/main/resources/db/migration/V4__Add_Mailbox_Notes.sql`:

```sql
-- V4__Add_Mailbox_Notes.sql
ALTER TABLE mailboxes ADD COLUMN notes VARCHAR(500);
```

#### Safe Defaults for `NOT NULL` Columns:
If the new column must not be null, you **must** supply a default value so existing production rows can be populated:

```sql
-- Safe: Existing rows receive the default value automatically
ALTER TABLE mailboxes ADD COLUMN display_mode VARCHAR(50) DEFAULT 'STANDARD' NOT NULL;
```

> [!WARNING]
> Running `ALTER TABLE mailboxes ADD COLUMN display_mode VARCHAR(50) NOT NULL;` without a `DEFAULT` will fail on any database containing existing rows because the existing rows would violate the non-null constraint.

### Step 2: Update the JPA Entity Model
Open the corresponding entity class in `xcmailr-webapp/src/main/java/models/` (e.g. `models/MBox.java`).

Add the new field, JPA annotations, and getter/setter:

```java
// Inside models/MBox.java

@Size(max = 500)
@Column(name = "notes", length = 500)
private String notes;

public String getNotes()
{
    return notes;
}

public void setNotes(final String notes)
{
    this.notes = notes;
}
```

### Step 3: Update Repositories & Services
If the new column should be queryable, add the query method in `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/repositories/MailboxRepository.java`:

```java
List<MBox> findByNotesContainingIgnoreCase(final String search);
```

---

## 4. Step-by-Step: Creating a New Table

Suppose a new feature introduces custom mailbox tagging via a new `mailbox_tags` table.

### Step 1: Author the Table Migration Script
Create `xcmailr-webapp/src/main/resources/db/migration/V4__Create_Mailbox_Tags.sql`:

```sql
-- V4__Create_Mailbox_Tags.sql

CREATE TABLE IF NOT EXISTS mailbox_tags (
    id          BIGINT NOT NULL,
    mailbox_id  BIGINT NOT NULL,
    tag_name    VARCHAR(100) NOT NULL,
    created_at  BIGINT NOT NULL,
    CONSTRAINT pk_mailbox_tags PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS mailbox_tags_seq;

-- Foreign key referencing mailboxes
ALTER TABLE mailbox_tags ADD CONSTRAINT fk_mailbox_tags_mbox
    FOREIGN KEY (mailbox_id) REFERENCES mailboxes (id) ON DELETE CASCADE;

-- Index for fast lookup by mailbox
CREATE INDEX IF NOT EXISTS ix_mailbox_tags_mbox ON mailbox_tags (mailbox_id);
```

### Step 2: Create the JPA Entity
Create `xcmailr-webapp/src/main/java/models/MailboxTag.java`:

```java
package models;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "mailbox_tags")
public class MailboxTag implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mailbox_tags_seq_gen")
    @SequenceGenerator(name = "mailbox_tags_seq_gen", sequenceName = "mailbox_tags_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "mailbox_id", nullable = false)
    private Long mailboxId;

    @NotEmpty
    @Size(max = 100)
    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    public MailboxTag()
    {
    }

    // Getters and Setters...
}
```

### Step 3: Create Spring Data JPA Repository
Create `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/repositories/MailboxTagRepository.java`:

```java
package com.xceptance.xcmailr.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import models.MailboxTag;

@Repository
public interface MailboxTagRepository extends JpaRepository<MailboxTag, Long>
{
    List<MailboxTag> findByMailboxId(final Long mailboxId);
    void deleteByMailboxId(final Long mailboxId);
}
```

---

## 5. Cross-Database Compatibility Guidelines

XCMailr supports embedded **H2** (development and integration tests), **PostgreSQL**, and **MySQL / MariaDB** (production). All SQL migration scripts must use cross-compatible ANSI SQL constructs:

| Purpose | Cross-Compatible Syntax | Avoid |
| :--- | :--- | :--- |
| **New Column** | `ALTER TABLE tbl ADD COLUMN col VARCHAR(255);` | Vendor-specific positioning like `AFTER col` (MySQL only). |
| **Data Types** | `VARCHAR(n)`, `BIGINT`, `INTEGER`, `BOOLEAN`, `BLOB` | `TEXT` (behaves differently across engines), `TINYINT(1)`, `BYTEA`. |
| **Sequences** | `CREATE SEQUENCE IF NOT EXISTS name;` | Relying exclusively on MySQL `AUTO_INCREMENT` without sequence fallback. |
| **Constraints** | Name all constraints explicitly: `CONSTRAINT pk_name PRIMARY KEY (id)` | Anonymous constraints (names differ across engines). |
| **Conditionals** | `IF NOT EXISTS` for tables, sequences, and indexes. | Unchecked DDL that fails if re-run. |

---

## 6. Testing & Verifying Migrations Locally

Always verify migrations locally using the automated test suite before committing:

### Step 1: Run Maven Test Suite
```bash
mvn clean test
```
The test suite boots Spring Boot using an in-memory H2 database:
1. Flyway executes all migrations from `V1` through your new `V<N>`.
2. Hibernate validates the entire schema against all entity models.
3. If any column name, type, or constraint does not match, the build will fail immediately with a descriptive error.

### Step 2: Test Against Persistent Database (Dev Profile)
Start the application locally:
```bash
mvn spring-boot:run -pl xcmailr-webapp -Dspring-boot.run.profiles=dev
```
Verify the startup logs:
```text
o.f.core.internal.command.DbMigrate : Current version of schema: 3
o.f.core.internal.command.DbMigrate : Migrating schema to version 4 - <Description>
o.f.core.internal.command.DbMigrate : Successfully applied 1 migration to schema (execution time ...)
```

---

## 7. Production Deployment Behavior

When deploying the updated application to production:

1. **Automatic Execution**: No manual SQL execution is required. Upon launch, Spring Boot's Flyway integration detects that the production database is at version 3 and automatically executes `V4` inside a database transaction.
2. **Transaction Rollback**: If the migration script encounters a syntax error or constraint failure, Flyway automatically rolls back the migration transaction and halts application startup, leaving existing data untouched.
3. **Audit Record**: Once applied successfully, Flyway inserts a row into `flyway_schema_history` recording the version number, description, script name, checksum, execution timestamp, and success status.
