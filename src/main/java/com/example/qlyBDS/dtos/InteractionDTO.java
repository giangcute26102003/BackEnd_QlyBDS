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
public class InteractionDTO {
    @NotNull(message = "Customer ID is required")
    @JsonProperty("customer_id")
    private Integer customerId;

    @NotNull(message = "Property ID is required")
    @JsonProperty("property_id")
    private Integer propertyId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @Size(max = 1000, message = "Details must not exceed 1000 characters")
    private String details;
}