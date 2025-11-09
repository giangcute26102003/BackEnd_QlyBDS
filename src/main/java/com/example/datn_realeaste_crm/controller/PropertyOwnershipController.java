package com.example.datn_realeaste_crm.controller;


import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.PropertyOwnershipRequest;
import com.example.datn_realeaste_crm.dto.response.PropertyOwnershipResponse;
import com.example.datn_realeaste_crm.security.PropertyOwnershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/property-ownerships")
@RequiredArgsConstructor
public class PropertyOwnershipController {
    
    private final PropertyOwnershipService propertyOwnershipService;
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<List<PropertyOwnershipResponse>> getUserOwnerships(@PathVariable Integer userId) {
        return ResponseEntity.ok(propertyOwnershipService.getUserOwnerships(userId));
    }
    
    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @propertyAuthorizationService.canAccessProperty(#propertyId)")
    public ResponseEntity<List<PropertyOwnershipResponse>> getPropertyOwnerships(@PathVariable Integer propertyId) {
        return ResponseEntity.ok(propertyOwnershipService.getPropertyOwnerships(propertyId));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Auditable(action = "ADD_PROPERTY_OWNERSHIP", entityType = "PropertyOwnership", logResult = true)
    public ResponseEntity<PropertyOwnershipResponse> addOwnership(@Valid @RequestBody PropertyOwnershipRequest request) {
        return new ResponseEntity<>(propertyOwnershipService.addOwnership(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{ownershipId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Auditable(action = "UPDATE_PROPERTY_OWNERSHIP", entityType = "PropertyOwnership", entityIdParam = "ownershipId")
    public ResponseEntity<PropertyOwnershipResponse> updateOwnership(
            @PathVariable Integer ownershipId, 
            @Valid @RequestBody PropertyOwnershipRequest request) {
        return ResponseEntity.ok(propertyOwnershipService.updateOwnership(ownershipId, request));
    }
    
    @DeleteMapping("/{ownershipId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Auditable(action = "DELETE_PROPERTY_OWNERSHIP", entityType = "PropertyOwnership", entityIdParam = "ownershipId")
    public ResponseEntity<Void> deleteOwnership(@PathVariable Integer ownershipId) {
        propertyOwnershipService.deleteOwnership(ownershipId);
        return ResponseEntity.ok().build();
    }
}