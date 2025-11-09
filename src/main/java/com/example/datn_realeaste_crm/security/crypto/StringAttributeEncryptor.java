package com.example.datn_realeaste_crm.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JPA AttributeConverter for automatic String field encryption
 * Uses AES-GCM for confidentiality and integrity
 */
@Converter
@Component
public class StringAttributeEncryptor implements AttributeConverter<String, byte[]> {

    private static AesGcmEncryptor staticEncryptor;

    @Autowired
    public void setKey(SecretKey dataEncryptionKey) {
        // JPA instantiates converters directly, so we use static injection
        StringAttributeEncryptor.staticEncryptor = new AesGcmEncryptor(dataEncryptionKey);
    }

    @Override
    public byte[] convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        return staticEncryptor.encrypt(attribute.getBytes(StandardCharsets.UTF_8), null);
    }

    @Override
    public String convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) return null;
        byte[] plain = staticEncryptor.decrypt(dbData, null);
        return new String(plain, StandardCharsets.UTF_8);
    }
}

