package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.dto.response.PermissionResponse;
import com.example.datn_realeaste_crm.dto.response.RoleResponse;
import com.example.datn_realeaste_crm.security.DepartmentAuthorizationService;
import com.example.datn_realeaste_crm.service.PermissionService;
import com.example.datn_realeaste_crm.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@Slf4j
public class PermissionController {

    private final PermissionService permissionService;
    private final RoleService roleService;
    private final DepartmentAuthorizationService departmentAuthService;

    // ==================== PERMISSION MANAGEMENT ====================

    /**
     * Lấy danh sách tất cả permissions - Chỉ Admin
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        log.info("Admin requesting all permissions");
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    /**
     * Lấy thông tin permission theo ID - Chỉ Admin
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable Integer id) {
        log.info("Admin requesting permission with ID: {}", id);
        return ResponseEntity.ok(permissionService.getPermissionById(id));
    }

    /**
     * Lấy permissions theo resource - Admin và Manager
     * Manager chỉ có thể xem permissions liên quan đến resources mà họ có quyền
     */
    @GetMapping("/resource/{resource}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<PermissionResponse>> getPermissionsByResource(@PathVariable String resource) {
        try {
            log.info("User requesting permissions for resource: {}", resource);
            
            // Admin có thể xem tất cả permissions
            if (hasRole("ROLE_ADMIN")) {
                return ResponseEntity.ok(permissionService.getPermissionsByResource(resource));
            }
            
            // Manager chỉ có thể xem permissions cho resources mà họ có quyền
            if (hasRole("ROLE_MANAGER")) {
                List<PermissionResponse> permissions = permissionService.getPermissionsByResourceForManager(resource);
                return ResponseEntity.ok(permissions);
            }
            
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            log.error("Error getting permissions for resource: {}", resource, e);
            throw e;
        }
    }

    /**
     * Lấy permissions theo category - Admin và Manager
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<PermissionResponse>> getPermissionsByCategory(@PathVariable String category) {
        try {
            log.info("User requesting permissions for category: {}", category);
            
            // Admin có thể xem tất cả permissions
            if (hasRole("ROLE_ADMIN")) {
                return ResponseEntity.ok(permissionService.getPermissionsByCategory(category));
            }
            
            // Manager chỉ có thể xem permissions cho categories mà họ có quyền
            if (hasRole("ROLE_MANAGER")) {
                List<PermissionResponse> permissions = permissionService.getPermissionsByCategoryForManager(category);
                return ResponseEntity.ok(permissions);
            }
            
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            log.error("Error getting permissions for category: {}", category, e);
            throw e;
        }
    }

    // ==================== ROLE-PERMISSION MANAGEMENT ====================

    /**
     * Lấy permissions của role cụ thể - Admin và Manager (limited)
     */
    @GetMapping("/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<PermissionResponse>> getPermissionsByRole(@PathVariable Integer roleId) {
        try {
            log.info("User requesting permissions for role ID: {}", roleId);
            
            // Admin có thể xem permissions của tất cả roles
            if (hasRole("ROLE_ADMIN")) {
                return ResponseEntity.ok(permissionService.getPermissionsByRole(roleId));
            }
            
            // Manager chỉ có thể xem permissions của roles mà họ có thể assign
            if (hasRole("ROLE_MANAGER")) {
                List<PermissionResponse> permissions = permissionService.getPermissionsByRoleForManager(roleId);
                return ResponseEntity.ok(permissions);
            }
            
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            log.error("Error getting permissions for role ID: {}", roleId, e);
            throw e;
        }
    }

    /**
     * Lấy danh sách roles có permission cụ thể - Chỉ Admin
     */
    @GetMapping("/{permissionId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleResponse>> getRolesByPermission(@PathVariable Integer permissionId) {
        try {
            log.info("Admin requesting roles with permission ID: {}", permissionId);
            return ResponseEntity.ok(permissionService.getRolesByPermission(permissionId));
        } catch (Exception e) {
            log.error("Error getting roles for permission ID: {}", permissionId, e);
            throw e;
        }
    }

    // ==================== USER PERMISSION CHECKING ====================

    /**
     * Kiểm tra user có permission cụ thể không - Admin, Manager và chính user đó
     */
    @GetMapping("/users/{userId}/check")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or @departmentAuthorizationService.canAccessUser(#userId)")
    public ResponseEntity<Map<String, Boolean>> checkUserPermissions(
            @PathVariable Integer userId,
            @RequestParam List<String> permissions) {
        
        try {
            log.info("Checking permissions {} for user ID: {}", permissions, userId);
            
            // Kiểm tra quyền truy cập user
            if (!departmentAuthService.canAccessUser(userId)) {
                log.warn("User does not have permission to check permissions for user ID: {}", userId);
                return ResponseEntity.status(403).build();
            }
            
            Map<String, Boolean> result = permissionService.checkUserPermissions(userId, permissions);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error checking permissions for user ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Lấy tất cả permissions của user - Admin, Manager và chính user đó
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or @departmentAuthorizationService.canAccessUser(#userId)")
    public ResponseEntity<List<PermissionResponse>> getUserPermissions(@PathVariable Integer userId) {
        try {
            log.info("Getting all permissions for user ID: {}", userId);
            
            // Kiểm tra quyền truy cập user
            if (!departmentAuthService.canAccessUser(userId)) {
                log.warn("User does not have permission to view permissions for user ID: {}", userId);
                return ResponseEntity.status(403).build();
            }
            
            List<PermissionResponse> permissions = permissionService.getUserPermissions(userId);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            log.error("Error getting permissions for user ID: {}", userId, e);
            throw e;
        }
    }

    // ==================== STATISTICS & REPORTING ====================

    /**
     * Lấy thống kê permissions - Chỉ Admin
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPermissionStatistics() {
        try {
            log.info("Admin requesting permission statistics");
            Map<String, Object> statistics = permissionService.getPermissionStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error getting permission statistics", e);
            throw e;
        }
    }

    /**
     * Lấy thống kê role-permission mapping - Chỉ Admin
     */
    @GetMapping("/roles/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRolePermissionStatistics() {
        try {
            log.info("Admin requesting role-permission statistics");
            Map<String, Object> statistics = permissionService.getRolePermissionStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error getting role-permission statistics", e);
            throw e;
        }
    }

    /**
     * Lấy ma trận permissions theo department - Admin và Manager
     */
    @GetMapping("/departments/{departmentId}/matrix")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @departmentAuthorizationService.belongsToDepartment(#departmentId))")
    public ResponseEntity<Map<String, Object>> getDepartmentPermissionMatrix(@PathVariable Integer departmentId) {
        try {
            log.info("Requesting permission matrix for department ID: {}", departmentId);
            
            // Manager chỉ có thể xem matrix của department mình
            if (hasRole("ROLE_MANAGER") && !departmentAuthService.belongsToDepartment(departmentId)) {
                log.warn("Manager does not have permission to view permission matrix for department ID: {}", departmentId);
                return ResponseEntity.status(403).build();
            }
            
            Map<String, Object> matrix = permissionService.getDepartmentPermissionMatrix(departmentId);
            return ResponseEntity.ok(matrix);
        } catch (Exception e) {
            log.error("Error getting permission matrix for department ID: {}", departmentId, e);
            throw e;
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Lấy danh sách tất cả resources available - Admin và Manager
     */
    @GetMapping("/resources")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<String>> getAllResources() {
        try {
            log.info("User requesting all available resources");
            List<String> resources = permissionService.getAllResources();
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            log.error("Error getting all resources", e);
            throw e;
        }
    }

    /**
     * Lấy danh sách tất cả categories available - Admin và Manager
     */
    @GetMapping("/categories")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<String>> getAllCategories() {
        try {
            log.info("User requesting all available categories");
            List<String> categories = permissionService.getAllCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error getting all categories", e);
            throw e;
        }
    }

    /**
     * Lấy danh sách actions available cho resource - Admin và Manager
     */
    @GetMapping("/resources/{resource}/actions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<String>> getActionsForResource(@PathVariable String resource) {
        try {
            log.info("User requesting actions for resource: {}", resource);
            List<String> actions = permissionService.getActionsForResource(resource);
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            log.error("Error getting actions for resource: {}", resource, e);
            throw e;
        }
    }

    /**
     * Helper method to check if current user has specific role
     */
    private boolean hasRole(String role) {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}