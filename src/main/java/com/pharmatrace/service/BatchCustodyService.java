package com.pharmatrace.service;

import com.pharmatrace.config.PharmaTraceProperties;
import com.pharmatrace.model.ProductBatch;
import com.pharmatrace.model.VerificationStatus;
import com.pharmatrace.repository.ProductBatchRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BatchCustodyService {

    private static final Logger log = LoggerFactory.getLogger(BatchCustodyService.class);

    public static final String HUB_MANUFACTURER = "Manufacturer Hub";
    public static final String HUB_DISTRIBUTOR = "Distributor Hub (College)";
    public static final String HUB_CONSUMER = "Consumer Hospital";

    public static final List<String> HUBS = List.of(
            HUB_MANUFACTURER,
            HUB_DISTRIBUTOR,
            HUB_CONSUMER
    );

    private final ProductBatchRepository batchRepository;
    private final PharmaTraceProperties properties;

    public BatchCustodyService(ProductBatchRepository batchRepository, PharmaTraceProperties properties) {
        this.batchRepository = batchRepository;
        this.properties = properties;
    }

    @PostConstruct
    @Transactional
    public void init() {
        String defaultBatchId = properties.getDefaultBatchId();
        Optional<ProductBatch> existing = batchRepository.findByBatchId(defaultBatchId);
        String primaryRfid = properties.getRfid().getAuthorizedUids().isEmpty()
                ? "2AF52B07"
                : properties.getRfid().getAuthorizedUids().get(0);

        if (existing.isEmpty()) {
            ProductBatch batch = new ProductBatch(
                    defaultBatchId,
                    properties.getDefaultProductName(),
                    primaryRfid,
                    HUB_MANUFACTURER,
                    1
            );
            batch.setLastTemperature(4.5);
            batch.setLastHumidity(45.0);
            batch.setLastStatus(VerificationStatus.AUTHENTIC_VERIFIED);
            batchRepository.save(batch);
            log.info("Initialized default pharmaceutical product batch: {} (Primary RFID: {})", defaultBatchId, primaryRfid);
        } else {
            ProductBatch batch = existing.get();
            // Synchronize batch bound RFID to the new primary authorized tag
            batch.setBoundRfidUid(primaryRfid);
            batchRepository.save(batch);
            log.info("Updated batch bound RFID UID to registered Tag 1: {}", primaryRfid);
        }
        log.info("Strictly registered authorized RFID tags: Tag 1 (2AF52B07), Tag 2 (659A1707)");
    }

    public ProductBatch getActiveBatch(String batchId) {
        if (batchId != null && !batchId.trim().isEmpty()) {
            return batchRepository.findByBatchId(batchId.trim())
                    .orElseGet(this::getOrCreateDefaultBatch);
        }
        return getOrCreateDefaultBatch();
    }

    private ProductBatch getOrCreateDefaultBatch() {
        return batchRepository.findByBatchId(properties.getDefaultBatchId())
                .orElseGet(() -> {
                    ProductBatch batch = new ProductBatch(
                            properties.getDefaultBatchId(),
                            properties.getDefaultProductName(),
                            properties.getRfid().getAuthorizedUids().get(0),
                            HUB_MANUFACTURER,
                            1
                    );
                    return batchRepository.save(batch);
                });
    }

    /**
     * Determines whether an RFID UID is authorized for the given batch or system-wide.
     * Strictly recognizes Tag 1 (2AF52B07) and Tag 2 (659A1707) as verified transit keys.
     * Any other UID returns false, triggering the unauthorized/tamper breach alarm.
     */
    public boolean isRfidAuthorized(String rfidUid, ProductBatch batch) {
        if (rfidUid == null || rfidUid.trim().isEmpty()) {
            return false;
        }

        String normalizedIncoming = normalizeRfid(rfidUid);

        // Strict explicit check for the two authorized physical RFID tags
        if ("2AF52B07".equalsIgnoreCase(normalizedIncoming) || "659A1707".equalsIgnoreCase(normalizedIncoming)) {
            return true;
        }

        // Check against configured system authorized RFID list
        for (String authorized : properties.getRfid().getAuthorizedUids()) {
            if (normalizeRfid(authorized).equalsIgnoreCase(normalizedIncoming)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a human-readable label identifying known authorized tags.
     */
    public String getTagLabel(String rfidUid) {
        if (rfidUid == null || rfidUid.trim().isEmpty()) {
            return "Empty Tag";
        }
        String normalized = normalizeRfid(rfidUid);
        if ("2AF52B07".equalsIgnoreCase(normalized)) {
            return "Tag 1 (Authorized Primary - 2AF52B07)";
        } else if ("659A1707".equalsIgnoreCase(normalized)) {
            return "Tag 2 (Authorized Custodian - 659A1707)";
        } else if (isRfidAuthorized(rfidUid, null)) {
            return "Authorized Tag (" + rfidUid + ")";
        }
        return "Unauthorized / Unregistered Tag";
    }

    public String normalizeRfid(String rfid) {
        if (rfid == null) return "";
        return rfid.replaceAll("[:\\s\\-_]", "").toUpperCase();
    }

    /**
     * Progresses custody to the next hub or sets the specific hub requested.
     */
    @Transactional
    public String advanceOrResolveHub(ProductBatch batch, String requestedLocation) {
        if (requestedLocation != null && !requestedLocation.trim().isEmpty()) {
            String trimmed = requestedLocation.trim().toLowerCase();
            if (trimmed.contains("distributor") || trimmed.contains("college") || trimmed.contains("transit")) {
                batch.setCurrentHub(HUB_DISTRIBUTOR);
                batch.setCustodyStep(2);
                return HUB_DISTRIBUTOR;
            } else if (trimmed.contains("consumer") || trimmed.contains("hospital") || trimmed.contains("clinic")) {
                batch.setCurrentHub(HUB_CONSUMER);
                batch.setCustodyStep(3);
                return HUB_CONSUMER;
            } else if (trimmed.contains("manufacturer") || trimmed.contains("plant") || trimmed.contains("origin")) {
                batch.setCurrentHub(HUB_MANUFACTURER);
                batch.setCustodyStep(1);
                return HUB_MANUFACTURER;
            }
        }

        // Auto-advance step if not at the final hub
        int currentStep = batch.getCustodyStep() != null ? batch.getCustodyStep() : 1;
        if (currentStep < 3) {
            currentStep++;
        }
        batch.setCustodyStep(currentStep);
        String hub = HUBS.get(currentStep - 1);
        batch.setCurrentHub(hub);
        return hub;
    }

    @Transactional
    public ProductBatch resetBatch(String batchId) {
        ProductBatch batch = getActiveBatch(batchId);
        batch.setCurrentHub(HUB_MANUFACTURER);
        batch.setCustodyStep(1);
        batch.setTampered(false);
        batch.setLastStatus(VerificationStatus.AUTHENTIC_VERIFIED);
        batch.setLastUpdated(LocalDateTime.now());
        return batchRepository.save(batch);
    }

    public double[] getHubCoordinates(String locationName) {
        if (locationName == null) {
            return new double[]{37.7749, -122.4194}; // Default to Manufacturer
        }
        String lower = locationName.toLowerCase();
        if (lower.contains("distributor") || lower.contains("college")) {
            return new double[]{37.5485, -122.3186}; // Distributor Hub (College)
        } else if (lower.contains("consumer") || lower.contains("hospital")) {
            return new double[]{37.3382, -121.8863}; // Consumer Hospital
        } else {
            return new double[]{37.7749, -122.4194}; // Manufacturer Hub
        }
    }
}
