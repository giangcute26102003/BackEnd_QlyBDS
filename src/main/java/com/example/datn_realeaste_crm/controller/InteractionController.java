package com.example.datn_realeaste_crm.controller;


import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.InteractionRequest;
import com.example.datn_realeaste_crm.dto.response.InteractionResponse;
import com.example.datn_realeaste_crm.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interactions")
@RequiredArgsConstructor
public class InteractionController {
    
    private final InteractionService interactionService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<InteractionResponse>> getAllInteractions(
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) Integer propertyId,
            Pageable pageable) {
        return ResponseEntity.ok(interactionService.getAllInteractions(customerId, propertyId, pageable));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('interaction_view')")
    public ResponseEntity<InteractionResponse> getInteraction(@PathVariable Integer id) {
        return ResponseEntity.ok(interactionService.getInteraction(id));
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('interaction_create')")
    @Auditable(action = "CREATE_INTERACTION", entityType = "Interaction", logResult = true)
    public ResponseEntity<InteractionResponse> createInteraction(@Valid @RequestBody InteractionRequest request) {
        return new ResponseEntity<>(interactionService.createInteraction(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('interaction_create')")
    @Auditable(action = "UPDATE_INTERACTION", entityType = "Interaction", entityIdParam = "id")
    public ResponseEntity<InteractionResponse> updateInteraction(
            @PathVariable Integer id, 
            @Valid @RequestBody InteractionRequest request) {
        return ResponseEntity.ok(interactionService.updateInteraction(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Auditable(action = "DELETE_INTERACTION", entityType = "Interaction", entityIdParam = "id")
    public ResponseEntity<Void> deleteInteraction(@PathVariable Integer id) {
        interactionService.deleteInteraction(id);
        return ResponseEntity.ok().build();
    }
}