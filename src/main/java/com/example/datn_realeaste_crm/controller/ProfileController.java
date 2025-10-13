package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.ChangePasswordRequest;
import com.example.datn_realeaste_crm.dto.request.ProfileUpdateRequest;
import com.example.datn_realeaste_crm.dto.response.UserActivityResponse;
import com.example.datn_realeaste_crm.dto.response.UserProfileResponse;
import com.example.datn_realeaste_crm.dto.response.UserStatisticsResponse;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.UserRepository;
import com.example.datn_realeaste_crm.service.UserService;
import com.example.datn_realeaste_crm.security.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profile", description = "User profile management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final DashboardService dashboardService;

    /**
     * Lấy thông tin profile của user hiện tại
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        try {
            User currentUser = getCurrentUser();
            log.info("Getting profile for user: {}", currentUser.getEmail());
            
            UserProfileResponse profile = userService.getCurrentUserProfile(currentUser.getUserId());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error getting current user profile", e);
            throw e;
        }
    }

    /**
     * Cập nhật profile của user hiện tại
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "UPDATE_PROFILE", entityType = "User", logResult = true)
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @Valid @RequestBody ProfileUpdateRequest request) {
        try {
            User currentUser = getCurrentUser();
            log.info("Updating profile for user: {}", currentUser.getEmail());
            
            UserProfileResponse updatedProfile = userService.updateCurrentUserProfile(currentUser.getUserId(), request);
            log.info("Successfully updated profile for user: {}", currentUser.getEmail());
            
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            log.error("Error updating current user profile", e);
            throw e;
        }
    }

    /**
     * Đổi mật khẩu cho user hiện tại
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "CHANGE_PASSWORD", entityType = "User")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            User currentUser = getCurrentUser();
            log.info("Changing password for user: {}", currentUser.getEmail());
            
            userService.changeCurrentUserPassword(currentUser.getUserId(), request);
            log.info("Successfully changed password for user: {}", currentUser.getEmail());
            
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            log.error("Error changing password for current user", e);
            throw e;
        }
    }

    /**
     * Lấy activity log của user hiện tại
     */
    @GetMapping("/activity-log")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UserActivityResponse>> getCurrentUserActivityLog(Pageable pageable) {
        try {
            User currentUser = getCurrentUser();
            log.debug("Getting activity log for user: {}", currentUser.getEmail());
            
            Page<UserActivityResponse> activityLog = userService.getCurrentUserActivityLog(currentUser.getUserId(), pageable);
            return ResponseEntity.ok(activityLog);
        } catch (Exception e) {
            log.error("Error getting current user activity log", e);
            throw e;
        }
    }

    /**
     * Lấy danh sách roles của user hiện tại
     */
    @GetMapping("/roles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUserRoles() {
        try {
            User currentUser = getCurrentUser();
            log.debug("Getting roles for user: {}", currentUser.getEmail());
            
            return ResponseEntity.ok(userService.getUserRoles(currentUser.getUserId()));
        } catch (Exception e) {
            log.error("Error getting current user roles", e);
            throw e;
        }
    }

    /**
     * Lấy thống kê cá nhân của user hiện tại
     */
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserStatisticsResponse> getCurrentUserStatistics() {
        try {
            User currentUser = getCurrentUser();
            log.debug("Getting statistics for user: {}", currentUser.getEmail());
            
            UserStatisticsResponse statistics = dashboardService.getUserStatistics(currentUser.getUserId());
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error getting current user statistics", e);
            throw e;
        }
    }

    /**
     * Lấy thông tin user hiện tại từ Authentication context
     * @return User entity của user hiện tại
     * @throws ResourceNotFoundException nếu không tìm thấy user
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
        
        User user = userRepository.findByEmail(email)
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