# Local Email Testing & Delivery Guide

This guide describes how to run and verify the entire end-to-end email pipeline in XCMailr locally without external dependencies or sending real emails, using **Mailpit** and the unified **`dev`** Spring Boot profile.

---

## 1. Architecture Overview

XCMailr manages two distinct email flows:

```
[Inbound Test Email]
        │
        ▼ (Port 25000 - SubEthaSMTP)
 ┌───────────────┐
 │ XCMailr App   │ ──► [Database: MBox & Mail stored]
 └───────┬───────┘
         │ (Outbound SMTP via Jakarta Mail)
         ▼
 ┌───────────────┐
 │ Mailpit Relay │ (Port 1025)
 └───────┬───────┘
         │
         ▼ (Web Inspection UI on Port 8025)
 [Developer Browser]
```

1. **Inbound Mail Server (Port 25000)**:
   - Embedded SubEthaSMTP server receiving incoming emails destined for configured mailboxes (e.g. `*@xcmailr.test`).
   - Mails are stored in the database and visible in the webmail dashboard.
2. **Outbound Mail Service (Port 1025 via Mailpit)**:
   - Jakarta Mail service dispatching user registration confirmation links, password resets, and forwarded mailbox emails.
   - When running locally under the `dev` profile, outbound emails route to **Mailpit** running on `localhost:1025`.

---

## 2. Quickstart: Starting the Local Environment

### Step 1: Start Mailpit (Mock SMTP Server)

Use the provided Docker Compose file:

```bash
docker compose -f docker-compose.dev.yml up -d
```

Or run via `docker run`:

```bash
docker run -d --name xcmailr-mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit:latest
```

- **SMTP Server**: `localhost:1025`
- **Web UI**: `http://localhost:8025`

### Step 2: Start XCMailr with the `dev` Profile

Run the web application with the `dev` profile active:

```bash
mvn spring-boot:run -pl xcmailr-webapp -Dspring-boot.run.profiles=dev
```

The `dev` profile (`application-dev.yml`) automatically configures:
- Outbound SMTP targeting `localhost:1025` without authentication or TLS.
- `require-confirmation: true` so registration emails are sent and can be tested.
- Detailed debug logging for controller actions and email dispatch.

---

## 3. End-to-End Testing Walkthrough

### 1. User Registration & Confirmation
1. Navigate to `http://localhost:8080/register`.
2. Register a new account (e.g., `hk1@varmail.de`).
3. Open the **Mailpit Web UI** at `http://localhost:8025`.
4. You will see the confirmation email from `admin@xcmailr.test`. Click the activation link inside the email to activate the account.
5. Log in to XCMailr at `http://localhost:8080/login`.

### 2. Create a Mailbox
1. In the dashboard (`http://localhost:8080/mailboxes`), click the **New Mailbox** button.
2. Enter a prefix (e.g., `testbox`) and select the domain `xcmailr.test`. Ensure **Forward incoming emails** is checked.
3. Click **Create Mailbox**. The modal closes smoothly and the mailbox appears in the list.

### 3. Send an Inbound Test Email
Send an email to XCMailr's inbound SMTP server on port `25000` using `curl`:

```bash
curl --url 'smtp://localhost:25000' \
  --mail-from 'sender@external.com' \
  --mail-rcpt 'testbox@xcmailr.test' \
  --upload-file - <<EOF
From: sender@external.com
To: testbox@xcmailr.test
Subject: Testing Local Round-Trip Delivery

This is a test message to verify inbound reception and outbound forwarding.
EOF
```

### 4. Verify Delivery
1. **Web Dashboard**: Refresh `http://localhost:8080/mailboxes`. The "Forwards" and "Mails" counters will increment. Click the mail count badge to read the stored email in the web console.
2. **Forwarded Email in Mailpit**: Switch to `http://localhost:8025`. You will see the forwarded email addressed to `hk1@varmail.de` with `Subject: [XCMailr] Testing Local Round-Trip Delivery`, complete with all original headers and forwarding metadata.

---

## 4. Production Configuration: Google Workspace SMTP Relay

In production environments, replace Mailpit with a secure external SMTP relay such as **Google Workspace SMTP Relay** or **Gmail App Passwords**:

### Production Configuration (`application.yml` / Environment Variables)

Configure the following environment variables on your production server:

```bash
# Google Workspace SMTP Relay
export SMTP_HOST="smtp-relay.gmail.com"
export SMTP_PORT="587"
export SMTP_USER="your-admin@yourdomain.com"
export SMTP_PASS="your-app-password"

# Set the administrative sender address (must match your authorized Google Workspace domain)
export ADMIN_ADDRESS="admin@yourdomain.com"
export APP_URL="https://xcmailr.yourdomain.com"
```

### Why is `admin.address` Used as the Sender for Forwarded Emails?
When forwarding emails, modern email receiving providers (Gmail, Microsoft 365, Yahoo) enforce strict **SPF (Sender Policy Framework)** and **DMARC** validation:
- If XCMailr preserved the original sender in the SMTP envelope `MAIL FROM: sender@external.com` while sending from your server's IP, the receiving server would reject the email as spoofed.
- XCMailr rewrites the envelope sender to `ADMIN_ADDRESS` (or adds an authenticated sender envelope) while preserving the original sender in the `Reply-To` and `From` message headers, ensuring 100% SPF alignment and reliable inbox delivery.
