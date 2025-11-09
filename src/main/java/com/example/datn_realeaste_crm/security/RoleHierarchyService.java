package com.example.datn_realeaste_crm.security;

import com.example.datn_realeaste_crm.entity.Role;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.RoleRepository;
import com.example.datn_realeaste_crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleHierarchyService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final com.example.datn_realeaste_crm.security.crypto.DeterministicHasher deterministicHasher;

    // Define role hierarchy levels (lower number = higher authority)
    private static final Map<String, Integer> ROLE_HIERARCHY = Map.of(
            "ADMIN", 1,
            "MANAGER", 2,
            "PROPERTY_OWNER", 3,
            "CONSULTANT", 4,
            "REVIEWER", 5
    );

    /**
     * Check if current user can manage target user based on role hierarchy and department
     */
    public boolean canManageUser(Integer targetUserId) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

            // Admin can manage everyone
            if (hasRole(currentUser, "ADMIN")) {
                // Admin cannot delete themselves
                if (currentUser.getUserId().equals(targetUserId)) {
                    log.warn("Admin {} attempted to manage their own account", currentUser.getEmail());
                    return false;
                }
                return true;
            }

            // Manager can only manage users with lower hierarchy roles in their department
            if (hasRole(currentUser, "MANAGER")) {
                return canManagerManageUser(currentUser, targetUser);
            }

            log.debug("User {} does not have permission to manage users", currentUser.getEmail());
            return false;

        } catch (Exception e) {
            log.error("Error checking user management permission", e);
            return false;
        }
    }

    /**
     * Check if current user can assign specific roles to target user
     */
    public boolean canAssignRoles(Integer targetUserId, Set<Integer> roleIds) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

            // Get role names from IDs
            Set<String> roleNames = roleRepository.findAllById(roleIds).stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toSet());

            // Admin can assign any role
            if (hasRole(currentUser, "ADMIN")) {
                return true;
            }

            // Manager can only assign roles lower than their own and within department
            if (hasRole(currentUser, "MANAGER")) {
                return canManagerAssignRoles(currentUser, targetUser, roleNames);
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking role assignment permission", e);
            return false;
        }
    }

    /**
     * Get roles that current user can assign to others
     */
    public Set<Role> getAssignableRoles() {
        try {
            User currentUser = getCurrentUser();

            if (hasRole(currentUser, "ADMIN")) {
                // Admin can assign all roles
                return new HashSet<>(roleRepository.findAll());
            }

            if (hasRole(currentUser, "MANAGER")) {
                // Manager can assign roles lower in hierarchy
                return roleRepository.findAll().stream()
                        .filter(role -> isRoleLowerInHierarchy("MANAGER", role.getRoleName()))
                        .collect(Collectors.toSet());
            }

            return new HashSet<>();

        } catch (Exception e) {
            log.error("Error getting assignable roles", e);
            return new HashSet<>();
        }
    }

    /**
     * Check if Manager can manage specific user
     */
    private boolean canManagerManageUser(User manager, User targetUser) {
        // Check department restriction
        if (manager.getDepartment() == null) {
            log.warn("Manager {} does not belong to any department", manager.getEmail());
            return false;
        }

        if (targetUser.getDepartment() == null || 
            !manager.getDepartment().getDepartmentId().equals(targetUser.getDepartment().getDepartmentId())) {
            log.debug("Manager {} cannot manage user {} (different departments)", 
                    manager.getEmail(), targetUser.getEmail());
            return false;
        }

        // Check role hierarchy - Manager can only manage lower roles
        Set<String> targetUserRoles = targetUser.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleName())
                .collect(Collectors.toSet());

        boolean canManage = targetUserRoles.stream()
                .allMatch(roleName -> isRoleLowerInHierarchy("MANAGER", roleName));

        if (canManage) {
            log.debug("Manager {} can manage user {} (same department, lower roles)", 
                    manager.getEmail(), targetUser.getEmail());
        } else {
            log.debug("Manager {} cannot manage user {} (higher or equal role hierarchy)", 
                    manager.getEmail(), targetUser.getEmail());
        }

        return canManage;
    }

    /**
     * Check if Manager can assign specific roles
     */
    private boolean canManagerAssignRoles(User manager, User targetUser, Set<String> roleNames) {
        // Check department restriction first
        if (manager.getDepartment() == null || targetUser.getDepartment() == null ||
            !manager.getDepartment().getDepartmentId().equals(targetUser.getDepartment().getDepartmentId())) {
            return false;
        }

        // Check if all roles are lower in hierarchy than MANAGER
        boolean canAssign = roleNames.stream()
                .allMatch(roleName -> isRoleLowerInHierarchy("MANAGER", roleName));

        log.debug("Manager {} {} assign roles {} to user {}", 
                manager.getEmail(), canAssign ? "can" : "cannot", roleNames, targetUser.getEmail());

        return canAssign;
    }

    /**
     * Check if role1 is higher in hierarchy than role2
     */
    private boolean isRoleHigherInHierarchy(String role1, String role2) {
        Integer level1 = ROLE_HIERARCHY.get(role1);
        Integer level2 = ROLE_HIERARCHY.get(role2);
        
        if (level1 == null || level2 == null) {
            log.warn("Unknown role in hierarchy check: {} or {}", role1, role2);
            return false;
        }
        
        return level1 < level2; // Lower number = higher authority
    }

    /**
     * Check if role1 is lower in hierarchy than role2
     */
    private boolean isRoleLowerInHierarchy(String role1, String role2) {
        Integer level1 = ROLE_HIERARCHY.get(role1);
        Integer level2 = ROLE_HIERARCHY.get(role2);
        
        if (level1 == null || level2 == null) {
            log.warn("Unknown role in hierarchy check: {} or {}", role1, role2);
            return false;
        }
        
        return level1 > level2; // Higher number = lower authority
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }
        
        String email = authentication.getName();
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        byte[] emailHash = deterministicHasher.emailHash(normalizedEmail);
        return userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Check if user has specific role
     */
    private boolean hasRole(User user, String roleName) {
        return user.getUserRoles().stream()
                .anyMatch(ur -> roleName.equals(ur.getRole().getRoleName()));
    }

    /**
     * Get user's highest role in hierarchy
     */
    public String getHighestRole(User user) {
        return user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleName())
                .min(Comparator.comparing(roleName -> ROLE_HIERARCHY.getOrDefault(roleName, Integer.MAX_VALUE)))
                .orElse("REVIEWER"); // Default to lowest role
    }

    /**
     * Check if current user can view target user
     */
    public boolean canViewUser(Integer targetUserId) {
        try {
            User currentUser = getCurrentUser();
            
            // Admin can view all users
            if (hasRole(currentUser, "ADMIN")) {
                return true;
            }

            // Manager can view users in their department
            if (hasRole(currentUser, "MANAGER")) {
                User targetUser = userRepository.findById(targetUserId)
                        .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));
                
                return currentUser.getDepartment() != null && 
                       targetUser.getDepartment() != null &&
                       currentUser.getDepartment().getDepartmentId().equals(targetUser.getDepartment().getDepartmentId());
            }

            // Users can view their own profile
            return currentUser.getUserId().equals(targetUserId);

        } catch (Exception e) {
            log.error("Error checking user view permission", e);
            return false;
        }
    }
} 