package com.pharmatrace.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an ingested IoT telemetry and RFID custody record.
 */
@Entity
@Table(name = "telemetry_logs", indexes = {
    @Index(name = "idx_batch_id", columnList = "batchId"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_status", columnList = "verificationStatus")
})
public class TelemetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String rfidUid;

    @Column(nullable = false, length = 100)
    private String batchId;

    @Column(nullable = false)
    private Double temperature;

    @Column(nullable = false)
    private Double humidity;

    @Column(nullable = false, length = 120)
    private String locationName;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationStatus verificationStatus;

    @Column(nullable = false, length = 100)
    private String txHash;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private Boolean buzzerActive = false;

    @Column(length = 20)
    private String ledColor = "GREEN";

    @Column(length = 500)
    private String notes;

    public TelemetryLog() {
    }

    public TelemetryLog(String rfidUid, String batchId, Double temperature, Double humidity,
                        String locationName, Double latitude, Double longitude,
                        VerificationStatus verificationStatus, String txHash, LocalDateTime timestamp) {
        this.rfidUid = rfidUid;
        this.batchId = batchId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.verificationStatus = verificationStatus;
        this.txHash = txHash;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRfidUid() {
        return rfidUid;
    }

    public void setRfidUid(String rfidUid) {
        this.rfidUid = rfidUid;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getBuzzerActive() {
        return buzzerActive != null && buzzerActive;
    }

    public void setBuzzerActive(Boolean buzzerActive) {
        this.buzzerActive = buzzerActive;
    }

    public String getLedColor() {
        return ledColor;
    }

    public void setLedColor(String ledColor) {
        this.ledColor = ledColor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
