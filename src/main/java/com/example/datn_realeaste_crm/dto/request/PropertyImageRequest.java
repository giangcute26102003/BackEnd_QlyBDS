package com.example.datn_realeaste_crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PropertyImageRequest {
    
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
    
    private String originalFilename;
}