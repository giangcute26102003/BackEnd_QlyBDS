package com.example.datn_realeaste_crm.security.crypto;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * HMAC-SHA-256 hasher for searchable encrypted fields
 * Provides deterministic hash for exact-match lookups
 */
public class DeterministicHasher {
    private final SecretKey hmacKey;

    public DeterministicHasher(byte[] key) {
        this.hmacKey = new SecretKeySpec(key, "HmacSHA256");
    }

    /**
     * Generate deterministic hash for email (normalized: trim + lowercase)
     */
    public byte[] emailHash(String email) {
        if (email == null) return null;
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return hmac(normalized);
    }

    /**
     * Generate deterministic hash for phone (normalized: digits only)
     */
    public byte[] phoneHash(String phone) {
        if (phone == null) return null;
        String normalized = phone.replaceAll("\\D", ""); // remove non-digits
        return hmac(normalized);
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }
}

