package com.example.qlyBDS.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertyDTO {
    @NotBlank(message = "Address is required")
    @JsonProperty("address_property")
    private String addressProperty;

    @NotBlank(message = "Property type is required")
    @JsonProperty("property_type")
    private String propertyType;

    @DecimalMin(value = "0.0", message = "Size must be positive")
    @DecimalMax(value = "10000.0", message = "Size must not exceed 10000")
    private BigDecimal size;

    @Min(value = 0, message = "Floor must be positive")
    private Integer floor;

    private String thumbnail;

    @Min(value = 0, message = "Bedrooms must be positive")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms must be positive")
    private Integer bathrooms;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be positive")
    private BigDecimal price;

    @JsonProperty("legal_documents")
    private String legalDocuments;

    private String availability;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    @JsonProperty("phone_owner")
    private String phoneOwner;

    @NotNull(message = "District ID is required")
    @JsonProperty("district_id")
    private Integer districtId;

//    @JsonProperty("image_urls")
//    private List<String> imageUrls;
}