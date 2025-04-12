package com.example.qlyBDS.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerRequirementsDTO {
    @NotNull(message = "Customer ID is required")
    @JsonProperty("customer_id")
    private Integer customerId;

    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String purpose;

    @DecimalMin(value = "0.0", message = "Minimum budget must be positive")
    @JsonProperty("budget_min")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0.0", message = "Maximum budget must be positive")
    @JsonProperty("budget_max")
    private BigDecimal budgetMax;

    @JsonProperty("preferred_location")
    private String preferredLocation;

    @JsonProperty("property_type")
    private String propertyType;

    @DecimalMin(value = "0.0", message = "Minimum size must be positive")
    @JsonProperty("size_min")
    private BigDecimal sizeMin;

    @Min(value = 0, message = "Bedrooms must be positive")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms must be positive")
    private Integer bathrooms;

    @JsonProperty("other_preferences")
    private String otherPreferences;
}