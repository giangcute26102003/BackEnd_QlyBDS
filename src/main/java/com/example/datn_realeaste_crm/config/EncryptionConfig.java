package com.example.datn_realeaste_crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Configuration for field-level encryption
 * Master key should be stored in secure secret manager (AWS Secrets Manager, HashiCorp Vault, etc.)
 */
@Configuration
public class EncryptionConfig {

    @Bean
    public SecretKey dataEncryptionKey(@Value("${encryption.master-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                "encryption.master-key must be 32 bytes (256-bit AES key encoded as base64)"
            );
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}

