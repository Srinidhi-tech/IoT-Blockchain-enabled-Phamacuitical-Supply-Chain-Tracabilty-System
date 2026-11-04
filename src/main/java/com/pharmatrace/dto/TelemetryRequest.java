package com.pharmatrace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TelemetryRequest {

    @NotBlank(message = "rfidUid is required")
    private String rfidUid;

    private String batchId;

    @NotNull(message = "temperature is required")
    private Double temperature;

    @NotNull(message = "humidity is required")
    private Double humidity;

    private String locationName;
    private Double latitude;
    private Double longitude;

    public TelemetryRequest() {
    }

    public TelemetryRequest(String rfidUid, String batchId, Double temperature, Double humidity, String locationName) {
        this.rfidUid = rfidUid;
        this.batchId = batchId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.locationName = locationName;
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
}
