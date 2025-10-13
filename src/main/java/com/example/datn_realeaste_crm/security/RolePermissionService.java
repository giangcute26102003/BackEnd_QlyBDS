package com.example.datn_realeaste_crm.security;

import com.example.datn_realeaste_crm.entity.Permission;
import com.example.datn_realeaste_crm.entity.Role;
import com.example.datn_realeaste_crm.entity.RolePermission;
import com.example.datn_realeaste_crm.repository.PermissionRepository;
import com.example.datn_realeaste_crm.repository.RolePermissionRepository;
import com.example.datn_realeaste_crm.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * Initialize system roles and permissions on application startup
     */
    // @PostConstruct
    @Transactional
    public void initRolesAndPermissions() {
        log.info("Initializing roles and permissions...");

        // Initialize permissions if they don't exist
        Arrays.stream(PermissionEnum.values()).forEach(this::createPermissionIfNotExists);

        // Initialize roles with their permissions
        initAdminRole();
        initManagerRole();
        initConsultantRole();
        initReviewerRole();
        initPropertyOwnerRole();

        log.info("Roles and permissions initialized successfully");
    }

    private Permission createPermissionIfNotExists(PermissionEnum permEnum) {
        String permName = permEnum.name();
        Optional<Permission> existingPerm = permissionRepository.findByPermissionName(permName);

        if (existingPerm.isPresent()) {
            return existingPerm.get();
        }

        Permission permission = new Permission();
        permission.setPermissionName(permName);

        // Set category, resource and action based on permission name
        String[] parts = permName.split("_", 2);
        if (parts.length > 0) {
            permission.setResource(parts[0].toLowerCase());
            if (parts.length > 1) {
                permission.setAction(parts[1].toLowerCase());
            }
        }

        permission.setDescription("Permission to " + permName.toLowerCase().replace('_', ' '));
        return permissionRepository.save(permission);
    }

    private void initAdminRole() {
        Role adminRole = getOrCreateRole(RoleEnum.ADMIN.name(), "Administrator with full system access");

        // Admin has all permissions
        Set<Permission> allPermissions = Arrays.stream(PermissionEnum.values())
                .map(this::createPermissionIfNotExists)
                .collect(Collectors.toSet());

        assignPermissionsToRole(adminRole, allPermissions);
    }

    private void initManagerRole() {
        Role managerRole = getOrCreateRole(RoleEnum.MANAGER.name(),
                "Department manager with limited administration rights");

        // Manager permissions
        Set<PermissionEnum> managerPerms = new HashSet<>(Arrays.asList(
                // User management (department only)
                PermissionEnum.USER_VIEW_DEPARTMENT,
                PermissionEnum.USER_UPDATE_DEPARTMENT,
                PermissionEnum.USER_VIEW_OWN,
                PermissionEnum.USER_UPDATE_OWN,

                // Property management
                PermissionEnum.PROPERTY_VIEW_ALL,
                PermissionEnum.PROPERTY_VIEW_DEPARTMENT,
                PermissionEnum.PROPERTY_CREATE,
                PermissionEnum.PROPERTY_UPDATE_DEPARTMENT,
                PermissionEnum.PROPERTY_ASSIGN,

                // Customer management
                PermissionEnum.CUSTOMER_VIEW_DEPARTMENT,
                PermissionEnum.CUSTOMER_CREATE,
                PermissionEnum.CUSTOMER_UPDATE_DEPARTMENT,
                PermissionEnum.CUSTOMER_ASSIGN,

                // Interaction management
                PermissionEnum.INTERACTION_VIEW_DEPARTMENT,
                PermissionEnum.INTERACTION_VIEW_OWN,
                PermissionEnum.INTERACTION_CREATE,
                PermissionEnum.INTERACTION_UPDATE_OWN,

                // Report management
                PermissionEnum.REPORT_VIEW_DEPARTMENT,
                PermissionEnum.REPORT_EXPORT));

        Set<Permission> managerPermissions = managerPerms.stream()
                .map(this::createPermissionIfNotExists)
                .collect(Collectors.toSet());

        assignPermissionsToRole(managerRole, managerPermissions);
    }

    private void initConsultantRole() {
        Role consultantRole = getOrCreateRole(RoleEnum.CONSULTANT.name(),
                "Property consultant with access to assigned properties and customers");

        // Consultant permissions
        Set<PermissionEnum> consultantPerms = new HashSet<>(Arrays.asList(
                // User management (own only)
                PermissionEnum.USER_VIEW_OWN,
                PermissionEnum.USER_UPDATE_OWN,

                // Property management (assigned only)
                PermissionEnum.PROPERTY_VIEW_ASSIGNED,
                PermissionEnum.PROPERTY_UPDATE_ASSIGNED,

                // Customer management (assigned only)
                PermissionEnum.CUSTOMER_VIEW_ASSIGNED,
                PermissionEnum.CUSTOMER_UPDATE_ASSIGNED,

                // Interaction management
                PermissionEnum.INTERACTION_VIEW_OWN,
                PermissionEnum.INTERACTION_CREATE,
                PermissionEnum.INTERACTION_UPDATE_OWN,

                // Appointment management
                PermissionEnum.APPOINTMENT_VIEW_OWN,
                PermissionEnum.APPOINTMENT_CREATE,
                PermissionEnum.APPOINTMENT_UPDATE_OWN));

        Set<Permission> consultantPermissions = consultantPerms.stream()
                .map(this::createPermissionIfNotExists)
                .collect(Collectors.toSet());

        assignPermissionsToRole(consultantRole, consultantPermissions);
    }

    private void initReviewerRole() {
        Role reviewerRole = getOrCreateRole(RoleEnum.REVIEWER.name(), "Reviewer who can approve or reject properties");

        // Reviewer permissions
        Set<PermissionEnum> reviewerPerms = new HashSet<>(Arrays.asList(
                // User management (own only)
                PermissionEnum.USER_VIEW_OWN,
                PermissionEnum.USER_UPDATE_OWN,

                // Property approval
                PermissionEnum.PROPERTY_VIEW_ALL,
                PermissionEnum.PROPERTY_APPROVE,
                PermissionEnum.PROPERTY_REJECT,

                // Report
                PermissionEnum.REPORT_VIEW_ALL));

        Set<Permission> reviewerPermissions = reviewerPerms.stream()
                .map(this::createPermissionIfNotExists)
                .collect(Collectors.toSet());

        assignPermissionsToRole(reviewerRole, reviewerPermissions);
    }

    private void initPropertyOwnerRole() {
        Role propertyOwnerRole = getOrCreateRole(RoleEnum.PROPERTY_OWNER.name(),
                "Property owner who can manage their own properties");

        // Property Owner permissions
        Set<PermissionEnum> propertyOwnerPerms = new HashSet<>(Arrays.asList(
                // User management (own only)
                PermissionEnum.USER_VIEW_OWN,
                PermissionEnum.USER_UPDATE_OWN,

                // Property management (owned only)
                PermissionEnum.PROPERTY_VIEW_OWNED,
                PermissionEnum.PROPERTY_CREATE,
                PermissionEnum.PROPERTY_UPDATE_OWNED,
                PermissionEnum.PROPERTY_DELETE,

                // Interaction view (property related)
                PermissionEnum.INTERACTION_VIEW_PROPERTY_RELATED,

                // Appointment management
                PermissionEnum.APPOINTMENT_VIEW_PROPERTY_RELATED,
                PermissionEnum.APPOINTMENT_CONFIRM_REJECT));

        Set<Permission> propertyOwnerPermissions = propertyOwnerPerms.stream()
                .map(this::createPermissionIfNotExists)
                .collect(Collectors.toSet());

        assignPermissionsToRole(propertyOwnerRole, propertyOwnerPermissions);
    }

    private Role getOrCreateRole(String roleName, String description) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleName(roleName);
                    role.setDescription(description);
                    return roleRepository.save(role);
                });
    }

    private void assignPermissionsToRole(Role role, Set<Permission> permissions) {
        // Clear existing permissions
        rolePermissionRepository.deleteByRoleRoleId(role.getRoleId());

        // Create new role-permission associations
        for (Permission permission : permissions) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRole(role);
            rolePermission.setPermission(permission);
            rolePermissionRepository.save(rolePermission);
        }
    }

    /**
     * Check if a user has a specific permission
     * 
     * @param user       The user to check
     * @param permission The permission to check for
     * @return true if the user has the permission, false otherwise
     */
    public boolean hasPermission(org.springframework.security.core.userdetails.UserDetails user,
            PermissionEnum permission) {
        return user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(permission.name()));
    }

    /**
     * Check if a user has a specific permission by string name
     * 
     * @param user           The user to check
     * @param permissionName The permission name to check for
     * @return true if the user has the permission, false otherwise
     */
    public boolean hasPermission(org.springframework.security.core.userdetails.UserDetails user,
            String permissionName) {
        try {
            PermissionEnum permission = PermissionEnum.valueOf(permissionName);
            return hasPermission(user, permission);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}