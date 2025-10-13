package com.example.datn_realeaste_crm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    
    private Integer userId;
    private String email;
    private String name;
    private String selectedRole;
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
}