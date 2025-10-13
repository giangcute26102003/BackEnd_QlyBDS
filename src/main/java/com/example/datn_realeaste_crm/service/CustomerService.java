package com.example.datn_realeaste_crm.service;


import com.example.datn_realeaste_crm.dto.request.CustomerRequest;
import com.example.datn_realeaste_crm.dto.response.CustomerResponse;
import com.example.datn_realeaste_crm.entity.Customer;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.exception.UnauthorizedException;

import com.example.datn_realeaste_crm.repository.CustomerRepository;
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
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    
    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        User currentUser = getCurrentUser();
        
        // Check if user has ADMIN role to see all customers
        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(userRole -> "ADMIN".equals(userRole.getRole().getRoleName()));
        
        if (isAdmin) {
            log.debug("Admin user {} accessing all customers", currentUser.getEmail());
            return customerRepository.findAllWithUser(pageable)
                    .map(this::convertToCustomerResponse);
        } else {
            log.debug("User {} accessing own customers only", currentUser.getEmail());
            return customerRepository.findByUserUserId(currentUser.getUserId(), pageable)
                    .map(this::convertToCustomerResponse);
        }
    }
    
    public CustomerResponse getCustomer(Integer id) {
        Customer customer = customerRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        
        // Check ownership unless user is admin
        validateCustomerAccess(customer);
        
        return convertToCustomerResponse(customer);
    }
    
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        User currentUser = getCurrentUser();
        
        Customer customer = new Customer();
        updateCustomerFromRequest(customer, request);
        customer.setUser(currentUser); // Assign to current user
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        
        Customer savedCustomer = customerRepository.save(customer);
        log.info("User {} created customer {}", currentUser.getEmail(), savedCustomer.getCustomerId());
        
        return convertToCustomerResponse(savedCustomer);
    }
    
    @Transactional
    public CustomerResponse updateCustomer(Integer id, CustomerRequest request) {
        Customer customer = customerRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        
        // Check ownership unless user is admin
        validateCustomerAccess(customer);
        
        updateCustomerFromRequest(customer, request);
        customer.setUpdatedAt(LocalDateTime.now());
        
        Customer updatedCustomer = customerRepository.save(customer);
        log.info("User {} updated customer {}", getCurrentUser().getEmail(), updatedCustomer.getCustomerId());
        
        return convertToCustomerResponse(updatedCustomer);
    }
    
    @Transactional
    public void deleteCustomer(Integer id) {
        Customer customer = customerRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        
        // Check ownership unless user is admin
        validateCustomerAccess(customer);
        
        customerRepository.delete(customer);
        log.info("User {} deleted customer {}", getCurrentUser().getEmail(), id);
    }
    
    // Customer requirement methods have been moved to CustomerRequirementService
    
    private void updateCustomerFromRequest(Customer customer, CustomerRequest request) {
        customer.setName(request.getName());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setDob(request.getDob());
    }
    
    private CustomerResponse convertToCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getCustomerId())
                .name(customer.getName())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .dob(customer.getDob())
                .userId(customer.getUser() != null ? customer.getUser().getUserId() : null)
                .userName(customer.getUser() != null ? customer.getUser().getName() : null)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
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
}