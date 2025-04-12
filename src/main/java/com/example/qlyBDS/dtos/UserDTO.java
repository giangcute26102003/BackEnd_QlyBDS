package com.example.qlyBDS.dtos;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    private String address;

    @NotBlank(message = "Password cannot be blank")
    private String password;


    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @NotNull(message = "Role ID is required")
    @JsonProperty("role_id")
    private Integer roleId;

    @NotNull(message = "Active status is required")
    @JsonProperty("is_active")
    private Boolean isActive;
}