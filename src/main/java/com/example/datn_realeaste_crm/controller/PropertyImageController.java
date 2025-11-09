package com.example.datn_realeaste_crm.controller;


import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.PropertyImageBulkRequest;
import com.example.datn_realeaste_crm.dto.request.PropertyImageRequest;
import com.example.datn_realeaste_crm.dto.response.PropertyImageResponse;
import com.example.datn_realeaste_crm.service.PropertyImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/properties/{propertyId}/images")
@RequiredArgsConstructor
public class PropertyImageController {
    
    private final PropertyImageService propertyImageService;
    
    @GetMapping
    public ResponseEntity<List<PropertyImageResponse>> getPropertyImages(@PathVariable Integer propertyId) {
        return ResponseEntity.ok(propertyImageService.getPropertyImages(propertyId));
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasAuthority('property_update') or @propertyAuthorizationService.isPropertyOwner(authentication, #propertyId)")
    @Auditable(action = "ADD_PROPERTY_IMAGE", entityType = "PropertyImage", logResult = true)
    public ResponseEntity<PropertyImageResponse> addPropertyImage(
            @PathVariable Integer propertyId,
            @RequestParam("file") MultipartFile file) {
        return new ResponseEntity<>(propertyImageService.addPropertyImage(propertyId, file), HttpStatus.CREATED);
    }
    
    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasAuthority('property_update') or @propertyAuthorizationService.isPropertyOwner(authentication, #propertyId)")
    @Auditable(action = "ADD_PROPERTY_IMAGES_BULK", entityType = "PropertyImage", logResult = true)
    public ResponseEntity<List<PropertyImageResponse>> addPropertyImagesBulk(
            @PathVariable Integer propertyId,
            @RequestParam("files") List<MultipartFile> files) {
        return new ResponseEntity<>(propertyImageService.addPropertyImagesBulk(propertyId, files), HttpStatus.CREATED);
    }
    
    @PostMapping("/url")
//    @PreAuthorize("hasAuthority('property_update') or @propertyAuthorizationService.isPropertyOwner(authentication, #propertyId)")
    @Auditable(action = "ADD_PROPERTY_IMAGE_URL", entityType = "PropertyImage", logResult = true)
    public ResponseEntity<PropertyImageResponse> addPropertyImageByUrl(
            @PathVariable Integer propertyId,
            @Valid @RequestBody PropertyImageRequest request) {
        return new ResponseEntity<>(propertyImageService.addPropertyImageByUrl(propertyId, request), HttpStatus.CREATED);
    }
    
    @PostMapping("/bulk-url")
//    @PreAuthorize("hasAuthority('property_update') or @propertyAuthorizationService.isPropertyOwner(authentication, #propertyId)")
    @Auditable(action = "ADD_PROPERTY_IMAGES_BULK_URL", entityType = "PropertyImage", logResult = true)
    public ResponseEntity<List<PropertyImageResponse>> addPropertyImagesByUrls(
            @PathVariable Integer propertyId,
            @Valid @RequestBody PropertyImageBulkRequest request) {
        return new ResponseEntity<>(propertyImageService.addPropertyImagesByUrls(propertyId, request), HttpStatus.CREATED);
    }
    
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAuthority('property_update') or @propertyAuthorizationService.isPropertyOwner(#propertyId)")
    @Auditable(action = "DELETE_PROPERTY_IMAGE", entityType = "PropertyImage", entityIdParam = "imageId")
    public ResponseEntity<Void> deletePropertyImage(
            @PathVariable Integer propertyId,
            @PathVariable Integer imageId) {
        propertyImageService.deletePropertyImage(propertyId, imageId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping(value = "/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('property_update') or @propertyAuthorizationService.isPropertyOwner(#propertyId)")
    @Auditable(action = "UPDATE_PROPERTY_IMAGE", entityType = "PropertyImage", entityIdParam = "imageId")
    public ResponseEntity<PropertyImageResponse> updatePropertyImage(
            @PathVariable Integer propertyId,
            @PathVariable Integer imageId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(propertyImageService.updatePropertyImage(propertyId, imageId, file));
    }
    
    @PutMapping("/{imageId}/url")
    @PreAuthorize("hasAuthority('property_update') or @propertyAuthorizationService.isPropertyOwner(#propertyId)")
    @Auditable(action = "UPDATE_PROPERTY_IMAGE_URL", entityType = "PropertyImage", entityIdParam = "imageId")
    public ResponseEntity<PropertyImageResponse> updatePropertyImageByUrl(
            @PathVariable Integer propertyId,
            @PathVariable Integer imageId,
            @Valid @RequestBody PropertyImageRequest request) {
        return ResponseEntity.ok(propertyImageService.updatePropertyImageByUrl(propertyId, imageId, request));
    }
}