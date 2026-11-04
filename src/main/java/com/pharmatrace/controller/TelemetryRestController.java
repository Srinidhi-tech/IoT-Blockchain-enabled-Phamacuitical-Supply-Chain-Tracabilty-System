package com.pharmatrace.controller;

import com.pharmatrace.dto.HardwareResponse;
import com.pharmatrace.dto.TelemetryRequest;
import com.pharmatrace.model.TelemetryLog;
import com.pharmatrace.service.BlockchainNotarizationService;
import com.pharmatrace.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for IoT hardware telemetry ingestion and telemetry querying.
 */
@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin(origins = "*")
public class TelemetryRestController {

    private final TelemetryService telemetryService;
    private final BlockchainNotarizationService blockchainService;

    public TelemetryRestController(TelemetryService telemetryService,
                                   BlockchainNotarizationService blockchainService) {
        this.telemetryService = telemetryService;
        this.blockchainService = blockchainService;
    }

    /**
     * Ingestion endpoint for hardware telemetry (RFID RC522/PN532 + DHT22 on ESP32/Arduino).
     */
    @PostMapping
    public ResponseEntity<HardwareResponse> ingestTelemetry(@Valid @RequestBody TelemetryRequest request) {
        HardwareResponse response = telemetryService.processTelemetry(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the latest telemetry log.
     */
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestTelemetry() {
        return telemetryService.getLatestTelemetry()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Returns recent telemetry logs for the dashboard audit trail.
     */
    @GetMapping("/history")
    public ResponseEntity<List<TelemetryLog>> getTelemetryHistory() {
        return ResponseEntity.ok(telemetryService.getRecentTelemetryLogs());
    }

    /**
     * Returns operational telemetry statistics and EVM blockchain status.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", telemetryService.getTotalLogsCount());
        stats.put("totalBreaches", telemetryService.getBreachCount());
        stats.put("evmConnected", blockchainService.isEvmConnected());
        stats.put("evmVersion", blockchainService.getEvmClientVersion());
        stats.put("evmStandbyReason", blockchainService.getStandbyReason());
        return ResponseEntity.ok(stats);
    }
}
