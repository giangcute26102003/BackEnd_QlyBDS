package com.example.datn_realeaste_crm.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final RolePermissionService rolePermissionService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null || !(permission instanceof String)) {
            return false;
        }

        String permissionName = (String) permission;
        if (authentication.getPrincipal() instanceof UserDetails) {
            return rolePermissionService.hasPermission((UserDetails) authentication.getPrincipal(), permissionName);
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
            String targetType, Object permission) {
        // Delegate to the first method
        return hasPermission(authentication, null, permission);
    }
}