package com.example.datn_realeaste_crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyAnalyticsResponse {
    private Long totalProperties;
    private Long pendingApproval;
    private Long approved;
    private Long rejected;
    private Long draft;
    private Long sold;
    private Long rented;
    
    private Map<String, Long> typeDistribution;
    private Map<String, Long> statusDistribution;
    private Map<String, Long> departmentDistribution;
    
    private BigDecimal averagePrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal totalValue;
    
    private Double averageSize;
    private Integer averageBedrooms;
    private Integer averageBathrooms;
    
    private Long propertiesThisMonth;
    private Long propertiesLastMonth;
    private Double growthRate;
    
    private Map<String, Object> additionalMetrics;
}