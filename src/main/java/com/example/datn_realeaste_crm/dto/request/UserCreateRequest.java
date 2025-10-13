package com.example.datn_realeaste_crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserCreateRequest {
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phoneNumber;
    
    private String address;
    
    private LocalDate dob;
    
    @NotNull(message = "Department ID is required")
    private Integer departmentId;
    
    private Set<Integer> roleIds; // Roles to assign to the user
    
    private Boolean isActive = true;
}