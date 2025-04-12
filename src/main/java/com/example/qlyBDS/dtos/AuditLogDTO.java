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
public class AuditLogDTO {
    private Integer logId;

    @JsonProperty("userId")
    private Integer userId;

    @NotBlank(message = "Action is required")
    private String action;

    @JsonProperty("entity_type")
    private String entityType;

    @JsonProperty("entity_id")
    private Integer entityId;

    private LocalDateTime timestamp;

    @JsonProperty("ip_address")
    private String ipAddress;
}