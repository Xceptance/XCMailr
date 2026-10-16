# Cloud Test Deployment Guide

This guide provides step-by-step instructions for deploying XCMailr to an **ephemeral cloud virtual machine** (e.g. Google Cloud Platform, AWS, or DigitalOcean) to run high-throughput endurance and load tests with the **XLT (Xceptance LoadTest) suite**, as well as functional verification of outbound email forwarding with **Mailpit**.

---

## 1. Architecture Overview

Running heavy load tests on the same host running the application causes CPU and memory contention (local tests can saturate CPU at 98.5%+). To accurately benchmark XCMailr's throughput and virtual thread concurrency, the application is deployed to an isolated cloud VM, while the XLT load generator runs from your local or test runner machine.

```
 +-------------------------------------+
 | Local Machine / CI Runner           |
 |                                     |
 |   XLT Load Generator                |
 |   (Embedded Mode or Distributed)    |
 +------------------+------------------+
                    |
       REST Traffic | SMTP Traffic
       (Port 8080)  | (Port 25000 / 2525)
                    |
 +------------------V---------------------------------------------+
 | Ephemeral Cloud VM Host (e.g., GCP e2-standard-4)              |
 |                                                                |
 |   Docker Bridge Network (xcmailr-test-net)                     |
 |   +------------------------------------+                       |
 |   | XCMailr Container (:8080, :25000)  |                       |
 |   +------------------+-----------------+                       |
 |                      | internal relay                          |
 |                      | (mailpit:1025)                          |
 |                      v                                         |
 |   +------------------------------------+      Port 8025        |
 |   | Mailpit Mock Relay & UI            +-------------------+   |
 |   +------------------------------------+                   |   |
 +------------------------------------------------------------│---+
                                                              |
                                                    Browser Inspection
                                                  http://<VM_IP>:8025
```

---

## 2. Provisioning an Ephemeral Google Cloud VM

### 2.1 Recommended Hardware Specifications

- **Machine Type**: `e2-standard-4` (4 vCPUs, 16 GB RAM) or `c2-standard-4` for compute-optimized testing.
- **Operating System**: Debian 12 / Ubuntu 24.04 LTS.
- **Disk**: 30 GB standard persistent disk.

### 2.2 Create Instance via `gcloud` CLI

Run from your local workstation (with `gcloud` authenticated):

```bash
gcloud compute instances create xcmailr-test-vm \
  --zone=europe-west3-a \
  --machine-type=e2-standard-4 \
  --image-family=debian-12 \
  --image-project=debian-cloud \
  --boot-disk-size=30GB \
  --tags=xcmailr-test-node
```

### 2.3 Configure Cloud Firewall Rules

Most cloud providers block outbound and restrict inbound port 25. Open HTTP port `8080`, inbound SMTP port (`25000` or custom `2525`), and Mailpit web interface (`8025`):

```bash
gcloud compute firewall-rules create allow-xcmailr-test \
  --direction=INGRESS \
  --priority=1000 \
  --network=default \
  --action=ALLOW \
  --rules=tcp:8080,tcp:25000,tcp:2525,tcp:8025 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=xcmailr-test-node
```

> **Security Note:** In production or shared projects, restrict `--source-ranges` to your public IP (`curl -s ifconfig.me`).

Retrieve the external IP of your instance:
```bash
gcloud compute instances describe xcmailr-test-vm \
  --zone=europe-west3-a \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
```

---

## 3. Deploying XCMailr on the Cloud Host

SSH into the cloud VM:
```bash
gcloud compute ssh xcmailr-test-vm --zone=europe-west3-a
```

Ensure Docker is installed:
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
```

> [!TIP]
> **Container Registry Access (`ghcr.io`)**
> 
> * **Public Package (Recommended)**: Once an organization owner sets package visibility to **Public** in GitHub Packages, any cloud machine can pull the image without authentication.
> * **Private Package**: If the image is private, authenticate Docker on the VM once using a GitHub Personal Access Token (with `read:packages` permission):
>   ```bash
>   echo "$GITHUB_PAT" | docker login ghcr.io -u <YOUR_GITHUB_USERNAME> --password-stdin
>   ```

### Option A: Standalone Container via `docker run`

Run XCMailr directly using the official GHCR image. This option uses custom inbound SMTP port `2525` to avoid cloud port 25 restrictions:

```bash
docker run -d \
  --name xcmailr \
  --restart unless-stopped \
  -p 8080:8080 \
  -p 2525:2525 \
  -e PORT=8080 \
  -e MBOX_PORT=2525 \
  -e ADMIN_API_TOKEN=xcmailr-admin-api-token-2026 \
  -e APP_URL="http://$(curl -s ifconfig.me):8080" \
  -v xcmailr-data:/data \
  ghcr.io/xceptance/xcmailr:latest
```

### Option B: Full Test Environment with Docker Compose (`docker-compose.test.yml`)

The test compose configuration starts both **XCMailr** and **Mailpit** mock relay on an isolated bridge network:

```bash
# Clone the repository or copy docker-compose.test.yml
git clone https://github.com/Xceptance/XCMailr.git
cd XCMailr

# Start the stack with inbound SMTP on port 2525
MBOX_PORT=2525 docker compose -f docker-compose.test.yml up -d
```

Verify that both containers are running and healthy:
```bash
docker compose -f docker-compose.test.yml ps
```

---

## 4. Pre-Flight Verification & Mailbox Setup

### 4.1 Verify Web and SMTP Endpoints

From your local machine, verify that the application responds:
```bash
export VM_IP="<EXTERNAL_VM_IP>"

# Check web UI / health
curl -I http://${VM_IP}:8080/

# Check inbound SMTP banner (using netcat or telnet)
nc -zv ${VM_IP} 2525
```

### 4.2 Pre-create the Benchmark Mailbox (`jw@xcmailr.test`)

The high-volume `SendMail` scenario requires a pre-existing mailbox `jw@xcmailr.test` with no expiration (`deactivationTime: 0`). Initialize it via the REST API before starting XLT:

```bash
curl -X POST "http://${VM_IP}:8080/api/v1/mailboxes" \
  -H "Authorization: Bearer xcmailr-admin-api-token-2026" \
  -H "Content-Type: application/json" \
  -d '{
    "address": "jw@xcmailr.test",
    "deactivationTime": 0,
    "forwardEnabled": false
  }'
```

Expected response:
```json
{
  "address": "jw@xcmailr.test",
  "status": "ACTIVE"
}
```

---

## 5. Executing the XLT Load Test Suite

From your local repository root, update the target host in `xcmailr-load-test-suite/config/project.properties`:

```properties
## Target Cloud VM
xcmailr.host = <EXTERNAL_VM_IP>
xcmailr.baseUrl = http://${xcmailr.host}:8080
xcmailr.apiToken = xcmailr-admin-api-token-2026

## SMTP Configuration
smtp.host = ${xcmailr.host}
smtp.port = 2525
smtp.requireStartTls = true
smtp.debug = false
```

Run the complete test suite with 3, 5, or 10 concurrent users:
```bash
cd xcmailr-load-test-suite
./run-load-tests.sh 5
```

Open the generated HTML report after the test completes to inspect response times and transactions per second.

---

## 6. Inspecting Forwarded & Outbound Emails with Mailpit

### 6.1 Mailpit Web UI Access

When running with `docker-compose.test.yml`, Mailpit captures all outbound SMTP traffic sent by XCMailr (user registration verification links, password resets, and forwarded emails).

Open your browser and navigate to:
```text
http://<EXTERNAL_VM_IP>:8025
```

### 6.2 Load Test vs Functional Test Isolation

- **During Load Testing**: Mailboxes created by the test suite specify `"forwardEnabled": false`. XCMailr retains messages in the database and bypasses outbound delivery. Mailpit remains completely idle, consuming ~15 MB RAM, 0% CPU, and causing zero disk I/O.
- **During Functional / QA Testing**: Set `"forwardEnabled": true` to inspect the full mail forwarding flow.

### 6.3 Test Forwarding Flow

1. Create a mailbox with forwarding enabled:
   ```bash
   curl -X POST "http://${VM_IP}:8080/api/v1/mailboxes" \
     -H "Authorization: Bearer xcmailr-admin-api-token-2026" \
     -H "Content-Type: application/json" \
     -d '{
       "address": "forward-test@xcmailr.test",
       "deactivationTime": 60,
       "forwardEnabled": true
     }'
   ```

2. Send a test email to the mailbox via inbound SMTP (port 2525):
   ```bash
   swaks --to forward-test@xcmailr.test \
         --from tester@external.com \
         --server ${VM_IP} \
         --port 2525 \
         --body "Hello Mailpit Forwarding Test"
   ```

3. Open `http://${VM_IP}:8025` in your browser. XCMailr forwards messages to the registered address of the mailbox owner (`admin@xcmailr.test` when using the default admin API token). The forwarded message appears immediately in the Mailpit inbox addressed to `admin@xcmailr.test` with header `X-FORWARDED-FROM: tester@external.com`, full headers, body preview, and raw MIME view.

### 6.4 Connecting to a Real External Relay (Optional)

To route outbound emails through a real SMTP server instead of Mailpit:

```bash
docker run -d \
  --name xcmailr \
  -p 8080:8080 \
  -p 2525:2525 \
  -e PORT=8080 \
  -e MBOX_PORT=2525 \
  -e SMTP_HOST="smtp.mailgun.org" \
  -e SMTP_PORT=587 \
  -e SMTP_USER="postmaster@yourdomain.com" \
  -e SMTP_PASS="secretpassword" \
  -e SMTP_AUTH=true \
  -e SMTP_TLS=true \
  -e SMTP_STARTTLS=true \
  ghcr.io/xceptance/xcmailr:latest
```

---

## 7. Clean Teardown to Eliminate Cloud Costs

Because the container image is published and immutable, **there is no need to keep the cloud VM running when tests are finished**. Destroying the instance eliminates lingering cloud costs.

### 7.1 Stop and Remove Containers (if keeping VM)

```bash
docker compose -f docker-compose.test.yml down -v
```

### 7.2 Terminate the Ephemeral Cloud VM (Recommended)

From your local machine:

```bash
# Delete the VM instance
gcloud compute instances delete xcmailr-test-vm \
  --zone=europe-west3-a \
  --quiet

# (Optional) Delete the firewall rule if no longer needed
gcloud compute firewall-rules delete allow-xcmailr-test \
  --quiet
```

All compute, memory, and network billing stops immediately. When a new test run is needed in the future, running the provisioning command spins up a clean, ready-to-test VM in under 60 seconds.

