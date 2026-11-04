/**
 * PharmaTrace Enterprise Dashboard JavaScript
 * Handles real-time polling, IoT simulation rig, copy actions, and UI updates.
 */

const HUBS = [
    "Manufacturer Hub",
    "Distributor Hub (College)",
    "Consumer Hospital"
];

let pollingInterval = null;
let isPollingPaused = false;
const POLLING_INTERVAL_MS = 3000;

document.addEventListener("DOMContentLoaded", () => {
    initClock();
    startPolling();
    setupEventHandlers();
});

function initClock() {
    const clockEl = document.getElementById("live-clock");
    const updateTime = () => {
        const now = new Date();
        if (clockEl) {
            clockEl.textContent = now.toLocaleTimeString();
        }
    };
    updateTime();
    setInterval(updateTime, 1000);
}

function startPolling() {
    if (pollingInterval) clearInterval(pollingInterval);
    pollingInterval = setInterval(() => {
        if (!isPollingPaused) {
            fetchDashboardUpdates();
        }
    }, POLLING_INTERVAL_MS);
}

function togglePolling() {
    isPollingPaused = !isPollingPaused;
    const btn = document.getElementById("toggle-polling-btn");
    const indicator = document.getElementById("polling-indicator");

    if (isPollingPaused) {
        if (btn) btn.textContent = "Resume Stream";
        if (indicator) {
            indicator.className = "pulse-dot";
            indicator.style.backgroundColor = "#94a3b8";
            indicator.style.animation = "none";
        }
    } else {
        if (btn) btn.textContent = "Pause Stream";
        if (indicator) {
            indicator.className = "pulse-dot";
            indicator.style.backgroundColor = "";
            indicator.style.animation = "";
        }
        fetchDashboardUpdates();
    }
}

async function fetchDashboardUpdates() {
    try {
        const [batchRes, latestRes, historyRes, statsRes] = await Promise.all([
            fetch("/api/batch/active"),
            fetch("/api/telemetry/latest"),
            fetch("/api/telemetry/history"),
            fetch("/api/telemetry/stats")
        ]);

        if (batchRes.ok) {
            const batch = await batchRes.json();
            updateBatchUI(batch);
        }

        if (latestRes.status === 200) {
            const latest = await latestRes.json();
            updateLatestTelemetryUI(latest);
        }

        if (historyRes.ok) {
            const history = await historyRes.json();
            updateAuditTableUI(history);
        }

        if (statsRes.ok) {
            const stats = await statsRes.json();
            updateStatsUI(stats);
        }
    } catch (err) {
        console.error("Error refreshing dashboard telemetry:", err);
    }
}

function updateBatchUI(batch) {
    if (!batch) return;

    const batchIdEl = document.getElementById("batch-id-val");
    if (batchIdEl) batchIdEl.textContent = batch.batchId || "—";

    const prodNameEl = document.getElementById("product-name-val");
    if (prodNameEl) prodNameEl.textContent = batch.productName || "Pharmaceutical Product";

    const boundRfidEl = document.getElementById("bound-rfid-val");
    if (boundRfidEl) boundRfidEl.textContent = batch.boundRfidUid || "—";

    const currentHubEl = document.getElementById("current-hub-val");
    if (currentHubEl) currentHubEl.textContent = batch.currentHub || "Manufacturer Hub";

    // Update Transit Stepper Progress
    const currentStep = batch.custodyStep || 1;
    for (let i = 1; i <= 3; i++) {
        const stepEl = document.getElementById(`stepper-step-${i}`);
        if (!stepEl) continue;

        stepEl.classList.remove("completed", "active");
        if (i < currentStep) {
            stepEl.classList.add("completed");
            stepEl.querySelector(".step-marker").innerHTML = "&#10003;";
        } else if (i === currentStep) {
            stepEl.classList.add("active");
            stepEl.querySelector(".step-marker").textContent = i;
        } else {
            stepEl.querySelector(".step-marker").textContent = i;
        }
    }
}

function updateLatestTelemetryUI(log) {
    if (!log) return;

    // 1. Update Real-Time RFID Verification Alert Banner
    const banner = document.getElementById("verification-banner");
    const bannerIcon = document.getElementById("banner-icon");
    const bannerTitle = document.getElementById("banner-title");
    const bannerDesc = document.getElementById("banner-desc");
    const buzzerPill = document.getElementById("buzzer-pill");
    const ledPill = document.getElementById("led-pill");

    if (banner) {
        banner.className = "verification-banner";
        if (log.verificationStatus === "UNAUTHORIZED_TAG_BREACH") {
            banner.classList.add("tamper-breach");
            if (bannerIcon) bannerIcon.textContent = "🚨";
            if (bannerTitle) bannerTitle.textContent = "CRITICAL SECURITY BREACH: UNAUTHORIZED RFID TAG DETECTED";
            if (bannerDesc) bannerDesc.textContent = `Tamper event logged! Tag [${log.rfidUid}] rejected. Active Hardware Buzzer [ENGAGED], LED [RED].`;
        } else if (log.verificationStatus === "THERMAL_BREACH") {
            banner.classList.add("thermal-breach");
            if (bannerIcon) bannerIcon.textContent = "⚠️";
            if (bannerTitle) bannerTitle.textContent = "COLD-CHAIN INTEGRITY WARNING: TEMPERATURE EXCURSION";
            if (bannerDesc) bannerDesc.textContent = `Excursion recorded: ${log.temperature.toFixed(1)}°C breaches safe cold-chain limits (2.0°C - 8.0°C).`;
        } else {
            banner.classList.add("authentic");
            if (bannerIcon) bannerIcon.textContent = "🛡️";
            if (bannerTitle) bannerTitle.textContent = "AUTHENTIC RFID VERIFIED — CUSTODY CHAIN VALID";
            if (bannerDesc) bannerDesc.textContent = `Authorized Tag [${log.rfidUid}] verified authentic at ${log.locationName}. Temperature & humidity safe.`;
        }
    }

    // Actuators
    if (buzzerPill) {
        if (log.buzzerActive) {
            buzzerPill.className = "actuator-pill buzzer-active";
            buzzerPill.innerHTML = "<span>🔊</span> Buzzer: ACTIVE ALARM";
        } else {
            buzzerPill.className = "actuator-pill";
            buzzerPill.innerHTML = "<span>🔇</span> Buzzer: SILENT";
        }
    }

    if (ledPill) {
        ledPill.className = "actuator-pill";
        const color = (log.ledColor || "GREEN").toUpperCase();
        if (color === "RED") {
            ledPill.classList.add("led-red");
            ledPill.innerHTML = "<span>🔴</span> LED: RED";
        } else if (color === "AMBER") {
            ledPill.classList.add("led-amber");
            ledPill.innerHTML = "<span>🟡</span> LED: AMBER";
        } else {
            ledPill.classList.add("led-green");
            ledPill.innerHTML = "<span>🟢</span> LED: GREEN";
        }
    }

    // 2. Temperature & Humidity Readout
    const tempEl = document.getElementById("temp-readout-val");
    if (tempEl) {
        tempEl.textContent = log.temperature.toFixed(1);
        if (log.temperature < 2.0 || log.temperature > 8.0) {
            tempEl.className = "temp-value breach";
        } else {
            tempEl.className = "temp-value";
        }
    }

    const humidityEl = document.getElementById("humidity-readout-val");
    if (humidityEl) {
        humidityEl.textContent = `${log.humidity.toFixed(1)}%`;
    }

    // Gauge Needle Position (Map 0°C to 12°C onto 0% to 100%)
    const needleEl = document.getElementById("gauge-needle");
    if (needleEl) {
        const clampedTemp = Math.max(0, Math.min(12, log.temperature));
        const percent = ((clampedTemp - 0) / (12 - 0)) * 100;
        needleEl.style.left = `calc(${percent}% - 2px)`;
    }

    // 3. Blockchain Keccak-256 Hash
    const hashEl = document.getElementById("latest-txhash-val");
    if (hashEl && log.txHash) {
        hashEl.textContent = log.txHash;
    }
}

function updateAuditTableUI(logs) {
    const tbody = document.getElementById("audit-table-body");
    if (!tbody || !Array.isArray(logs)) return;

    if (logs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: #94a3b8; padding: 2rem;">No telemetry ingestion logs recorded yet. Use the simulation buttons above to inject events.</td></tr>`;
        return;
    }

    let rowsHtml = "";
    logs.forEach(item => {
        let statusBadge = "";
        if (item.verificationStatus === "AUTHENTIC_VERIFIED") {
            statusBadge = `<span class="badge badge-success">&#10003; Authentic Verified</span>`;
        } else if (item.verificationStatus === "UNAUTHORIZED_TAG_BREACH") {
            statusBadge = `<span class="badge badge-danger">&#9888; Tag Breach (Tamper)</span>`;
        } else {
            statusBadge = `<span class="badge badge-warning">&#9888; Thermal Excursion</span>`;
        }

        let actuatorBadge = "";
        if (item.buzzerActive) {
            actuatorBadge = `<span class="badge badge-danger">Buzzer ON | ${item.ledColor || 'RED'}</span>`;
        } else {
            actuatorBadge = `<span class="badge badge-success">Buzzer OFF | ${item.ledColor || 'GREEN'}</span>`;
        }

        const formattedTime = item.timestamp ? item.timestamp.replace("T", " ").substring(0, 19) : "—";
        const truncatedHash = item.txHash ? `${item.txHash.substring(0, 10)}...${item.txHash.substring(item.txHash.length - 8)}` : "—";
        
        let tagPill = "";
        const cleanUid = (item.rfidUid || "").replace(/[:\s\-_]/g, "").toUpperCase();
        if (cleanUid === "2AF52B07") {
            tagPill = ` <span class="badge badge-success" style="font-size: 0.65rem;">Tag 1 (Auth)</span>`;
        } else if (cleanUid === "659A1707") {
            tagPill = ` <span class="badge badge-success" style="font-size: 0.65rem;">Tag 2 (Auth)</span>`;
        }

        rowsHtml += `
            <tr>
                <td style="font-weight: 500; font-size: 0.78rem;">${formattedTime}</td>
                <td><span style="font-weight: 600;">${escapeHtml(item.locationName || '—')}</span></td>
                <td class="mono-cell">${escapeHtml(item.batchId || '—')}</td>
                <td class="mono-cell" style="font-weight: 600;">${escapeHtml(item.rfidUid || '—')}${tagPill}</td>
                <td>
                    <span style="font-weight: 700; ${item.temperature < 2.0 || item.temperature > 8.0 ? 'color: #dc2626;' : 'color: #166534;'}">
                        ${item.temperature.toFixed(1)}°C
                    </span>
                    <span style="color: #64748b; font-size: 0.75rem; margin-left: 0.25rem;">(${item.humidity.toFixed(1)}% RH)</span>
                </td>
                <td>${statusBadge}</td>
                <td>${actuatorBadge}</td>
                <td class="mono-cell" style="color: #0284c7;" title="${escapeHtml(item.txHash || '')}">
                    ${truncatedHash}
                    <button class="copy-btn" onclick="copyToClipboard('${escapeHtml(item.txHash || '')}', this)" style="margin-left: 0.25rem;">Copy</button>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = rowsHtml;
}

function updateStatsUI(stats) {
    const totalCountEl = document.getElementById("total-logs-count");
    if (totalCountEl && stats.totalLogs !== undefined) {
        totalCountEl.textContent = stats.totalLogs;
    }

    const breachCountEl = document.getElementById("total-breaches-count");
    if (breachCountEl && stats.totalBreaches !== undefined) {
        breachCountEl.textContent = stats.totalBreaches;
    }

    const evmPill = document.getElementById("evm-status-pill");
    if (evmPill) {
        if (stats.evmConnected) {
            evmPill.className = "status-pill online";
            evmPill.innerHTML = `<span class="pulse-dot"></span> EVM Node Online (${escapeHtml(stats.evmVersion || '')})`;
        } else {
            evmPill.className = "status-pill evm-standby";
            evmPill.innerHTML = `<span class="pulse-dot"></span> EVM Standby (Keccak-256 Notarization)`;
        }
    }
}

function setupEventHandlers() {
    const customForm = document.getElementById("custom-telemetry-form");
    if (customForm) {
        customForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const rfidUid = document.getElementById("form-rfid-uid").value.trim();
            const batchId = document.getElementById("form-batch-id").value.trim();
            const temperature = parseFloat(document.getElementById("form-temperature").value);
            const humidity = parseFloat(document.getElementById("form-humidity").value);
            const locationName = document.getElementById("form-location-name").value;

            await sendTelemetryPayload({
                rfidUid,
                batchId,
                temperature,
                humidity,
                locationName
            });
        });
    }
}

/**
 * Sends a telemetry payload to POST /api/telemetry and triggers an immediate dashboard refresh.
 */
async function sendTelemetryPayload(payload) {
    try {
        const response = await fetch("/api/telemetry", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const err = await response.text();
            showToast(`Ingestion error: ${err}`, "danger");
            return;
        }

        const data = await response.json();
        showFeedbackModal(data);
        await fetchDashboardUpdates();
    } catch (err) {
        console.error("Telemetry ingestion network error:", err);
        showToast("Network error contacting telemetry ingestion API.", "danger");
    }
}

/**
 * Quick Simulation Trigger Functions
 */
async function simulateTag1DistributorScan() {
    const batchId = document.getElementById("batch-id-val")?.textContent?.trim() || "VAX-B4920-COVID19";
    await sendTelemetryPayload({
        rfidUid: "2AF52B07",
        batchId: batchId,
        temperature: 4.5,
        humidity: 46.0,
        locationName: "Distributor Hub (College)"
    });
}

async function simulateTag2DistributorScan() {
    const batchId = document.getElementById("batch-id-val")?.textContent?.trim() || "VAX-B4920-COVID19";
    await sendTelemetryPayload({
        rfidUid: "659A1707",
        batchId: batchId,
        temperature: 5.2,
        humidity: 47.5,
        locationName: "Distributor Hub (College)"
    });
}

async function simulateAuthorizedScan(hubName, temp, humidity) {
    const boundRfid = document.getElementById("bound-rfid-val")?.textContent?.trim() || "2AF52B07";
    const batchId = document.getElementById("batch-id-val")?.textContent?.trim() || "VAX-B4920-COVID19";

    await sendTelemetryPayload({
        rfidUid: boundRfid,
        batchId: batchId,
        temperature: temp,
        humidity: humidity,
        locationName: hubName
    });
}

async function simulateUnauthorizedScan() {
    const batchId = document.getElementById("batch-id-val")?.textContent?.trim() || "VAX-B4920-COVID19";

    await sendTelemetryPayload({
        rfidUid: "99AABBCC",
        batchId: batchId,
        temperature: 4.8,
        humidity: 48.0,
        locationName: "Distributor Hub (College)"
    });
}

async function simulateThermalExcursion() {
    const boundRfid = document.getElementById("bound-rfid-val")?.textContent?.trim() || "2AF52B07";
    const batchId = document.getElementById("batch-id-val")?.textContent?.trim() || "VAX-B4920-COVID19";

    await sendTelemetryPayload({
        rfidUid: boundRfid,
        batchId: batchId,
        temperature: 11.4, // Excursion above 8.0°C!
        humidity: 62.0,
        locationName: "Distributor Hub (College)"
    });
}

async function resetBatchCustody() {
    const batchId = document.getElementById("batch-id-val")?.textContent?.trim() || "VAX-B4920-COVID19";
    try {
        const res = await fetch(`/api/batch/reset?batchId=${encodeURIComponent(batchId)}`, {
            method: "POST"
        });
        if (res.ok) {
            showToast("Batch custody reset to Manufacturer Hub successfully.", "success");
            await fetchDashboardUpdates();
        }
    } catch (err) {
        console.error("Reset error:", err);
    }
}

function showFeedbackModal(data) {
    const modalEl = document.getElementById("response-feedback-card");
    const jsonEl = document.getElementById("response-json-preview");
    if (modalEl && jsonEl) {
        jsonEl.textContent = JSON.stringify(data, null, 2);
        modalEl.style.display = "block";
    }
}

function hideFeedbackModal() {
    const modalEl = document.getElementById("response-feedback-card");
    if (modalEl) modalEl.style.display = "none";
}

function copyToClipboard(text, btnElement) {
    if (!text) return;
    navigator.clipboard.writeText(text).then(() => {
        const originalText = btnElement ? btnElement.textContent : "";
        if (btnElement) btnElement.textContent = "Copied!";
        setTimeout(() => {
            if (btnElement) btnElement.textContent = originalText;
        }, 1500);
    }).catch(err => {
        console.error("Failed to copy:", err);
    });
}

function showToast(message, type = "success") {
    const toast = document.createElement("div");
    toast.style.position = "fixed";
    toast.style.bottom = "20px";
    toast.style.right = "20px";
    toast.style.backgroundColor = type === "danger" ? "#ef4444" : "#10b981";
    toast.style.color = "#ffffff";
    toast.style.padding = "0.75rem 1.25rem";
    toast.style.borderRadius = "6px";
    toast.style.boxShadow = "0 4px 6px -1px rgb(0 0 0 / 0.1)";
    toast.style.zIndex = "9999";
    toast.style.fontSize = "0.85rem";
    toast.style.fontWeight = "600";
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transition = "opacity 0.5s ease";
        setTimeout(() => toast.remove(), 500);
    }, 3000);
}

function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
}
