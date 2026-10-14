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
   curl -L -O https://github.com/Xceptance/XLT/releases/download/v8.6.0/xlt-8.6.0.zip
   unzip xlt-8.6.0.zip
   ```
2. Ensure shell scripts have execute permissions:
   ```bash
   chmod +x ~/tools/xlt-8.6.0/bin/*.sh
   ```

### Step 2: Build the Load Test Suite

Build the test suite and copy runtime dependencies to `target/dependency` (required by XLT):

```bash
cd /Users/rbaumgarten/checkouts/XCMailr
mvn clean test-compile package -pl xcmailr-load-test-suite
```

Verify that `xcmailr-load-test-suite/target/test-classes` and `xcmailr-load-test-suite/target/dependency` are populated.

---

## 4. Running the Entire Suite at Different Concurrency Levels

To run the **entire suite**, pass all test cases in the `com.xceptance.xlt.loadtests` property:
```bash
-Dcom.xceptance.xlt.loadtests="SendMail SendAndCheckMail CreateAndDeleteMailbox"
```

All three scenarios will execute concurrently in parallel threads against the application.

---

### Scenario A: Total 3 Users across the Suite (1 User per Test Case - Quick Smoke Test)

Runs 1 concurrent thread for each test case simultaneously (3 users total) for 1 minute:

```bash
~/tools/xlt-8.6.0/bin/mastercontroller.sh -embedded -auto -report \
  -Dcom.xceptance.xlt.mastercontroller.testSuitePath=$(pwd)/xcmailr-load-test-suite \
  -Dcom.xceptance.xlt.loadtests="SendMail SendAndCheckMail CreateAndDeleteMailbox" \
  -Dcom.xceptance.xlt.loadtests.default.users=1 \
  -Dcom.xceptance.xlt.loadtests.default.warmUpPeriod=5s \
  -Dcom.xceptance.xlt.loadtests.default.measurementPeriod=1m \
  -Dxcmailr.apiToken=xcmailr-admin-api-token-2026
```

---

### Scenario B: Total 5 Users across the Suite (Balanced Baseline Validation)

Allocates 2 users to `SendAndCheckMail`, 2 users to `SendMail`, and 1 user to `CreateAndDeleteMailbox` (5 users total) for 2 minutes:

```bash
~/tools/xlt-8.6.0/bin/mastercontroller.sh -embedded -auto -report \
  -Dcom.xceptance.xlt.mastercontroller.testSuitePath=$(pwd)/xcmailr-load-test-suite \
  -Dcom.xceptance.xlt.loadtests="SendMail SendAndCheckMail CreateAndDeleteMailbox" \
  -Dcom.xceptance.xlt.loadtests.SendAndCheckMail.users=2 \
  -Dcom.xceptance.xlt.loadtests.SendMail.users=2 \
  -Dcom.xceptance.xlt.loadtests.CreateAndDeleteMailbox.users=1 \
  -Dcom.xceptance.xlt.loadtests.default.warmUpPeriod=10s \
  -Dcom.xceptance.xlt.loadtests.default.measurementPeriod=2m \
  -Dcom.xceptance.xlt.loadtests.default.shutdownPeriod=10s \
  -Dxcmailr.apiToken=xcmailr-admin-api-token-2026
```

---

### Scenario C: Total 10 Users across the Suite (Pre-Cloud Stress Test)

Allocates 4 users to `SendAndCheckMail`, 4 users to `SendMail`, and 2 users to `CreateAndDeleteMailbox` (10 users total) for 5 minutes:

```bash
~/tools/xlt-8.6.0/bin/mastercontroller.sh -embedded -auto -report \
  -Dcom.xceptance.xlt.mastercontroller.testSuitePath=$(pwd)/xcmailr-load-test-suite \
  -Dcom.xceptance.xlt.loadtests="SendMail SendAndCheckMail CreateAndDeleteMailbox" \
  -Dcom.xceptance.xlt.loadtests.SendAndCheckMail.users=4 \
  -Dcom.xceptance.xlt.loadtests.SendMail.users=4 \
  -Dcom.xceptance.xlt.loadtests.CreateAndDeleteMailbox.users=2 \
  -Dcom.xceptance.xlt.loadtests.default.warmUpPeriod=15s \
  -Dcom.xceptance.xlt.loadtests.default.measurementPeriod=5m \
  -Dcom.xceptance.xlt.loadtests.default.shutdownPeriod=15s \
  -Dxcmailr.apiToken=xcmailr-admin-api-token-2026
```

---

### Scenario D: Sequential Execution (Run Whole Suite One Case at a Time)

If you prefer testing each scenario in the suite individually one after another with e.g. 5 users each, replace `-auto` with `-sequential`:

```bash
~/tools/xlt-8.6.0/bin/mastercontroller.sh -embedded -sequential -report \
  -Dcom.xceptance.xlt.mastercontroller.testSuitePath=$(pwd)/xcmailr-load-test-suite \
  -Dcom.xceptance.xlt.loadtests="SendMail SendAndCheckMail CreateAndDeleteMailbox" \
  -Dcom.xceptance.xlt.loadtests.default.users=5 \
  -Dcom.xceptance.xlt.loadtests.default.warmUpPeriod=10s \
  -Dcom.xceptance.xlt.loadtests.default.measurementPeriod=2m \
  -Dxcmailr.apiToken=xcmailr-admin-api-token-2026
```

---

## 5. Command Options Reference

| Option / Property | Description |
| :--- | :--- |
| `-embedded` | Runs the test agent embedded in the same JVM. No remote agents required. |
| `-auto` | Non-interactive parallel mode: starts all scenarios simultaneously, collects results, and finishes. |
| `-sequential` | Non-interactive sequential mode: runs each scenario in the suite one after the other. |
| `-report` | Automatically generates the interactive HTML report immediately after completion. |
| `-Dcom.xceptance.xlt.mastercontroller.testSuitePath` | Absolute path to `xcmailr-load-test-suite`. |
| `-Dcom.xceptance.xlt.loadtests` | Test cases to include in the suite (space or comma-delimited). |
| `-Dcom.xceptance.xlt.loadtests.default.users` | Fallback user concurrency applied to each test case. |
| `-Dcom.xceptance.xlt.loadtests.<testCase>.users` | Granular user count allocated to a specific test case. |
| `-Dcom.xceptance.xlt.loadtests.default.warmUpPeriod` | Period to run traffic before measurements start (e.g. `10s`). |
| `-Dcom.xceptance.xlt.loadtests.default.measurementPeriod` | Duration of the active measurement window (e.g. `2m`, `5m`). |
| `-Dcom.xceptance.xlt.loadtests.default.shutdownPeriod` | Grace period for threads to complete their active iteration (e.g. `10s`). |
| `-Dxcmailr.apiToken` | Bearer API token used to authenticate against XCMailr REST endpoints. |
| `-Dxcmailr.baseUrl` | Base URL of the target app (defaults to `http://localhost:8080`). |

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
| `HTTP 401 Unauthorized` | Invalid or missing API token | Verify `-Dxcmailr.apiToken=xcmailr-admin-api-token-2026` is passed. Check token in `application.yml`. |
| `Connection refused: connect` on port 25000 | Inbound SMTP server not running | Verify `xcmailr-webapp` is started and `mbox.port: 25000` is open. |
| `Mailbox not found` / Transaction code 100 in `SendMail` | `jw@xcmailr.test` not created in DB | Pre-create `jw@xcmailr.test` via curl (see Prerequisites section). |
| `FileNotFoundException: mastercontroller.properties` | Wrong working directory or invalid `XLT_HOME` | Use absolute paths when invoking `~/tools/xlt-8.6.0/bin/mastercontroller.sh`. |
