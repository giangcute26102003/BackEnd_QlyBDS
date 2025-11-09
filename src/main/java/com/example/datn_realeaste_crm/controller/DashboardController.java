package com.example.datn_realeaste_crm.controller;


import com.example.datn_realeaste_crm.dto.response.DashboardStatisticsResponse;
import com.example.datn_realeaste_crm.dto.response.UserStatisticsResponse;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.UserRepository;
import com.example.datn_realeaste_crm.security.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {
    
    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final com.example.datn_realeaste_crm.security.crypto.DeterministicHasher deterministicHasher;
    
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<DashboardStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(dashboardService.getStatistics());
    }
    
    @GetMapping("/my-statistics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(dashboardService.getUserStatistics(currentUser.getUserId()));
    }

    /**
     * Lấy thông tin user hiện tại từ Authentication context
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in security context");
            throw new ResourceNotFoundException("No authenticated user found");
        }
        
        String email = authentication.getName();
        if (email == null || email.trim().isEmpty()) {
            log.error("No username found in authentication");
            throw new ResourceNotFoundException("No username found in authentication");
        }
        
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        byte[] emailHash = deterministicHasher.emailHash(normalizedEmail);
        User user = userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
        
        if (!user.getIsActive()) {
            log.error("User account is inactive: {}", email);
            throw new ResourceNotFoundException("User account is inactive");
        }
        
        return user;
    }
}