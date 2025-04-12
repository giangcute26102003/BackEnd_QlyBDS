package com.example.qlyBDS.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DistrictDTO {
    private Integer id;

    @NotBlank(message = "District name is required")
    @Size(min = 2, max = 50, message = "District name must be between 2 and 50 characters")
    private String name;
}