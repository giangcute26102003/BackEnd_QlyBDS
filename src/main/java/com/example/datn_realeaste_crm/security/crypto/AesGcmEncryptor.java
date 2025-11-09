package com.example.datn_realeaste_crm.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-GCM encryptor for field-level encryption
 * Each encryption uses a random IV for security
 */
public class AesGcmEncryptor {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN = 12;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmEncryptor(SecretKey key) {
        this.key = key;
    }

    /**
     * Encrypt plaintext with AES-GCM
     * @param plaintext data to encrypt
     * @param aad additional authenticated data (optional)
     * @return IV + ciphertext + tag concatenated
     */
    public byte[] encrypt(byte[] plaintext, byte[] aad) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            if (aad != null) cipher.updateAAD(aad);
            byte[] cipherText = cipher.doFinal(plaintext);
            
            // Concatenate IV + cipherText
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * Decrypt AES-GCM encrypted data
     * @param ivPlusCipherText IV + ciphertext + tag
     * @param aad additional authenticated data (must match encryption)
     * @return plaintext
     */
    public byte[] decrypt(byte[] ivPlusCipherText, byte[] aad) {
        if (ivPlusCipherText == null) return null;
        try {
            byte[] iv = Arrays.copyOfRange(ivPlusCipherText, 0, IV_LEN);
            byte[] cipherText = Arrays.copyOfRange(ivPlusCipherText, IV_LEN, ivPlusCipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            if (aad != null) cipher.updateAAD(aad);
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}

