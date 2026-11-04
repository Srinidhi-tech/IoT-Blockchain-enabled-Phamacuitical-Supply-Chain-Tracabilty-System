package com.pharmatrace.dto;

import com.pharmatrace.model.VerificationStatus;
import java.time.LocalDateTime;

/**
 * Actuator instructions payload returned to IoT hardware (ESP32/Arduino/Raspberry Pi).
 */
public class HardwareResponse {

    private boolean success;
    private boolean buzzer;
    private String led; // "GREEN", "RED", "AMBER"
    private VerificationStatus verificationStatus;
    private String txHash;
    private String batchId;
    private String locationName;
    private String message;
    private LocalDateTime timestamp;

    public HardwareResponse() {
    }

    public HardwareResponse(boolean success, boolean buzzer, String led,
                            VerificationStatus verificationStatus, String txHash,
                            String batchId, String locationName, String message,
                            LocalDateTime timestamp) {
        this.success = success;
        this.buzzer = buzzer;
        this.led = led;
        this.verificationStatus = verificationStatus;
        this.txHash = txHash;
        this.batchId = batchId;
        this.locationName = locationName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isBuzzer() {
        return buzzer;
    }

    public void setBuzzer(boolean buzzer) {
        this.buzzer = buzzer;
    }

    public String getLed() {
        return led;
    }

    public void setLed(String led) {
        this.led = led;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
