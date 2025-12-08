package com.example.datn_realeaste_crm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class UserRoleId implements Serializable {
    
    @Column(name = "user_id")
    private Integer userId;
    
    @Column(name = "role_id")
    private Integer roleId;
}
