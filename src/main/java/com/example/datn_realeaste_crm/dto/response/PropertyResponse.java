package com.example.datn_realeaste_crm.dto.response;

import com.example.datn_realeaste_crm.entity.AvailabilityStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PropertyResponse {
    
    private Integer id;
    private String address;
    private String propertyType;
    private BigDecimal size;
    private Integer floor;
    private String thumbnail;
    private Integer bedrooms;
    private Integer bathrooms;
    private String description;
    private BigDecimal price;
    private String legalDocuments;
    private AvailabilityStatus availability;
    private String availabilityText;
    private String phoneOwner;
    private Integer districtId;
    private String districtName;
    private Integer departmentId;
    private String departmentName;
    private Integer userId;
    private String userName;
    private List<PropertyImageResponse> images;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}