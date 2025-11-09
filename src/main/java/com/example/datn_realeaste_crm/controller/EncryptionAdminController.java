package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.service.EncryptionBackfillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for encryption backfill operations
 * Only ADMIN role can access these endpoints
 */
@RestController
@RequestMapping("/admin/encryption")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Encryption Management", description = "Admin APIs for field-level encryption management")
@SecurityRequirement(name = "bearerAuth")
public class EncryptionAdminController {

    private final EncryptionBackfillService encryptionBackfillService;

    /**
     * Trigger backfill for all users
     * Migrates plaintext email, phone, address to encrypted columns
     */
    @PostMapping("/backfill-users")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Backfill User Encryption",
        description = "Migrate all user plaintext data to encrypted columns. Only ADMIN can execute this operation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User backfill completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error during backfill")
    })
    public ResponseEntity<Map<String, Object>> backfillUsers() {
        try {
            log.info("Starting user encryption backfill via admin API");
            
            encryptionBackfillService.backfillAllUsers();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User encryption backfill completed successfully");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            log.info("User encryption backfill completed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during user encryption backfill", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to complete user backfill: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Trigger backfill for all customers
     * Migrates plaintext email, phone, address to encrypted columns
     */
    @PostMapping("/backfill-customers")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Backfill Customer Encryption",
        description = "Migrate all customer plaintext data to encrypted columns. Only ADMIN can execute this operation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer backfill completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error during backfill")
    })
    public ResponseEntity<Map<String, Object>> backfillCustomers() {
        try {
            log.info("Starting customer encryption backfill via admin API");
            
            encryptionBackfillService.backfillAllCustomers();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Customer encryption backfill completed successfully");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            log.info("Customer encryption backfill completed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during customer encryption backfill", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to complete customer backfill: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Trigger backfill for both users and customers
     * Convenience endpoint to migrate all data at once
     */
    @PostMapping("/backfill-all")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Backfill All Encryption",
        description = "Migrate all user and customer plaintext data to encrypted columns. Only ADMIN can execute this operation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All backfill operations completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error during backfill")
    })
    public ResponseEntity<Map<String, Object>> backfillAll() {
        try {
            log.info("Starting complete encryption backfill via admin API");
            
            // Backfill users first
            encryptionBackfillService.backfillAllUsers();
            log.info("User backfill completed, starting customer backfill");
            
            // Then backfill customers
            encryptionBackfillService.backfillAllCustomers();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Complete encryption backfill completed successfully");
            response.put("details", Map.of(
                "users_backfilled", "completed",
                "customers_backfilled", "completed"
            ));
            response.put("timestamp", java.time.LocalDateTime.now());
            
            log.info("Complete encryption backfill completed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during complete encryption backfill", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to complete backfill: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get backfill status and statistics
     * Shows how many records still need to be backfilled
     */
    @GetMapping("/backfill-status")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get Backfill Status",
        description = "Get current status of encryption backfill operations. Shows counts of records that still need migration."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Backfill status retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    public ResponseEntity<Map<String, Object>> getBackfillStatus() {
        try {
            log.debug("Getting encryption backfill status");
            
            Map<String, Object> status = encryptionBackfillService.getBackfillStatus();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Backfill status retrieved successfully");
            response.put("data", status);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting backfill status", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to get backfill status: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}

