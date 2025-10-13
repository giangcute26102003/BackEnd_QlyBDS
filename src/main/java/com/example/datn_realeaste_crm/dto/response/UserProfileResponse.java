package com.example.datn_realeaste_crm.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserProfileResponse {
    
    private Integer userId;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;
    
    private Boolean isActive;
    
    // Department info
    private Integer departmentId;
    private String departmentName;
    
    // Role info
    private Set<String> roles;
    private Set<String> permissions;
    
    // Statistics
    private Integer totalProperties;
    private Integer totalCustomers;
    private Integer totalInteractions;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;
}