package com.example.datn_realeaste_crm.config;

import com.example.datn_realeaste_crm.security.CustomMethodSecurityExpressionHandler;
import com.example.datn_realeaste_crm.security.CustomPermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class MethodSecurityConfig {

    private final CustomMethodSecurityExpressionHandler customMethodSecurityExpressionHandler;
    private final CustomPermissionEvaluator customPermissionEvaluator;

    @Bean
    @Primary
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        customMethodSecurityExpressionHandler.setPermissionEvaluator(customPermissionEvaluator);
        return customMethodSecurityExpressionHandler;
    }
}
