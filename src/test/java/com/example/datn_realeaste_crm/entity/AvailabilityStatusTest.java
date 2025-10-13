package com.example.datn_realeaste_crm.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvailabilityStatusTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testFromCode() {
        assertEquals(AvailabilityStatus.NOT_AVAILABLE, AvailabilityStatus.fromCode(0));
        assertEquals(AvailabilityStatus.PENDING, AvailabilityStatus.fromCode(1));
        assertEquals(AvailabilityStatus.AVAILABLE, AvailabilityStatus.fromCode(2));
        assertEquals(AvailabilityStatus.DEPOSITED, AvailabilityStatus.fromCode(3));
        assertEquals(AvailabilityStatus.SOLD, AvailabilityStatus.fromCode(4));
    }

    @Test
    void testFromCodeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AvailabilityStatus.fromCode(99));
    }

    @Test
    void testFromString() {
        assertEquals(AvailabilityStatus.PENDING, AvailabilityStatus.fromString("PENDING"));
        assertEquals(AvailabilityStatus.AVAILABLE, AvailabilityStatus.fromString("APPROVED"));
        assertEquals(AvailabilityStatus.AVAILABLE, AvailabilityStatus.fromString("AVAILABLE"));
        assertEquals(AvailabilityStatus.DEPOSITED, AvailabilityStatus.fromString("DEPOSITED"));
        assertEquals(AvailabilityStatus.SOLD, AvailabilityStatus.fromString("SOLD"));
        assertEquals(AvailabilityStatus.NOT_AVAILABLE, AvailabilityStatus.fromString("REJECTED"));
        assertEquals(AvailabilityStatus.NOT_AVAILABLE, AvailabilityStatus.fromString("UNKNOWN"));
        assertEquals(AvailabilityStatus.NOT_AVAILABLE, AvailabilityStatus.fromString(null));
        assertEquals(AvailabilityStatus.NOT_AVAILABLE, AvailabilityStatus.fromString(""));
    }

    @Test
    void testGetCode() {
        assertEquals(0, AvailabilityStatus.NOT_AVAILABLE.getCode());
        assertEquals(1, AvailabilityStatus.PENDING.getCode());
        assertEquals(2, AvailabilityStatus.AVAILABLE.getCode());
        assertEquals(3, AvailabilityStatus.DEPOSITED.getCode());
        assertEquals(4, AvailabilityStatus.SOLD.getCode());
    }

    @Test
    void testGetDescription() {
        assertEquals("Not Available", AvailabilityStatus.NOT_AVAILABLE.getDescription());
        assertEquals("Pending Approval", AvailabilityStatus.PENDING.getDescription());
        assertEquals("Available", AvailabilityStatus.AVAILABLE.getDescription());
        assertEquals("Deposited", AvailabilityStatus.DEPOSITED.getDescription());
        assertEquals("Sold", AvailabilityStatus.SOLD.getDescription());
    }

    @Test
    void testJsonSerialization() throws Exception {
        // Test serialization
        assertEquals("0", objectMapper.writeValueAsString(AvailabilityStatus.NOT_AVAILABLE));
        assertEquals("1", objectMapper.writeValueAsString(AvailabilityStatus.PENDING));
        assertEquals("2", objectMapper.writeValueAsString(AvailabilityStatus.AVAILABLE));
        assertEquals("3", objectMapper.writeValueAsString(AvailabilityStatus.DEPOSITED));
        assertEquals("4", objectMapper.writeValueAsString(AvailabilityStatus.SOLD));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        // Test deserialization
        assertEquals(AvailabilityStatus.NOT_AVAILABLE, objectMapper.readValue("0", AvailabilityStatus.class));
        assertEquals(AvailabilityStatus.PENDING, objectMapper.readValue("1", AvailabilityStatus.class));
        assertEquals(AvailabilityStatus.AVAILABLE, objectMapper.readValue("2", AvailabilityStatus.class));
        assertEquals(AvailabilityStatus.DEPOSITED, objectMapper.readValue("3", AvailabilityStatus.class));
        assertEquals(AvailabilityStatus.SOLD, objectMapper.readValue("4", AvailabilityStatus.class));
    }

    @Test
    void testJsonDeserializationInvalid() {
        assertThrows(Exception.class, () -> objectMapper.readValue("99", AvailabilityStatus.class));
    }
}
