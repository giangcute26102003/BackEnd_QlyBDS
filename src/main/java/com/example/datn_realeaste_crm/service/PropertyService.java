package com.example.datn_realeaste_crm.service;

import com.example.datn_realeaste_crm.dto.request.PropertyRequest;
import com.example.datn_realeaste_crm.dto.request.PropertyFilterRequest;
import com.example.datn_realeaste_crm.dto.response.*;
import com.example.datn_realeaste_crm.entity.*;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.DepartmentRepository;
import com.example.datn_realeaste_crm.repository.DistrictRepository;
import com.example.datn_realeaste_crm.repository.*;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final DistrictRepository districtRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PropertyOwnershipRepository propertyOwnershipRepository;
    private final UserDistrictAccessRepository userDistrictAccessRepository;
    private final ReviewRepository reviewRepository;
    private final com.example.datn_realeaste_crm.security.crypto.DeterministicHasher deterministicHasher;
    private final S3Service s3Service;

    public Page<PropertyResponse> getAllProperties(String propertyType, Integer districtId, Integer minPrice,
            Integer maxPrice, Integer bedrooms, Pageable pageable) {
        Specification<Property> spec = Specification.where(null);

        if (propertyType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("propertyType"), propertyType));
        }

        if (districtId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("district").get("id"), districtId));
        }

        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), new BigDecimal(minPrice)));
        }

        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), new BigDecimal(maxPrice)));
        }

        if (bedrooms != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("bedrooms"), bedrooms));
        }

        return propertyRepository.findAll(spec, pageable)
                .map(this::convertToPropertyResponse);
    }

    // Method overload to support PropertyController
    public List<PropertyResponse> getAllProperties() {
        return propertyRepository.findAll().stream()
                .map(this::convertToPropertyResponse)
                .collect(Collectors.toList());
    }

    public List<PropertyResponse> getPropertiesOwnedByCurrentUser() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return List.of();
        }

        List<PropertyOwnership> ownerships = propertyOwnershipRepository
                .findByUserUserId(currentUser.getUserId());

        return ownerships.stream()
                .map(ownership -> convertToPropertyResponse(ownership.getProperty()))
                .collect(Collectors.toList());
    }

    public List<PropertyResponse> getPropertiesAssignedToCurrentUser() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return List.of();
        }

        List<UserDistrictAccess> accesses = userDistrictAccessRepository
                .findByUserUserId(currentUser.getUserId());

        // Get all properties in the districts the user has access to
        List<Integer> districtIds = accesses.stream()
                .map(access -> access.getDistrict().getId())
                .collect(Collectors.toList());

        if (districtIds.isEmpty()) {
            return List.of();
        }

        List<Property> properties = propertyRepository.findByDistrictIdIn(districtIds);
        return properties.stream()
                .map(this::convertToPropertyResponse)
                .collect(Collectors.toList());
    }

    public List<PropertyResponse> getPropertiesByDepartment() {
        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getDepartment() == null) {
            return List.of();
        }

        Integer departmentId = currentUser.getDepartment().getDepartmentId();
        List<Property> properties = propertyRepository.findAll()
                .stream()
                .filter(p -> p.getDepartment() != null &&
                        p.getDepartment().getDepartmentId().equals(departmentId))
                .collect(Collectors.toList());

        return properties.stream()
                .map(this::convertToPropertyResponse)
                .collect(Collectors.toList());
    }

    public List<PropertyResponse> getPropertiesPendingApproval() {
        List<Property> pendingProperties = propertyRepository.findAll()
                .stream()
                .filter(p -> AvailabilityStatus.PENDING.equals(p.getAvailability()))
                .collect(Collectors.toList());

        return pendingProperties.stream()
                .map(this::convertToPropertyResponse)
                .collect(Collectors.toList());
    }

    public PropertyResponse getProperty(Integer id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        PropertyResponse response = convertToPropertyResponse(property);
        // Populate images for detail endpoint only
        if (property.getImages() != null && !property.getImages().isEmpty()) {
            List<PropertyImageResponse> imageResponses = property.getImages().stream()
                    .map(img -> PropertyImageResponse.builder()
                            .id(img.getId())
                            .propertyId(property.getPropertyId())
                            .imageUrl(img.getImageUrl())
                            .originalFilename(img.getOriginalFilename())
                            .fileSize(img.getFileSize())
                            .contentType(img.getContentType())
                            .uploadedAt(img.getUploadedAt())
                            .build())
                    .collect(Collectors.toList());
            response.setImages(imageResponses);
        }

        return response;
    }

    // Alias for getProperty to match controller method
    public PropertyResponse getPropertyById(Integer id) {
        return getProperty(id);
    }

    @Transactional
    public PropertyResponse createProperty(PropertyRequest propertyRequest) {
        Property property = new Property();

        // Always get user from authentication context
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to create a property");
        }

        property.setUser(currentUser);

        // Set department from user if not provided in request
        if (propertyRequest.getDepartmentId() == null && currentUser.getDepartment() != null) {
            property.setDepartment(currentUser.getDepartment());
        }

        updatePropertyFromRequest(property, propertyRequest);
        property.setCreatedAt(LocalDateTime.now());
        property.setUpdatedAt(LocalDateTime.now());

        Property savedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(savedProperty);
    }

    // Method overload to support Object parameter
    @Transactional
    public PropertyResponse createProperty(Object propertyDto) {
        if (propertyDto instanceof PropertyRequest) {
            return createProperty((PropertyRequest) propertyDto);
        } else {
            throw new IllegalArgumentException("Invalid property request format");
        }
    }

    @Transactional
    public PropertyResponse updateProperty(Integer id, PropertyRequest propertyRequest) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        // Always get user from authentication context
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to update a property");
        }

        // Set userId in the request
        propertyRequest.setUserId(currentUser.getUserId());

        updatePropertyFromRequest(property, propertyRequest);
        property.setUpdatedAt(LocalDateTime.now());

        Property updatedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(updatedProperty);
    }

    // Method overload to support Object parameter
    @Transactional
    public PropertyResponse updateProperty(Integer id, Object propertyDto) {
        if (propertyDto instanceof PropertyRequest) {
            return updateProperty(id, (PropertyRequest) propertyDto);
        } else {
            throw new IllegalArgumentException("Invalid property request format");
        }
    }

    @Transactional
    public void deleteProperty(Integer id) {
        propertyRepository.deleteById(id);
    }

    @Transactional
    public PropertyResponse approveProperty(Integer id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        property.setAvailability(AvailabilityStatus.AVAILABLE);
        property.setUpdatedAt(LocalDateTime.now());

        Property updatedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(updatedProperty);
    }

    @Transactional
    public PropertyResponse rejectProperty(Integer id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        property.setAvailability(AvailabilityStatus.NOT_AVAILABLE);
        property.setUpdatedAt(LocalDateTime.now());

        Property updatedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(updatedProperty);
    }

    // Method overload to support reason parameter
    @Transactional
    public PropertyResponse rejectProperty(Integer id, String reason) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        property.setAvailability(AvailabilityStatus.NOT_AVAILABLE);
        property.setDescription(property.getDescription() + "\n[Rejection reason: " + reason + "]");
        property.setUpdatedAt(LocalDateTime.now());

        Property updatedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(updatedProperty);
    }

    @Transactional
    public PropertyResponse assignPropertyDistrictToUser(Integer propertyId, Integer userId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Assign the property's district to the user
        Integer districtId = property.getDistrict().getId();
        
        // Check if district access already exists
        Optional<UserDistrictAccess> existingAccess = userDistrictAccessRepository
                .findByUserUserIdAndDistrictId(userId, districtId);

        if (existingAccess.isEmpty()) {
            UserDistrictAccess access = UserDistrictAccess.builder()
                    .user(user)
                    .district(property.getDistrict())
                    .accessGrantedAt(LocalDateTime.now())
                    .build();
            userDistrictAccessRepository.save(access);
        }

        return convertToPropertyResponse(property);
    }

    private void updatePropertyFromRequest(Property property, PropertyRequest request) {
        property.setAddressProperty(request.getAddress());
        property.setPropertyType(request.getPropertyType());
        property.setSize(request.getSize());
        property.setFloor(request.getFloor());
        property.setThumbnail(request.getThumbnail());
        property.setBedrooms(request.getBedrooms());
        property.setBathrooms(request.getBathrooms());
        property.setDescription(request.getDescription());
        property.setPrice(request.getPrice());
        property.setLegalDocuments(request.getLegalDocuments());
        property.setPhoneOwner(request.getPhoneOwner());

        // Set availability from request or default to PENDING for new properties
        if (request.getAvailability() != null) {
            property.setAvailability(request.getAvailability());
        } else if (property.getAvailability() == null) {
            property.setAvailability(AvailabilityStatus.PENDING);
        }

        // Set district if provided
        if (request.getDistrictId() != null) {
            property.setDistrict(districtRepository.findById(request.getDistrictId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "District not found with id: " + request.getDistrictId())));
        }

        // Set department if provided
        if (request.getDepartmentId() != null) {
            property.setDepartment(departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id: " + request.getDepartmentId())));
        }
    }

    private PropertyResponse convertToPropertyResponse(Property property) {
        return PropertyResponse.builder()
                .id(property.getPropertyId())
                .address(property.getAddressProperty())
                .propertyType(property.getPropertyType())
                .size(property.getSize())
                .floor(property.getFloor())
                .thumbnail(property.getThumbnail())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .description(property.getDescription())
                .price(property.getPrice())
                .legalDocuments(property.getLegalDocuments())
                .availability(property.getAvailability())
                .availabilityText(property.getAvailability() != null ? property.getAvailability().getDescription() : null)
                .phoneOwner(property.getPhoneOwner())
                .districtId(property.getDistrict() != null ? property.getDistrict().getId() : null)
                .districtName(property.getDistrict() != null ? property.getDistrict().getName() : null)
                .departmentId(property.getDepartment() != null ? property.getDepartment().getDepartmentId() : null)
                .departmentName(property.getDepartment() != null ? property.getDepartment().getName() : null)
                .userId(property.getUser() != null ? property.getUser().getUserId() : null)
                .userName(property.getUser() != null ? property.getUser().getName() : null)
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }

    // ===== ENHANCED SEARCH AND FILTERING =====
    public Page<PropertyResponse> getAllProperties(String propertyType, Integer districtId, Integer minPrice,
            Integer maxPrice, Integer bedrooms, String availability, String sortBy, String sortDir, Pageable pageable) {
        Specification<Property> spec = Specification.where(null);

        if (propertyType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("propertyType"), propertyType));
        }

        if (districtId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("district").get("id"), districtId));
        }

        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), new BigDecimal(minPrice)));
        }

        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), new BigDecimal(maxPrice)));
        }

        if (bedrooms != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("bedrooms"), bedrooms));
        }

        if (availability != null) {
            try {
                AvailabilityStatus status = AvailabilityStatus.fromCode(Integer.parseInt(availability));
                spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), status));
            } catch (NumberFormatException e) {
                // Try to parse as string for backward compatibility
                AvailabilityStatus status = AvailabilityStatus.fromString(availability);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), status));
            }
        }

        return propertyRepository.findAll(spec, pageable)
                .map(this::convertToPropertyResponse);
    }

    public Page<PropertyResponse> searchProperties(String keyword, String propertyType, Integer districtId,
                                                   BigDecimal minPrice, BigDecimal maxPrice, Integer minBedrooms, Integer maxBedrooms,
                                                   Double minSize, Double maxSize, String availability, Pageable pageable) {

        Specification<Property> spec = buildSearchSpecification(keyword, propertyType, districtId,
                minPrice, maxPrice, minBedrooms, maxBedrooms, minSize, maxSize, availability);

        return propertyRepository.findAll(spec, pageable)
                .map(this::convertToPropertyResponse);
    }

    private Specification<Property> buildSearchSpecification(String keyword, String propertyType, Integer districtId,
                                                             BigDecimal minPrice, BigDecimal maxPrice, Integer minBedrooms, Integer maxBedrooms,
                                                             Double minSize, Double maxSize, String availability) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Predicate address = cb.like(cb.lower(root.get("addressProperty")), pattern);
                Predicate description = cb.like(cb.lower(root.get("description")), pattern);
                Predicate type = cb.like(cb.lower(root.get("propertyType")), pattern);
                predicates.add(cb.or(address, description, type));
            }

            if (propertyType != null && !propertyType.isEmpty()) {
                predicates.add(cb.equal(root.get("propertyType"), propertyType));
            }

            if (districtId != null) {
                predicates.add(cb.equal(root.get("district").get("id"), districtId));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (minBedrooms != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), minBedrooms));
            }

            if (maxBedrooms != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bedrooms"), maxBedrooms));
            }

            if (minSize != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("size"), minSize));
            }

            if (maxSize != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("size"), maxSize));
            }

            if (availability != null && !availability.isEmpty()) {
                try {
                    AvailabilityStatus status = AvailabilityStatus.fromCode(Integer.parseInt(availability));
                    predicates.add(cb.equal(root.get("availability"), status));
                } catch (NumberFormatException e) {
                    // Try to parse as string for backward compatibility
                    AvailabilityStatus status = AvailabilityStatus.fromString(availability);
                    predicates.add(cb.equal(root.get("availability"), status));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ===== PROPERTY STATUS MANAGEMENT =====
    @Transactional
    public PropertyResponse updatePropertyStatus(Integer id, AvailabilityStatus status, String reason) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        AvailabilityStatus oldStatus = property.getAvailability();
        property.setAvailability(status);
        
        if (reason != null && !reason.trim().isEmpty()) {
            String statusLog = String.format("\n[Status changed from %s to %s on %s - Reason: %s]", 
                oldStatus != null ? oldStatus.getDescription() : "Unknown", 
                status.getDescription(), 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), reason);
            property.setDescription(property.getDescription() + statusLog);
        }
        
        property.setUpdatedAt(LocalDateTime.now());
        Property updatedProperty = propertyRepository.save(property);
        return convertToPropertyResponse(updatedProperty);
    }

    @Transactional
    public PropertyResponse publishProperty(Integer id) {
        return updatePropertyStatus(id, AvailabilityStatus.AVAILABLE, "Property published");
    }

    @Transactional
    public PropertyResponse unpublishProperty(Integer id) {
        return updatePropertyStatus(id, AvailabilityStatus.PENDING, "Property unpublished");
    }

    // ===== BULK OPERATIONS =====
    @Transactional
    public List<PropertyResponse> bulkApproveProperties(List<Integer> propertyIds) {
        List<PropertyResponse> results = new ArrayList<>();
        for (Integer id : propertyIds) {
            try {
                results.add(approveProperty(id));
            } catch (Exception e) {
                // Log error but continue with other properties
                System.err.println("Failed to approve property " + id + ": " + e.getMessage());
            }
        }
        return results;
    }

    @Transactional
    public List<PropertyResponse> bulkRejectProperties(List<Integer> propertyIds, String reason) {
        List<PropertyResponse> results = new ArrayList<>();
        for (Integer id : propertyIds) {
            try {
                results.add(rejectProperty(id, reason));
            } catch (Exception e) {
                // Log error but continue with other properties
                System.err.println("Failed to reject property " + id + ": " + e.getMessage());
            }
        }
        return results;
    }

    @Transactional
    public void bulkAssignProperties(List<Integer> propertyIds, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Get all unique districts from the properties
        Set<Integer> districtIds = new HashSet<>();
        for (Integer propertyId : propertyIds) {
            try {
                Property property = propertyRepository.findById(propertyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));
                districtIds.add(property.getDistrict().getId());
            } catch (Exception e) {
                // Log error but continue with other properties
                System.err.println("Failed to get property " + propertyId + ": " + e.getMessage());
            }
        }

        // Assign all unique districts to the user
        for (Integer districtId : districtIds) {
            try {
                District district = districtRepository.findById(districtId)
                        .orElseThrow(() -> new ResourceNotFoundException("District not found with id: " + districtId));
                
                // Check if district access already exists
                if (!userDistrictAccessRepository.existsByUserUserIdAndDistrictId(userId, districtId)) {
                    UserDistrictAccess access = UserDistrictAccess.builder()
                            .user(user)
                            .district(district)
                            .accessGrantedAt(LocalDateTime.now())
                            .build();
                    userDistrictAccessRepository.save(access);
                }
            } catch (Exception e) {
                // Log error but continue with other districts
                System.err.println("Failed to assign district " + districtId + " to user " + userId + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    public List<PropertyResponse> bulkUpdatePropertyStatus(List<Integer> propertyIds, String status, String reason) {
        List<PropertyResponse> results = new ArrayList<>();
        AvailabilityStatus availabilityStatus;
        
        try {
            availabilityStatus = AvailabilityStatus.fromCode(Integer.parseInt(status));
        } catch (NumberFormatException e) {
            availabilityStatus = AvailabilityStatus.fromString(status);
        }
        
        for (Integer id : propertyIds) {
            try {
                results.add(updatePropertyStatus(id, availabilityStatus, reason));
            } catch (Exception e) {
                // Log error but continue with other properties
                System.err.println("Failed to update status for property " + id + ": " + e.getMessage());
            }
        }
        return results;
    }


    // ===== PROPERTY ANALYTICS =====
    public PropertyAnalyticsResponse getPropertyAnalyticsOverview() {
        List<Property> allProperties = propertyRepository.findAll();
        
        // Status distribution
        Map<AvailabilityStatus, Long> statusDistributionByEnum = allProperties.stream()
                .collect(Collectors.groupingBy(Property::getAvailability, Collectors.counting()));
        
        // Convert to String keys for response
        Map<String, Long> statusDistribution = statusDistributionByEnum.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> entry.getKey().name(), 
                    Map.Entry::getValue
                ));
        
        // Type distribution
        Map<String, Long> typeDistribution = allProperties.stream()
                .collect(Collectors.groupingBy(Property::getPropertyType, Collectors.counting()));
        
        // Department distribution
        Map<String, Long> departmentDistribution = allProperties.stream()
                .filter(p -> p.getDepartment() != null)
                .collect(Collectors.groupingBy(p -> p.getDepartment().getName(), Collectors.counting()));
        
        // Price statistics
        OptionalDouble avgPrice = allProperties.stream()
                .filter(p -> p.getPrice() != null)
                .mapToDouble(p -> p.getPrice().doubleValue())
                .average();
        Optional<BigDecimal> minPrice = allProperties.stream()
                .map(Property::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo);
        Optional<BigDecimal> maxPrice = allProperties.stream()
                .map(Property::getPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo);
        BigDecimal totalValue = allProperties.stream()
                .map(Property::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Size and room statistics
        OptionalDouble avgSize = allProperties.stream()
                .filter(p -> p.getSize() != null)
                .mapToDouble(p -> p.getSize().doubleValue())
                .average();
        OptionalDouble avgBedrooms = allProperties.stream()
                .filter(p -> p.getBedrooms() != null)
                .mapToInt(Property::getBedrooms)
                .average();
        OptionalDouble avgBathrooms = allProperties.stream()
                .filter(p -> p.getBathrooms() != null)
                .mapToInt(Property::getBathrooms)
                .average();
        
        // Growth metrics (simplified - would need more complex date filtering in real implementation)
        long propertiesThisMonth = allProperties.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(1)))
                .count();
        long propertiesLastMonth = allProperties.stream()
                .filter(p -> p.getCreatedAt() != null && 
                            p.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(2)) && 
                            p.getCreatedAt().isBefore(LocalDateTime.now().minusMonths(1)))
                .count();
        
        double growthRate = propertiesLastMonth > 0 ? 
            ((double)(propertiesThisMonth - propertiesLastMonth) / propertiesLastMonth) * 100 : 0;
        
        return PropertyAnalyticsResponse.builder()
                .totalProperties((long) allProperties.size())
                .pendingApproval(statusDistribution.getOrDefault("PENDING", 0L))
                .approved(statusDistribution.getOrDefault("AVAILABLE", 0L))
                .rejected(statusDistribution.getOrDefault("NOT_AVAILABLE", 0L))
                .draft(statusDistribution.getOrDefault("PENDING", 0L))
                .sold(statusDistribution.getOrDefault("SOLD", 0L))
                .rented(statusDistribution.getOrDefault("DEPOSITED", 0L))
                .typeDistribution(typeDistribution)
                .statusDistribution(statusDistribution)
                .departmentDistribution(departmentDistribution)
                .averagePrice(BigDecimal.valueOf(avgPrice.orElse(0.0)))
                .minPrice(minPrice.orElse(BigDecimal.ZERO))
                .maxPrice(maxPrice.orElse(BigDecimal.ZERO))
                .totalValue(totalValue)
                .averageSize(avgSize.orElse(0.0))
                .averageBedrooms((int) avgBedrooms.orElse(0.0))
                .averageBathrooms((int) avgBathrooms.orElse(0.0))
                .propertiesThisMonth(propertiesThisMonth)
                .propertiesLastMonth(propertiesLastMonth)
                .growthRate(growthRate)
                .build();
    }

    public Map<String, Object> getDepartmentPropertyAnalytics(Integer departmentId) {
        List<Property> departmentProperties = propertyRepository.findAll()
                .stream()
                .filter(p -> p.getDepartment() != null && p.getDepartment().getDepartmentId().equals(departmentId))
                .collect(Collectors.toList());
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalProperties", departmentProperties.size());
        analytics.put("pendingApproval", departmentProperties.stream().filter(p -> AvailabilityStatus.PENDING.equals(p.getAvailability())).count());
        analytics.put("approved", departmentProperties.stream().filter(p -> AvailabilityStatus.AVAILABLE.equals(p.getAvailability())).count());
        
        return analytics;
    }

    public Map<String, Object> getUserPropertyAnalytics(Integer userId) {
        List<PropertyOwnership> ownerships = propertyOwnershipRepository.findByUserUserId(userId);
        List<UserDistrictAccess> accesses = userDistrictAccessRepository.findByUserUserId(userId);
        
        // Count properties in districts the user has access to
        List<Integer> districtIds = accesses.stream()
                .map(access -> access.getDistrict().getId())
                .collect(Collectors.toList());
        
        long assignedPropertiesCount = 0;
        if (!districtIds.isEmpty()) {
            assignedPropertiesCount = propertyRepository.findByDistrictIdIn(districtIds).size();
        }
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("ownedProperties", ownerships.size());
        analytics.put("assignedProperties", assignedPropertiesCount);
        analytics.put("assignedDistricts", accesses.size());
        
        return analytics;
    }

    public Map<String, Object> getPropertyPerformanceReport(Integer departmentId, Integer userId, String startDate, String endDate) {
        // This would need more complex implementation based on interaction data
        Map<String, Object> report = new HashMap<>();
        report.put("message", "Performance report feature - to be implemented based on interaction data");
        return report;
    }

    // ===== ROLE-BASED PROPERTY MANAGEMENT =====
    public Page<PropertyResponse> getMyProperties(String status, String propertyType, Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return Page.empty();
        }

        Specification<Property> spec = Specification.where(
            (root, query, cb) -> cb.equal(root.get("user").get("userId"), currentUser.getUserId())
        );

        if (status != null) {
            try {
                AvailabilityStatus availabilityStatus = AvailabilityStatus.fromCode(Integer.parseInt(status));
                spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), availabilityStatus));
            } catch (NumberFormatException e) {
                AvailabilityStatus availabilityStatus = AvailabilityStatus.fromString(status);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), availabilityStatus));
            }
        }

        if (propertyType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("propertyType"), propertyType));
        }

        return propertyRepository.findAll(spec, pageable)
                .map(this::convertToPropertyResponse);
    }

    public Page<PropertyResponse> getDepartmentProperties(Integer departmentId, String status, String propertyType, Pageable pageable) {
        Specification<Property> spec = Specification.where(
            (root, query, cb) -> cb.equal(root.get("department").get("departmentId"), departmentId)
        );

        if (status != null) {
            try {
                AvailabilityStatus availabilityStatus = AvailabilityStatus.fromCode(Integer.parseInt(status));
                spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), availabilityStatus));
            } catch (NumberFormatException e) {
                AvailabilityStatus availabilityStatus = AvailabilityStatus.fromString(status);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), availabilityStatus));
            }
        }

        if (propertyType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("propertyType"), propertyType));
        }

        return propertyRepository.findAll(spec, pageable)
                .map(this::convertToPropertyResponse);
    }

    public Page<PropertyResponse> getConsultantProperties(Integer consultantId, String status, Pageable pageable) {
        List<UserDistrictAccess> accesses = userDistrictAccessRepository.findByUserUserId(consultantId);
        List<Integer> districtIds = accesses.stream()
                .map(access -> access.getDistrict().getId())
                .collect(Collectors.toList());

        if (districtIds.isEmpty()) {
            return Page.empty();
        }

        Specification<Property> spec = Specification.where(
            (root, query, cb) -> root.get("district").get("id").in(districtIds)
        );

        if (status != null) {
            try {
                AvailabilityStatus availabilityStatus = AvailabilityStatus.fromCode(Integer.parseInt(status));
                spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), availabilityStatus));
            } catch (NumberFormatException e) {
                AvailabilityStatus availabilityStatus = AvailabilityStatus.fromString(status);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), availabilityStatus));
            }
        }

        return propertyRepository.findAll(spec, pageable)
                .map(this::convertToPropertyResponse);
    }

    // ===== PROPERTY TRANSFER AND OWNERSHIP =====
    @Transactional
    public PropertyResponse transferProperty(Integer id, Integer newOwnerId, String reason) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        User newOwner = userRepository.findById(newOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + newOwnerId));

        // Update property owner
        User oldOwner = property.getUser();
        property.setUser(newOwner);
        
        // Add transfer log to description
        String transferLog = String.format("\n[Property transferred from %s to %s on %s - Reason: %s]", 
            oldOwner.getName(), newOwner.getName(), 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
            reason != null ? reason : "No reason provided");
        property.setDescription(property.getDescription() + transferLog);
        
        property.setUpdatedAt(LocalDateTime.now());
        Property updatedProperty = propertyRepository.save(property);
        
        return convertToPropertyResponse(updatedProperty);
    }

    @Transactional
    public PropertyResponse duplicateProperty(Integer id) {
        Property originalProperty = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to duplicate a property");
        }

        Property duplicatedProperty = new Property();
        duplicatedProperty.setAddressProperty(originalProperty.getAddressProperty() + " (Copy)");
        duplicatedProperty.setPropertyType(originalProperty.getPropertyType());
        duplicatedProperty.setSize(originalProperty.getSize());
        duplicatedProperty.setFloor(originalProperty.getFloor());
        duplicatedProperty.setThumbnail(originalProperty.getThumbnail());
        duplicatedProperty.setBedrooms(originalProperty.getBedrooms());
        duplicatedProperty.setBathrooms(originalProperty.getBathrooms());
        duplicatedProperty.setDescription("Duplicated from property #" + id + "\n" + originalProperty.getDescription());
        duplicatedProperty.setPrice(originalProperty.getPrice());
        duplicatedProperty.setLegalDocuments(originalProperty.getLegalDocuments());
        duplicatedProperty.setPhoneOwner(originalProperty.getPhoneOwner());
        duplicatedProperty.setDistrict(originalProperty.getDistrict());
        duplicatedProperty.setDepartment(originalProperty.getDepartment());
        duplicatedProperty.setUser(currentUser);
        duplicatedProperty.setAvailability(AvailabilityStatus.AVAILABLE);
        duplicatedProperty.setCreatedAt(LocalDateTime.now());
        duplicatedProperty.setUpdatedAt(LocalDateTime.now());

        Property savedProperty = propertyRepository.save(duplicatedProperty);
        return convertToPropertyResponse(savedProperty);
    }

    // ===== PROPERTY BOOKMARKS =====
    @Transactional
    public void bookmarkProperty(Integer id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to bookmark a property");
        }

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        // Check if district access already exists
        Integer districtId = property.getDistrict().getId();
        Optional<UserDistrictAccess> existingAccess = userDistrictAccessRepository
                .findByUserUserIdAndDistrictId(currentUser.getUserId(), districtId);

        if (existingAccess.isEmpty()) {
            UserDistrictAccess access = UserDistrictAccess.builder()
                    .user(currentUser)
                    .district(property.getDistrict())
                    .accessGrantedAt(LocalDateTime.now())
                    .build();
            userDistrictAccessRepository.save(access);
        }
    }

    @Transactional
    public void removeBookmark(Integer id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to remove bookmark");
        }

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        Integer districtId = property.getDistrict().getId();
        Optional<UserDistrictAccess> access = userDistrictAccessRepository
                .findByUserUserIdAndDistrictId(currentUser.getUserId(), districtId);

        access.ifPresent(userDistrictAccessRepository::delete);
    }

    public Page<PropertyResponse> getBookmarkedProperties(Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return Page.empty();
        }

        List<UserDistrictAccess> accesses = userDistrictAccessRepository.findByUserUserId(currentUser.getUserId());
        List<Integer> districtIds = accesses.stream()
                .map(access -> access.getDistrict().getId())
                .collect(Collectors.toList());

        if (districtIds.isEmpty()) {
            return Page.empty();
        }

        Specification<Property> spec = Specification.where(
            (root, query, cb) -> root.get("district").get("id").in(districtIds)
        );

        return propertyRepository.findAll(spec, pageable)
                .map(this::convertToPropertyResponse);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            } else if (principal instanceof String) {
                // Lấy username từ principal và tìm User tương ứng (hash-based)
                String username = (String) principal;
                String normalized = username == null ? null : username.trim().toLowerCase(java.util.Locale.ROOT);
                byte[] emailHash = deterministicHasher.emailHash(normalized);
                return userRepository.findByEmailHash(emailHash).orElse(null);
            }
        }
        return null;
    }

    /**
     * Get properties accessible by current user through user_district_access
     * with availability != 0, sorted by creation date descending (newest first)
     */
    public Page<PropertyResponse> getAccessibleProperties(PropertyFilterRequest filterRequest, Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated");
        }

        // Create custom pageable with sorting if specified
        Pageable sortedPageable = createSortedPageable(pageable, filterRequest.getSortBy(), filterRequest.getSortDirection());

        Page<Property> properties;
        
        // Check if any filters are applied
        if (hasFilters(filterRequest)) {
            properties = propertyRepository.findAccessiblePropertiesWithFilter(
                currentUser.getUserId(),
                filterRequest.getPropertyType(),
                filterRequest.getDistrictId(),
                filterRequest.getMinPrice(),
                filterRequest.getMaxPrice(),
                filterRequest.getMinBedrooms(),
                filterRequest.getMaxBedrooms(),
                filterRequest.getMinBathrooms(),
                filterRequest.getMaxBathrooms(),
                filterRequest.getMinSize(),
                filterRequest.getMaxSize(),
                filterRequest.getFloor(),
                filterRequest.getAvailability(),
                filterRequest.getKeyword(),
                sortedPageable
            );
        } else {
            // No filters applied, use simple query
            properties = propertyRepository.findAccessiblePropertiesByUserId(
                currentUser.getUserId(),
                sortedPageable
            );
        }

        return properties.map(this::convertToPropertyResponse);
    }

    /**
     * Check if any filters are applied in the request
     */
    private boolean hasFilters(PropertyFilterRequest filterRequest) {
        return filterRequest.getPropertyType() != null ||
               filterRequest.getDistrictId() != null ||
               filterRequest.getMinPrice() != null ||
               filterRequest.getMaxPrice() != null ||
               filterRequest.getMinBedrooms() != null ||
               filterRequest.getMaxBedrooms() != null ||
               filterRequest.getMinBathrooms() != null ||
               filterRequest.getMaxBathrooms() != null ||
               filterRequest.getMinSize() != null ||
               filterRequest.getMaxSize() != null ||
               filterRequest.getFloor() != null ||
               filterRequest.getAvailability() != null ||
               filterRequest.getKeyword() != null;
    }

    /**
     * Create a pageable with custom sorting
     */
    private Pageable createSortedPageable(Pageable pageable, String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "createdAt"; // Default sort by creation date
        }
        
        if (sortDirection == null || sortDirection.trim().isEmpty()) {
            sortDirection = "desc"; // Default descending order
        }

        org.springframework.data.domain.Sort.Direction direction = 
            "asc".equalsIgnoreCase(sortDirection) ? 
            org.springframework.data.domain.Sort.Direction.ASC : 
            org.springframework.data.domain.Sort.Direction.DESC;

        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(direction, sortBy);
        
        return org.springframework.data.domain.PageRequest.of(
            pageable.getPageNumber(), 
            pageable.getPageSize(), 
            sort
        );
    }

    // ===== PROPERTY THUMBNAIL MANAGEMENT =====
    @Transactional
    public PropertyResponse uploadPropertyThumbnail(Integer id, org.springframework.web.multipart.MultipartFile file) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Delete old thumbnail from S3 if exists
        if (property.getThumbnail() != null && !property.getThumbnail().isEmpty()) {
            try {
                s3Service.deleteFile(property.getThumbnail());
            } catch (Exception e) {
                // Log but continue - old file might not exist
                System.err.println("Failed to delete old thumbnail: " + e.getMessage());
            }
        }

        // Upload new thumbnail to S3
        String folderPath = "properties/" + id + "/thumbnail";
        String thumbnailUrl = s3Service.uploadFile(file, folderPath);

        // Update property with new thumbnail URL
        property.setThumbnail(thumbnailUrl);
        property.setUpdatedAt(LocalDateTime.now());
        Property updatedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(updatedProperty);
    }

    @Transactional
    public PropertyResponse updatePropertyThumbnail(Integer id, org.springframework.web.multipart.MultipartFile file) {
        // Same logic as upload
        return uploadPropertyThumbnail(id, file);
    }

    @Transactional
    public void deletePropertyThumbnail(Integer id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        // Delete thumbnail from S3 if exists
        if (property.getThumbnail() != null && !property.getThumbnail().isEmpty()) {
            s3Service.deleteFile(property.getThumbnail());
        }

        // Remove thumbnail URL from property
        property.setThumbnail(null);
        property.setUpdatedAt(LocalDateTime.now());
        propertyRepository.save(property);
    }

    // ===== PROPERTY OWNER MANAGEMENT =====
    
    /**
     * Get all properties owned by current user with pagination and filters
     */
    public Page<PropertyResponse> getOwnedProperties(String status, String propertyType, Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated");
        }

        Specification<Property> spec = Specification.where(
            (root, query, cb) -> cb.equal(root.get("user").get("userId"), currentUser.getUserId())
        );

        // Apply status filter
        if (status != null && !status.isEmpty()) {
            try {
                AvailabilityStatus availabilityStatus = AvailabilityStatus.fromCode(Integer.parseInt(status));
                spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), availabilityStatus));
            } catch (NumberFormatException e) {
                AvailabilityStatus availabilityStatus = AvailabilityStatus.fromString(status);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), availabilityStatus));
            }
        }

        // Apply propertyType filter
        if (propertyType != null && !propertyType.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("propertyType"), propertyType));
        }

        return propertyRepository.findAll(spec, pageable)
                .map(this::convertToPropertyResponse);
    }

    /**
     * Update property owned by current user
     * Status will be automatically reset to PENDING (1) after update
     */
    @Transactional
    public PropertyResponse updateOwnedProperty(Integer id, PropertyRequest propertyRequest) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated");
        }

        // Verify ownership - check both direct user field and PropertyOwnership table
        boolean isOwner = false;
        
        // Check 1: Direct user ownership (creator)
        if (property.getUser() != null && property.getUser().getUserId().equals(currentUser.getUserId())) {
            isOwner = true;
        }
        
        // // Check 2: PropertyOwnership table (owner/co-owner)
        // if (!isOwner) {
        //     Optional<PropertyOwnership> ownership = propertyOwnershipRepository
        //             .findByUserUserIdAndPropertyPropertyId(currentUser.getUserId(), id);
        //     if (ownership.isPresent() && 
        //         (ownership.get().getOwnershipType().equals("owner") || 
        //          ownership.get().getOwnershipType().equals("co-owner"))) {
        //         isOwner = true;
        //     }
        // }
        
        // if (!isOwner) {
        //     throw new AccessDeniedException("You can only update your own properties");
        // }

        // Update property fields from request
        updatePropertyFromRequest(property, propertyRequest);
        
        // IMPORTANT: Reset status to PENDING after owner updates
        property.setAvailability(AvailabilityStatus.PENDING);
        property.setUpdatedAt(LocalDateTime.now());

        Property updatedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(updatedProperty);
    }

    /**
     * Get all reviews for a property owned by current user
     */
    public Page<ReviewResponse> getOwnedPropertyReviews(Integer propertyId, Pageable pageable) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated");
        }

        // Verify ownership
        if (!property.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("You can only view reviews for your own properties");
        }

        // Get all reviews for this property
        Page<Review> reviews = reviewRepository.findByPropertyPropertyId(propertyId, pageable);

        return reviews.map(this::convertToReviewResponse);
    }

    /**
     * Convert Review entity to ReviewResponse DTO
     */
    private ReviewResponse convertToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getReviewId())
                .userId(review.getUser().getUserId())
                .userName(review.getUser().getName())
                .propertyId(review.getProperty().getPropertyId())
                .propertyAddress(review.getProperty().getAddressProperty())
                .comment(review.getComment())
                .action(review.getAction())
                .createdAt(review.getCreatedAt())
                .build();
    }

}