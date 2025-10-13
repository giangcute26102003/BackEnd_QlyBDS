package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.UserCreateRequest;
import com.example.datn_realeaste_crm.dto.request.UserSearchRequest;
import com.example.datn_realeaste_crm.dto.request.UserUpdateRequest;
import com.example.datn_realeaste_crm.dto.response.RoleResponse;
import com.example.datn_realeaste_crm.dto.response.UserResponse;
import com.example.datn_realeaste_crm.entity.Role;
import com.example.datn_realeaste_crm.security.RoleHierarchyService;
import com.example.datn_realeaste_crm.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user-management")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User management APIs with role-based access control")
@SecurityRequirement(name = "bearerAuth")
public class UserManagementController {

    private final UserService userService;
    private final RoleHierarchyService roleHierarchyService;

    // ==================== USER CRUD OPERATIONS ====================

    @Operation(summary = "Get all users", 
               description = "Retrieve users with role-based access control. Admin can see all users, Manager can see users in their department only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @Parameter(description = "Filter by user name") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by email") @RequestParam(required = false) String email,
            @Parameter(description = "Filter by department ID") @RequestParam(required = false) Integer departmentId,
            @Parameter(description = "Filter by role name") @RequestParam(required = false) String roleName,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Pagination parameters") Pageable pageable) {
        
        try {
            log.info("Getting users list with filters - name: {}, email: {}, dept: {}, role: {}, active: {}", 
                    name, email, departmentId, roleName, isActive);

            UserSearchRequest searchRequest = new UserSearchRequest();
            searchRequest.setName(name);
            searchRequest.setEmail(email);
            searchRequest.setDepartmentId(departmentId);
            searchRequest.setRoleName(roleName);
            searchRequest.setIsActive(isActive);

            Page<UserResponse> users = userService.searchUsersWithAuthorization(searchRequest, pageable);
            
            log.info("Successfully retrieved {} users", users.getTotalElements());
            return ResponseEntity.ok(users);

        } catch (Exception e) {
            log.error("Error getting users list", e);
            throw e;
        }
    }

    /**
     * Lấy thông tin user theo ID với phân quyền
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer userId) {
        try {
            log.info("Getting user details for ID: {}", userId);

            // Check permission using role hierarchy service
            if (!roleHierarchyService.canViewUser(userId)) {
                log.warn("Current user does not have permission to view user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            UserResponse user = userService.getUserById(userId);
            log.info("Successfully retrieved user: {}", user.getEmail());
            
            return ResponseEntity.ok(user);

        } catch (Exception e) {
            log.error("Error getting user by ID: {}", userId, e);
            throw e;
        }
    }

    @Operation(summary = "Create new user", 
               description = "Create new user with role assignment. Admin can assign any role, Manager can only assign lower hierarchy roles within their department.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied or insufficient permissions for role assignment",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "User already exists",
                    content = @Content)
    })
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Auditable(action = "CREATE_USER", entityType = "User", logParams = true, logResult = true)
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "User creation data") @Valid @RequestBody UserCreateRequest request) {
        try {
            log.info("Creating new user with email: {} in department: {}", request.getEmail(), request.getDepartmentId());

            // Validate role assignment permissions
            if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
                // For new user, we check if current user can assign these roles
                // We'll create a temporary validation by checking against role hierarchy
                if (!canAssignRolesToNewUser(request.getRoleIds())) {
                    log.warn("Current user does not have permission to assign roles: {}", request.getRoleIds());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(null);
                }
            }

            UserResponse createdUser = userService.createUser(request);
            log.info("Successfully created user: {} with ID: {}", createdUser.getEmail(), createdUser.getUserId());

            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("Error creating user with email: {}", request.getEmail(), e);
            throw e;
        }
    }

    /**
     * Cập nhật thông tin user
     */
    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Auditable(action = "UPDATE_USER", entityType = "User", entityIdParam = "userId", logParams = true)
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody UserUpdateRequest request) {
        
        try {
            log.info("Updating user ID: {}", userId);

            // Check management permission
            if (!roleHierarchyService.canManageUser(userId)) {
                log.warn("Current user does not have permission to update user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            UserResponse updatedUser = userService.updateUser(userId, request);
            log.info("Successfully updated user: {}", updatedUser.getEmail());

            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            log.error("Error updating user ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Xóa user - chỉ Admin hoặc Manager (với ràng buộc)
     */
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Auditable(action = "DELETE_USER", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<String> deleteUser(@PathVariable Integer userId) {
        try {
            log.info("Attempting to delete user ID: {}", userId);

            // Check management permission
            if (!roleHierarchyService.canManageUser(userId)) {
                log.warn("Current user does not have permission to delete user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to delete this user");
            }

            userService.deleteUser(userId);
            log.info("Successfully deleted user ID: {}", userId);

            return ResponseEntity.ok("User deleted successfully");

        } catch (Exception e) {
            log.error("Error deleting user ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Kích hoạt/Vô hiệu hóa user
     */
    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Auditable(action = "UPDATE_USER_STATUS", entityType = "User", entityIdParam = "userId", logParams = true)
    public ResponseEntity<String> updateUserStatus(
            @PathVariable Integer userId,
            @RequestParam Boolean isActive) {
        
        try {
            log.info("Updating status for user ID: {} to {}", userId, isActive);

            // Check management permission
            if (!roleHierarchyService.canManageUser(userId)) {
                log.warn("Current user does not have permission to update status for user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to update this user's status");
            }

            userService.updateUserStatus(userId, isActive);
            log.info("Successfully updated status for user ID: {} to {}", userId, isActive);

            return ResponseEntity.ok("User status updated successfully");

        } catch (Exception e) {
            log.error("Error updating status for user ID: {}", userId, e);
            throw e;
        }
    }

    // ==================== ROLE ASSIGNMENT OPERATIONS ====================

    /**
     * Gán roles cho user với phân quyền hierarchy
     */
    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Auditable(action = "ASSIGN_USER_ROLES", entityType = "UserRole", entityIdParam = "userId", logParams = true)
    public ResponseEntity<String> assignRolesToUser(
            @PathVariable Integer userId,
            @RequestBody Set<Integer> roleIds) {
        
        try {
            log.info("Assigning roles {} to user ID: {}", roleIds, userId);

            // Check if current user can assign these roles to target user
            if (!roleHierarchyService.canAssignRoles(userId, roleIds)) {
                log.warn("Current user does not have permission to assign roles {} to user ID: {}", roleIds, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to assign these roles to this user");
            }

            // Assign roles one by one
            for (Integer roleId : roleIds) {
                userService.assignRole(userId, roleId);
            }

            log.info("Successfully assigned roles {} to user ID: {}", roleIds, userId);
            return ResponseEntity.ok("Roles assigned successfully");

        } catch (Exception e) {
            log.error("Error assigning roles to user ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Gỡ bỏ roles khỏi user
     */
    @DeleteMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Auditable(action = "REMOVE_USER_ROLES", entityType = "UserRole", entityIdParam = "userId", logParams = true)
    public ResponseEntity<String> removeRolesFromUser(
            @PathVariable Integer userId,
            @RequestBody Set<Integer> roleIds) {
        
        try {
            log.info("Removing roles {} from user ID: {}", roleIds, userId);

            // Check management permission
            if (!roleHierarchyService.canManageUser(userId)) {
                log.warn("Current user does not have permission to remove roles from user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to modify this user's roles");
            }

            // Remove roles one by one
            for (Integer roleId : roleIds) {
                userService.removeRole(userId, roleId);
            }

            log.info("Successfully removed roles {} from user ID: {}", roleIds, userId);
            return ResponseEntity.ok("Roles removed successfully");

        } catch (Exception e) {
            log.error("Error removing roles from user ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Lấy danh sách roles của user
     */
    @GetMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<RoleResponse>> getUserRoles(@PathVariable Integer userId) {
        try {
            log.info("Getting roles for user ID: {}", userId);

            // Check view permission
            if (!roleHierarchyService.canViewUser(userId)) {
                log.warn("Current user does not have permission to view roles for user ID: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<RoleResponse> roles = userService.getUserRoles(userId);
            log.info("Successfully retrieved {} roles for user ID: {}", roles.size(), userId);

            return ResponseEntity.ok(roles);

        } catch (Exception e) {
            log.error("Error getting roles for user ID: {}", userId, e);
            throw e;
        }
    }

    // ==================== UTILITY ENDPOINTS ====================

    /**
     * Lấy danh sách roles có thể assign dựa trên hierarchy
     */
    @GetMapping("/assignable-roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<RoleResponse>> getAssignableRoles() {
        try {
            log.info("Getting assignable roles for current user");

            Set<Role> assignableRoles = roleHierarchyService.getAssignableRoles();
            
            List<RoleResponse> roleResponses = assignableRoles.stream()
                    .map(role -> RoleResponse.builder()
                            .roleId(role.getRoleId())
                            .roleName(role.getRoleName())
                            .description(role.getDescription())
                            .createdAt(role.getCreatedAt())
                            .updatedAt(role.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());

            log.info("Successfully retrieved {} assignable roles", roleResponses.size());
            return ResponseEntity.ok(roleResponses);

        } catch (Exception e) {
            log.error("Error getting assignable roles", e);
            throw e;
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Check if current user can assign roles to new user
     */
    private boolean canAssignRolesToNewUser(Set<Integer> roleIds) {
        try {
            Set<Role> assignableRoles = roleHierarchyService.getAssignableRoles();
            Set<Integer> assignableRoleIds = assignableRoles.stream()
                    .map(Role::getRoleId)
                    .collect(Collectors.toSet());

            return assignableRoleIds.containsAll(roleIds);

        } catch (Exception e) {
            log.error("Error checking role assignment permission for new user", e);
            return false;
        }
    }
} 