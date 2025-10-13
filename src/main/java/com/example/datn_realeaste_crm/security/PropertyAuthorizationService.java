package com.example.datn_realeaste_crm.security;

import com.example.datn_realeaste_crm.entity.Property;
import com.example.datn_realeaste_crm.entity.PropertyOwnership;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.repository.PropertyOwnershipRepository;
import com.example.datn_realeaste_crm.repository.PropertyRepository;
import com.example.datn_realeaste_crm.repository.UserDistrictAccessRepository;
import com.example.datn_realeaste_crm.repository.UserRepository;
import com.example.datn_realeaste_crm.security.RoleEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service to handle property-specific authorization checks
 */
@Service
@RequiredArgsConstructor
public class PropertyAuthorizationService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyOwnershipRepository propertyOwnershipRepository;
    private final UserDistrictAccessRepository userDistrictAccessRepository;

    /**
     * Check if the current user can access the specified property
     * 
     * @param propertyId Property ID to check
     * @return true if the user can access the property, false otherwise
     */
    public boolean canAccessProperty(Integer propertyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Get current user from authentication
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        // Check if user has admin role
        if (hasRole(authentication, RoleEnum.ADMIN.name())) {
            return true;
        }

        // Check if user is the owner of the property
        if (isPropertyOwner(propertyId)) {
            return true;
        }

        // Check if user is a manager of the property's department
        if (hasRole(authentication, RoleEnum.MANAGER.name()) && isPropertyInUserDepartment(propertyId)) {
            return true;
        }

        // Check if user is assigned to the property
        return isAssignedToProperty(propertyId);
    }

    /**
     * Check if the current user is the owner of the specified property
     * 
     * @param propertyId Property ID to check
     * @return true if the user is the owner, false otherwise
     */
    public boolean isPropertyOwner(Integer propertyId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        Optional<PropertyOwnership> ownership = propertyOwnershipRepository.findByUserUserIdAndPropertyPropertyId(
                currentUser.getUserId(), propertyId);

        return ownership.isPresent() &&
                (ownership.get().getOwnershipType().equals("owner") ||
                        ownership.get().getOwnershipType().equals("co-owner"));
    }

    /**
     * Check if the current user is assigned to the specified property
     * (now checks if user has access to the property's district)
     * 
     * @param propertyId Property ID to check
     * @return true if the user is assigned to the property's district, false otherwise
     */
    public boolean isAssignedToProperty(Integer propertyId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        // Get the property to find its district
        Optional<Property> property = propertyRepository.findById(propertyId);
        if (property.isEmpty()) {
            return false;
        }

        // Check if user has access to the property's district
        return userDistrictAccessRepository.findByUserUserIdAndDistrictId(
                currentUser.getUserId(), property.get().getDistrict().getId()).isPresent();
    }

    /**
     * Check if the property belongs to the user's department
     * 
     * @param propertyId Property ID to check
     * @return true if the property is in the user's department, false otherwise
     */
    public boolean isPropertyInUserDepartment(Integer propertyId) {
        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getDepartment() == null) {
            return false;
        }

        Optional<Property> property = propertyRepository.findById(propertyId);
        if (property.isEmpty() || property.get().getDepartment() == null) {
            return false;
        }

        return property.get().getDepartment().getDepartmentId().equals(
                currentUser.getDepartment().getDepartmentId());
    }

    /**
     * Check if the user belongs to the specified department
     * 
     * @param departmentId Department ID to check
     * @return true if the user belongs to the department, false otherwise
     */
    public boolean belongsToDepartment(Integer departmentId) {
        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getDepartment() == null) {
            return false;
        }

        return currentUser.getDepartment().getDepartmentId().equals(departmentId);
    }

    /**
     * Check if the current user is assigned to the customer with the given ID
     * 
     * @param customerId The customer ID to check
     * @return true if the user is assigned to the customer, false otherwise
     */
    public boolean isAssignedToCustomer(Integer customerId) {
        // This would need to be implemented based on your customer assignment model
        // For now, this is a placeholder
        return false;
    }

    /**
     * Check if the authentication has a specific permission
     * 
     * @param permission The permission to check for
     * @return true if the authentication has the permission, false otherwise
     */
    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(permission));
    }

    /**
     * For backward compatibility
     */
    public boolean canAccessProperty(Authentication authentication, Integer propertyId) {
        // Get current user from authentication
        if (!(authentication.getPrincipal() instanceof User)) {
            return false;
        }

        User user = (User) authentication.getPrincipal();

        // Check if user is admin or has property_view permission
        if (user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + RoleEnum.ADMIN.name()) ||
                a.getAuthority().equals("property_view"))) {
            return true;
        }

        // Check if user is Manager and property belongs to their department
        if (user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + RoleEnum.MANAGER.name()))) {
            Optional<Property> property = propertyRepository.findById(propertyId);
            if (property.isPresent() && property.get().getDepartment() != null &&
                    user.getDepartment() != null &&
                    property.get().getDepartment().getDepartmentId().equals(user.getDepartment().getDepartmentId())) {
                return true;
            }
        }

        // Check if user is Consultant with district access
        if (user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + RoleEnum.CONSULTANT.name()))) {
            Optional<Property> property = propertyRepository.findById(propertyId);
            if (property.isPresent()) {
                return userDistrictAccessRepository.findByUserUserIdAndDistrictId(
                        user.getUserId(), property.get().getDistrict().getId()).isPresent();
            }
            return false;
        }

        // Check if user is the owner
        return isPropertyOwner(authentication, propertyId);
    }

    /**
     * For backward compatibility
     */
    public boolean isPropertyOwner(Authentication authentication, Integer propertyId) {
        if (!(authentication.getPrincipal() instanceof User)) {
            return false;
        }

        User user = (User) authentication.getPrincipal();

        // Check if user is admin
        if (user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + RoleEnum.ADMIN.name()))) {
            return true;
        }

        // Check if user is the owner
        Optional<PropertyOwnership> ownership = propertyOwnershipRepository
                .findByUserUserIdAndPropertyPropertyId(user.getUserId(), propertyId);

        return ownership.isPresent() &&
                (ownership.get().getOwnershipType().equals("owner") ||
                        ownership.get().getOwnershipType().equals("co-owner"));
    }

    /**
     * Get the current authenticated user from the security context
     * 
     * @return The current user or null if not authenticated
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Check if the authentication has the specified role
     * 
     * @param authentication The authentication to check
     * @param role           The role to check for
     * @return true if the authentication has the role, false otherwise
     */
    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(role));
    }
}