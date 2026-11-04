package com.pharmatrace.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity tracking pharmaceutical product batches, bound RFID tags, and multi-hub custody lifecycle.
 */
@Entity
@Table(name = "product_batches")
public class ProductBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String batchId;

    @Column(nullable = false, length = 150)
    private String productName;

    @Column(nullable = false, length = 100)
    private String boundRfidUid;

    @Column(nullable = false, length = 120)
    private String currentHub;

    @Column(nullable = false)
    private Integer custodyStep; // 1: Manufacturer, 2: Distributor (College), 3: Consumer Hospital

    @Column(nullable = false)
    private Boolean tampered = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private VerificationStatus lastStatus;

    @Column
    private Double lastTemperature;

    @Column
    private Double lastHumidity;

    @Column(length = 100)
    private String lastTxHash;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @Column(nullable = false)
    private LocalDateTime creationDate;

    public ProductBatch() {
    }

    public ProductBatch(String batchId, String productName, String boundRfidUid, String currentHub, Integer custodyStep) {
        this.batchId = batchId;
        this.productName = productName;
        this.boundRfidUid = boundRfidUid;
        this.currentHub = currentHub;
        this.custodyStep = custodyStep;
        this.tampered = false;
        this.lastStatus = VerificationStatus.AUTHENTIC_VERIFIED;
        this.creationDate = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBoundRfidUid() {
        return boundRfidUid;
    }

    public void setBoundRfidUid(String boundRfidUid) {
        this.boundRfidUid = boundRfidUid;
    }

    public String getCurrentHub() {
        return currentHub;
    }

    public void setCurrentHub(String currentHub) {
        this.currentHub = currentHub;
    }

    public Integer getCustodyStep() {
        return custodyStep;
    }

    public void setCustodyStep(Integer custodyStep) {
        this.custodyStep = custodyStep;
    }

    public Boolean getTampered() {
        return tampered != null && tampered;
    }

    public void setTampered(Boolean tampered) {
        this.tampered = tampered;
    }

    public VerificationStatus getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(VerificationStatus lastStatus) {
        this.lastStatus = lastStatus;
    }

    public Double getLastTemperature() {
        return lastTemperature;
    }

    public void setLastTemperature(Double lastTemperature) {
        this.lastTemperature = lastTemperature;
    }

    public Double getLastHumidity() {
        return lastHumidity;
    }

    public void setLastHumidity(Double lastHumidity) {
        this.lastHumidity = lastHumidity;
    }

    public String getLastTxHash() {
        return lastTxHash;
    }

    public void setLastTxHash(String lastTxHash) {
        this.lastTxHash = lastTxHash;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
