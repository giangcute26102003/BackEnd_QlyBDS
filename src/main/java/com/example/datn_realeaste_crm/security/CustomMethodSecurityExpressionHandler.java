package com.example.datn_realeaste_crm.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    private final RolePermissionService rolePermissionService;
    private final PropertyAuthorizationService propertyAuthorizationService;

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication,
            org.aopalliance.intercept.MethodInvocation invocation) {
        CustomSecurityExpressionRoot root = new CustomSecurityExpressionRoot(authentication, rolePermissionService);
        root.setPropertyAuthorizationService(propertyAuthorizationService); // inject
        root.setThis(invocation.getThis());
        return root;
    }
}
