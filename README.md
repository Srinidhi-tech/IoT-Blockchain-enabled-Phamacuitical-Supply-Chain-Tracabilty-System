# PharmaTrace Enterprise Platform

> **Production-Ready Spring Boot + Thymeleaf Pharmaceutical Cold-Chain Telemetry, Multi-Hub RFID Custody Validation & Blockchain Notarization Platform**

PharmaTrace provides end-to-end hardware-to-software telemetry ingestion, multi-hub RFID custody validation, H2 database persistence, and an enterprise corporate light theme dashboard.

---

## 🏛️ Architecture Overview

```
                      +---------------------------------------+
                      |  IoT Hardware (ESP32 / Arduino / RPi) |
                      |    - RFID Reader (RC522 / PN532)      |
                      |    - DHT22 Temperature & Humidity     |
                      |    - Actuators: Active Buzzer & LED   |
                      +-------------------+-------------------+
                                          |
                                          | HTTP POST /api/telemetry
                                          v
+-----------------------------------------------------------------------------------+
|                        PharmaTrace Spring Boot Platform                           |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  | TelemetryRestController: Ingestion endpoint & Actuator Command Dispatcher  |  |
|  +-----------------------------------------------------------------------------+  |
|                                          |                                        |
|         +--------------------------------+-------------------------------+        |
|         |                                |                               |        |
|         v                                v                               v        |
|  +---------------+             +-------------------+           +---------------+  |
|  |  RFID Tag     |             | WHO Cold-Chain    |           | Web3j Engine  |  |
|  |  Validation   |             | Threshold Check   |           | Keccak-256    |  |
|  |  & Custody    |             | (2.0°C - 8.0°C)   |           | EVM Standby   |  |
|  +---------------+             +-------------------+           +---------------+  |
|         |                                |                               |        |
|         +--------------------------------+-------------------------------+        |
|                                          |                                        |
|                                          v                                        |
|  +-----------------------------------------------------------------------------+  |
|  | Persistent H2 File Database (jdbc:h2:file:./data/coldchaindb)               |  |
|  |   - TelemetryLog (rfidUid, batchId, temp, humidity, status, txHash, etc.)   |  |
|  |   - ProductBatch (batchId, boundRfidUid, currentHub, custodyStep, tampered) |  |
|  +-----------------------------------------------------------------------------+  |
|                                          |                                        |
|                                          v                                        |
|  +-----------------------------------------------------------------------------+  |
|  | Real-Time Corporate Light Dashboard (Thymeleaf, Slate Theme, Live Polling)  |  |
|  +-----------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## 🚀 Key Functional Capabilities

### 1. Data Models & H2 Database Persistence
- **H2 File Location**: `jdbc:h2:file:./data/coldchaindb`
- **H2 Console**: Accessible at `http://localhost:8080/h2-console`
- **TelemetryLog Entity**:
  - `rfidUid`: Scanned RFID Tag UID (String)
  - `batchId`: Pharmaceutical Batch ID (String)
  - `temperature`: Temperature in °C (Double)
  - `humidity`: Relative Humidity in % (Double)
  - `locationName`: Current Hub ("Manufacturer Hub", "Distributor Hub (College)", "Consumer Hospital")
  - `latitude` & `longitude`: Geolocation coordinates
  - `verificationStatus`: `AUTHENTIC_VERIFIED`, `UNAUTHORIZED_TAG_BREACH`, `THERMAL_BREACH`
  - `txHash`: Web3j Keccak-256 64-hex deterministic cryptographic seal (`0x...`)
  - `timestamp`: Event recorded timestamp (LocalDateTime)
  - `buzzerActive` & `ledColor`: Actuator state record

### 2. RFID Tag Binding & Multi-Hub Custody Validation
- **Authorized Tag Binding**: Configured via `pharmatrace.rfid.authorized-uids`. Specifically registered physical tags:
  - **Tag 1 (Authorized Primary)**: `306F4D53`
  - **Tag 2 (Authorized / Team Member)**: `21801466`
  - Legacy test tags: `TAG-A-04E19C82`, `04:A2:8B:E1:9C:20:80`, `CARD-A-01`
- **Multi-Hub Custody Sequence**:
  1. `Manufacturer Hub` (Origin Pharma Plant)
  2. `Distributor Hub (College)` (Regional Cold-Storage & College Logistics)
  3. `Consumer Hospital` (Destination Clinical Dispensary)
- **Actuator Directives Returned to Hardware**:
  - **Authorized Scan + Safe Temperature (2.0°C – 8.0°C)**:
    - Status: `AUTHENTIC_VERIFIED`
    - `"buzzer": false`, `"led": "GREEN"`
    - Custody state advances smoothly across the 3 hubs.
  - **Unauthorized Scan (Card B / Unknown UID)**:
    - Status: `UNAUTHORIZED_TAG_BREACH`
    - `"buzzer": true`, `"led": "RED"` (Instructs hardware to sound active alarm buzzer and illuminate red LED)
    - Batch marked `tampered = true`, alert banner flashes red on dashboard.
  - **Authorized Scan + Temperature Excursion (<2.0°C or >8.0°C)**:
    - Status: `THERMAL_BREACH`
    - `"buzzer": true`, `"led": "AMBER"` (Warning alert for temperature excursion)

### 3. Blockchain Notarization & Automatic EVM Standby Fallback
- **Web3j Keccak-256 Cryptographic Hash**: Every telemetry event is cryptographically sealed into `txHash` by computing the Keccak-256 digest over the canonical payload:
  `${batchId}|${rfidUid}|${temperature}|${humidity}|${locationName}|${status}|${timestamp}`
- **Automatic EVM Standby Fallback**:
  - At startup and runtime, the system probes the configured EVM RPC URL (`http://127.0.0.1:8545`).
  - If unreachable or no active blockchain subscription exists, the service seamlessly operates in **EVM Standby Mode**.
  - All hashes are deterministically generated and notarized offline with zero downtime and zero RPC costs.

### 4. Modern Corporate Light Theme Dashboard (`/`)
- Enterprise palette: Slate background (`#f8fafc`), white card containers (`#ffffff`), subtle slate borders (`#e2e8f0`).
- **Real-Time Visual Cards**:
  - **Active Product Batch & Bound RFID UID**: Product details, bound tag, copy buttons.
  - **Live DHT22 Telemetry**: Temperature readout with dynamic safe-zone gauge (2.0°C – 8.0°C WHO bracket) and relative humidity.
  - **3-Hub Transit Stepper**: Visual progress tracker showing completed, active, and upcoming hubs.
  - **RFID Verification Banner**: Real-time verdict (Green authentic, flashing Red security tamper breach, Amber thermal alert).
  - **Blockchain Ledger**: Latest Keccak-256 transaction hash with quick copy.
  - **Hardware Simulation & Test Rig**: Quick-action simulation buttons to test Hub 1, Hub 2, Hub 3, Card B tamper breaches, and thermal excursions directly from the browser.
  - **Immutable Audit Trail Table**: Complete chronological event log with timestamps, hub locations, verdict badges, actuator states, and transaction hashes.

---

## 🛠️ REST API Specification

### 1. Hardware Telemetry Ingestion
- **Endpoint**: `POST /api/telemetry`
- **Request Body**:
  ```json
  {
    "rfidUid": "306F4D53",
    "batchId": "VAX-B4920-COVID19",
    "temperature": 4.5,
    "humidity": 45.2,
    "locationName": "Distributor Hub (College)"
  }
  ```
- **Response**:
  ```json
  {
    "success": true,
    "buzzer": false,
    "led": "GREEN",
    "verificationStatus": "AUTHENTIC_VERIFIED",
    "txHash": "0x586c4bb5758485d711e22725b0f663880b39feaadecf6b5f2a19bf7d44a1c0be",
    "batchId": "VAX-B4920-COVID19",
    "locationName": "Distributor Hub (College)",
    "message": "AUTHENTIC RFID VERIFIED: Tag 1 (Authorized Primary) [306F4D53] custody validated at Distributor Hub (College). Cold-chain safe at 4.5°C.",
    "timestamp": "2026-11-02T17:20:29.443526"
  }
  ```

### 2. Retrieve Latest Telemetry
- **Endpoint**: `GET /api/telemetry/latest`

### 3. Retrieve Telemetry History
- **Endpoint**: `GET /api/telemetry/history`

### 4. System Telemetry & EVM Stats
- **Endpoint**: `GET /api/telemetry/stats`

### 5. Active Batch Custody
- **Endpoint**: `GET /api/batch/active`

### 6. Reset Batch Custody (For Demo Runs)
- **Endpoint**: `POST /api/batch/reset?batchId=VAX-B4920-COVID19`

---

## 🏃 Running the Application

### Prerequisites:
- Java 17+ (Eclipse Temurin 17 installed)
- Maven 3.9+ (or `./mvnw` included in the root directory)

### Start Command:
```bash
./mvnw spring-boot:run
```

Once started, open your browser at:
- **Dashboard**: `http://localhost:8080/`
- **H2 Web Console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/coldchaindb`, User: `sa`, Password: blank)
