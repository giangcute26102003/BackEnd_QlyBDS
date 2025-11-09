package com.example.datn_realeaste_crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ManagerDashboardResponse {
    private Integer departmentId;
    private String departmentName;
    private Integer totalEmployees;
    private Integer activeEmployees;
    private Integer totalProperties;
    private Integer availableProperties;
    private Integer soldProperties;
    private Integer pendingProperties;
    private Integer totalCustomers;
    private Integer totalInteractionsThisMonth;
    private Integer newPropertiesThisMonth;
    private Double averagePropertyPrice;
    private Double totalRevenueThisMonth;
}

