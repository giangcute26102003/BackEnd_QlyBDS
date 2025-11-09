package com.example.datn_realeaste_crm.audit;

import com.example.datn_realeaste_crm.entity.AuditLog;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final com.example.datn_realeaste_crm.security.crypto.DeterministicHasher deterministicHasher;



    @Pointcut("@annotation(Auditable)")
    public void auditableMethod() {
    }
    
    @AfterReturning(pointcut = "auditableMethod()", returning = "result")
    public void logAuditAfterMethod(JoinPoint joinPoint, Object result) {
        try {
            // Get authentication details
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                log.warn("No authenticated user found for audit logging");
                return;
            }
            
            // Get method details
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Auditable auditable = method.getAnnotation(Auditable.class);
            
            // Create audit entry
            AuditLog auditLog = new AuditLog();
            auditLog.setUser(currentUser);
            auditLog.setAction(auditable.action());
            auditLog.setEntityType(auditable.entityType());
            
            // Try to extract entity ID if specified in annotation
            if (!auditable.entityIdParam().isEmpty()) {
                int paramIndex = findParameterIndexByName(signature, auditable.entityIdParam());
                if (paramIndex != -1 && joinPoint.getArgs().length > paramIndex) {
                    Object idValue = joinPoint.getArgs()[paramIndex];
                    if (idValue instanceof Integer) {
                        auditLog.setEntityId((Integer) idValue);
                    }
                }
            }
            
            // Save before value if provided
            if (auditable.logParams()) {
                auditLog.setPreviousValue(objectMapper.writeValueAsString(joinPoint.getArgs()));
            }
            
            // Save result if needed
            if (auditable.logResult() && result != null) {
                auditLog.setNewValue(objectMapper.writeValueAsString(result));
            }
            
            // Set timestamp and IP address
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setIpAddress(getClientIp());
            
            // Set department if available
            if (currentUser.getDepartment() != null) {
                auditLog.setDepartment(currentUser.getDepartment());
            }
            
            // Save audit log
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to log audit information", e);
        }
    }

//    private User getCurrentUser() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication != null && authentication.getPrincipal() instanceof User) {
//            return (User) authentication.getPrincipal();
//        }
//        return null;
//    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            } else if (principal instanceof String) {
                // Lấy username từ principal và tìm User tương ứng
                String username = (String) principal;
                String normalized = username == null ? null : username.trim().toLowerCase(java.util.Locale.ROOT);
                byte[] emailHash = deterministicHasher.emailHash(normalized);
                return userRepository.findByEmailHash(emailHash)
                        .orElse(null);
            }
        }
        return null;
    }

    private int findParameterIndexByName(MethodSignature signature, String paramName) {
        String[] parameterNames = signature.getParameterNames();
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName)) {
                return i;
            }
        }
        return -1;
    }
    
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xff = request.getHeader("X-Forwarded-For");
                return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("Failed to get client IP address", e);
        }
        return "unknown";
    }
}