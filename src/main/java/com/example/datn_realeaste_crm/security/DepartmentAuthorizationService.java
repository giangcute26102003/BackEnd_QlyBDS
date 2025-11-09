package com.example.datn_realeaste_crm.security;

import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service để xử lý logic phân quyền theo department
 * - Admin: có thể thao tác với tất cả users
 * - Manager: chỉ có thể thao tác với users trong cùng department
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentAuthorizationService {

    private final UserRepository userRepository;
    private final com.example.datn_realeaste_crm.security.crypto.DeterministicHasher deterministicHasher;

    /**
     * Kiểm tra xem user hiện tại có quyền truy cập user khác không
     * @param targetUserId ID của user cần kiểm tra quyền truy cập
     * @return true nếu có quyền, false nếu không
     */
    public boolean canAccessUser(Integer targetUserId) {
        try {
            User currentUser = getCurrentUser();
            
            // Admin có thể truy cập tất cả users
            if (hasRole("ROLE_ADMIN")) {
                log.debug("Admin {} can access user {}", currentUser.getEmail(), targetUserId);
                return true;
            }
            
            // User có thể truy cập chính mình
            if (currentUser.getUserId().equals(targetUserId)) {
                log.debug("User {} can access their own profile", currentUser.getEmail());
                return true;
            }
            
            // Manager chỉ có thể truy cập users trong cùng department
            if (hasRole("ROLE_MANAGER")) {
                return canManagerAccessUser(currentUser, targetUserId);
            }
            
            // Các role khác không có quyền truy cập users khác
            log.debug("User {} does not have permission to access user {}", currentUser.getEmail(), targetUserId);
            return false;
            
        } catch (Exception e) {
            log.error("Error checking user access permission", e);
            return false;
        }
    }

    /**
     * Kiểm tra xem user hiện tại có quyền chỉnh sửa user khác không
     * @param targetUserId ID của user cần kiểm tra quyền chỉnh sửa
     * @return true nếu có quyền, false nếu không
     */
    public boolean canModifyUser(Integer targetUserId) {
        try {
            User currentUser = getCurrentUser();
            
            // Admin có thể chỉnh sửa tất cả users
            if (hasRole("ROLE_ADMIN")) {
                log.debug("Admin {} can modify user {}", currentUser.getEmail(), targetUserId);
                return true;
            }
            
            // User có thể chỉnh sửa profile của chính mình (limited)
            if (currentUser.getUserId().equals(targetUserId)) {
                log.debug("User {} can modify their own profile", currentUser.getEmail());
                return true;
            }
            
            // Manager có thể chỉnh sửa users trong cùng department
            if (hasRole("ROLE_MANAGER")) {
                return canManagerAccessUser(currentUser, targetUserId);
            }
            
            // Các role khác không có quyền chỉnh sửa users khác
            log.debug("User {} does not have permission to modify user {}", currentUser.getEmail(), targetUserId);
            return false;
            
        } catch (Exception e) {
            log.error("Error checking user modification permission", e);
            return false;
        }
    }

    /**
     * Kiểm tra xem user hiện tại có quyền xóa user khác không
     * @param targetUserId ID của user cần kiểm tra quyền xóa
     * @return true nếu có quyền, false nếu không
     */
    public boolean canDeleteUser(Integer targetUserId) {
        try {
            User currentUser = getCurrentUser();
            
            // Chỉ Admin mới có quyền xóa users
            if (hasRole("ROLE_ADMIN")) {
                // Admin không thể xóa chính mình
                if (currentUser.getUserId().equals(targetUserId)) {
                    log.warn("Admin {} attempted to delete their own account", currentUser.getEmail());
                    return false;
                }
                log.debug("Admin {} can delete user {}", currentUser.getEmail(), targetUserId);
                return true;
            }
            
            // Các role khác không có quyền xóa users
            log.debug("User {} does not have permission to delete user {}", currentUser.getEmail(), targetUserId);
            return false;
            
        } catch (Exception e) {
            log.error("Error checking user deletion permission", e);
            return false;
        }
    }

    /**
     * Kiểm tra xem Manager có thể truy cập user trong cùng department không
     * @param manager Manager user
     * @param targetUserId ID của user cần kiểm tra
     * @return true nếu cùng department, false nếu không
     */
    private boolean canManagerAccessUser(User manager, Integer targetUserId) {
        try {
            // Manager phải thuộc về một department
            if (manager.getDepartment() == null) {
                log.warn("Manager {} does not belong to any department", manager.getEmail());
                return false;
            }
            
            // Lấy thông tin target user
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Target user not found with id: " + targetUserId));
            
            // Target user phải thuộc về department
            if (targetUser.getDepartment() == null) {
                log.debug("Target user {} does not belong to any department", targetUser.getEmail());
                return false;
            }
            
            // Kiểm tra cùng department
            boolean sameDepart = manager.getDepartment().getDepartmentId()
                    .equals(targetUser.getDepartment().getDepartmentId());
            
            if (sameDepart) {
                log.debug("Manager {} can access user {} (same department: {})", 
                    manager.getEmail(), targetUser.getEmail(), manager.getDepartment().getName());
            } else {
                log.debug("Manager {} cannot access user {} (different departments)", 
                    manager.getEmail(), targetUser.getEmail());
            }
            
            return sameDepart;
            
        } catch (Exception e) {
            log.error("Error checking manager department access", e);
            return false;
        }
    }

    /**
     * Lấy danh sách user IDs mà user hiện tại có quyền truy cập
     * @return Set of user IDs
     */
    public java.util.Set<Integer> getAccessibleUserIds() {
        try {
            User currentUser = getCurrentUser();
            
            // Admin có thể truy cập tất cả users
            if (hasRole("ROLE_ADMIN")) {
                return userRepository.findAll().stream()
                        .map(User::getUserId)
                        .collect(java.util.stream.Collectors.toSet());
            }
            
            // Manager có thể truy cập users trong cùng department
            if (hasRole("ROLE_MANAGER") && currentUser.getDepartment() != null) {
                return userRepository.findByDepartmentDepartmentId(currentUser.getDepartment().getDepartmentId())
                        .stream()
                        .map(User::getUserId)
                        .collect(java.util.stream.Collectors.toSet());
            }
            
            // Các role khác chỉ có thể truy cập chính mình
            return java.util.Set.of(currentUser.getUserId());
            
        } catch (Exception e) {
            log.error("Error getting accessible user IDs", e);
            return java.util.Set.of();
        }
    }

    /**
     * Lấy thông tin user hiện tại từ Authentication context
     * @return User entity của user hiện tại
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }
        
        String email = authentication.getName();
        if (email == null || email.trim().isEmpty()) {
            throw new ResourceNotFoundException("No username found in authentication");
        }
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        byte[] emailHash = deterministicHasher.emailHash(normalizedEmail);
        
        User user = userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        if (!user.getIsActive()) {
            throw new ResourceNotFoundException("User account is inactive");
        }
        
        return user;
    }

    /**
     * Kiểm tra xem user hiện tại có thuộc về department cụ thể không
     * @param departmentId ID của department cần kiểm tra
     * @return true nếu thuộc về department, false nếu không
     */
    public boolean belongsToDepartment(Integer departmentId) {
        try {
            User currentUser = getCurrentUser();
            
            if (currentUser.getDepartment() == null) {
                log.debug("User {} does not belong to any department", currentUser.getEmail());
                return false;
            }
            
            boolean belongs = currentUser.getDepartment().getDepartmentId().equals(departmentId);
            log.debug("User {} {} to department ID: {}", 
                currentUser.getEmail(), belongs ? "belongs" : "does not belong", departmentId);
            
            return belongs;
        } catch (Exception e) {
            log.error("Error checking department membership", e);
            return false;
        }
    }

    /**
     * Kiểm tra xem user hiện tại có role cụ thể không
     * @param role Role cần kiểm tra (ví dụ: "ROLE_ADMIN", "ROLE_MANAGER")
     * @return true nếu có role, false nếu không
     */
    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
} 