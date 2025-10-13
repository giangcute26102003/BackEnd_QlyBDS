package com.example.datn_realeaste_crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyPerformanceResponse {
    private Integer departmentId;
    private String departmentName;
    private Integer userId;
    private String userName;
    
    private LocalDateTime reportStartDate;
    private LocalDateTime reportEndDate;
    
    private Long totalProperties;
    private Long activeProperties;
    private Long soldProperties;
    private Long rentedProperties;
    
    private Long totalInteractions;
    private Long totalViewings;
    private Long totalInquiries;
    
    private Double conversionRate;
    private Double averageTimeToSell;
    private Double averageTimeToRent;
    
    private Map<String, Long> monthlyPerformance;
    private List<PropertyResponse> topPerformingProperties;
    private List<PropertyResponse> underPerformingProperties;
    
    private Map<String, Object> kpiMetrics;
}