package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.PropertyRequest;
import com.example.datn_realeaste_crm.dto.response.PropertyResponse;
import com.example.datn_realeaste_crm.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    public ResponseEntity<?> getAllProperties() {
        // Public endpoint, no security
        return ResponseEntity.ok(propertyService.getAllProperties());
    }

    @GetMapping("/owned")
   @PreAuthorize("hasAuthority('PROPERTY_OWNER')")
    public ResponseEntity<?> getOwnedProperties() {
        // Return properties owned by the current user
        return ResponseEntity.ok(propertyService.getPropertiesOwnedByCurrentUser());
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('CONSULTANT') and hasPermission(null, 'PROPERTY_VIEW_ASSIGNED')")
    public ResponseEntity<?> getAssignedProperties() {
        // Return properties assigned to the current user
        return ResponseEntity.ok(propertyService.getPropertiesAssignedToCurrentUser());
    }

    @GetMapping("/department")
    @PreAuthorize("hasRole('MANAGER') and hasPermission(null, 'PROPERTY_VIEW_DEPARTMENT')")
    public ResponseEntity<?> getDepartmentProperties() {
        // Return properties belonging to the current user's department
        return ResponseEntity.ok(propertyService.getPropertiesByDepartment());
    }

    @GetMapping("/pending-approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER') and hasPermission(null, 'PROPERTY_APPROVE')")
    public ResponseEntity<?> getPropertiesPendingApproval() {
        // Return properties pending approval
        return ResponseEntity.ok(propertyService.getPropertiesPendingApproval());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPropertyById(@PathVariable Integer id) {
        // This is a public endpoint, but we might want to restrict access to certain
        // properties
        return ResponseEntity.ok(propertyService.getPropertyById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROPERTY_OWNER') or hasPermission(null, 'PROPERTY_CREATE')")
    @Auditable(action = "CREATE_PROPERTY", entityType = "Property", logResult = true)
    public ResponseEntity<?> createProperty(@Valid @RequestBody PropertyRequest propertyRequest) {
        // Create a new property
        return ResponseEntity.ok(propertyService.createProperty(propertyRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("(hasRole('ADMIN') and hasPermission(null, 'PROPERTY_UPDATE_ALL')) or " +
            "(hasRole('MANAGER') and hasPermission(null, 'PROPERTY_UPDATE_DEPARTMENT') and " +
            "@propertyAuth.belongsToDepartment(#propertyRequest.departmentId)) or " +
            "(hasRole('CONSULTANT') and hasPermission(null, 'PROPERTY_UPDATE_ASSIGNED') and " +
            "@propertyAuth.isAssignedToProperty(#id)) or " +
            "(hasRole('PROPERTY_OWNER') and hasPermission(null, 'PROPERTY_UPDATE_OWNED') and " +
            "@propertyAuth.isPropertyOwner(#id))")
    @Auditable(action = "UPDATE_PROPERTY", entityType = "Property", entityIdParam = "id", logParams = true)
    public ResponseEntity<?> updateProperty(@PathVariable Integer id,
            @Valid @RequestBody PropertyRequest propertyRequest) {
        // Update an existing property
        return ResponseEntity.ok(propertyService.updateProperty(id, propertyRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasPermission(null, 'PROPERTY_DELETE')")
    @Auditable(action = "DELETE_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<?> deleteProperty(@PathVariable Integer id) {
        // Delete a property
        propertyService.deleteProperty(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/assign/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and hasPermission(null, 'PROPERTY_ASSIGN')")
    @Auditable(action = "ASSIGN_PROPERTY", entityType = "Property", entityIdParam = "id", logParams = true)
    public ResponseEntity<?> assignPropertyToUser(@PathVariable Integer id, @PathVariable Integer userId) {
        // Assign a property to a user
        return ResponseEntity.ok(propertyService.assignPropertyToUser(id, userId));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER') and hasPermission('PROPERTY_APPROVE')")
    @Auditable(action = "APPROVE_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<?> approveProperty(@PathVariable Integer id) {
        // Approve a property
        return ResponseEntity.ok(propertyService.approveProperty(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER') and hasPermission(null, 'PROPERTY_REJECT')")
    @Auditable(action = "REJECT_PROPERTY", entityType = "Property", entityIdParam = "id", logParams = true)
    public ResponseEntity<?> rejectProperty(@PathVariable Integer id, @RequestBody String reason) {
        // Reject a property
        return ResponseEntity.ok(propertyService.rejectProperty(id, reason));
    }
}