package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.PropertyRequest;
import com.example.datn_realeaste_crm.dto.request.PropertyFilterRequest;
import com.example.datn_realeaste_crm.dto.response.PropertyAnalyticsResponse;
import com.example.datn_realeaste_crm.dto.response.PropertyResponse;
import com.example.datn_realeaste_crm.entity.AvailabilityStatus;
import com.example.datn_realeaste_crm.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            Pageable pageable) {
        // Public endpoint with advanced filtering
        return ResponseEntity.ok(propertyService.getAllProperties(
            propertyType, districtId, minPrice, maxPrice, bedrooms, availability, sortBy, sortDir, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PropertyResponse>> searchProperties(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) Integer districtId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer maxBedrooms,
            @RequestParam(required = false) Double minSize,
            @RequestParam(required = false) Double maxSize,
            @RequestParam(required = false) String availability,
            Pageable pageable) {
        // Advanced search endpoint
        return ResponseEntity.ok(propertyService.searchProperties(
            keyword, propertyType, districtId, minPrice, maxPrice,
            minBedrooms, maxBedrooms, minSize, maxSize, availability, pageable));
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
//    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER') and hasPermission(null, 'property_approve')")
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
    @PreAuthorize("(hasRole('ADMIN')) or " +
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
    @Auditable(action = "ASSIGN_PROPERTY_DISTRICT", entityType = "Property", entityIdParam = "id", logParams = true)
    public ResponseEntity<?> assignPropertyDistrictToUser(@PathVariable Integer id, @PathVariable Integer userId) {
        // Assign the property's district to a user
        return ResponseEntity.ok(propertyService.assignPropertyDistrictToUser(id, userId));
    }

    @PostMapping("/{id}/approve")
//    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER') and hasPermission('PROPERTY_APPROVE')")
    @Auditable(action = "APPROVE_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<?> approveProperty(@PathVariable Integer id) {
        // Approve a property
        return ResponseEntity.ok(propertyService.approveProperty(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    @Auditable(action = "REJECT_PROPERTY", entityType = "Property", entityIdParam = "id", logParams = true)
    public ResponseEntity<?> rejectProperty(@PathVariable Integer id, @RequestBody String reason) {
        // Reject a property
        return ResponseEntity.ok(propertyService.rejectProperty(id, reason));
    }

    // ===== PROPERTY STATUS MANAGEMENT =====
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or " +
            "(hasRole('MANAGER') and @propertyAuthorizationService.isPropertyInUserDepartment(#id)) or " +
            "(hasRole('PROPERTY_OWNER') and @propertyAuthorizationService.isPropertyOwner(#id))")
    @Auditable(action = "UPDATE_PROPERTY_STATUS", entityType = "Property", entityIdParam = "id", logParams = true)
    public ResponseEntity<PropertyResponse> updatePropertyStatus(
            @PathVariable Integer id, 
            @RequestParam Integer status,
            @RequestParam(required = false) String reason) {
        AvailabilityStatus availabilityStatus = AvailabilityStatus.fromCode(status);
        return ResponseEntity.ok(propertyService.updatePropertyStatus(id, availabilityStatus, reason));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN') or " +
            "(hasRole('MANAGER') and @propertyAuthorizationService.isPropertyInUserDepartment(#id)) or " +
            "(hasRole('PROPERTY_OWNER') and @propertyAuthorizationService.isPropertyOwner(#id))")
    @Auditable(action = "PUBLISH_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<PropertyResponse> publishProperty(@PathVariable Integer id) {
        return ResponseEntity.ok(propertyService.publishProperty(id));
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN') or " +
            "(hasRole('MANAGER') and @propertyAuthorizationService.isPropertyInUserDepartment(#id)) or " +
            "(hasRole('PROPERTY_OWNER') and @propertyAuthorizationService.isPropertyOwner(#id))")
    @Auditable(action = "UNPUBLISH_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<PropertyResponse> unpublishProperty(@PathVariable Integer id) {
        return ResponseEntity.ok(propertyService.unpublishProperty(id));
    }

    // ===== BULK OPERATIONS =====
    @PostMapping("/bulk/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    @Auditable(action = "BULK_APPROVE_PROPERTIES", entityType = "Property", logParams = true)
    public ResponseEntity<List<PropertyResponse>> bulkApproveProperties(@RequestBody List<Integer> propertyIds) {
        return ResponseEntity.ok(propertyService.bulkApproveProperties(propertyIds));
    }

    @PostMapping("/bulk/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    @Auditable(action = "BULK_REJECT_PROPERTIES", entityType = "Property", logParams = true)
    public ResponseEntity<List<PropertyResponse>> bulkRejectProperties(
            @RequestBody List<Integer> propertyIds, 
            @RequestParam String reason) {
        return ResponseEntity.ok(propertyService.bulkRejectProperties(propertyIds, reason));
    }

    @PostMapping("/bulk/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "BULK_ASSIGN_PROPERTIES", entityType = "Property", logParams = true)
    public ResponseEntity<String> bulkAssignProperties(
            @RequestBody List<Integer> propertyIds, 
            @RequestParam Integer userId) {
        propertyService.bulkAssignProperties(propertyIds, userId);
        return ResponseEntity.ok("Properties assigned successfully");
    }

    @PostMapping("/bulk/update-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "BULK_UPDATE_PROPERTY_STATUS", entityType = "Property", logParams = true)
    public ResponseEntity<List<PropertyResponse>> bulkUpdatePropertyStatus(
            @RequestBody List<Integer> propertyIds, 
            @RequestParam String status,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(propertyService.bulkUpdatePropertyStatus(propertyIds, status, reason));
    }

    // ===== PROPERTY ANALYTICS AND REPORTS =====
    @GetMapping("/analytics/overview")
//    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PropertyAnalyticsResponse> getPropertyAnalyticsOverview() {
        return ResponseEntity.ok(propertyService.getPropertyAnalyticsOverview());
    }

    @GetMapping("/analytics/department/{departmentId}")
    @PreAuthorize("hasRole('ADMIN') or " +
            "(hasRole('MANAGER') and @departmentAuthorizationService.belongsToDepartment(#departmentId))")
    public ResponseEntity<?> getDepartmentPropertyAnalytics(@PathVariable Integer departmentId) {
        return ResponseEntity.ok(propertyService.getDepartmentPropertyAnalytics(departmentId));
    }

    @GetMapping("/analytics/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<?> getUserPropertyAnalytics(@PathVariable Integer userId) {
        return ResponseEntity.ok(propertyService.getUserPropertyAnalytics(userId));
    }

    @GetMapping("/reports/performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getPropertyPerformanceReport(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(propertyService.getPropertyPerformanceReport(departmentId, userId, startDate, endDate));
    }

    // ===== PROPERTY MANAGEMENT FOR DIFFERENT ROLES =====
    @GetMapping("/my-properties")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PropertyResponse>> getMyProperties(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String propertyType,
            Pageable pageable) {
        return ResponseEntity.ok(propertyService.getMyProperties(status, propertyType, pageable));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('ADMIN') or " +
            "(hasRole('MANAGER') and @departmentAuthorizationService.belongsToDepartment(#departmentId))")
    public ResponseEntity<Page<PropertyResponse>> getDepartmentProperties(
            @PathVariable Integer departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String propertyType,
            Pageable pageable) {
        return ResponseEntity.ok(propertyService.getDepartmentProperties(departmentId, status, propertyType, pageable));
    }

    @GetMapping("/consultant/{consultantId}")
    @PreAuthorize("hasRole('ADMIN') or " +
            "(hasRole('MANAGER') and @userService.isUserInSameDepartment(#consultantId)) or " +
            "#consultantId == authentication.principal.userId")
    public ResponseEntity<Page<PropertyResponse>> getConsultantProperties(
            @PathVariable Integer consultantId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(propertyService.getConsultantProperties(consultantId, status, pageable));
    }

    // ===== PROPERTY TRANSFER AND OWNERSHIP =====
    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasRole('ADMIN') or @propertyAuthorizationService.isPropertyOwner(#id)")
    @Auditable(action = "TRANSFER_PROPERTY", entityType = "Property", entityIdParam = "id", logParams = true)
    public ResponseEntity<PropertyResponse> transferProperty(
            @PathVariable Integer id,
            @RequestParam Integer newOwnerId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(propertyService.transferProperty(id, newOwnerId, reason));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('ADMIN') or @propertyAuthorizationService.canAccessProperty(#id)")
    @Auditable(action = "DUPLICATE_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<PropertyResponse> duplicateProperty(@PathVariable Integer id) {
        return ResponseEntity.ok(propertyService.duplicateProperty(id));
    }

    // ===== PROPERTY FAVORITES AND BOOKMARKS =====
    @PostMapping("/{id}/bookmark")
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "BOOKMARK_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<String> bookmarkProperty(@PathVariable Integer id) {
        propertyService.bookmarkProperty(id);
        return ResponseEntity.ok("Property bookmarked successfully");
    }

    @DeleteMapping("/{id}/bookmark")
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "REMOVE_BOOKMARK_PROPERTY", entityType = "Property", entityIdParam = "id")
    public ResponseEntity<String> removeBookmark(@PathVariable Integer id) {
        propertyService.removeBookmark(id);
        return ResponseEntity.ok("Bookmark removed successfully");
    }

    @GetMapping("/bookmarked")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PropertyResponse>> getBookmarkedProperties(Pageable pageable) {
        return ResponseEntity.ok(propertyService.getBookmarkedProperties(pageable));
    }

    @GetMapping("/accessible")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get properties accessible by current user", 
               description = "Retrieve properties that the current user has access to through user_district_access table. " +
                           "Only properties with availability != 0 are returned, sorted by creation date (newest first). " +
                           "Supports advanced filtering and pagination.")
    @ApiResponse(responseCode = "200", description = "Properties retrieved successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<Page<PropertyResponse>> getAccessibleProperties(
            @Parameter(description = "Property type filter") @RequestParam(required = false) String propertyType,
            @Parameter(description = "District ID filter") @RequestParam(required = false) Integer districtId,
            @Parameter(description = "Minimum price filter") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Minimum bedrooms filter") @RequestParam(required = false) Integer minBedrooms,
            @Parameter(description = "Maximum bedrooms filter") @RequestParam(required = false) Integer maxBedrooms,
            @Parameter(description = "Minimum bathrooms filter") @RequestParam(required = false) Integer minBathrooms,
            @Parameter(description = "Maximum bathrooms filter") @RequestParam(required = false) Integer maxBathrooms,
            @Parameter(description = "Minimum size filter") @RequestParam(required = false) BigDecimal minSize,
            @Parameter(description = "Maximum size filter") @RequestParam(required = false) BigDecimal maxSize,
            @Parameter(description = "Floor filter") @RequestParam(required = false) Integer floor,
            @Parameter(description = "Availability status filter (0-4)") @RequestParam(required = false) Integer availability,
            @Parameter(description = "Keyword search in address and description") @RequestParam(required = false) String keyword,
            @Parameter(description = "Sort by field (default: createdAt)") @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: asc or desc (default: desc)") @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            Pageable pageable) {
        
        PropertyFilterRequest filterRequest = new PropertyFilterRequest();
        filterRequest.setPropertyType(propertyType);
        filterRequest.setDistrictId(districtId);
        filterRequest.setMinPrice(minPrice);
        filterRequest.setMaxPrice(maxPrice);
        filterRequest.setMinBedrooms(minBedrooms);
        filterRequest.setMaxBedrooms(maxBedrooms);
        filterRequest.setMinBathrooms(minBathrooms);
        filterRequest.setMaxBathrooms(maxBathrooms);
        filterRequest.setMinSize(minSize);
        filterRequest.setMaxSize(maxSize);
        filterRequest.setFloor(floor);
        filterRequest.setAvailability(availability);
        filterRequest.setKeyword(keyword);
        filterRequest.setSortBy(sortBy);
        filterRequest.setSortDirection(sortDirection);
        
        return ResponseEntity.ok(propertyService.getAccessibleProperties(filterRequest, pageable));
    }
}