package com.example.datn_realeaste_crm.controller;



import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.CustomerRequest;
import com.example.datn_realeaste_crm.dto.request.CustomerRequirementRequest;
import com.example.datn_realeaste_crm.dto.response.*;
import com.example.datn_realeaste_crm.service.CustomerService;
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
@RequestMapping("/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Management", description = "Customer management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {
    
    private final CustomerService customerService;
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my customers", description = "Get paginated list of customers belonging to the current user.")
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(Pageable pageable) {
        try {
            Page<CustomerResponse> customers = customerService.getAllCustomers(pageable);
            log.debug("Retrieved {} customers", customers.getTotalElements());
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            log.error("Error getting customers", e);
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer by ID", description = "Get customer details by ID. Only owner or admin can access.")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Integer id) {
        try {
            CustomerResponse customer = customerService.getCustomer(id);
            log.debug("Retrieved customer {}", id);
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.error("Error getting customer {}", id, e);
            throw e;
        }
    }
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "CREATE_CUSTOMER", entityType = "Customer", logResult = true)
    @Operation(summary = "Create new customer", description = "Create a new customer assigned to current user.")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        try {
            CustomerResponse customer = customerService.createCustomer(request);
            log.info("Created customer {}", customer.getId());
            return new ResponseEntity<>(customer, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating customer", e);
            throw e;
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "UPDATE_CUSTOMER", entityType = "Customer", entityIdParam = "id")
    @Operation(summary = "Update customer", description = "Update customer information. Only owner or admin can update.")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Integer id, 
            @Valid @RequestBody CustomerRequest request) {
        try {
            CustomerResponse customer = customerService.updateCustomer(id, request);
            log.info("Updated customer {}", id);
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.error("Error updating customer {}", id, e);
            throw e;
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "DELETE_CUSTOMER", entityType = "Customer", entityIdParam = "id")
    @Operation(summary = "Delete customer", description = "Delete customer. Only owner or admin can delete.")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        try {
            customerService.deleteCustomer(id);
            log.info("Deleted customer {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting customer {}", id, e);
            throw e;
        }
    }
    
    // Note: Customer requirement endpoints have been moved to CustomerRequirementController
    // Use /customer-requirements endpoints instead
}