package com.example.datn_realeaste_crm.security;

import java.security.SecureRandom;
import java.util.Base64;

public class GenerateKeys {
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] masterKey = new byte[32];
        byte[] hashKey = new byte[32];
        random.nextBytes(masterKey);
        random.nextBytes(hashKey);

        System.out.println("ENCRYPTION_MASTER_KEY_BASE64=" + Base64.getEncoder().encodeToString(masterKey));
        System.out.println("ENCRYPTION_HASH_KEY_BASE64=" + Base64.getEncoder().encodeToString(hashKey));
    }
}
