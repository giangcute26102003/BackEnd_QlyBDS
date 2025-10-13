package com.example.datn_realeaste_crm.security;

import lombok.Setter;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Custom security expression root that provides additional security expressions
 * for method security annotations
 */
public class CustomSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private final RolePermissionService rolePermissionService;
    /**
     * -- SETTER --
     *  Set the property authorization service
     *
     */
    @Setter
    private PropertyAuthorizationService propertyAuthorizationService;
    private Object filterObject;
    private Object returnObject;
    private Object target;

    public CustomSecurityExpressionRoot(Authentication authentication, RolePermissionService rolePermissionService) {
        super(authentication);
        this.rolePermissionService = rolePermissionService;
        this.propertyAuthorizationService = null; // Will be set via setter
    }

    public CustomSecurityExpressionRoot(Authentication authentication,
            RolePermissionService rolePermissionService,
            PropertyAuthorizationService propertyAuthorizationService) {
        super(authentication);
        this.rolePermissionService = rolePermissionService;
        this.propertyAuthorizationService = propertyAuthorizationService;
    }

    /**
     * Check if the current user has the specified permission
     * 
     * @param permission The permission to check
     * @return true if the user has the permission, false otherwise
     */
    public boolean hasPermission(String permission) {
        Object principal = this.getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {
            return false;
        }

        try {
            PermissionEnum permissionEnum = PermissionEnum.valueOf(permission);
            return rolePermissionService.hasPermission(userDetails, permissionEnum);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if the current user belongs to the department with the given ID
     * 
     * @param departmentId The department ID to check
     * @return true if the user belongs to the department, false otherwise
     */
    public boolean belongsToDepartment(Integer departmentId) {
        if (propertyAuthorizationService == null) {
            return false;
        }
        return propertyAuthorizationService.belongsToDepartment(departmentId);
    }

    /**
     * Check if the current user is assigned to the property with the given ID
     * 
     * @param propertyId The property ID to check
     * @return true if the user is assigned to the property, false otherwise
     */
    public boolean isAssignedToProperty(Integer propertyId) {
        if (propertyAuthorizationService == null) {
            return false;
        }
        return propertyAuthorizationService.isAssignedToProperty(propertyId);
    }

    /**
     * Check if the current user is assigned to the customer with the given ID
     * 
     * @param customerId The customer ID to check
     * @return true if the user is assigned to the customer, false otherwise
     */
    public boolean isAssignedToCustomer(Integer customerId) {
        // This would require accessing the customer assignment information
        // For now, we'll return false and implement this later
        return false;
    }

    /**
     * Check if the current user is the owner of the property with the given ID
     * 
     * @param propertyId The property ID to check
     * @return true if the user is the owner of the property, false otherwise
     */
    public boolean isPropertyOwner(Integer propertyId) {
        if (propertyAuthorizationService == null) {
            return false;
        }
        return propertyAuthorizationService.isPropertyOwner(propertyId);
    }

    @Override
    public Object getFilterObject() {
        return this.filterObject;
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getReturnObject() {
        return this.returnObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }

    public void setThis(Object aThis) {
    }
}
