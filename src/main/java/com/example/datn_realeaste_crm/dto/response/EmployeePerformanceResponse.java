package com.example.datn_realeaste_crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePerformanceResponse {
    private Integer userId;
    private String name;
    private String email;
    private String phoneNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Properties statistics
    private Integer totalProperties;
    private Integer newPropertiesAdded;
    private Integer soldProperties;
    private Integer activeProperties;
    
    // Customer statistics
    private Integer totalCustomers;
    private Integer newCustomersAcquired;
    private Integer totalInteractions;
    
    // Financial metrics
    private BigDecimal totalRevenue;
    private BigDecimal averageDealValue;
    
    // Performance metrics
    private Double conversionRate;
    private Double customerSatisfactionScore;
    private Integer averageResponseTimeHours;
    
    // Activity breakdown
    private Integer callsMade;
    private Integer meetingsHeld;
    private Integer emailsSent;
    
    // Recent activities
    private List<InteractionResponse> recentInteractions;
}

