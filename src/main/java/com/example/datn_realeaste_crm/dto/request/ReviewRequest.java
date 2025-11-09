package com.example.datn_realeaste_crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    
    @NotNull(message = "Property ID is required")
    private Integer propertyId;
    
    @NotBlank(message = "Comment is required")
    private String comment;
    
    private String action;
}