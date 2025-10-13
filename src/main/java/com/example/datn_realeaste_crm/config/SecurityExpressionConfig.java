package com.example.datn_realeaste_crm.config;

import com.example.datn_realeaste_crm.security.PropertyAuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure beans for security expressions
 */
@Configuration
public class SecurityExpressionConfig {

    /**
     * Register the PropertyAuthorizationService as a bean to be used in SpEL
     * expressions
     * 
     * @param propertyAuthorizationService The service to register
     * @return The same service instance
     */
    @Bean("propertyAuth")
    public PropertyAuthorizationService propertyAuthorizationService(
            PropertyAuthorizationService propertyAuthorizationService) {
        return propertyAuthorizationService;
    }
}