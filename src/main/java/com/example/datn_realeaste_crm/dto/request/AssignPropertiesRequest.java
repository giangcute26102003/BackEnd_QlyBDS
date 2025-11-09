package com.example.datn_realeaste_crm.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssignPropertiesRequest {
    
    @NotEmpty(message = "Property IDs list cannot be empty")
    private List<@NotNull(message = "Property ID cannot be null") Integer> propertyIds;
}

