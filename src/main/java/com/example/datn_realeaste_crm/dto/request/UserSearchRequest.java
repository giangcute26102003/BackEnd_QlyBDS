package com.example.datn_realeaste_crm.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UserSearchRequest {
    
    private String name;
    private String email;
    private String phoneNumber;
    private Integer departmentId;
    private List<String> roles;
    private String roleName; // For single role filtering
    private Boolean isActive;
    private LocalDate createdFrom;
    private LocalDate createdTo;
    private String sortBy = "createdAt"; // name, email, createdAt, updatedAt
    private String sortDirection = "DESC"; // ASC, DESC
} 