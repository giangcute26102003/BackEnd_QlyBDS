package com.example.qlyBDS.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePropertyRequest {
    private String addressProperty;
    private String propertyType;
    private BigDecimal size;
    private Integer floor;
    private String thumbnail;

    @Min(value = 0, message = "Bedrooms must be non-negative")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms must be non-negative")
    private Integer bathrooms;

    private String description;

    @DecimalMin(value = "0.0", message = "Price must be positive")
    private BigDecimal price;

    private String legalDocuments;
    private String availability;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneOwner;

    private Integer districtId;
    private List<String> imageUrls;
}