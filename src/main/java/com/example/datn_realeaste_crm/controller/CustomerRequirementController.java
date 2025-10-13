package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.CustomerRequirementRequest;
import com.example.datn_realeaste_crm.dto.response.CustomerRequirementResponse;
import com.example.datn_realeaste_crm.service.CustomerRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer-requirements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Requirements", description = "Customer requirement management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CustomerRequirementController {
    
    private final CustomerRequirementService customerRequirementService;
    
    /**
     * Get all customer requirements with pagination
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all customer requirements", 
               description = "Get paginated list of customer requirements. Admin sees all, regular users see only their customers' requirements.")
    public ResponseEntity<Page<CustomerRequirementResponse>> getAllRequirements(Pageable pageable) {
        try {
            Page<CustomerRequirementResponse> requirements = customerRequirementService.getAllRequirements(pageable);
            log.debug("Retrieved {} customer requirements", requirements.getTotalElements());
            return ResponseEntity.ok(requirements);
        } catch (Exception e) {
            log.error("Error getting customer requirements", e);
            throw e;
        }
    }
    
    /**
     * Get customer requirement by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer requirement by ID", 
               description = "Get customer requirement details by ID. Only owner or admin can access.")
    public ResponseEntity<CustomerRequirementResponse> getRequirement(@PathVariable Integer id) {
        try {
            CustomerRequirementResponse requirement = customerRequirementService.getRequirement(id);
            log.debug("Retrieved customer requirement {}", id);
            return ResponseEntity.ok(requirement);
        } catch (Exception e) {
            log.error("Error getting customer requirement {}", id, e);
            throw e;
        }
    }
    
    /**
     * Get requirements for a specific customer
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get requirements for customer", 
               description = "Get all requirements for a specific customer. Only owner or admin can access.")
    public ResponseEntity<Page<CustomerRequirementResponse>> getCustomerRequirements(
            @PathVariable Integer customerId, 
            Pageable pageable) {
        try {
            Page<CustomerRequirementResponse> requirements = customerRequirementService.getCustomerRequirements(customerId, pageable);
            log.debug("Retrieved {} requirements for customer {}", requirements.getTotalElements(), customerId);
            return ResponseEntity.ok(requirements);
        } catch (Exception e) {
            log.error("Error getting requirements for customer {}", customerId, e);
            throw e;
        }
    }
    
    /**
     * Create new customer requirement
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "CREATE_CUSTOMER_REQUIREMENT", entityType = "CustomerRequirement", logResult = true)
    @Operation(summary = "Create customer requirement", 
               description = "Create a new requirement for a customer. Only owner or admin can create.")
    public ResponseEntity<CustomerRequirementResponse> createRequirement(
            @Valid @RequestBody CustomerRequirementRequest request) {
        try {
            CustomerRequirementResponse requirement = customerRequirementService.createRequirement(request);
            log.info("Created customer requirement {} for customer {}", requirement.getId(), request.getCustomerId());
            return new ResponseEntity<>(requirement, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating customer requirement", e);
            throw e;
        }
    }
    
    /**
     * Update existing customer requirement
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "UPDATE_CUSTOMER_REQUIREMENT", entityType = "CustomerRequirement", entityIdParam = "id")
    @Operation(summary = "Update customer requirement", 
               description = "Update customer requirement information. Only owner or admin can update.")
    public ResponseEntity<CustomerRequirementResponse> updateRequirement(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerRequirementRequest request) {
        try {
            CustomerRequirementResponse requirement = customerRequirementService.updateRequirement(id, request);
            log.info("Updated customer requirement {}", id);
            return ResponseEntity.ok(requirement);
        } catch (Exception e) {
            log.error("Error updating customer requirement {}", id, e);
            throw e;
        }
    }
    
    /**
     * Delete customer requirement
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "DELETE_CUSTOMER_REQUIREMENT", entityType = "CustomerRequirement", entityIdParam = "id")
    @Operation(summary = "Delete customer requirement", 
               description = "Delete customer requirement. Only owner or admin can delete.")
    public ResponseEntity<Void> deleteRequirement(@PathVariable Integer id) {
        try {
            customerRequirementService.deleteRequirement(id);
            log.info("Deleted customer requirement {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting customer requirement {}", id, e);
            throw e;
        }
    }
}
