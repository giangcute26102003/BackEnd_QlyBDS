package com.example.datn_realeaste_crm.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for bulk uploading property images by URLs
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PropertyImageBulkRequest {
    
    @NotEmpty(message = "Image URLs list cannot be empty")
    private List<PropertyImageUrlData> images;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PropertyImageUrlData {
        private String imageUrl;
        private String originalFilename;
    }
}

