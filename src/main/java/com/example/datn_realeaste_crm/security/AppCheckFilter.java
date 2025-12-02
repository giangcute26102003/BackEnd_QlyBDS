package com.example.datn_realeaste_crm.security;

import com.google.firebase.FirebaseApp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * App Check Filter
 * Verify Firebase App Check token để đảm bảo request đến từ Android app chính thống
 */
@Component
@RequiredArgsConstructor
public class AppCheckFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AppCheckFilter.class);
    private static final String APP_CHECK_HEADER = "X-Firebase-AppCheck";
    
    private final AppCheckVerificationService verificationService;
    
    // List các endpoint không cần App Check (public endpoints)
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/health",
            "/api/public",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        logger.debug("App Check Filter - Processing request: {}", requestPath);

        // Lấy App Check token từ header
        String appCheckToken = request.getHeader(APP_CHECK_HEADER);

        // Nếu thiếu token
        if (appCheckToken == null || appCheckToken.trim().isEmpty()) {
            logger.warn("Request thiếu App Check token - Path: {}", requestPath);
            sendUnauthorizedResponse(response, "Missing App Check token");
            return;
        }

        // Kiểm tra xem Firebase đã được khởi tạo chưa
        if (FirebaseApp.getApps().isEmpty()) {
            logger.error("Firebase chưa được khởi tạo - REJECT request - Path: {}", requestPath);
            logger.error("Vui lòng copy file firebase-service-account.json vào src/main/resources/");
            sendUnauthorizedResponse(response, "App Check not configured");
            return;
        }

        try {
            // Verify token với Firebase
            logger.debug("Đang verify App Check token...");
            
            boolean isValid = verificationService.verifyToken(appCheckToken);
            
            if (isValid) {
                //  Token hợp lệ - cho phép request tiếp tục
                logger.debug("App Check token hợp lệ - Path: {}", requestPath);
                filterChain.doFilter(request, response);
            } else {
                logger.error(" App Check token không hợp lệ - Path: {}", requestPath);
                sendUnauthorizedResponse(response, "Invalid App Check token");
            }

        } catch (Exception e) {
            // Lỗi khi verify token
            logger.error("App Check verification thất bại - Path: {}, Error: {}",
                    requestPath, e.getMessage());
            sendUnauthorizedResponse(response, "Invalid App Check token");
        }
    }

    /**
     * Skip App Check cho một số endpoint công khai
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        boolean shouldSkip = EXCLUDED_PATHS.stream()
                .anyMatch(path::startsWith);
        
        if (shouldSkip) {
            logger.debug("Skipping App Check for public endpoint: {}", path);
        }
        
        return !shouldSkip;
    }

    /**
     * Gửi response 401 Unauthorized
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
                "{\"error\": \"%s\", \"message\": \"Request từ nguồn không tin cậy\"}",
                message
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}

