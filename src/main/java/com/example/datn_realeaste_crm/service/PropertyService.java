package com.example.datn_realeaste_crm.service;

import com.example.datn_realeaste_crm.dto.request.PropertyRequest;
import com.example.datn_realeaste_crm.dto.response.*;
import com.example.datn_realeaste_crm.entity.*;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.DepartmentRepository;
import com.example.datn_realeaste_crm.repository.DistrictRepository;
import com.example.datn_realeaste_crm.repository.*;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final DistrictRepository districtRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PropertyOwnershipRepository propertyOwnershipRepository;
    private final UserPropertyAccessRepository userPropertyAccessRepository;

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

        List<UserPropertyAccess> accesses = userPropertyAccessRepository
                .findByUserUserId(currentUser.getUserId());

        return accesses.stream()
                .map(access -> convertToPropertyResponse(access.getProperty()))
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
                .filter(p -> "PENDING".equals(p.getAvailability()))
                .collect(Collectors.toList());

        return pendingProperties.stream()
                .map(this::convertToPropertyResponse)
                .collect(Collectors.toList());
    }

    public PropertyResponse getProperty(Integer id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        return convertToPropertyResponse(property);
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

        property.setAvailability("APPROVED");
        property.setUpdatedAt(LocalDateTime.now());

        Property updatedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(updatedProperty);
    }

    @Transactional
    public PropertyResponse rejectProperty(Integer id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        property.setAvailability("REJECTED");
        property.setUpdatedAt(LocalDateTime.now());

        Property updatedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(updatedProperty);
    }

    // Method overload to support reason parameter
    @Transactional
    public PropertyResponse rejectProperty(Integer id, String reason) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        property.setAvailability("REJECTED");
        property.setDescription(property.getDescription() + "\n[Rejection reason: " + reason + "]");
        property.setUpdatedAt(LocalDateTime.now());

        Property updatedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(updatedProperty);
    }

    @Transactional
    public PropertyResponse assignPropertyToUser(Integer propertyId, Integer userId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if access already exists
        Optional<UserPropertyAccess> existingAccess = userPropertyAccessRepository
                .findByUserUserIdAndPropertyPropertyId(userId, propertyId);

        if (existingAccess.isEmpty()) {
            UserPropertyAccess access = new UserPropertyAccess();
            access.setUser(user);
            access.setProperty(property);
            // Create a new assign record with basic fields
            userPropertyAccessRepository.save(access);
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

        // Set availability to PENDING for new properties or when updating
        if (property.getAvailability() == null) {
            property.setAvailability("PENDING");
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

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            } else if (principal instanceof String) {
                // Lấy username từ principal và tìm User tương ứng
                String username = (String) principal;
                return userRepository.findByEmail(username)
                        .orElse(null);
            }
        }
        return null;
    }

}