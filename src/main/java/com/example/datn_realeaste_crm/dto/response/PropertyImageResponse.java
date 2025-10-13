package com.example.datn_realeaste_crm.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PropertyImageResponse {
    
    private Integer id;
    private Integer propertyId;
    private String imageUrl;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
}