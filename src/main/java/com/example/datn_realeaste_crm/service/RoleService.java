package com.example.datn_realeaste_crm.service;

import com.example.datn_realeaste_crm.dto.request.RoleRequest;
import com.example.datn_realeaste_crm.dto.response.PermissionResponse;
import com.example.datn_realeaste_crm.dto.response.RoleResponse;
import com.example.datn_realeaste_crm.entity.Permission;
import com.example.datn_realeaste_crm.entity.Role;
import com.example.datn_realeaste_crm.entity.RolePermission;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceAlreadyExistsException;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.PermissionRepository;
import com.example.datn_realeaste_crm.repository.RolePermissionRepository;
import com.example.datn_realeaste_crm.repository.RoleRepository;
import com.example.datn_realeaste_crm.repository.UserRepository;
import com.example.datn_realeaste_crm.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToRoleResponse)
                .collect(Collectors.toList());
    }

    public RoleResponse getRoleById(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return convertToRoleResponse(role);
    }

    public Role findByRoleName(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElse(null);
    }

    @Transactional
    public RoleResponse createRole(RoleRequest roleRequest) {
        // Check if role name already exists
        if (roleRepository.findByRoleName(roleRequest.getRoleName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Role already exists with name: " + roleRequest.getRoleName());
        }

        Role role = new Role();
        role.setRoleName(roleRequest.getRoleName());
        role.setDescription(roleRequest.getDescription());
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());

        Role savedRole = roleRepository.save(role);

        // Assign permissions if provided
        if (roleRequest.getPermissionIds() != null && !roleRequest.getPermissionIds().isEmpty()) {
            assignPermissionsToRole(savedRole, roleRequest.getPermissionIds());
        }

        return convertToRoleResponse(savedRole);
    }

    @Transactional
    public RoleResponse updateRole(Integer id, RoleRequest roleRequest) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        // Check if the new role name already exists (and it's not the current role)
        if (!role.getRoleName().equals(roleRequest.getRoleName()) &&
                roleRepository.findByRoleName(roleRequest.getRoleName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Role already exists with name: " + roleRequest.getRoleName());
        }

        role.setRoleName(roleRequest.getRoleName());
        role.setDescription(roleRequest.getDescription());
        role.setUpdatedAt(LocalDateTime.now());

        Role updatedRole = roleRepository.save(role);

        // Update permissions if provided
        if (roleRequest.getPermissionIds() != null) {
            // Clear existing permissions
            rolePermissionRepository.deleteByRoleRoleId(role.getRoleId());

            // Assign new permissions
            if (!roleRequest.getPermissionIds().isEmpty()) {
                assignPermissionsToRole(updatedRole, roleRequest.getPermissionIds());
            }
        }

        return convertToRoleResponse(updatedRole);
    }

    @Transactional
    public void deleteRole(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        // Check if role is in use
        if (!userRoleRepository.findByRoleRoleId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete role because it is assigned to users");
        }

        // Delete all role permissions
        rolePermissionRepository.deleteByRoleRoleId(id);

        // Delete the role
        roleRepository.delete(role);
    }

    @Transactional
    public RoleResponse updateRolePermissions(Integer roleId, Set<Integer> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        // Clear existing permissions
        rolePermissionRepository.deleteByRoleRoleId(roleId);

        // Assign new permissions
        if (permissionIds != null && !permissionIds.isEmpty()) {
            assignPermissionsToRole(role, permissionIds);
        }

        return convertToRoleResponse(role);
    }

    /**
     * Get role statistics
     */
    public Map<String, Object> getRoleStatistics() {
        log.debug("Getting role statistics");
        
        try {
            long totalRoles = roleRepository.count();
            
            // Count users by role
            Map<String, Long> usersByRole = userRoleRepository.findAll().stream()
                    .collect(Collectors.groupingBy(
                            ur -> ur.getRole().getRoleName(),
                            Collectors.counting()
                    ));
            
            // Count permissions by role
            Map<String, Long> permissionsByRole = rolePermissionRepository.findAll().stream()
                    .collect(Collectors.groupingBy(
                            rp -> rp.getRole().getRoleName(),
                            Collectors.counting()
                    ));
            
            // Most assigned roles
            List<Map<String, Object>> mostAssignedRoles = usersByRole.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(entry -> {
                        Map<String, Object> roleData = new HashMap<>();
                        roleData.put("roleName", entry.getKey());
                        roleData.put("userCount", entry.getValue());
                        return roleData;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalRoles", totalRoles);
            statistics.put("usersByRole", usersByRole);
            statistics.put("permissionsByRole", permissionsByRole);
            statistics.put("mostAssignedRoles", mostAssignedRoles);
            
            return statistics;
            
        } catch (Exception e) {
            log.error("Error getting role statistics", e);
            throw new RuntimeException("Failed to get role statistics", e);
        }
    }

    /**
     * Get user statistics
     */
    public Map<String, Object> getUserStatistics() {
        log.debug("Getting user statistics");
        
        try {
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.countByIsActive(true);
            long inactiveUsers = totalUsers - activeUsers;
            
            // Count users by department
            Map<String, Long> usersByDepartment = userRepository.findAll().stream()
                    .filter(user -> user.getDepartment() != null)
                    .collect(Collectors.groupingBy(
                            user -> user.getDepartment().getName(),
                            Collectors.counting()
                    ));
            
            // Count users without department
            long usersWithoutDepartment = userRepository.findAll().stream()
                    .mapToLong(user -> user.getDepartment() == null ? 1 : 0)
                    .sum();
            
            // Recent registrations (last 30 days)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long recentRegistrations = userRepository.findAll().stream()
                    .mapToLong(user -> user.getCreatedAt() != null && 
                              user.getCreatedAt().isAfter(thirtyDaysAgo) ? 1 : 0)
                    .sum();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalUsers", totalUsers);
            statistics.put("activeUsers", activeUsers);
            statistics.put("inactiveUsers", inactiveUsers);
            statistics.put("usersByDepartment", usersByDepartment);
            statistics.put("usersWithoutDepartment", usersWithoutDepartment);
            statistics.put("recentRegistrations", recentRegistrations);
            
            return statistics;
            
        } catch (Exception e) {
            log.error("Error getting user statistics", e);
            throw new RuntimeException("Failed to get user statistics", e);
        }
    }

    private void assignPermissionsToRole(Role role, Set<Integer> permissionIds) {
        for (Integer permissionId : permissionIds) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permissionId));

            RolePermission rolePermission = new RolePermission();
            rolePermission.setRole(role);
            rolePermission.setPermission(permission);
            rolePermissionRepository.save(rolePermission);
        }
    }

    private RoleResponse convertToRoleResponse(Role role) {
        Set<PermissionResponse> permissions = new HashSet<>();

        if (role.getRolePermissions() != null) {
            permissions = role.getRolePermissions().stream()
                    .map(rp -> convertToPermissionResponse(rp.getPermission()))
                    .collect(Collectors.toSet());
        }

        return RoleResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .permissions(permissions)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
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
}