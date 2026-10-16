# Local Legacy Data Migration & Cutover Testing Guide

This runbook guides you through testing database backward compatibility and cutover locally. You will run the original legacy XCMailr application in an isolated git worktree, populate real database records—including an **administrator** and a **standard non-admin user** with mailboxes and emails—and then attach the modernized Spring Boot application to that database to verify authentication, data visibility, and sequence safety.

---

## 1. Architecture & Verification Concept

```
+-----------------------------------------------------------------------------------------+
|                                    Worktree & Cutover Flow                              |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|  1. ../XCMailr-legacy  (branch: develop)                                                |
|     - Step 1: mvn clean process-classes -pl xcmailr-webapp  (compile + Ebean enhance)   |
|     - Step 2: mvn ninja:run -pl xcmailr-webapp              (Jetty :8080, SMTP :10025)  |
|     - Database generated at: xcmailr-webapp/target/xcmailr.mv.db                         |
|     - Admin: admin@xcmailr.test (pwd: 1234)                                             |
|     - Regular user: jane.doe@example.com (pwd: Password123!)                            |
|     - Mailbox: jane-box@xcmailr.test + received test email                              |
|     - Stop legacy server (Ctrl+C)                                                       |
|                                                                                         |
|                                     │                                                   |
|                                     │ (points datasource to legacy relative path)       |
|                                     ▼                                                   |
|                                                                                         |
|  2. .  (current workspace, branch: modernizeXCMailr)                                   |
|     - Run modernized Spring Boot application (Tomcat on :8080, SMTP on :25000)          |
|     - Log in as jane.doe@example.com (pwd: Password123!) -> Verifies non-admin user     |
|     - Log in as admin@xcmailr.test (pwd: 1234)           -> Verifies admin preservation |
|     - Create new mailbox and receive new mail            -> Verifies sequence safety    |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

---

## 2. Phase 1: Create Legacy Worktree, Build & Populate Real Data

### Step 1: Create the Worktree
From the root of your current repository, create an isolated worktree for the `develop` branch in a sibling directory:

```bash
git worktree add ../XCMailr-legacy develop
cd ../XCMailr-legacy
```

> [!NOTE]
> `git worktree` creates an independent checkout folder sharing the same underlying git object store. Your current branch and any uncommitted workspace changes remain completely untouched.

---

### Step 2: Build & Enhance Legacy Entity Bytecode

Legacy XCMailr uses Ebean ORM, which requires bytecode enhancement (weaving accessors and dirty-tracking into entity classes such as `MailStatisticsKey` and `MBox`). The enhancement plugin binds to Maven's `process-classes` phase.

Run the build step to compile sources and weave Ebean enhancements:

```bash
mvn clean process-classes -pl xcmailr-webapp
```

Verify that the output contains the `ebean:enhance` execution:
```text
[INFO] --- enhance:15.12.0:enhance (main) @ xcmailr-webapp ---
[INFO] classSource=.../xcmailr-webapp/target/classes transformArgs=debug=1
[INFO] Enhanced .../models/MailStatisticsKey.class
[INFO] Enhanced .../models/MBox.class
[INFO] BUILD SUCCESS
```

---

### Step 3: Start the Legacy Application Server

Launch the legacy Ninja development server (Jetty):

```bash
mvn ninja:run -pl xcmailr-webapp
```

*(Alternatively, you can run both steps together in a single command: `mvn clean process-classes ninja:run -pl xcmailr-webapp`)*.

Wait until you see the Ninja startup banner:
```text
 _______  .___ _______        ____.  _____   
 \      \ |   |\      \      |    | /  _  \  
 /   |   \|   |/   |   \     |    |/  /_\  \ 
/    |    \   /    |    \/\__|    /    |    \  https://www.ninjaframework.org
\____|__  /___\____|__  /\________\____|__  /  @ninjaframework
     web\/framework   \/                  \/   6.9.0

INFO  [main] - Ninja application started in ...ms
```

The legacy application is now running with:
- **Web UI**: `http://localhost:8080`
- **Inbound SMTP**: `localhost:10025`
- **H2 Database File**: `xcmailr-webapp/target/xcmailr.mv.db`

---

### Step 4: Register a Standard Non-Admin User

1. Open `http://localhost:8080` in your web browser.
2. Click **Register** in the top navigation (or visit `http://localhost:8080/register`).
3. Fill in the user registration form:
   - **First Name**: `Jane`
   - **Surname**: `Doe`
   - **Email**: `jane.doe@example.com`
   - **Language**: `en`
   - **Password**: `Password123!`
   - **Confirm Password**: `Password123!`
4. Click **Register**. The registration is stored in the database with `active = false`.

> [!IMPORTANT]
> **Why `example.com` instead of `xcmailr.test`?**
> In XCMailr, a user's account email is their real external forwarding destination. To prevent infinite forwarding loops, the application strictly forbids registering accounts with its own managed domains (`xcmailr.test`, `ccmailr.test`). Registering with an `@xcmailr.test` address triggers the anti-loop error: *"Email addresses containing this domain are not allowed."* Always use an external domain (e.g. `@example.com` or `@company.org`) for user accounts.

---

### Step 5: Activate Jane's Account via Admin Console

By default, accounts require confirmation. Activate Jane's account directly using the pre-seeded legacy administrator:

1. Click **Sign In** in the top-right corner.
2. Log in with the legacy administrator credentials:
   - **Email**: `admin@xcmailr.test`
   - **Password**: `1234`
3. Click **Admin** $\rightarrow$ **Users** in the navigation bar.
4. Locate `jane.doe@example.com` in the user table.
5. Click **Activate** next to Jane's entry (status updates to active).
6. Click **Sign Out**.

---

### Step 6: Create a Mailbox and Send a Legacy Email for Jane

1. Click **Sign In** and authenticate as the regular user:
   - **Email**: `jane.doe@example.com`
   - **Password**: `Password123!`
2. Navigate to **Email Addresses** $\rightarrow$ **Add Email**:
   - **Address**: `jane-box`
   - **Domain**: `xcmailr.test`
   - **Active**: Checked
   - **Forward Emails**: Unchecked
   - Click **Save**.
   - Confirm that `jane-box@xcmailr.test` appears in the list.
3. Open a separate terminal and send an email to Jane's mailbox via the legacy SMTP port (`10025`):

   ```bash
   python3 - << 'EOF'
   import smtplib
   from email.mime.text import MIMEText

   msg = MIMEText("Hello Jane! This message was generated in the legacy XCMailr application.")
   msg['Subject'] = "Legacy Mail for Jane"
   msg['From'] = "colleague@external.org"
   msg['To'] = "jane-box@xcmailr.test"

   with smtplib.SMTP('localhost', 10025) as server:
       server.send_message(msg)
       print("Message delivered to legacy SMTP server!")
   EOF
   ```

4. Refresh your browser at `http://localhost:8080` and click on `jane-box@xcmailr.test`.
   - Confirm the email with subject `"Legacy Mail for Jane"` is displayed and readable.
5. Click **Sign Out**.

---

### Step 7: Gracefully Shut Down Legacy Application

In the terminal running `mvn ninja:run`:
- Press `Ctrl + C` to stop the server.
- The H2 database cleanly releases file locks and flushes all changes to `xcmailr-webapp/target/xcmailr.mv.db`.

---

## 3. Phase 2: Start Modernized Spring Boot Against the Legacy DB

Switch back to your primary workspace and launch the modernized application pointing to the legacy database file via a relative path.

### Step 1: Return to the Primary Workspace
```bash
cd ../XCMailr
```

### Step 2: Launch Modernized XCMailr
Launch Spring Boot with the `dev` profile and pass the relative path to the legacy database using `-Dspring-boot.run.arguments`:

```bash
mvn spring-boot:run -pl xcmailr-webapp \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:../../XCMailr-legacy/xcmailr-webapp/target/xcmailr;IFEXISTS=TRUE;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"
```

*(Alternatively, you can pass it via environment variable)*:
```bash
SPRING_DATASOURCE_URL="jdbc:h2:../../XCMailr-legacy/xcmailr-webapp/target/xcmailr;IFEXISTS=TRUE;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE" \
mvn spring-boot:run -pl xcmailr-webapp -Dspring-boot.run.profiles=dev
```

> [!NOTE]
> **Why `../../`, `-Dspring-boot.run.arguments`, and `;IFEXISTS=TRUE`?**
> 1. **`../../`**: When Maven executes `spring-boot:run` with `-pl xcmailr-webapp`, the working directory of the running process is `xcmailr-webapp/`. Two upward steps (`../../`) are required to reach the sibling workspace directory `../XCMailr-legacy`.
> 2. **`-Dspring-boot.run.arguments="--spring.datasource.url=..."`**: `spring-boot:run` forks a separate JVM for the application. Passing this via `spring-boot.run.arguments` ensures the argument is passed directly to Spring Boot rather than being consumed solely by Maven.
> 3. **`;IFEXISTS=TRUE`**: Tells H2 to fail fast with an error if the database file is missing or the path is incorrect, preventing H2 from silently creating a brand-new empty database.

---

## 4. Phase 3: Verification Checklist

### Checkpoint 1: Inspect Startup Logs
Verify the console output for the following critical milestones:
1. **Flyway Schema Check**:
   - **Initial Migration Cutover (First Run)**:
     Legacy XCMailr managed its schema via Ebean ORM rather than Flyway, so `flyway_schema_history` does not exist yet. On the first run, Flyway baselines the legacy schema at version `0` and applies migrations V1–V3 (all statements use non-destructive `IF NOT EXISTS` / `IF EXISTS` logic, preserving all legacy data and completing in milliseconds):
     ```text
     o.f.c.i.database.base.BaseDatabaseType   : Database: jdbc:h2:../../XCMailr-legacy/xcmailr-webapp/target/xcmailr (H2 2.3)
     o.f.core.internal.command.DbBaseline     : Creating Schema History table "PUBLIC"."flyway_schema_history" with baseline ...
     o.f.core.internal.command.DbBaseline     : Successfully baselined schema with version: 0
     o.f.core.internal.command.DbMigrate      : Current version of schema "PUBLIC": 0
     o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "1 - Initial Setup"
     o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "2 - Change Mail Message Type"
     o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "3 - Remove Mail2MailBox Reference"
     o.f.core.internal.command.DbMigrate      : Successfully applied 3 migrations to schema "PUBLIC", now at version v3
     ```
   - **Subsequent Application Restarts**:
     Once baselined and migrated, subsequent runs report schema version 3 is already up to date:
     ```text
     o.f.core.internal.command.DbMigrate      : Current version of schema "PUBLIC": 3
     o.f.core.internal.command.DbMigrate      : Schema "PUBLIC" is up to date. No migration necessary.
     ```
2. **Hibernate Schema Validation**:
   Spring Boot boots cleanly without `SchemaManagementException`, confirming all JPA entities match existing database columns.
3. **Admin Account Preservation & Default Domain Seeding**:
   ```text
   c.x.xcmailr.config.AdminUserInitializer  : Administrator account 'admin@xcmailr.test' already exists. Leaving credentials and permissions untouched.
   c.x.xcmailr.config.AdminUserInitializer  : Seeding default domain in whitelist: xcmailr.test
   ```
   *(The admin's password and privileges are left untouched, while `xcmailr.test` is seeded into `register_domains` if absent, ensuring the web UI mailbox creation dropdown is immediately populated.)*

---

### Checkpoint 2: Standard Non-Admin User Authentication & Data
1. Navigate to `http://localhost:8080/login`.
2. Sign in as Jane:
   - **Email**: `jane.doe@example.com`
   - **Password**: `Password123!`
3. Verify:
   - **Authentication**: Password succeeds immediately via `BCryptPasswordEncoder` verifying the legacy `$2a$` hash without requiring a password reset.
   - **Role**: Only standard user controls are visible (no Admin navigation).
   - **Mailbox**: `jane-box@xcmailr.test` is listed.
   - **Email Content**: Click `jane-box@xcmailr.test` $\rightarrow$ historical email `"Legacy Mail for Jane"` renders sender, subject, timestamp, and message body intact.

---

### Checkpoint 3: Sequence Continuity & Inbound SMTP Delivery
With Jane still logged in:
1. **Create a New Mailbox**:
   - Add mailbox `jane-modern@xcmailr.test`.
   - Confirm it saves successfully (validates that `mailboxes_seq` advances past legacy IDs without primary key collision).
2. **Receive an Email on Modernized Port 25000**:
   - In a separate terminal, dispatch an email to the new address:
   ```bash
   python3 - << 'EOF'
   import smtplib
   from email.mime.text import MIMEText

   msg = MIMEText("Testing reception in modernized Spring Boot!")
   msg['Subject'] = "Modernized Inbound Test"
   msg['From'] = "colleague@external.org"
   msg['To'] = "jane-modern@xcmailr.test"

   with smtplib.SMTP('localhost', 25000) as server:
       server.send_message(msg)
       print("Message delivered to modernized SMTP server!")
   EOF
   ```
3. Refresh the inbox: confirm both the legacy email and the new email appear side-by-side.
4. Click **Sign Out**.

---

### Checkpoint 4: Administrator User Authentication
1. Navigate to `http://localhost:8080/login`.
2. Sign in as the administrator:
   - **Email**: `admin@xcmailr.test`
   - **Password**: `1234`
3. Verify:
   - Login succeeds.
   - **Admin** menu is accessible.
   - **Admin** $\rightarrow$ **Users** lists both `admin@xcmailr.test` and `jane.doe@example.com`.
4. Click **Sign Out**.

---

## 5. Phase 4: Cleanup

When you are finished testing:
1. Stop the modernized Spring Boot application (`Ctrl + C`).
2. Remove the temporary legacy worktree:
   ```bash
   git worktree remove ../XCMailr-legacy --force
   ```
