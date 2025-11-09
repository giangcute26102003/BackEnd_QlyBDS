package com.example.datn_realeaste_crm.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    
    private Integer id;
    private Integer userId;
    private String userName;
    private Integer propertyId;
    private String propertyAddress;
    private String comment;
    private String action;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}