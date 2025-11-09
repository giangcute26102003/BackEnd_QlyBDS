package com.example.datn_realeaste_crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentStatisticsResponse {
    private Integer departmentId;
    private String departmentName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Properties statistics
    private Integer totalProperties;
    private Integer newProperties;
    private Integer soldProperties;
    private Integer activeListings;
    private Map<String, Integer> propertiesByType;
    private Map<String, Integer> propertiesByStatus;
    
    // Employee statistics
    private Integer totalEmployees;
    private Integer activeEmployees;
    
    // Financial statistics
    private BigDecimal totalRevenue;
    private BigDecimal averagePropertyPrice;
    private BigDecimal highestPropertyPrice;
    private BigDecimal lowestPropertyPrice;
    
    // Customer statistics
    private Integer totalCustomers;
    private Integer newCustomers;
    private Integer totalInteractions;
    
    // Performance metrics
    private Double conversionRate;
    private Double averageResponseTime;
}

