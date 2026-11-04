package com.pharmatrace.controller;

import com.pharmatrace.config.PharmaTraceProperties;
import com.pharmatrace.model.ProductBatch;
import com.pharmatrace.service.BatchCustodyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for batch custody lifecycle management, reset, and authorized tag discovery.
 */
@RestController
@RequestMapping("/api/batch")
@CrossOrigin(origins = "*")
public class BatchRestController {

    private final BatchCustodyService custodyService;
    private final PharmaTraceProperties properties;

    public BatchRestController(BatchCustodyService custodyService, PharmaTraceProperties properties) {
        this.custodyService = custodyService;
        this.properties = properties;
    }

    /**
     * Retrieves the active pharmaceutical batch custody record.
     */
    @GetMapping("/active")
    public ResponseEntity<ProductBatch> getActiveBatch(@RequestParam(required = false) String batchId) {
        ProductBatch batch = custodyService.getActiveBatch(batchId);
        return ResponseEntity.ok(batch);
    }

    /**
     * Resets the active batch to Manufacturer Hub and clears tamper flags (for testing/demo runs).
     */
    @PostMapping("/reset")
    public ResponseEntity<ProductBatch> resetBatch(@RequestParam(required = false) String batchId) {
        String idToReset = (batchId != null && !batchId.trim().isEmpty())
                ? batchId.trim()
                : properties.getDefaultBatchId();
        ProductBatch resetBatch = custodyService.resetBatch(idToReset);
        return ResponseEntity.ok(resetBatch);
    }

    /**
     * Returns list of registered authorized RFID Tag UIDs.
     */
    @GetMapping("/authorized-tags")
    public ResponseEntity<Map<String, Object>> getAuthorizedTags() {
        Map<String, Object> data = new HashMap<>();
        data.put("authorizedUids", properties.getRfid().getAuthorizedUids());
        data.put("primaryAuthorizedUid", "2AF52B07");
        data.put("tag1", "2AF52B07");
        data.put("tag2", "659A1707");
        data.put("defaultBatchId", properties.getDefaultBatchId());
        data.put("hubs", BatchCustodyService.HUBS);
        return ResponseEntity.ok(data);
    }
}
