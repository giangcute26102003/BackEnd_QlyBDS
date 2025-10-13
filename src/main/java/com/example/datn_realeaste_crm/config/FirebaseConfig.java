package com.example.datn_realeaste_crm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * Firebase Configuration
 * Khởi tạo Firebase Admin SDK để sử dụng App Check verification
 */
@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final String SERVICE_ACCOUNT_FILE = "datn-2ea3f-firebase-adminsdk-fbsvc-ac9cbda277.json";

    @PostConstruct
    public void initialize() {
        try {
            // Kiểm tra xem Firebase đã được khởi tạo chưa
            if (!FirebaseApp.getApps().isEmpty()) {
                logger.info("Firebase đã được khởi tạo trước đó");
                return;
            }

            // Đọc service account JSON từ resources
            ClassPathResource serviceAccount = new ClassPathResource(SERVICE_ACCOUNT_FILE);
            
            if (!serviceAccount.exists()) {
                logger.warn("⚠️ Không tìm thấy file {}", SERVICE_ACCOUNT_FILE);
                logger.warn("Firebase App Check sẽ KHÔNG hoạt động!");
                logger.warn("Vui lòng tải service account JSON từ Firebase Console");
                return;
            }

            // Khởi tạo Firebase với credentials
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            serviceAccount.getInputStream()))
                    .build();

            FirebaseApp.initializeApp(options);
            logger.info("✅ Firebase Admin SDK đã được khởi tạo thành công");
            logger.info("✅ App Check verification đã sẵn sàng");

        } catch (IOException e) {
            logger.error("❌ Lỗi khi đọc file {}: {}", SERVICE_ACCOUNT_FILE, e.getMessage());
            logger.error("Firebase App Check sẽ KHÔNG hoạt động!");
        } catch (Exception e) {
            logger.error("❌ Lỗi khi khởi tạo Firebase Admin SDK: {}", e.getMessage());
            logger.error("Firebase App Check sẽ KHÔNG hoạt động!");
        }
    }
}

