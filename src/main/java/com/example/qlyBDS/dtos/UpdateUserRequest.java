package com.example.qlyBDS.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    @JsonProperty("phone_number")
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("role_id")
    private Integer roleId;
}