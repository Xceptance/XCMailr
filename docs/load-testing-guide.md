# Load Testing Guide

This guide describes how to run the **complete XCMailr load test suite** using **XLT (Xceptance LoadTest) in Embedded Mode** with 3, 5, or 10 concurrent users to validate application performance, concurrency, and stability before cloud deployment.

---

## 1. Architecture Overview

In **Embedded Mode**, XLT runs both the test coordinator (Master Controller) and the test generator (Agent Controller) inside a single JVM on your local machine. No remote agent daemons or cloud instances are required.

```
+------------------------------------------------------------------------------------+
| Local Machine (Single JVM)                                                         |
|                                                                                    |
|  [ mastercontroller.sh -embedded -auto -report ]                                   |
|       │                                                                            |
|       └──► Spawns In-Process Embedded AgentController                              |
|                 │                                                                  |
|                 └──► Spawns Concurrent User Threads Across All Test Scenarios:     |
|                           ├── Users running: SendMail                              |
|                           ├── Users running: SendAndCheckMail                      |
|                           └── Users running: CreateAndDeleteMailbox                |
|                                     │                                              |
+-------------------------------------│----------------------------------------------+
                                      │ Combined Traffic Generation
                                      ▼
+------------------------------------------------------------------------------------+
| XCMailr Web Application                                                            |
|                                                                                    |
|   1. REST API (Port 8080)     : Dynamic mailbox creation & deletion                |
|   2. Inbound SMTP (Port 25000): Email delivery via STARTTLS                       |
|   3. REST API (Port 8080)     : Polling & message validation                       |
+------------------------------------------------------------------------------------+
                                      │
                                      ▼ Results
+------------------------------------------------------------------------------------+
| Interactive HTML Performance Report (<XLT_HOME>/reports/<timestamp>/index.html)     |
+------------------------------------------------------------------------------------+
```

### The Test Scenarios in the Suite

The suite located in `xcmailr-load-test-suite` contains three distinct scenarios that execute together:

| Test Scenario | Protocol & Target | Purpose / Workload |
| :--- | :--- | :--- |
| **`SendAndCheckMail`** | REST (8080) + SMTP (25000) | **End-to-End**: Creates mailbox via REST, delivers mail via SMTP with STARTTLS, polls and validates payload via REST, deletes mailbox. |
| **`SendMail`** | Inbound SMTP (25000) | **High-Throughput Ingestion**: Direct SMTP flood to `jw@xcmailr.test`, testing socket listeners, TLS negotiation, and delivery pipeline. |
| **`CreateAndDeleteMailbox`**| REST API (8080) | **API Throughput**: Repeatedly creates and deletes mailboxes, exercising token authentication and database CRUD operations. |

---

## 2. Prerequisites

1. **Java Development Kit (JDK 17 or 21)** installed and available on `PATH`.
2. **Maven 3.8+** installed.
3. **Running XCMailr Application**:
   Start the application locally:
   ```bash
   mvn spring-boot:run -pl xcmailr-webapp
   ```
   * HTTP / REST API: `http://localhost:8080`
   * Inbound SMTP: `localhost:25000` (STARTTLS enabled)
   * Default Admin API Token: `xcmailr-admin-api-token-2026` (bootstrapped automatically from `application.yml`)
4. **Pre-Create the `jw@xcmailr.test` Mailbox (Optional but Recommended)**:
   The `SendMail` scenario targets `jw@xcmailr.test`. To ensure emails sent by `SendMail` exercise full MIME parsing and database persistence (rather than being dropped with transaction code 100 "Mailbox not found"), create it once before testing:
   ```bash
   curl -X POST http://localhost:8080/api/v1/mailboxes \
     -H "Authorization: Bearer xcmailr-admin-api-token-2026" \
     -H "Content-Type: application/json" \
     -d '{"address":"jw@xcmailr.test"}'
   ```

---

## 3. Step-by-Step Setup

### Step 1: Download and Extract XLT

The test suite is compiled against XLT version **8.6.0** (defined in `xcmailr-load-test-suite/pom.xml`).

1. Download the official XLT archive:
   ```bash
   mkdir -p ~/tools
   cd ~/tools
   curl -O https://lab.xceptance.de/releases/xlt/8.6.0/xlt-8.6.0.zip
   unzip xlt-8.6.0.zip
   ```
2. Ensure shell scripts have execute permissions:
   ```bash
   chmod +x ~/tools/xlt-8.6.0/bin/*.sh
   ```

### Step 2: Build the Load Test Suite and Verify Configuration

1. **Verify Global Properties**:
   The suite's target URL and admin API token are configured in `xcmailr-load-test-suite/config/project.properties`:
   ```properties
   xcmailr.host = localhost
   xcmailr.baseUrl = http://${xcmailr.host}:8080
   xcmailr.apiToken = xcmailr-admin-api-token-2026
   ```
   Ensure `xcmailr.apiToken` matches the admin token configured in `application.yml` (default: `xcmailr-admin-api-token-2026`).

2. **Build the Test Suite and Dependencies**:
   Build the test suite and copy runtime dependencies to `target/dependency` and agent classpath overrides to `patches/lib` (required by XLT):
   ```bash
   cd /Users/rbaumgarten/checkouts/XCMailr
   mvn clean test-compile package -pl xcmailr-load-test-suite
   ```
   Verify that `xcmailr-load-test-suite/target/test-classes`, `xcmailr-load-test-suite/target/dependency`, and `xcmailr-load-test-suite/patches/lib` are populated.

---

## 4. Running the Entire Suite at Different Concurrency Levels

In XLT, test-run configurations and user allocations are managed through dedicated **profile properties files** in `xcmailr-load-test-suite/config/`. Select a profile using MasterController's `-testPropertiesFile <filename>` option:

| Profile File | Total Users | Measurement Period | Workload Allocation |
| :--- | :--- | :--- | :--- |
| **`test.properties`** / **`test-3users.properties`** | **3 Users** | 1 minute | 1 SendAndCheckMail, 1 SendMail, 1 CreateAndDeleteMailbox |
| **`test-5users.properties`** | **5 Users** | 2 minutes | 2 SendAndCheckMail, 2 SendMail, 1 CreateAndDeleteMailbox |
| **`test-10users.properties`** | **10 Users** | 5 minutes | 4 SendAndCheckMail, 4 SendMail, 2 CreateAndDeleteMailbox |

All three scenarios (`SendMail`, `SendAndCheckMail`, `CreateAndDeleteMailbox`) execute concurrently in parallel threads against the application.

---

### Scenario A: Total 3 Users across the Suite (1 User per Test Case - Quick Smoke Test)

Runs 1 concurrent thread for each test case simultaneously (3 users total) for 1 minute:

```bash
~/tools/xlt-8.6.0/bin/mastercontroller.sh -embedded -auto -report \
  -Dcom.xceptance.xlt.mastercontroller.testSuitePath=$(pwd)/xcmailr-load-test-suite
```
*(By default, MasterController loads `config/test.properties`. You can also explicitly specify `-testPropertiesFile test-3users.properties`.)*

---

### Scenario B: Total 5 Users across the Suite (Balanced Baseline Validation)

Allocates 2 users to `SendAndCheckMail`, 2 users to `SendMail`, and 1 user to `CreateAndDeleteMailbox` (5 users total) for 2 minutes:

```bash
~/tools/xlt-8.6.0/bin/mastercontroller.sh -embedded -auto -report \
  -Dcom.xceptance.xlt.mastercontroller.testSuitePath=$(pwd)/xcmailr-load-test-suite \
  -testPropertiesFile test-5users.properties
```

---

### Scenario C: Total 10 Users across the Suite (Pre-Cloud Stress Test)

Allocates 4 users to `SendAndCheckMail`, 4 users to `SendMail`, and 2 users to `CreateAndDeleteMailbox` (10 users total) for 5 minutes:

```bash
~/tools/xlt-8.6.0/bin/mastercontroller.sh -embedded -auto -report \
  -Dcom.xceptance.xlt.mastercontroller.testSuitePath=$(pwd)/xcmailr-load-test-suite \
  -testPropertiesFile test-10users.properties
```

---

### Scenario D: Sequential Execution (Run Whole Suite One Case at a Time)

If you prefer testing each scenario in the suite individually one after another with e.g. 5 users each, replace `-auto` with `-sequential`:

```bash
~/tools/xlt-8.6.0/bin/mastercontroller.sh -embedded -sequential -report \
  -Dcom.xceptance.xlt.mastercontroller.testSuitePath=$(pwd)/xcmailr-load-test-suite \
  -testPropertiesFile test-5users.properties
```

---

## 5. Command Options Reference

| Option / Parameter | Description |
| :--- | :--- |
| `-embedded` | Runs the test agent embedded in the same JVM environment. No remote agent daemons required. |
| `-auto` | Non-interactive parallel mode: starts all scenarios simultaneously, collects results, and finishes. |
| `-sequential` | Non-interactive sequential mode: runs each scenario in the active profile one after the other. |
| `-report` | Automatically generates the interactive HTML report immediately after test completion. |
| `-testPropertiesFile <file>` | Loads a specific test profile from `xcmailr-load-test-suite/config/` (defaults to `test.properties`). |
| `-Dcom.xceptance.xlt.mastercontroller.testSuitePath=<path>` | Absolute path to the test suite directory. |

---

## 6. Evaluating Reports and Metrics

Once the run completes, XLT writes the HTML report to:

```text
~/tools/xlt-8.6.0/reports/<timestamp>/index.html
```

Open the report in your default browser:

```bash
open ~/tools/xlt-8.6.0/reports/*/index.html
```

### Key Metrics to Check Across the Suite

1. **Scenario Overview**:
   * The summary page lists separate sections for `SendMail`, `SendAndCheckMail`, and `CreateAndDeleteMailbox`.
2. **Action Timers (Transactions)**:
   * **`CreateMailbox`**: REST response time under concurrent database inserts (median should be < 50ms).
   * **`SendMail`**: SMTP delivery time including STARTTLS handshake (typically < 100ms locally).
   * **`CheckMail`**: Time spent polling for message delivery and verifying payload (typically < 1.5s).
   * **`DeleteMailbox`**: REST cleanup response time under concurrent deletes (< 50ms).
3. **Error Rates**:
   * Must be **0.0%** across all test scenarios.
   * Any non-zero error rate highlights HTTP 5xx responses, connection timeouts, or verification mismatches.
4. **Combined Request & Network Throughput**:
   * Charts total actions/second and network bandwidth across all concurrent scenarios.

---

## 7. Application Health Monitoring Under Load

While the load test executes, check the running XCMailr application:

1. **Application Logs**:
   ```bash
   tail -f log/xcmailr.log
   ```
   * Confirm there are no database connection pool exhaustion errors (`HikariPool - Connection is not available`).
   * Verify that log rotation occurs cleanly if log volume exceeds 10MB without filling disk space.
2. **Database Concurrency**:
   * H2 runs in `AUTO_SERVER=TRUE` mode. Verify no table lock contention occurs during simultaneous mailbox creation, message inserts, and cleanups.
3. **Inbound SMTP Listener**:
   * SubEthaSMTP accepts incoming connections asynchronously. Ensure connections are negotiated and closed cleanly without socket leaks.

---

## 8. Troubleshooting

| Issue | Cause | Resolution |
| :--- | :--- | :--- |
| `IllegalArgumentException: Parameter 'apiToken' must neither be null nor empty nor blank` | Missing API token in test configuration | Set `xcmailr.apiToken = xcmailr-admin-api-token-2026` in `xcmailr-load-test-suite/config/project.properties`. |
| `IllegalAccessError: failed to access class LogSupport from class MailcapCommandMap` | Classpath conflict between XLT 8.6.0's `javax.activation-api` and package-private `angus-activation` | Rebuild the suite with `mvn clean package -pl xcmailr-load-test-suite` so `jakarta.activation` is copied to `patches/lib/`. |
| `Agent exited unexpectedly (exit code: 3)` or unexpected user count / duration | CLI `-D` flags ignored by MasterController | MasterController only reads `-D` for `mastercontroller.properties`. Configure profiles via `config/*.properties` and specify `-testPropertiesFile <file>`. |
| `HTTP 401 Unauthorized` | API token mismatch | Verify `xcmailr.apiToken` in `project.properties` matches `xcmailr.admin.api-token` in `application.yml`. |
| `Connection refused: connect` on port 25000 | Inbound SMTP server not running | Verify `xcmailr-webapp` is started and `mbox.port: 25000` is open. |
| `Mailbox not found` / Transaction code 100 in `SendMail` | `jw@xcmailr.test` not created in DB | Pre-create `jw@xcmailr.test` via curl (see Prerequisites section). |
| `FileNotFoundException: mastercontroller.properties` | Wrong working directory or invalid `XLT_HOME` | Use absolute paths when invoking `~/tools/xlt-8.6.0/bin/mastercontroller.sh`. |
