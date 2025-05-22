package com.example.datn_realeaste_crm.controller;


import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.PropertyAccessRequest;
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

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(departmentId, isActive, pageable));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or #id == authentication.principal.userId")
    public ResponseEntity<UserResponse> getUser(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Auditable(action = "CREATE_USER", entityType = "User", logResult = true)
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return new ResponseEntity<>(userService.createUser(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #id == authentication.principal.userId")
    @Auditable(action = "UPDATE_USER", entityType = "User", entityIdParam = "id")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") Integer id, 
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }
    
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Auditable(action = "DEACTIVATE_USER", entityType = "User", entityIdParam = "id")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }
    
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Auditable(action = "ACTIVATE_USER", entityType = "User", entityIdParam = "id")
    public ResponseEntity<UserResponse> activateUser(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }
    
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Auditable(action = "ASSIGN_ROLE", entityType = "User", entityIdParam = "id")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable("id") Integer userId, 
            @Valid @RequestBody RoleAssignmentRequest request) {
        return ResponseEntity.ok(userService.assignRole(userId, request.getRoleId()));
    }
    
    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Auditable(action = "REMOVE_ROLE", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<UserResponse> removeRole(
            @PathVariable("userId") Integer userId,
            @PathVariable("roleId") Integer roleId) {
        return ResponseEntity.ok(userService.removeRole(userId, roleId));
    }
    
    @PostMapping("/property-access")
    @PreAuthorize("hasAuthority('user_assign_property')")
    @Auditable(action = "ASSIGN_PROPERTY_ACCESS", entityType = "UserPropertyAccess")
    public ResponseEntity<Void> assignPropertyAccess(@Valid @RequestBody PropertyAccessRequest request) {
        userService.assignPropertyAccess(request.getUserId(), request.getPropertyId());
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/property-access")
    @PreAuthorize("hasAuthority('user_assign_property')")
    @Auditable(action = "REMOVE_PROPERTY_ACCESS", entityType = "UserPropertyAccess")
    public ResponseEntity<Void> removePropertyAccess(@Valid @RequestBody PropertyAccessRequest request) {
        userService.removePropertyAccess(request.getUserId(), request.getPropertyId());
        return ResponseEntity.ok().build();
    }
    @PostMapping("/change-password")
    @Auditable(action = "CHANGE_MYPASSWORD", entityType =  "changpassword")
    public boolean changepassword(@Valid @RequestBody  String currentPassword, String newPassword){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((User) authentication.getPrincipal()).getUserId();
        try{
            userService.changePassword(userId, currentPassword, newPassword);

        }
        catch (Exception e){
            throw e;
        }
        return true;
    }
}