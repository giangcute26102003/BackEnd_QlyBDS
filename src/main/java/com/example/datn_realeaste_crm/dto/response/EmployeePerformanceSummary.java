package com.example.datn_realeaste_crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePerformanceSummary {
    private Integer userId;
    private String name;
    private String email;
    
    // Key metrics
    private Integer totalProperties;
    private Integer soldProperties;
    private Integer totalCustomers;
    private Integer totalInteractions;
    private BigDecimal totalRevenue;
    
    // Performance indicators
    private Double conversionRate;
    private Integer performanceScore;
    private String performanceLevel; // "Excellent", "Good", "Average", "Needs Improvement"
}

