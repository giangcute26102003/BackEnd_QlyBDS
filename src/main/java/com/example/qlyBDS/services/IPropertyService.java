//package com.example.qlyBDS.services;
//
//import com.realestate.dto.request.CreatePropertyRequest;
//import com.realestate.dto.request.UpdatePropertyRequest;
//import com.realestate.dto.response.PropertyResponse;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import java.math.BigDecimal;
//
//public interface IPropertyService {
//    PropertyResponse createProperty(CreatePropertyRequest request);
//    PropertyResponse updateProperty(Integer propertyId, UpdatePropertyRequest request);
//    void deleteProperty(Integer propertyId);
//    PropertyResponse getPropertyById(Integer propertyId);
//    Page<PropertyResponse> getAllProperties(Pageable pageable);
//    Page<PropertyResponse> searchProperties(
//            String propertyType,
//            Integer districtId,
//            BigDecimal minPrice,
//            BigDecimal maxPrice,
//            Integer minBedrooms,
//            Pageable pageable
//    );
//    Page<PropertyResponse> getPropertiesByUserId(Integer userId, Pageable pageable);
//    Page<PropertyResponse> getPropertiesByDistrict(Integer districtId, Pageable pageable);
//}