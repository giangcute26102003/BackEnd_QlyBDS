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
public class EmployeeStatisticsSummary {
    private Integer userId;
    private String name;
    private String email;
    private String phoneNumber;
    
    private Integer totalProperties;
    private Integer activeProperties;
    private Integer soldProperties;
    
    private Integer totalCustomers;
    private Integer activeCustomers;
    
    private Integer totalInteractions;
    private Integer interactionsThisMonth;
    
    private BigDecimal totalRevenue;
    private BigDecimal revenueThisMonth;
    
    private Double conversionRate;
    private String status; // "Active", "Inactive"
}

