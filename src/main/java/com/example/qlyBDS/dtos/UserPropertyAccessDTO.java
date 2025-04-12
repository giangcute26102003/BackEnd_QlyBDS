package com.example.qlyBDS.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPropertyAccessDTO {
    private Integer id;

    @NotNull(message = "User ID is required")
    @JsonProperty("userId")
    private Integer userId;

    @NotNull(message = "Property ID is required")
    @JsonProperty("propertyId")
    private Integer propertyId;

    @JsonProperty("access_granted_at")
    private LocalDateTime accessGrantedAt;
}
