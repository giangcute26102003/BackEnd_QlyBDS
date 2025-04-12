package com.example.qlyBDS.dtos;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FavoriteDTO {
    @NotNull(message = "User ID is required")
    @JsonProperty("userId")
    private Integer userId;

    @NotNull(message = "Property ID is required")
    @JsonProperty("property_id")
    private Integer propertyId;
}
