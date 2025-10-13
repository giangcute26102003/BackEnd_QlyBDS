package com.example.datn_realeaste_crm.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerRequirementRequest {
    
    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    private Integer customerId;
    
    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String purpose;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Minimum budget must be greater than 0")
    private BigDecimal budgetMin;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum budget must be greater than 0")
    private BigDecimal budgetMax;
    
    @Size(max = 500, message = "Preferred location must not exceed 500 characters")
    private String preferredLocation;
    
    @Size(max = 100, message = "Property type must not exceed 100 characters")
    private String propertyType;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Minimum size must be greater than 0")
    private BigDecimal sizeMin;
    
    @Min(value = 0, message = "Number of bedrooms cannot be negative")
    @Max(value = 50, message = "Number of bedrooms cannot exceed 50")
    private Integer bedrooms;
    
    @Min(value = 0, message = "Number of bathrooms cannot be negative")
    @Max(value = 50, message = "Number of bathrooms cannot exceed 50")
    private Integer bathrooms;
    
    @Size(max = 1000, message = "Other preferences must not exceed 1000 characters")
    private String otherPreferences;
    
    // Custom validation method for budget range
    @AssertTrue(message = "Maximum budget must be greater than minimum budget")
    public boolean isBudgetRangeValid() {
        if (budgetMin == null || budgetMax == null) {
            return true; // Allow null values, other validators will handle them
        }
        return budgetMax.compareTo(budgetMin) >= 0;
    }
}