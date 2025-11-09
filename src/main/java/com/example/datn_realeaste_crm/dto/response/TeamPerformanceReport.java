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
public class TeamPerformanceReport {
    private Integer departmentId;
    private String departmentName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Overall team metrics
    private Integer totalEmployees;
    private BigDecimal totalRevenue;
    private Integer totalPropertiesSold;
    private Integer totalNewProperties;
    private Integer totalCustomersAcquired;
    private Integer totalInteractions;
    
    // Performance metrics
    private Double averageConversionRate;
    private Double teamProductivity;
    private BigDecimal revenuePerEmployee;
    
    // Individual employee performances
    private List<EmployeePerformanceSummary> employeePerformances;
    
    // Trends
    private List<DailyPerformanceData> dailyPerformance;
}

