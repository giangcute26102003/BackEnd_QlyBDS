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

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyController {
    
    private final PropertyService propertyService;
    
    @GetMapping
    public ResponseEntity<Page<PropertyResponse>> getAllProperties(
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) Integer districtId,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer bedrooms,
            Pageable pageable) {
        return ResponseEntity.ok(propertyService.getAllProperties(propertyType, districtId, minPrice, maxPrice, bedrooms, pageable));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('property_view') or @propertyAuthorizationService.canAccessProperty(authentication, #id)")
    public ResponseEntity<PropertyResponse> getProperty(@PathVariable Integer id) {
        return ResponseEntity.ok(propertyService.getProperty(id));
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('property_create')")
    @Auditable(action = "CREATE_PROPERTY", entityType = "Property", logResult = true)
    public ResponseEntity<PropertyResponse> createProperty(@Valid @RequestBody PropertyRequest propertyRequest) {
        return ResponseEntity.ok(propertyService.createProperty(propertyRequest));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('property_update') or @propertyAuthorizationService.isPropertyOwner(authentication, #id)")
    @Auditable(action = "UPDATE_PROPERTY", entityType = "Property", entityIdParam = "id", logParams = true)
    public ResponseEntity<PropertyResponse> updateProperty(
            @PathVariable Integer id,
            @Valid @RequestBody PropertyRequest propertyRequest) {
        return ResponseEntity.ok(propertyService.updateProperty(id, propertyRequest));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Auditable(action = "DELETE_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<Void> deleteProperty(@PathVariable Integer id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('property_approve')")
    @Auditable(action = "APPROVE_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<PropertyResponse> approveProperty(@PathVariable Integer id) {
        return ResponseEntity.ok(propertyService.approveProperty(id));
    }
    
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('property_reject')")
    @Auditable(action = "REJECT_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<PropertyResponse> rejectProperty(@PathVariable Integer id) {
        return ResponseEntity.ok(propertyService.rejectProperty(id));
    }
}