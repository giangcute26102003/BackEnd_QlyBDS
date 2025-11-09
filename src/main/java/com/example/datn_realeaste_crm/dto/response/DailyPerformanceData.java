package com.example.datn_realeaste_crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyPerformanceData {
    private LocalDate date;
    private Integer newProperties;
    private Integer soldProperties;
    private Integer interactions;
    private Integer newCustomers;
    private BigDecimal revenue;
}

