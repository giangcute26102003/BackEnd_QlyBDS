package com.example.datn_realeaste_crm.service;

import com.example.datn_realeaste_crm.dto.request.CustomerRequirementRequest;
import com.example.datn_realeaste_crm.dto.response.CustomerRequirementResponse;
import com.example.datn_realeaste_crm.entity.Customer;
import com.example.datn_realeaste_crm.entity.CustomerRequirements;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.exception.UnauthorizedException;
import com.example.datn_realeaste_crm.repository.CustomerRepository;
import com.example.datn_realeaste_crm.repository.CustomerRequirementsRepository;
import com.example.datn_realeaste_crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerRequirementService {
    
    private final CustomerRequirementsRepository customerRequirementsRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    
    /**
     * Get all requirements with authorization filtering
     */
    public Page<CustomerRequirementResponse> getAllRequirements(Pageable pageable) {
        User currentUser = getCurrentUser();
        
        // Check if user has ADMIN role to see all requirements
        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(userRole -> "ADMIN".equals(userRole.getRole().getRoleName()));
        
        if (isAdmin) {
            log.debug("Admin user {} accessing all requirements", currentUser.getEmail());
            return customerRequirementsRepository.findAllWithCustomerAndUser(pageable)
                    .map(this::convertToCustomerRequirementResponse);
        } else {
            log.debug("User {} accessing own customer requirements only", currentUser.getEmail());
            return customerRequirementsRepository.findByCustomerUserUserId(currentUser.getUserId(), pageable)
                    .map(this::convertToCustomerRequirementResponse);
        }
    }
    
    /**
     * Get requirement by ID with authorization check
     */
    public CustomerRequirementResponse getRequirement(Integer id) {
        CustomerRequirements requirement = customerRequirementsRepository.findByIdWithCustomerAndUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer requirement not found with id: " + id));
        
        // Check ownership unless user is admin
        validateRequirementAccess(requirement);
        
        return convertToCustomerRequirementResponse(requirement);
    }
    
    /**
     * Get requirements for a specific customer
     */
    public Page<CustomerRequirementResponse> getCustomerRequirements(Integer customerId, Pageable pageable) {
        Customer customer = customerRepository.findByIdWithUser(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        
        // Check customer ownership unless user is admin
        validateCustomerAccess(customer);
        
        return customerRequirementsRepository.findByCustomerCustomerId(customerId, pageable)
                .map(this::convertToCustomerRequirementResponse);
    }
    
    /**
     * Create new requirement
     */
    @Transactional
    public CustomerRequirementResponse createRequirement(CustomerRequirementRequest request) {
        Customer customer = customerRepository.findByIdWithUser(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        
        // Check customer ownership unless user is admin
        validateCustomerAccess(customer);
        
        CustomerRequirements requirement = new CustomerRequirements();
        updateRequirementFromRequest(requirement, request);
        requirement.setCustomer(customer);
        requirement.setCreatedAt(LocalDateTime.now());
        requirement.setUpdatedAt(LocalDateTime.now());
        
        CustomerRequirements savedRequirement = customerRequirementsRepository.save(requirement);
        log.info("User {} created requirement {} for customer {}", getCurrentUser().getEmail(), 
                savedRequirement.getRequirementId(), customer.getCustomerId());
        
        return convertToCustomerRequirementResponse(savedRequirement);
    }
    
    /**
     * Update existing requirement
     */
    @Transactional
    public CustomerRequirementResponse updateRequirement(Integer id, CustomerRequirementRequest request) {
        CustomerRequirements requirement = customerRequirementsRepository.findByIdWithCustomerAndUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer requirement not found with id: " + id));
        
        // Check ownership unless user is admin
        validateRequirementAccess(requirement);
        
        // If customer ID is being changed, validate access to new customer
        if (!requirement.getCustomer().getCustomerId().equals(request.getCustomerId())) {
            Customer newCustomer = customerRepository.findByIdWithUser(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
            validateCustomerAccess(newCustomer);
            requirement.setCustomer(newCustomer);
        }
        
        updateRequirementFromRequest(requirement, request);
        requirement.setUpdatedAt(LocalDateTime.now());
        
        CustomerRequirements updatedRequirement = customerRequirementsRepository.save(requirement);
        log.info("User {} updated requirement {}", getCurrentUser().getEmail(), id);
        
        return convertToCustomerRequirementResponse(updatedRequirement);
    }
    
    /**
     * Delete requirement
     */
    @Transactional
    public void deleteRequirement(Integer id) {
        CustomerRequirements requirement = customerRequirementsRepository.findByIdWithCustomerAndUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer requirement not found with id: " + id));
        
        // Check ownership unless user is admin
        validateRequirementAccess(requirement);
        
        customerRequirementsRepository.delete(requirement);
        log.info("User {} deleted requirement {}", getCurrentUser().getEmail(), id);
    }
    
    /**
     * Update requirement fields from request
     */
    private void updateRequirementFromRequest(CustomerRequirements requirement, CustomerRequirementRequest request) {
        requirement.setPurpose(request.getPurpose());
        requirement.setBudgetMin(request.getBudgetMin());
        requirement.setBudgetMax(request.getBudgetMax());
        requirement.setPreferredLocation(request.getPreferredLocation());
        requirement.setPropertyType(request.getPropertyType());
        requirement.setSizeMin(request.getSizeMin());
        requirement.setBedrooms(request.getBedrooms());
        requirement.setBathrooms(request.getBathrooms());
        requirement.setOtherPreferences(request.getOtherPreferences());
    }
    
    /**
     * Convert entity to response DTO
     */
    private CustomerRequirementResponse convertToCustomerRequirementResponse(CustomerRequirements requirement) {
        return CustomerRequirementResponse.builder()
                .id(requirement.getRequirementId())
                .customerId(requirement.getCustomer().getCustomerId())
                .customerName(requirement.getCustomer().getName())
                .purpose(requirement.getPurpose())
                .budgetMin(requirement.getBudgetMin())
                .budgetMax(requirement.getBudgetMax())
                .preferredLocation(requirement.getPreferredLocation())
                .propertyType(requirement.getPropertyType())
                .sizeMin(requirement.getSizeMin())
                .bedrooms(requirement.getBedrooms())
                .bathrooms(requirement.getBathrooms())
                .otherPreferences(requirement.getOtherPreferences())
                .createdAt(requirement.getCreatedAt())
                .updatedAt(requirement.getUpdatedAt())
                .build();
    }
    
    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
    
    /**
     * Validate customer access - only owner or admin can access
     */
    private void validateCustomerAccess(Customer customer) {
        User currentUser = getCurrentUser();
        
        // Admin can access all customers
        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(userRole -> "ADMIN".equals(userRole.getRole().getRoleName()));
        
        if (isAdmin) {
            return; // Admin has access to all customers
        }
        
        // Check if current user owns this customer
        if (customer.getUser() == null || !customer.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("You don't have permission to access this customer");
        }
    }
    
    /**
     * Validate requirement access - only owner or admin can access
     */
    private void validateRequirementAccess(CustomerRequirements requirement) {
        validateCustomerAccess(requirement.getCustomer());
    }
}
