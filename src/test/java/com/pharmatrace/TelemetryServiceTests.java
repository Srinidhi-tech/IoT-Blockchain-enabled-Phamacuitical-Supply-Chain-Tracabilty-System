package com.pharmatrace;

import com.pharmatrace.dto.HardwareResponse;
import com.pharmatrace.dto.TelemetryRequest;
import com.pharmatrace.model.ProductBatch;
import com.pharmatrace.model.TelemetryLog;
import com.pharmatrace.model.VerificationStatus;
import com.pharmatrace.repository.ProductBatchRepository;
import com.pharmatrace.repository.TelemetryLogRepository;
import com.pharmatrace.service.BatchCustodyService;
import com.pharmatrace.service.BlockchainNotarizationService;
import com.pharmatrace.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TelemetryServiceTests {

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private BatchCustodyService custodyService;

    @Autowired
    private BlockchainNotarizationService blockchainService;

    @Autowired
    private TelemetryLogRepository telemetryRepository;

    @Autowired
    private ProductBatchRepository batchRepository;

    @BeforeEach
    void setUp() {
        telemetryRepository.deleteAll();
        custodyService.resetBatch("VAX-B4920-COVID19");
    }

    @Test
    @DisplayName("Tag 1 (2AF52B07): Valid authorized transit scan returns HTTP 200, buzzer FALSE, led GREEN")
    void testTag1AuthorizedScan() {
        TelemetryRequest request = new TelemetryRequest(
                "2AF52B07",
                "VAX-B4920-COVID19",
                4.5,
                46.0,
                "Distributor Hub (College)"
        );

        HardwareResponse response = telemetryService.processTelemetry(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertFalse(response.isBuzzer(), "Buzzer must be FALSE for Tag 1 (2AF52B07)");
        assertEquals("GREEN", response.getLed());
        assertEquals(VerificationStatus.AUTHENTIC_VERIFIED, response.getVerificationStatus());
        assertEquals("Distributor Hub (College)", response.getLocationName());
        assertNotNull(response.getTxHash());
    }

    @Test
    @DisplayName("Tag 2 (659A1707 / Authorized Custodian): Valid authorized transit scan returns buzzer FALSE, led GREEN")
    void testTag2AuthorizedScan() {
        TelemetryRequest request = new TelemetryRequest(
                "659A1707",
                "VAX-B4920-COVID19",
                5.1,
                47.0,
                "Distributor Hub (College)"
        );

        HardwareResponse response = telemetryService.processTelemetry(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertFalse(response.isBuzzer(), "Buzzer must be FALSE for Tag 2 (659A1707)");
        assertEquals("GREEN", response.getLed());
        assertEquals(VerificationStatus.AUTHENTIC_VERIFIED, response.getVerificationStatus());
        assertEquals("Distributor Hub (College)", response.getLocationName());
        assertNotNull(response.getTxHash());
    }

    @Test
    @DisplayName("Completely unknown UID: Triggers tamper breach with buzzer ACTIVE and led RED")
    void testUnknownUidTriggersTamperBreach() {
        TelemetryRequest request = new TelemetryRequest(
                "99AABBCC",
                "VAX-B4920-COVID19",
                4.5,
                45.0,
                "Distributor Hub (College)"
        );

        HardwareResponse response = telemetryService.processTelemetry(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.isBuzzer(), "Buzzer must be TRUE for unregistered tag");
        assertEquals("RED", response.getLed());
        assertEquals(VerificationStatus.UNAUTHORIZED_TAG_BREACH, response.getVerificationStatus());
    }

    @Test
    @DisplayName("Authorized RFID Tag + Normal Temp: Verification AUTHENTIC_VERIFIED and Buzzer OFF")
    void testAuthorizedScanNormalTemperature() {
        TelemetryRequest request = new TelemetryRequest(
                "2AF52B07",
                "VAX-B4920-COVID19",
                4.5,
                45.0,
                "Manufacturer Hub"
        );

        HardwareResponse response = telemetryService.processTelemetry(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertFalse(response.isBuzzer(), "Buzzer must be false for authentic scan");
        assertEquals("GREEN", response.getLed());
        assertEquals(VerificationStatus.AUTHENTIC_VERIFIED, response.getVerificationStatus());
        assertNotNull(response.getTxHash(), "Keccak-256 hash must be present");
        assertTrue(response.getTxHash().startsWith("0x"), "Hash must begin with 0x prefix");
        assertEquals(66, response.getTxHash().length(), "Keccak-256 hash should be 66 characters (0x + 64 hex)");

        // Check H2 Database persistence
        List<TelemetryLog> logs = telemetryRepository.findAll();
        assertFalse(logs.isEmpty());
        TelemetryLog savedLog = logs.get(0);
        assertEquals("TAG-A-04E19C82", savedLog.getRfidUid());
        assertEquals(VerificationStatus.AUTHENTIC_VERIFIED, savedLog.getVerificationStatus());
        assertEquals(response.getTxHash(), savedLog.getTxHash());
    }

    @Test
    @DisplayName("Unauthorized RFID Tag (Card B): Verification UNAUTHORIZED_TAG_BREACH and Buzzer ON")
    void testUnauthorizedScanTriggersAlarm() {
        TelemetryRequest request = new TelemetryRequest(
                "CARD-B-UNAUTHORIZED-99",
                "VAX-B4920-COVID19",
                5.0,
                48.0,
                "Distributor Hub (College)"
        );

        HardwareResponse response = telemetryService.processTelemetry(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.isBuzzer(), "Buzzer must be ACTIVE for unauthorized RFID breach");
        assertEquals("RED", response.getLed());
        assertEquals(VerificationStatus.UNAUTHORIZED_TAG_BREACH, response.getVerificationStatus());

        // Check Batch is marked tampered in DB
        ProductBatch batch = custodyService.getActiveBatch("VAX-B4920-COVID19");
        assertTrue(batch.getTampered(), "Batch must be marked tampered");

        // Check log in H2
        List<TelemetryLog> logs = telemetryRepository.findAll();
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).getBuzzerActive());
        assertEquals("RED", logs.get(0).getLedColor());
    }

    @Test
    @DisplayName("Authorized RFID Tag + Thermal Excursion: Verification THERMAL_BREACH and Buzzer Active")
    void testThermalExcursionBreach() {
        TelemetryRequest request = new TelemetryRequest(
                "TAG-A-04E19C82",
                "VAX-B4920-COVID19",
                12.5, // Exceeds WHO upper bound of 8.0°C
                50.0,
                "Distributor Hub (College)"
        );

        HardwareResponse response = telemetryService.processTelemetry(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.isBuzzer(), "Buzzer must trigger on thermal excursion");
        assertEquals("AMBER", response.getLed());
        assertEquals(VerificationStatus.THERMAL_BREACH, response.getVerificationStatus());
    }

    @Test
    @DisplayName("Multi-hub custody state progression across the 3 hubs")
    void testMultiHubCustodyProgression() {
        // Step 1: Manufacturer
        TelemetryRequest req1 = new TelemetryRequest("TAG-A-04E19C82", "VAX-B4920-COVID19", 4.0, 45.0, "Manufacturer Hub");
        telemetryService.processTelemetry(req1);
        ProductBatch b1 = custodyService.getActiveBatch("VAX-B4920-COVID19");
        assertEquals(1, b1.getCustodyStep());
        assertEquals("Manufacturer Hub", b1.getCurrentHub());

        // Step 2: Distributor (College)
        TelemetryRequest req2 = new TelemetryRequest("TAG-A-04E19C82", "VAX-B4920-COVID19", 4.8, 46.0, "Distributor Hub (College)");
        telemetryService.processTelemetry(req2);
        ProductBatch b2 = custodyService.getActiveBatch("VAX-B4920-COVID19");
        assertEquals(2, b2.getCustodyStep());
        assertEquals("Distributor Hub (College)", b2.getCurrentHub());

        // Step 3: Consumer Hospital
        TelemetryRequest req3 = new TelemetryRequest("TAG-A-04E19C82", "VAX-B4920-COVID19", 3.9, 44.0, "Consumer Hospital");
        telemetryService.processTelemetry(req3);
        ProductBatch b3 = custodyService.getActiveBatch("VAX-B4920-COVID19");
        assertEquals(3, b3.getCustodyStep());
        assertEquals("Consumer Hospital", b3.getCurrentHub());
    }

    @Test
    @DisplayName("Blockchain Notarization: Keccak-256 deterministic cryptographic hash calculation")
    void testKeccak256HashNotarization() {
        String hash1 = blockchainService.generateTelemetryTxHash(
                "VAX-B4920-COVID19", "TAG-A-04E19C82", 4.5, 45.0, "Manufacturer Hub", "AUTHENTIC_VERIFIED", null
        );
        String hash2 = blockchainService.generateTelemetryTxHash(
                "VAX-B4920-COVID19", "TAG-A-04E19C82", 4.5, 45.0, "Manufacturer Hub", "AUTHENTIC_VERIFIED", null
        );

        assertNotNull(hash1);
        assertTrue(hash1.startsWith("0x"));
        assertEquals(hash1, hash2, "Deterministic Keccak-256 hashes must match for identical inputs");
    }
}
