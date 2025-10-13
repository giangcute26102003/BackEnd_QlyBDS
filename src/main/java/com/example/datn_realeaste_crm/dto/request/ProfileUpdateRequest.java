package com.example.datn_realeaste_crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {
    
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;
    
    @Email(message = "Email must be valid")
    private String email;
    
    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phoneNumber;
    
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
    
    private LocalDate dob;
}