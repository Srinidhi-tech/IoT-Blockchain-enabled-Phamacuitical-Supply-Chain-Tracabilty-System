package com.pharmatrace.service;

import com.pharmatrace.config.PharmaTraceProperties;
import com.pharmatrace.dto.HardwareResponse;
import com.pharmatrace.dto.TelemetryRequest;
import com.pharmatrace.model.ProductBatch;
import com.pharmatrace.model.TelemetryLog;
import com.pharmatrace.model.VerificationStatus;
import com.pharmatrace.repository.ProductBatchRepository;
import com.pharmatrace.repository.TelemetryLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final TelemetryLogRepository telemetryRepository;
    private final ProductBatchRepository batchRepository;
    private final BatchCustodyService custodyService;
    private final BlockchainNotarizationService blockchainService;
    private final PharmaTraceProperties properties;

    public TelemetryService(TelemetryLogRepository telemetryRepository,
                            ProductBatchRepository batchRepository,
                            BatchCustodyService custodyService,
                            BlockchainNotarizationService blockchainService,
                            PharmaTraceProperties properties) {
        this.telemetryRepository = telemetryRepository;
        this.batchRepository = batchRepository;
        this.custodyService = custodyService;
        this.blockchainService = blockchainService;
        this.properties = properties;
    }

    /**
     * Ingests IoT hardware telemetry, performs RFID custody validation, evaluates WHO cold-chain
     * temperature boundaries, computes Keccak-256 blockchain hash, and stores record in H2.
     */
    @Transactional
    public HardwareResponse processTelemetry(TelemetryRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String batchId = (request.getBatchId() != null && !request.getBatchId().trim().isEmpty())
                ? request.getBatchId().trim()
                : properties.getDefaultBatchId();

        ProductBatch batch = custodyService.getActiveBatch(batchId);
        String rfidUid = request.getRfidUid() != null ? request.getRfidUid().trim() : "";
        Double temperature = request.getTemperature() != null ? request.getTemperature() : 0.0;
        Double humidity = request.getHumidity() != null ? request.getHumidity() : 0.0;

        boolean authorized = custodyService.isRfidAuthorized(rfidUid, batch);
        VerificationStatus status;
        boolean buzzerActive;
        String ledColor;
        String responseMessage;
        String resolvedLocation;

        Double minTemp = properties.getTemperature().getMin();
        Double maxTemp = properties.getTemperature().getMax();

        if (!authorized) {
            // UNAUTHORIZED TAG / CARD B DETECTED
            status = VerificationStatus.UNAUTHORIZED_TAG_BREACH;
            buzzerActive = true;
            ledColor = "RED";
            resolvedLocation = (request.getLocationName() != null && !request.getLocationName().trim().isEmpty())
                    ? request.getLocationName().trim()
                    : batch.getCurrentHub();
            batch.setTampered(true);
            responseMessage = String.format(
                    "CRITICAL TAMPER BREACH: Unauthorized RFID Tag UID [%s] rejected! Hardware Buzzer [ACTIVE], LED [RED].",
                    rfidUid
            );
            log.warn("TAMPER ALERT: Unauthorized RFID tag {} detected for batch {}", rfidUid, batchId);
        } else {
            // AUTHORIZED RFID TAG MATCHED (Tag 1: 306F4D53, Tag 2: 21801466, or bound UID)
            resolvedLocation = custodyService.advanceOrResolveHub(batch, request.getLocationName());
            String tagLabel = custodyService.getTagLabel(rfidUid);

            // Evaluate temperature against WHO limits (2.0°C - 8.0°C)
            if (temperature < minTemp || temperature > maxTemp) {
                status = VerificationStatus.THERMAL_BREACH;
                buzzerActive = true; // Actuator alert for temperature breach
                ledColor = "AMBER";
                responseMessage = String.format(
                        "COLD-CHAIN WARNING: Temperature %.1f°C breaches WHO limits (%.1f°C - %.1f°C). Buzzer warning active.",
                        temperature, minTemp, maxTemp
                );
                log.warn("THERMAL BREACH: Batch {} recorded {}°C at {}", batchId, temperature, resolvedLocation);
            } else {
                status = VerificationStatus.AUTHENTIC_VERIFIED;
                buzzerActive = false;
                ledColor = "GREEN";
                batch.setTampered(false); // Validated custody by authorized tag clears active alarm
                responseMessage = String.format(
                        "AUTHENTIC RFID VERIFIED: %s [%s] custody validated at %s. Cold-chain safe at %.1f°C.",
                        tagLabel, rfidUid, resolvedLocation, temperature
                );
                log.info("AUTHENTIC SCAN: Batch {} verified at {} with {}", batchId, resolvedLocation, tagLabel);
            }
        }

        // Determine coordinates
        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();
        if (latitude == null || longitude == null) {
            double[] coords = custodyService.getHubCoordinates(resolvedLocation);
            latitude = coords[0];
            longitude = coords[1];
        }

        // Compute Keccak-256 cryptographic hash (EVM Notarization)
        String txHash = blockchainService.generateTelemetryTxHash(
                batchId, rfidUid, temperature, humidity, resolvedLocation, status.name(), now
        );

        // Persist TelemetryLog to H2 Database
        TelemetryLog logEntity = new TelemetryLog(
                rfidUid,
                batchId,
                temperature,
                humidity,
                resolvedLocation,
                latitude,
                longitude,
                status,
                txHash,
                now
        );
        logEntity.setBuzzerActive(buzzerActive);
        logEntity.setLedColor(ledColor);
        logEntity.setNotes(responseMessage);
        telemetryRepository.save(logEntity);

        // Update ProductBatch state in H2 Database
        batch.setLastStatus(status);
        batch.setLastTemperature(temperature);
        batch.setLastHumidity(humidity);
        batch.setLastTxHash(txHash);
        batch.setLastUpdated(now);
        batchRepository.save(batch);

        return new HardwareResponse(
                status == VerificationStatus.AUTHENTIC_VERIFIED,
                buzzerActive,
                ledColor,
                status,
                txHash,
                batchId,
                resolvedLocation,
                responseMessage,
                now
        );
    }

    public List<TelemetryLog> getRecentTelemetryLogs() {
        return telemetryRepository.findTop25ByOrderByTimestampDesc();
    }

    public List<TelemetryLog> getAllTelemetryLogs() {
        return telemetryRepository.findAllByOrderByTimestampDesc();
    }

    public Optional<TelemetryLog> getLatestTelemetry() {
        return telemetryRepository.findTop1ByOrderByTimestampDesc();
    }

    public long getTotalLogsCount() {
        return telemetryRepository.count();
    }

    public long getBreachCount() {
        return telemetryRepository.countByVerificationStatus(VerificationStatus.UNAUTHORIZED_TAG_BREACH) +
               telemetryRepository.countByVerificationStatus(VerificationStatus.THERMAL_BREACH);
    }
}
