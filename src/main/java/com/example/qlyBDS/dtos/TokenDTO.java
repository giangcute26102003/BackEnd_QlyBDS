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
public class TokenDTO {
    @NotBlank(message = "Token is required")
    private String token;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expiration_date")
    private LocalDateTime expirationDate;

    private Boolean revoked;

    private Boolean expired;

    @JsonProperty("userId")
    private Integer userId;
}