# XCMailr Load Test Suite

This module contains load, stress, and functional verification test scenarios for [XCMailr](https://github.com/Xceptance/XCMailr) based on the [XLT (Xceptance LoadTest)](https://www.xceptance.com/en/xlt/) framework and the Java [XCMailr Client](https://github.com/Xceptance/XCMailr/tree/master/xcmailr-client) library.

---

## Architecture & Overview

The test suite validates performance and functional correctness across both protocol entry points of XCMailr:
1. **REST API**: Mailbox provisioning, listing, mail retrieval, and cleanup via `XCMailrClient`.
2. **SMTP Protocol**: Inbound email delivery and throughput via JavaMail / Commons Email connected to XCMailr's embedded SubEtha SMTP service.

XLT measures client transaction response times and success/failure rates, outputting detailed reports.

---

## Available Test Scenarios

The suite provides three distinct test classes located in `src/test/java/xcmailr/loadtesting/tests/`:

| Test Class | Protocol / Mechanism | Description |
|---|---|---|
| `CreateAndDeleteMailbox` | HTTP / REST API | Creates a temporary randomized mailbox (`<random16>@<domain>`) via `XCMailrClient`, validates that it is active, and immediately deletes it. |
| `SendMail` | SMTP Sockets | Generates high-volume email delivery to configured mailboxes over SMTP (with optional STARTTLS encryption). |
| `SendAndCheckMail` | SMTP + HTTP / REST | Complete end-to-end loop: creates a mailbox via REST API, delivers a multipart email with random subject and body over SMTP, polls the REST API until the mail arrives, asserts subject and body contents, and cleans up the mailbox. |

---

## Configuration Properties

Default settings are specified in `config/project.properties`. You can override any property either directly in that file or on the command line using `-D<property>=<value>`.

| Property | Default Value | Description |
|---|---|---|
| `xcmailr.host` | `localhost` | Hostname of the target XCMailr server. |
| `xcmailr.baseUrl` | `http://${xcmailr.host}:8080` | Base URL of the XCMailr REST API. |
| `xcmailr.apiToken` | *(empty)* | **Required.** The API token associated with an active XCMailr user account. |
| `smtp.host` | `${xcmailr.host}` | Hostname or IP of the XCMailr inbound SMTP server. |
| `smtp.port` | `25000` | Port number of the XCMailr inbound SMTP server. |
| `smtp.requireStartTls` | `true` | Whether STARTTLS encryption is negotiated during SMTP sessions. |
| `smtp.debug` | `false` | Enables verbose JavaMail protocol debugging output in logs. |

---

## Manual Execution Guide

You can run individual test scenarios against a running XCMailr instance using Maven Surefire from the command line.

### Pre-requisites
1. **Running XCMailr Instance**: Ensure XCMailr is running (e.g. locally via `mvn spring-boot:run -pl xcmailr-webapp` or deployed remotely).
2. **User Account & API Token**:
   - Log into the XCMailr web console (default local URL: `http://localhost:8080`).
   - Navigate to your user profile and generate/copy your API token.
   - *Note*: The test suite utility class `xcmailr.loadtesting.util.Utils` initializes `XCMailrClient` during class loading; therefore, `-Dxcmailr.apiToken` must be specified for **all** test scenarios (even for `SendMail`, where any non-blank dummy token like `-Dxcmailr.apiToken=token` suffices if only testing SMTP socket delivery).

---

### Running Scenarios via Maven CLI

#### 1. Test Inbound SMTP Mail Submission (`SendMail`)
Sends an email over SMTP to `jw@xcmailr.test` with optional STARTTLS negotiation. This is a **pure SMTP throughput benchmark**:
```bash
mvn test -pl xcmailr-load-test-suite \
  -Dtest=SendMail \
  -Dxcmailr.host=localhost \
  -Dxcmailr.apiToken=dummy \
  -Dsmtp.port=25000 \
  -Dsmtp.requireStartTls=true
```

> **Architectural Note on `SendMail` vs. `SendAndCheckMail`:**
>
> - **Protocol Scope**: `SendMail` only exercises the inbound SMTP socket (port 25000). It does **not** make any HTTP REST API requests.
> - **API Token Parameter**: The shared test harness utility (`Utils`) statically initializes `XCMailrClient` when loaded, which requires `xcmailr.apiToken` to be non-blank. For `SendMail`, any non-blank value (such as `-Dxcmailr.apiToken=dummy`) succeeds because the SMTP server does not authenticate REST tokens.
> - **Mailbox Handling & Black-Holing**: In accordance with disposable email server design (to prevent backscatter spam and user enumeration), XCMailr accepts emails (`250 OK`) for any address belonging to a managed domain (e.g. `xcmailr.test`).
>   - If the recipient mailbox (`jw@xcmailr.test`) **does not exist**, XCMailr accepts the email, drops the content without persisting it to the database, and logs transaction status `100` ("Mailbox not found"). `SendMail` completes successfully because the SMTP delivery succeeded.
>   - If `jw@xcmailr.test` **was pre-created**, XCMailr persists the message body and metadata to the database.
> - If you want an end-to-end test that **authenticates via REST**, creates a mailbox, delivers an email, and **verifies that the message was stored in the database**, run `SendAndCheckMail` (Scenario 3 below).

#### 2. Test Mailbox Creation and Deletion (`CreateAndDeleteMailbox`)
Runs a single pass of `CreateAndDeleteMailbox` via the REST API (requires a valid user API token):
```bash
mvn test -pl xcmailr-load-test-suite \
  -Dtest=CreateAndDeleteMailbox \
  -Dxcmailr.baseUrl=http://localhost:8080 \
  -Dxcmailr.apiToken=<YOUR_VALID_API_TOKEN>
```

#### 3. Test Full SMTP + REST Verification Roundtrip (`SendAndCheckMail`)
Creates a temporary mailbox via the REST API, delivers an email over SMTP with STARTTLS, polls the REST API until the mail arrives, asserts subject and body contents, and cleans up the mailbox. This scenario verifies both protocol layers and requires a valid API token:
```bash
mvn test -pl xcmailr-load-test-suite \
  -Dtest=SendAndCheckMail \
  -Dxcmailr.baseUrl=http://localhost:8080 \
  -Dxcmailr.apiToken=<YOUR_VALID_API_TOKEN> \
  -Dxcmailr.host=localhost \
  -Dsmtp.port=25000 \
  -Dsmtp.requireStartTls=true
```

---

## Inspecting Results & Reports

When a test executes with XLT:
- Console summaries display elapsed runtime and transaction counts.
- Transaction data and logs are recorded in the `results/` directory:
  ```
  xcmailr-load-test-suite/results/<TestClassName>/0/output/<timestamp>/index.html
  ```
- Open `index.html` in any browser to inspect transaction request/response details, timing charts, and stack traces if an error occurs.
