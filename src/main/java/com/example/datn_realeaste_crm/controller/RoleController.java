package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.RoleAssignmentRequest;
import com.example.datn_realeaste_crm.dto.request.RoleRequest;
import com.example.datn_realeaste_crm.dto.request.UserSearchRequest;
import com.example.datn_realeaste_crm.dto.response.RoleResponse;
import com.example.datn_realeaste_crm.dto.response.UserResponse;
import com.example.datn_realeaste_crm.security.DepartmentAuthorizationService;
import com.example.datn_realeaste_crm.service.RoleService;
import com.example.datn_realeaste_crm.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Role Management", description = "Role and permission management APIs")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;
    private final DepartmentAuthorizationService departmentAuthService;

    // ==================== ROLE MANAGEMENT ====================

    /**
     * Lấy danh sách tất cả roles - Chỉ Admin
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        log.info("Admin requesting all roles");
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * Lấy thông tin role theo ID - Chỉ Admin
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Integer id) {
        log.info("Admin requesting role with ID: {}", id);
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    /**
     * Tạo role mới - Chỉ Admin
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "CREATE_ROLE", entityType = "Role", logResult = true)
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest roleRequest) {
        log.info("Admin creating new role: {}", roleRequest.getRoleName());
        return new ResponseEntity<>(roleService.createRole(roleRequest), HttpStatus.CREATED);
    }

    /**
     * Cập nhật role - Chỉ Admin
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "UPDATE_ROLE", entityType = "Role", entityIdParam = "id", logParams = true)
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Integer id,
            @Valid @RequestBody RoleRequest roleRequest) {
        log.info("Admin updating role ID: {} with data: {}", id, roleRequest.getRoleName());
        return ResponseEntity.ok(roleService.updateRole(id, roleRequest));
    }

    /**
     * Xóa role - Chỉ Admin
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Auditable(action = "DELETE_ROLE", entityType = "Role", entityIdParam = "id")
    public ResponseEntity<Void> deleteRole(@PathVariable Integer id) {
        log.info("Admin deleting role ID: {}", id);
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cập nhật permissions của role - Chỉ Admin
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "UPDATE_ROLE_PERMISSIONS", entityType = "Role", entityIdParam = "id", logParams = true)
    public ResponseEntity<RoleResponse> updateRolePermissions(
            @PathVariable Integer id,
            @RequestBody Set<Integer> permissionIds) {
        log.info("Admin updating permissions for role ID: {} with {} permissions", id, permissionIds.size());
        return ResponseEntity.ok(roleService.updateRolePermissions(id, permissionIds));
    }

    // ==================== USER MANAGEMENT ====================

    /**
     * Lấy danh sách users với phân quyền theo role
     * - Admin: có thể xem tất cả users
     * - Manager: chỉ xem users trong department của mình
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            Pageable pageable) {
        
        try {
            log.info("User requesting users list with filters - dept: {}, active: {}, email: {}, name: {}", 
                    departmentId, isActive, email, name);
            
            // Tạo search request
            UserSearchRequest searchRequest = new UserSearchRequest();
            searchRequest.setDepartmentId(departmentId);
            searchRequest.setIsActive(isActive);
            searchRequest.setEmail(email);
            searchRequest.setName(name);
            
            Page<UserResponse> users = userService.searchUsersWithAuthorization(searchRequest, pageable);
            log.info("Returning {} users", users.getTotalElements());
            
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error getting users list", e);
            throw e;
        }
    }

    /**
     * Lấy thông tin user theo ID với phân quyền
     * - Admin: có thể xem tất cả users
     * - Manager: chỉ xem users trong department của mình
     * - User: chỉ xem thông tin của chính mình
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or @departmentAuthorizationService.canAccessUser(#userId)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer userId) {
        try {
            log.info("User requesting user details for ID: {}", userId);
            
            // Kiểm tra quyền truy cập
            if (!departmentAuthService.canAccessUser(userId)) {
                log.warn("User does not have permission to access user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            UserResponse user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error getting user by ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Gán role cho user - Chỉ Admin và Manager (trong department)
     */
    @PostMapping("/users/{userId}/assign-role")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @departmentAuthorizationService.canModifyUser(#userId))")
    @Auditable(action = "ASSIGN_USER_ROLE", entityType = "UserRole", entityIdParam = "userId", logParams = true)
    public ResponseEntity<String> assignRoleToUser(
            @PathVariable Integer userId,
            @Valid @RequestBody RoleAssignmentRequest request) {
        
        try {
            log.info("Assigning role {} to user ID: {}", request.getRoleId(), userId);
            
            // Kiểm tra quyền modify user
            if (!departmentAuthService.canModifyUser(userId)) {
                log.warn("User does not have permission to modify user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to modify this user");
            }
            
            userService.assignRole(userId, request.getRoleId());
            log.info("Successfully assigned role {} to user ID: {}", request.getRoleId(), userId);
            
            return ResponseEntity.ok("Role assigned successfully");
        } catch (Exception e) {
            log.error("Error assigning role to user ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Gỡ bỏ role khỏi user - Chỉ Admin và Manager (trong department)
     */
    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @departmentAuthorizationService.canModifyUser(#userId))")
    @Auditable(action = "REMOVE_USER_ROLE", entityType = "UserRole", entityIdParam = "userId", logParams = true)
    public ResponseEntity<String> removeRoleFromUser(
            @PathVariable Integer userId,
            @PathVariable Integer roleId) {
        
        try {
            log.info("Removing role {} from user ID: {}", roleId, userId);
            
            // Kiểm tra quyền modify user
            if (!departmentAuthService.canModifyUser(userId)) {
                log.warn("User does not have permission to modify user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to modify this user");
            }
            
            userService.removeRole(userId, roleId);
            log.info("Successfully removed role {} from user ID: {}", roleId, userId);
            
            return ResponseEntity.ok("Role removed successfully");
        } catch (Exception e) {
            log.error("Error removing role from user ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Lấy danh sách roles của user - Với phân quyền
     */
    @GetMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or @departmentAuthorizationService.canAccessUser(#userId)")
    public ResponseEntity<List<RoleResponse>> getUserRoles(@PathVariable Integer userId) {
        try {
            log.info("Getting roles for user ID: {}", userId);
            
            // Kiểm tra quyền truy cập
            if (!departmentAuthService.canAccessUser(userId)) {
                log.warn("User does not have permission to access user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<RoleResponse> roles = userService.getUserRoles(userId);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("Error getting roles for user ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Kích hoạt/Vô hiệu hóa user - Chỉ Admin và Manager (trong department)
     */
    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @departmentAuthorizationService.canModifyUser(#userId))")
    @Auditable(action = "UPDATE_USER_STATUS", entityType = "User", entityIdParam = "userId", logParams = true)
    public ResponseEntity<String> updateUserStatus(
            @PathVariable Integer userId,
            @RequestParam Boolean isActive) {
        
        try {
            log.info("Updating status for user ID: {} to {}", userId, isActive);
            
            // Kiểm tra quyền modify user
            if (!departmentAuthService.canModifyUser(userId)) {
                log.warn("User does not have permission to modify user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to modify this user");
            }
            
            userService.updateUserStatus(userId, isActive);
            log.info("Successfully updated status for user ID: {} to {}", userId, isActive);
            
            return ResponseEntity.ok("User status updated successfully");
        } catch (Exception e) {
            log.error("Error updating status for user ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Xóa user - Chỉ Admin
     */
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "DELETE_USER", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<String> deleteUser(@PathVariable Integer userId) {
        try {
            log.info("Admin deleting user ID: {}", userId);
            
            // Kiểm tra quyền xóa user
            if (!departmentAuthService.canDeleteUser(userId)) {
                log.warn("Admin cannot delete user ID: {} (possibly trying to delete themselves)", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Cannot delete this user");
            }
            
            userService.deleteUser(userId);
            log.info("Successfully deleted user ID: {}", userId);
            
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting user ID: {}", userId, e);
            throw e;
        }
    }

    // ==================== STATISTICS & REPORTING ====================

    /**
     * Lấy thống kê roles - Chỉ Admin
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRoleStatistics() {
        try {
            log.info("Admin requesting role statistics");
            // Implementation tùy theo yêu cầu business
            return ResponseEntity.ok("Role statistics endpoint - to be implemented");
        } catch (Exception e) {
            log.error("Error getting role statistics", e);
            throw e;
        }
    }

    /**
     * Lấy thống kê users theo department - Admin và Manager
     */
    @GetMapping("/users/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getUserStatistics(
            @RequestParam(required = false) Integer departmentId) {
        try {
            log.info("Requesting user statistics for department: {}", departmentId);
            // Implementation tùy theo yêu cầu business
            return ResponseEntity.ok("User statistics endpoint - to be implemented");
        } catch (Exception e) {
            log.error("Error getting user statistics", e);
            throw e;
        }
    }
}