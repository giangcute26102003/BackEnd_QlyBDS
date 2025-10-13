package com.example.datn_realeaste_crm.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DistrictAccessRequest {
    
    @NotNull(message = "User ID is required")
    private Integer userId;
    
    @NotNull(message = "District ID is required")
    private Integer districtId;
}
