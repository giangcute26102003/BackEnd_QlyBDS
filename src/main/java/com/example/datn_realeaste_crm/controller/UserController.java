package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.DistrictAccessRequest;
import com.example.datn_realeaste_crm.dto.request.RoleAssignmentRequest;
import com.example.datn_realeaste_crm.dto.request.UserCreateRequest;
import com.example.datn_realeaste_crm.dto.request.UserUpdateRequest;
import com.example.datn_realeaste_crm.dto.response.UserResponse;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.example.datn_realeaste_crm.dto.request.UserSearchRequest;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
//    @PreAuthorize("hasPermission('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(departmentId, isActive, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or #id == authentication.principal.userId")
    public ResponseEntity<UserResponse> getUser(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "CREATE_USER", entityType = "User", logResult = true)
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return new ResponseEntity<>(userService.createUser(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    @Auditable(action = "UPDATE_USER", entityType = "User", entityIdParam = "id")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "DEACTIVATE_USER", entityType = "User", entityIdParam = "id")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "ACTIVATE_USER", entityType = "User", entityIdParam = "id")
    public ResponseEntity<UserResponse> activateUser(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "ASSIGN_ROLE", entityType = "User", entityIdParam = "id")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable("id") Integer userId,
            @Valid @RequestBody RoleAssignmentRequest request) {
        return ResponseEntity.ok(userService.assignRole(userId, request.getRoleId()));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "REMOVE_ROLE", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<UserResponse> removeRole(
            @PathVariable("userId") Integer userId,
            @PathVariable("roleId") Integer roleId) {
        return ResponseEntity.ok(userService.removeRole(userId, roleId));
    }

    @PostMapping("/district-access")
    @PreAuthorize("hasAuthority('user_assign_property')")
    @Auditable(action = "ASSIGN_DISTRICT_ACCESS", entityType = "UserDistrictAccess")
    public ResponseEntity<Void> assignDistrictAccess(@Valid @RequestBody DistrictAccessRequest request) {
        userService.assignDistrictAccess(request.getUserId(), request.getDistrictId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/district-access")
    @PreAuthorize("hasAuthority('user_assign_property')")
    @Auditable(action = "REMOVE_DISTRICT_ACCESS", entityType = "UserDistrictAccess")
    public ResponseEntity<Void> removeDistrictAccess(@Valid @RequestBody DistrictAccessRequest request) {
        userService.removeDistrictAccess(request.getUserId(), request.getDistrictId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    @Auditable(action = "CHANGE_MYPASSWORD", entityType = "changpassword")
    public boolean changepassword(@Valid @RequestBody String currentPassword, String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((User) authentication.getPrincipal()).getUserId();
        try {
            userService.changePassword(userId, currentPassword, newPassword);

        } catch (Exception e) {
            throw e;
        }
        return true;
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or #id == authentication.principal.userId")
    public ResponseEntity<?> getUserRoles(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(userService.getUserRoles(id));
    }

    /**
     * Tìm kiếm users với nhiều tiêu chí
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestBody UserSearchRequest searchRequest,
            Pageable pageable) {
        // TODO: Implement advanced user search
        // return ResponseEntity.ok(userService.searchUsers(searchRequest, pageable));
        return ResponseEntity.ok(userService.getAllUsers(null, null, pageable));
    }

    /**
     * Export danh sách users ra Excel/CSV
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> exportUsers(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Boolean isActive) {
        // TODO: Implement export functionality
        // return userService.exportUsers(format, departmentId, isActive);
        return ResponseEntity.ok("Export functionality - to be implemented");
    }

    /**
     * Bulk import users từ Excel/CSV
     */
    @PostMapping("/bulk-import")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "BULK_IMPORT_USERS", entityType = "User", logResult = true)
    public ResponseEntity<String> bulkImportUsers(
            @RequestParam("file") String fileContent,
            @RequestParam(defaultValue = "excel") String format) {
        // TODO: Implement bulk import functionality
        // return ResponseEntity.ok(userService.bulkImportUsers(fileContent, format));
        return ResponseEntity.ok("Bulk import functionality - to be implemented");
    }

    /**
     * Lấy user statistics tổng quan
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getUserStatistics() {
        // TODO: Implement user statistics
        // return ResponseEntity.ok(userService.getUserStatistics());
        return ResponseEntity.ok("User statistics - to be implemented");
    }
}