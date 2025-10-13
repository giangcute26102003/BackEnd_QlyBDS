package com.example.datn_realeaste_crm.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the availability status of a property
 */
public enum AvailabilityStatus {
    NOT_AVAILABLE(0, "Not Available"),
    PENDING(1, "Pending Approval"),
    AVAILABLE(2, "Available"),
    DEPOSITED(3, "Deposited"),
    SOLD(4, "Sold");

    private final int code;
    private final String description;

    AvailabilityStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Create AvailabilityStatus from integer code for JSON deserialization
     */
    @JsonCreator
    public static AvailabilityStatus fromCode(int code) {
        for (AvailabilityStatus status : AvailabilityStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid availability status code: " + code);
    }

    /**
     * Create AvailabilityStatus from string value (for migration purposes)
     */
    public static AvailabilityStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NOT_AVAILABLE;
        }
        
        switch (value.toUpperCase()) {
            case "PENDING":
                return PENDING;
            case "APPROVED":
            case "AVAILABLE":
                return AVAILABLE;
            case "DEPOSITED":
                return DEPOSITED;
            case "SOLD":
                return SOLD;
            case "REJECTED":
            case "NOT_AVAILABLE":
            default:
                return NOT_AVAILABLE;
        }
    }

    @Override
    public String toString() {
        return description;
    }
}
