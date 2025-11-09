package com.example.datn_realeaste_crm.config;

import com.example.datn_realeaste_crm.security.crypto.DeterministicHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

/**
 * Configuration for deterministic hashing (for searchable encrypted fields)
 * Hash key should be separate from encryption key and stored securely
 */
@Configuration
public class HashConfig {
    
    @Bean
    public DeterministicHasher deterministicHasher(@Value("${encryption.hash-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                "encryption.hash-key must be 32 bytes (encoded as base64)"
            );
        }
        return new DeterministicHasher(keyBytes);
    }
}

