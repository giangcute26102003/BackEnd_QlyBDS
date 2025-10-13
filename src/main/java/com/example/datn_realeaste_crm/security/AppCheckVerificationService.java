package com.example.datn_realeaste_crm.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service để verify Firebase App Check token
 * Sử dụng REST API vì Firebase Admin SDK Java chưa hỗ trợ trực tiếp
 */
@Service
public class AppCheckVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(AppCheckVerificationService.class);
    private static final String PROJECT_ID = "datn-2ea3f";
    private static final String VERIFY_URL = 
            "https://firebaseappcheck.googleapis.com/v1/projects/" + PROJECT_ID + ":verifyAppCheckToken";
    
    // Flag để enable/disable strict verification (cho development)
    private static final boolean STRICT_VERIFICATION = false; // Set true cho production
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Verify App Check token
     * 
     * @param token App Check token từ Android app
     * @return true nếu token hợp lệ
     */
    public boolean verifyToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("App Check token is null or empty");
            return false;
        }

        // Development mode: Accept any token format
        if (!STRICT_VERIFICATION) {
            logger.warn("⚠️ DEVELOPMENT MODE: App Check verification is relaxed");
            logger.warn("⚠️ Token length: {} - Accepting for development", token.length());
            return token.length() > 10; // Basic check
        }

        // Production mode: Verify with Firebase
        return verifyWithFirebase(token);
    }

    /**
     * Verify token với Firebase REST API
     */
    private boolean verifyWithFirebase(String token) {
        HttpURLConnection connection = null;
        
        try {
            logger.debug("Verifying App Check token with Firebase API...");
            
            // Tạo request payload
            String jsonPayload = String.format("{\"app_check_token\": \"%s\"}", token);
            
            // Tạo connection
            URL url = new URL(VERIFY_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Gửi request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Đọc response
            int responseCode = connection.getResponseCode();
            logger.debug("Firebase API response code: {}", responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    // Parse response JSON
                    JsonNode jsonResponse = objectMapper.readTree(response.toString());
                    boolean isValid = jsonResponse.has("token") || jsonResponse.has("alreadyConsumed");
                    
                    if (isValid) {
                        logger.info("✅ App Check token verified successfully");
                    } else {
                        logger.warn("❌ App Check token verification failed");
                    }
                    
                    return isValid;
                }
            } else {
                // Đọc error response
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    
                    logger.error("❌ Firebase API error ({}): {}", responseCode, errorResponse.toString());
                }
                
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ Error verifying App Check token: {}", e.getMessage(), e);
            return false;
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Decode JWT token (không verify signature - chỉ để debug)
     */
    public void debugToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(
                        java.util.Base64.getUrlDecoder().decode(parts[1]),
                        StandardCharsets.UTF_8
                );
                logger.debug("Token payload: {}", payload);
            }
        } catch (Exception e) {
            logger.debug("Could not decode token: {}", e.getMessage());
        }
    }
}

