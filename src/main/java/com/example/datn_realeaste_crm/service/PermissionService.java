package com.example.datn_realeaste_crm.service;

import com.example.datn_realeaste_crm.dto.response.PermissionResponse;
import com.example.datn_realeaste_crm.dto.response.RoleResponse;
import com.example.datn_realeaste_crm.entity.Permission;
import com.example.datn_realeaste_crm.entity.Role;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.PermissionRepository;
import com.example.datn_realeaste_crm.repository.RolePermissionRepository;
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
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;

    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::convertToPermissionResponse)
                .collect(Collectors.toList());
    }

    public PermissionResponse getPermissionById(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));
        return convertToPermissionResponse(permission);
    }

    public List<PermissionResponse> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource).stream()
                .map(this::convertToPermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get permissions by resource for Manager - filtered based on role capabilities
     */
    public List<PermissionResponse> getPermissionsByResourceForManager(String resource) {
        log.debug("Getting permissions by resource for manager: {}", resource);
        
        try {
            // Get all permissions for the resource
            List<Permission> allPermissions = permissionRepository.findByResource(resource);
            
            // Filter permissions based on manager capabilities
            // Manager should only see permissions they can actually assign
            List<Permission> managerRelevantPermissions = allPermissions.stream()
                    .filter(this::isPermissionRelevantForManager)
                    .collect(Collectors.toList());
            
            return managerRelevantPermissions.stream()
                    .map(this::convertToPermissionResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting permissions by resource for manager", e);
            throw new RuntimeException("Failed to get permissions by resource", e);
        }
    }

    public List<PermissionResponse> getPermissionsByCategory(String category) {
        return permissionRepository.findByAction(category).stream()
                .map(this::convertToPermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get permissions by category for Manager - filtered
     */
    public List<PermissionResponse> getPermissionsByCategoryForManager(String category) {
        log.debug("Getting permissions by category for manager: {}", category);
        
        try {
            List<Permission> allPermissions = permissionRepository.findByAction(category);
            
            List<Permission> managerRelevantPermissions = allPermissions.stream()
                    .filter(this::isPermissionRelevantForManager)
                    .collect(Collectors.toList());
            
            return managerRelevantPermissions.stream()
                    .map(this::convertToPermissionResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting permissions by category for manager", e);
            throw new RuntimeException("Failed to get permissions by category", e);
        }
    }

    /**
     * Get permissions by role
     */
    public List<PermissionResponse> getPermissionsByRole(Integer roleId) {
        log.debug("Getting permissions for role: {}", roleId);
        
        try {
            return rolePermissionRepository.findByRoleRoleId(roleId).stream()
                    .map(rp -> convertToPermissionResponse(rp.getPermission()))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting permissions by role", e);
            throw new RuntimeException("Failed to get permissions by role", e);
        }
    }

    /**
     * Get permissions by role for Manager - only roles they can manage
     */
    public List<PermissionResponse> getPermissionsByRoleForManager(Integer roleId) {
        log.debug("Getting permissions by role for manager: {}", roleId);
        
        try {
            // Check if manager can view this role
            if (!canManagerViewRole(roleId)) {
                log.warn("Manager cannot view permissions for role: {}", roleId);
                return new ArrayList<>();
            }
            
            return getPermissionsByRole(roleId);
            
        } catch (Exception e) {
            log.error("Error getting permissions by role for manager", e);
            throw new RuntimeException("Failed to get permissions by role", e);
        }
    }

    /**
     * Get roles that have specific permission
     */
    public List<RoleResponse> getRolesByPermission(Integer permissionId) {
        log.debug("Getting roles by permission: {}", permissionId);
        
        try {
            return rolePermissionRepository.findByPermissionPermissionId(permissionId).stream()
                    .map(rp -> convertToRoleResponse(rp.getRole()))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting roles by permission", e);
            throw new RuntimeException("Failed to get roles by permission", e);
        }
    }

    /**
     * Check user permissions
     */
    public Map<String, Boolean> checkUserPermissions(Integer userId, List<String> permissionNames) {
        log.debug("Checking permissions for user: {}, permissions: {}", userId, permissionNames);
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            
            // Get all user permissions
            Set<String> userPermissions = user.getUserRoles().stream()
                    .flatMap(ur -> ur.getRole().getRolePermissions().stream())
                    .map(rp -> rp.getPermission().getPermissionName())
                    .collect(Collectors.toSet());
            
            // Check each requested permission
            Map<String, Boolean> result = new HashMap<>();
            for (String permissionName : permissionNames) {
                result.put(permissionName, userPermissions.contains(permissionName));
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error checking user permissions", e);
            throw new RuntimeException("Failed to check user permissions", e);
        }
    }

    /**
     * Get all permissions for user
     */
    public List<PermissionResponse> getUserPermissions(Integer userId) {
        log.debug("Getting all permissions for user: {}", userId);
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            
            Set<Permission> userPermissions = user.getUserRoles().stream()
                    .flatMap(ur -> ur.getRole().getRolePermissions().stream())
                    .map(rp -> rp.getPermission())
                    .collect(Collectors.toSet());
            
            return userPermissions.stream()
                    .map(this::convertToPermissionResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting user permissions", e);
            throw new RuntimeException("Failed to get user permissions", e);
        }
    }

    /**
     * Get permission statistics
     */
    public Map<String, Object> getPermissionStatistics() {
        log.debug("Getting permission statistics");
        
        try {
            long totalPermissions = permissionRepository.count();
            
            // Count by resource
            Map<String, Long> permissionsByResource = permissionRepository.findAll().stream()
                    .collect(Collectors.groupingBy(
                            Permission::getResource,
                            Collectors.counting()
                    ));
            
            // Count by action
            Map<String, Long> permissionsByAction = permissionRepository.findAll().stream()
                    .collect(Collectors.groupingBy(
                            Permission::getAction,
                            Collectors.counting()
                    ));
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalPermissions", totalPermissions);
            statistics.put("permissionsByResource", permissionsByResource);
            statistics.put("permissionsByAction", permissionsByAction);
            
            return statistics;
            
        } catch (Exception e) {
            log.error("Error getting permission statistics", e);
            throw new RuntimeException("Failed to get permission statistics", e);
        }
    }

    /**
     * Get role-permission statistics
     */
    public Map<String, Object> getRolePermissionStatistics() {
        log.debug("Getting role-permission statistics");
        
        try {
            long totalRolePermissions = rolePermissionRepository.count();
            
            // Most common permissions
            Map<String, Long> mostCommonPermissions = rolePermissionRepository.findAll().stream()
                    .collect(Collectors.groupingBy(
                            rp -> rp.getPermission().getPermissionName(),
                            Collectors.counting()
                    ));
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalRolePermissions", totalRolePermissions);
            statistics.put("mostCommonPermissions", mostCommonPermissions);
            
            return statistics;
            
        } catch (Exception e) {
            log.error("Error getting role-permission statistics", e);
            throw new RuntimeException("Failed to get role-permission statistics", e);
        }
    }

    /**
     * Get department permission matrix
     */
    public Map<String, Object> getDepartmentPermissionMatrix(Integer departmentId) {
        log.debug("Getting department permission matrix for department: {}", departmentId);
        
        try {
            // Get all users in department
            List<User> departmentUsers = userRepository.findByDepartmentDepartmentId(departmentId);
            
            Map<String, Object> matrix = new HashMap<>();
            
            for (User user : departmentUsers) {
                Set<String> userPermissions = user.getUserRoles().stream()
                        .flatMap(ur -> ur.getRole().getRolePermissions().stream())
                        .map(rp -> rp.getPermission().getPermissionName())
                        .collect(Collectors.toSet());
                
                matrix.put(user.getName(), userPermissions);
            }
            
            return matrix;
            
        } catch (Exception e) {
            log.error("Error getting department permission matrix", e);
            throw new RuntimeException("Failed to get department permission matrix", e);
        }
    }

    /**
     * Get all unique resources
     */
    public List<String> getAllResources() {
        log.debug("Getting all unique resources");
        
        try {
            return permissionRepository.findAll().stream()
                    .map(Permission::getResource)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting all resources", e);
            throw new RuntimeException("Failed to get all resources", e);
        }
    }

    /**
     * Get all unique categories (actions)
     */
    public List<String> getAllCategories() {
        log.debug("Getting all unique categories");
        
        try {
            return permissionRepository.findAll().stream()
                    .map(Permission::getAction)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting all categories", e);
            throw new RuntimeException("Failed to get all categories", e);
        }
    }

    /**
     * Get actions for specific resource
     */
    public List<String> getActionsForResource(String resource) {
        log.debug("Getting actions for resource: {}", resource);
        
        try {
            return permissionRepository.findByResource(resource).stream()
                    .map(Permission::getAction)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting actions for resource", e);
            throw new RuntimeException("Failed to get actions for resource", e);
        }
    }

    /**
     * Helper method to check if permission is relevant for manager
     */
    private boolean isPermissionRelevantForManager(Permission permission) {
        // Manager should not see admin-only permissions
        String permissionName = permission.getPermissionName().toUpperCase();
        
        // Exclude admin-only permissions
        if (permissionName.contains("DELETE_ALL") || 
            permissionName.contains("SYSTEM_") ||
            permissionName.contains("ADMIN_")) {
            return false;
        }
        
        // Include department-level and general permissions
        return permissionName.contains("DEPARTMENT") || 
               permissionName.contains("VIEW") || 
               permissionName.contains("CREATE") || 
               permissionName.contains("UPDATE");
    }

    /**
     * Helper method to check if manager can view specific role
     */
    private boolean canManagerViewRole(Integer roleId) {
        try {
            // Manager can view MANAGER, CONSULTANT, and other non-admin roles
            // but not ADMIN role
            return rolePermissionRepository.findByRoleRoleId(roleId).stream()
                    .map(rp -> rp.getRole().getRoleName())
                    .noneMatch(roleName -> "ADMIN".equalsIgnoreCase(roleName));
                    
        } catch (Exception e) {
            log.error("Error checking if manager can view role", e);
            return false;
        }
    }

    private PermissionResponse convertToPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .permissionId(permission.getPermissionId())
                .permissionName(permission.getPermissionName())
                .resource(permission.getResource())
                .action(permission.getAction())
                .description(permission.getDescription())
                .build();
    }

    private RoleResponse convertToRoleResponse(Role role) {
        return RoleResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}