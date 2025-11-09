package com.example.datn_realeaste_crm.service;


import com.example.datn_realeaste_crm.dto.request.InteractionRequest;
import com.example.datn_realeaste_crm.dto.response.*;
import com.example.datn_realeaste_crm.entity.Customer;
import com.example.datn_realeaste_crm.entity.Interaction;
import com.example.datn_realeaste_crm.entity.Property;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionService {
    
    private final InteractionRepository interactionRepository;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final com.example.datn_realeaste_crm.security.crypto.DeterministicHasher deterministicHasher;
    
    public Page<InteractionResponse> getAllInteractions(Integer customerId, Integer propertyId, Pageable pageable) {
        Page<Interaction> interactions;
        
        if (customerId != null && propertyId != null) {
            interactions = interactionRepository.findByCustomerCustomerIdAndPropertyPropertyId(
                    customerId, propertyId, pageable);
        } else if (customerId != null) {
            interactions = interactionRepository.findByCustomerCustomerId(customerId, pageable);
        } else if (propertyId != null) {
            interactions = interactionRepository.findByPropertyPropertyId(propertyId, pageable);
        } else {
            interactions = interactionRepository.findAll(pageable);
        }
        
        return interactions.map(this::convertToInteractionResponse);
    }
    
    public InteractionResponse getInteraction(Integer id) {
        Interaction interaction = interactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interaction not found with id: " + id));
        
        return convertToInteractionResponse(interaction);
    }
    
    @Transactional
    public InteractionResponse createInteraction(InteractionRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + request.getPropertyId()));
        
        Interaction interaction = new Interaction();
        interaction.setCustomer(customer);
        interaction.setProperty(property);
        interaction.setDate(request.getDate());
        interaction.setDetails(request.getDetails());
        interaction.setCreatedAt(LocalDateTime.now());
        interaction.setUpdatedAt(LocalDateTime.now());
        
        Interaction savedInteraction = interactionRepository.save(interaction);
        log.info("Interaction created: ID={}, Customer={}, Property={}", 
                savedInteraction.getInteractionId(), customer.getCustomerId(), property.getPropertyId());
        
        return convertToInteractionResponse(savedInteraction);
    }
    
    /**
     * Get interactions for current user's customers
     */
    public Page<InteractionResponse> getMyInteractions(Integer propertyId, Pageable pageable) {
        User currentUser = getCurrentUser();
        
        // Get all interactions for customers assigned to current user
        Page<Interaction> interactions;
        
        if (propertyId != null) {
            interactions = interactionRepository.findByCustomer_User_UserIdAndPropertyPropertyId(
                    currentUser.getUserId(), propertyId, pageable);
        } else {
            interactions = interactionRepository.findByCustomer_User_UserId(
                    currentUser.getUserId(), pageable);
        }
        
        log.debug("Found {} interactions for user {}", interactions.getTotalElements(), currentUser.getUserId());
        
        return interactions.map(this::convertToInteractionResponse);
    }
    
    /**
     * Get interaction statistics for current user
     */
    public Map<String, Object> getMyInteractionStatistics() {
        User currentUser = getCurrentUser();
        
        long totalInteractions = interactionRepository.countByCustomer_User_UserId(currentUser.getUserId());
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInteractions", totalInteractions);
        stats.put("userId", currentUser.getUserId());
        stats.put("userName", currentUser.getName());
        
        return stats;
    }
    
    /**
     * Get interactions for a specific property (only property owner can view)
     */
    public Page<InteractionResponse> getPropertyInteractions(Integer propertyId, Pageable pageable) {
        User currentUser = getCurrentUser();
        
        // Verify property exists
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));
        
        // Check if current user is the owner of the property
        if (!property.getUser().getUserId().equals(currentUser.getUserId())) {
            log.warn("User {} attempted to access interactions for property {} but is not the owner", 
                    currentUser.getUserId(), propertyId);
            throw new AccessDeniedException("You do not have permission to view interactions for this property");
        }
        
        log.info("User {} (owner) accessing interactions for property {}", 
                currentUser.getUserId(), propertyId);
        
        Page<Interaction> interactions = interactionRepository.findByPropertyPropertyId(propertyId, pageable);
        
        return interactions.map(this::convertToInteractionResponse);
    }
    
    @Transactional
    public InteractionResponse updateInteraction(Integer id, InteractionRequest request) {
        Interaction interaction = interactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interaction not found with id: " + id));
        
        if (!interaction.getCustomer().getCustomerId().equals(request.getCustomerId())) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
            interaction.setCustomer(customer);
        }
        
        if (!interaction.getProperty().getPropertyId().equals(request.getPropertyId())) {
            Property property = propertyRepository.findById(request.getPropertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + request.getPropertyId()));
            interaction.setProperty(property);
        }
        
        interaction.setDate(request.getDate());
        interaction.setDetails(request.getDetails());
        interaction.setUpdatedAt(LocalDateTime.now());
        
        Interaction updatedInteraction = interactionRepository.save(interaction);
        
        return convertToInteractionResponse(updatedInteraction);
    }
    
    @Transactional
    public void deleteInteraction(Integer id) {
        if (!interactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Interaction not found with id: " + id);
        }
        
        interactionRepository.deleteById(id);
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        byte[] emailHash = deterministicHasher.emailHash(email);
        
        return userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
    
    private InteractionResponse convertToInteractionResponse(Interaction interaction) {
        return InteractionResponse.builder()
                .id(interaction.getInteractionId())
                .customerId(interaction.getCustomer().getCustomerId())
                .customerName(interaction.getCustomer().getName())
                .propertyId(interaction.getProperty().getPropertyId())
                .propertyAddress(interaction.getProperty().getAddressProperty())
                .propertyType(interaction.getProperty().getPropertyType())
                .date(interaction.getDate())
                .details(interaction.getDetails())
                .createdAt(interaction.getCreatedAt())
                .updatedAt(interaction.getUpdatedAt())
                .build();
    }
}