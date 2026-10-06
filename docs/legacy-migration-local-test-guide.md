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
|     - Run legacy Ninja framework (Jetty on :8080, SMTP on :10025)                       |
|     - Database generated at: xcmailr-webapp/target/xcmailr.mv.db                         |
|     - Admin: admin@xcmailr.test (pwd: 1234)                                             |
|     - Regular user: jane.doe@xcmailr.test (pwd: Password123!)                           |
|     - Mailbox: jane-box@xcmailr.test + received test email                              |
|     - Stop legacy server (Ctrl+C)                                                       |
|                                                                                         |
|                                     │                                                   |
|                                     │ (points datasource to legacy relative path)       |
|                                     ▼                                                   |
|                                                                                         |
|  2. .  (current workspace, branch: modernizeXCMailr)                                   |
|     - Run modernized Spring Boot application (Tomcat on :8080, SMTP on :25000)          |
|     - Log in as jane.doe@xcmailr.test (pwd: Password123!) -> Verifies non-admin user    |
|     - Log in as admin@xcmailr.test (pwd: 1234)           -> Verifies admin preservation |
|     - Create new mailbox and receive new mail            -> Verifies sequence safety    |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

---

## 2. Phase 1: Create Legacy Worktree & Populate Real Data

### Step 1: Create the Worktree
From the root of your current repository, create an isolated worktree for the `develop` branch in a sibling directory:

```bash
git worktree add ../XCMailr-legacy develop
cd ../XCMailr-legacy
```

> [!NOTE]
> `git worktree` creates an independent checkout folder sharing the same underlying git object store. Your current branch and any uncommitted workspace changes remain completely untouched.

### Step 2: Start the Legacy Application
Compile and launch the legacy Ninja development server:

```bash
mvn compile ninja:run -pl xcmailr-webapp
```

Wait until you see the Ninja startup banner and confirmation message:
```text
INFO  [main] - Ninja application started in ...ms
```

The legacy application is now running with:
- **Web UI**: `http://localhost:8080`
- **Inbound SMTP**: `localhost:10025`
- **H2 Database File**: `xcmailr-webapp/target/xcmailr.mv.db`

---

### Step 3: Register a Standard Non-Admin User

1. Open `http://localhost:8080` in your web browser.
2. Click **Register** in the top navigation (or visit `http://localhost:8080/register`).
3. Fill in the user registration form:
   - **First Name**: `Jane`
   - **Surname**: `Doe`
   - **Email**: `jane.doe@xcmailr.test`
   - **Language**: `en`
   - **Password**: `Password123!`
   - **Confirm Password**: `Password123!`
4. Click **Register**. The registration is stored in the database with `active = false`.

---

### Step 4: Activate Jane's Account via Admin Console

By default, accounts require confirmation. Activate Jane's account directly using the pre-seeded legacy administrator:

1. Click **Sign In** in the top-right corner.
2. Log in with the legacy administrator credentials:
   - **Email**: `admin@xcmailr.test`
   - **Password**: `1234`
3. Click **Admin** $\rightarrow$ **Users** in the navigation bar.
4. Locate `jane.doe@xcmailr.test` in the user table.
5. Click **Activate** next to Jane's entry (status updates to active).
6. Click **Sign Out**.

---

### Step 5: Create a Mailbox and Send a Legacy Email for Jane

1. Click **Sign In** and authenticate as the regular user:
   - **Email**: `jane.doe@xcmailr.test`
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

### Step 6: Gracefully Shut Down Legacy Application

In the terminal running `mvn compile ninja:run`:
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
Run Spring Boot with the `dev` profile and pass the relative path to the legacy database using `-Dspring.datasource.url`:

```bash
mvn spring-boot:run -pl xcmailr-webapp \
  -Dspring-boot.run.profiles=dev \
  -Dspring.datasource.url="jdbc:h2:../XCMailr-legacy/xcmailr-webapp/target/xcmailr;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"
```

---

## 4. Phase 3: Verification Checklist

### Checkpoint 1: Inspect Startup Logs
Verify the console output for the following critical milestones:
1. **Flyway Schema Check**:
   ```text
   o.f.core.internal.command.DbMigrate : Current version of schema: 3
   o.f.core.internal.command.DbMigrate : Schema is up to date. No migration necessary.
   ```
2. **Hibernate Schema Validation**:
   Spring Boot boots cleanly without `SchemaManagementException`, confirming all JPA entities match existing database columns.
3. **Admin Account Preservation**:
   ```text
   c.x.xcmailr.config.AdminUserInitializer : Administrator account 'admin@xcmailr.test' already exists. Leaving credentials and permissions untouched.
   ```

---

### Checkpoint 2: Standard Non-Admin User Authentication & Data
1. Navigate to `http://localhost:8080/login`.
2. Sign in as Jane:
   - **Email**: `jane.doe@xcmailr.test`
   - **Password**: `Password123!`
3. Verify:
   - **Authentication**: Password succeeds immediately via `BCryptPasswordEncoder` verifying the legacy `$2a$` hash.
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
   - **Admin** $\rightarrow$ **Users** lists both `admin@xcmailr.test` and `jane.doe@xcmailr.test`.
4. Click **Sign Out**.

---

## 5. Phase 4: Cleanup

When you are finished testing:
1. Stop the modernized Spring Boot application (`Ctrl + C`).
2. Remove the temporary legacy worktree:
   ```bash
   git worktree remove ../XCMailr-legacy --force
   ```
