package com.pharmatrace.model;

/**
 * Verification status verdicts for RFID and cold-chain telemetry events.
 */
public enum VerificationStatus {
    AUTHENTIC_VERIFIED("Authentic RFID Verified", "badge-success", "Normal Operation"),
    UNAUTHORIZED_TAG_BREACH("Unauthorized Tag Breach", "badge-danger", "Active Security Tamper"),
    THERMAL_BREACH("Thermal Excursion Breach", "badge-warning", "Cold-Chain Integrity Breached");

    private final String label;
    private final String badgeClass;
    private final String description;

    VerificationStatus(String label, String badgeClass, String description) {
        this.label = label;
        this.badgeClass = badgeClass;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBreach() {
        return this != AUTHENTIC_VERIFIED;
    }
}
