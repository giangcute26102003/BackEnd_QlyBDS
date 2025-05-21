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

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final DistrictRepository districtRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

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

    public PropertyResponse getProperty(Integer id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        return convertToPropertyResponse(property);
    }

    @Transactional
    public PropertyResponse createProperty(PropertyRequest propertyRequest) {
        Property property = new Property();

        // Get user from request instead of authentication context
        User user = null;
        if (propertyRequest.getUserId() != null) {
            user = userRepository.findById(propertyRequest.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with id: " + propertyRequest.getUserId()));
            property.setUser(user);

            // Set department if user has one and departmentId is not provided
            if (propertyRequest.getDepartmentId() == null && user.getDepartment() != null) {
                property.setDepartment(user.getDepartment());
            }
        } else {
            // Fallback to security context if userId not provided
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User currentUser = (User) authentication.getPrincipal();
                property.setUser(currentUser);

                // Set department if user has one and departmentId is not provided
                if (propertyRequest.getDepartmentId() == null && currentUser.getDepartment() != null) {
                    property.setDepartment(currentUser.getDepartment());
                }
            }
        }

        updatePropertyFromRequest(property, propertyRequest);
        property.setCreatedAt(LocalDateTime.now());
        property.setUpdatedAt(LocalDateTime.now());

        Property savedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(savedProperty);
    }

    @Transactional
    public PropertyResponse updateProperty(Integer id, PropertyRequest propertyRequest) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        updatePropertyFromRequest(property, propertyRequest);
        property.setUpdatedAt(LocalDateTime.now());

        Property updatedProperty = propertyRepository.save(property);

        return convertToPropertyResponse(updatedProperty);
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
}