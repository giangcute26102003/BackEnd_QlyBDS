package com.example.datn_realeaste_crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PropertiesStatusReport {
    private Integer departmentId;
    private String departmentName;
    
    private Integer totalProperties;
    private Integer availableProperties;
    private Integer soldProperties;
    private Integer pendingProperties;
    private Integer unavailableProperties;
    
    // Properties by type
    private Map<String, Integer> propertiesByType;
    
    // Properties by status breakdown
    private Map<String, Integer> propertiesByStatus;
    
    // Properties by district
    private Map<String, Integer> propertiesByDistrict;
    
    // Properties by assigned user
    private Map<String, Integer> propertiesByUser;
}

