# Production Deployment & Migration Guide

This guide provides step-by-step instructions for deploying the modernized Spring Boot XCMailr application in a production environment using an existing database (PostgreSQL, MySQL/MariaDB, or embedded H2).

---

## 1. Prerequisites & System Requirements

- **Java 25 Runtime**: OpenJDK 25 (e.g. Eclipse Temurin 25 or vendor equivalent) installed on the production server.
- **Network Ports**:
  - Web HTTP listener: Port `8080` (or behind Nginx/Apache reverse proxy on `80`/`443`).
  - Inbound SMTP listener: Port `25000` (or port `25` with appropriate bind capabilities).
  - Outbound SMTP relay: Typically port `587` (STARTTLS) or `465` (SSL).
- **Database Access**: Read/write access to the existing XCMailr database.

---

## 2. Phase 1: Pre-Deployment Database Backup

Always take a complete database backup prior to initiating service cutover.

### Option A: PostgreSQL
```bash
pg_dump -h <db-host> -U xcmailr -d xcmailr > xcmailr_backup_$(date +%Y%m%d_%H%M%S).sql
```

### Option B: MySQL / MariaDB
```bash
mysqldump -h <db-host> -u xcmailr -p xcmailr > xcmailr_backup_$(date +%Y%m%d_%H%M%S).sql
```

### Option C: Embedded H2
1. Stop the legacy XCMailr process to release file locks.
2. Create a timestamped copy of the database file:
```bash
cp /var/lib/xcmailr/xcmailrDB.mv.db /var/lib/xcmailr/xcmailrDB.mv.db.backup_$(date +%Y%m%d_%H%M%S)
```

---

## 3. Phase 2: Build Executable Artifact

Build and package the production jar from the repository root:

```bash
mvn clean package -DskipTests
```

The resulting executable fat jar will be located at:
```text
xcmailr-webapp/target/xcmailr-webapp-3.1.0.jar
```

Copy this jar file to the target production server (e.g. `/opt/xcmailr/xcmailr-webapp.jar`).

---

## 4. Phase 3: Configure Production Environment

Configure production parameters either using environment variables or by providing an external configuration file.

### Recommended: Environment Variables (`/etc/xcmailr/xcmailr.env`)

Create an environment configuration file:

```bash
# Database Configuration (Connect to existing production DB)
SPRING_DATASOURCE_URL="jdbc:postgresql://db.internal.example.com:5432/xcmailr"
SPRING_DATASOURCE_USERNAME="xcmailr"
SPRING_DATASOURCE_PASSWORD="your_secure_db_password"
SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver"

# Web Server & Base URL
PORT=8080
APP_URL="https://mail.example.com"

# Existing Administrator Account (Ensures initializer skips bootstrapping)
ADMIN_ADDRESS="admin@example.com"
ADMIN_PASSWORD="existing_admin_password"

# Inbound SMTP Server (Listening for incoming test emails)
MBOX_HOST="mail.example.com"
MBOX_PORT=25000

# Outbound SMTP Relay (For forwarding test emails to destination inboxes)
SMTP_HOST="smtp.relay.example.com"
SMTP_PORT=587
SMTP_USER="smtp-relay-user"
SMTP_PASS="smtp-relay-password"

# Self-Registration Settings
APP_REQUIRE_CONFIRMATION=true
```

> [!NOTE]
> If using embedded H2 in production, set:
> `SPRING_DATASOURCE_URL="jdbc:h2:/var/lib/xcmailr/xcmailrDB;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"`

### Alternative: External Properties/YAML File
You can provide an external file to override defaults:
```bash
java -jar /opt/xcmailr/xcmailr-webapp.jar --spring.config.additional-location=file:/etc/xcmailr/application-prod.yml
```

---

## 5. Phase 4: Service Cutover & Systemd Setup

### Step 1: Stop the Legacy Service
```bash
systemctl stop xcmailr-legacy
systemctl disable xcmailr-legacy
```

### Step 2: Configure Systemd Service Unit
Create `/etc/systemd/system/xcmailr.service`:

```ini
[Unit]
Description=XCMailr Email Testing Platform
After=network.target postgresql.service

[Service]
Type=simple
User=xcmailr
Group=xcmailr
WorkingDirectory=/opt/xcmailr
EnvironmentFile=/etc/xcmailr/xcmailr.env
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/xcmailr/xcmailr-webapp.jar
Restart=on-failure
RestartSec=10
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

### Step 3: Enable and Start Modernized Application
```bash
systemctl daemon-reload
systemctl enable xcmailr
systemctl start xcmailr
```

---

## 6. Phase 5: Startup Verification Milestones

Inspect the startup logs immediately to verify successful database attachment:

```bash
journalctl -u xcmailr -f
```

Look for these critical milestone messages:

1. **Flyway Schema Check**:
   ```text
   o.f.core.internal.command.DbMigrate : Current version of schema: 3
   o.f.core.internal.command.DbMigrate : Schema is up to date. No migration necessary.
   ```
2. **Hibernate Schema Validation**:
   ```text
   org.hibernate.orm.connections.pooling : ...
   org.hibernate.validator.internal.util.Version : ...
   ```
   *(No `SchemaManagementException` appears, proving entity mappings match existing tables perfectly).*
3. **Administrator Account Preservation**:
   ```text
   c.x.xcmailr.config.AdminUserInitializer : Administrator account 'admin@example.com' already exists. Leaving credentials and permissions untouched.
   ```
4. **Listener Initialization**:
   ```text
   c.x.x.s.SmtpServerService : Inbound SMTP Server started on port 25000
   o.s.b.w.e.t.TomcatWebServer : Tomcat started on port 8080 (http)
   ```
5. **Actuator Health Endpoint**:
   ```bash
   curl -s http://localhost:8080/actuator/health
   # Returns: {"status":"UP"}
   ```

---

## 7. Phase 6: Post-Deployment Smoke Test Checklist

- [ ] **Web Console Access**: Navigate to `https://mail.example.com/login`. Confirm the branded login screen loads.
- [ ] **Legacy User Login**: Log in with an existing user account and verify successful authentication without password reset.
- [ ] **Mailbox Inspection**: Verify that existing mailboxes, forward addresses, and expiration statuses appear on the dashboard.
- [ ] **All Mails Inbox**: Click **All Mails** from the mailbox navigation dropdown and confirm that historical emails are visible.
- [ ] **Inbound SMTP Reception**: Send a test email from an external client to an existing mailbox address. Confirm it appears in the webmail view.
- [ ] **Outbound Relay / Forwarding**: If email forwarding is enabled for a test mailbox, confirm the forwarded message reaches the destination inbox.

---

## 8. Rollback Contingency Plan

In the unlikely event that an issue arises during cutover:

1. **Stop the New Service**:
   ```bash
   systemctl stop xcmailr
   ```
2. **Restore Database (if required)**:
   - For PostgreSQL: `psql -h <host> -U xcmailr -d xcmailr < xcmailr_backup_*.sql`
   - For MySQL: `mysql -h <host> -u xcmailr -p xcmailr < xcmailr_backup_*.sql`
   - For H2: `cp /var/lib/xcmailr/xcmailrDB.mv.db.backup_* /var/lib/xcmailr/xcmailrDB.mv.db`
3. **Restart Legacy Service**:
   ```bash
   systemctl start xcmailr-legacy
   ```

