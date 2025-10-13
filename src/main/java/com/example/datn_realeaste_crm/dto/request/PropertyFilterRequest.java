package com.example.datn_realeaste_crm.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PropertyFilterRequest {
    
    private String propertyType;
    private Integer districtId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer minBedrooms;
    private Integer maxBedrooms;
    private Integer minBathrooms;
    private Integer maxBathrooms;
    private BigDecimal minSize;
    private BigDecimal maxSize;
    private Integer floor;
    private String keyword; // Tìm kiếm trong address hoặc description
    
    // Filter theo availability status
    private Integer availability;
    
    // Sort options
    private String sortBy = "createdAt"; // Default sort by created date
    private String sortDirection = "desc"; // Default descending order (newest first)
}
