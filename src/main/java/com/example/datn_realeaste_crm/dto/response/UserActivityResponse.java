package com.example.datn_realeaste_crm.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserActivityResponse {
    
    private Integer logId;
    private String action;
    private String entityType;
    private Integer entityId;
    private String description;
    private String ipAddress;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    // Optional: Include changed values for detailed audit
    private String previousValue;
    private String newValue;
}
