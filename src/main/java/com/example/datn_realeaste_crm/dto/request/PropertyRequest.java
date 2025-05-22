package com.example.datn_realeaste_crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PropertyRequest {

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Property type is required")
    private String propertyType;

    private BigDecimal size;

    private Integer floor;

    private String thumbnail;

    private Integer bedrooms;

    private Integer bathrooms;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    private BigDecimal price;

    private String legalDocuments;

    private String phoneOwner;

    @NotNull(message = "District ID is required")
    private Integer districtId;

    private Integer departmentId;

    private Integer userId;
}