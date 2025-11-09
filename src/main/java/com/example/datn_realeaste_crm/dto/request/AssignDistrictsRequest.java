package com.example.datn_realeaste_crm.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO cho việc phân công districts cho employee
 * Manager sẽ assign các districts mà employee có quyền xem/quản lý properties
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssignDistrictsRequest {
    
    @NotEmpty(message = "District IDs list cannot be empty")
    private List<@NotNull(message = "District ID cannot be null") Integer> districtIds;
}

