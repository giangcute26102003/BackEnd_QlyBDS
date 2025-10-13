package com.example.datn_realeaste_crm.service;


import com.example.datn_realeaste_crm.dto.request.PropertyImageRequest;
import com.example.datn_realeaste_crm.dto.response.PropertyImageResponse;
import com.example.datn_realeaste_crm.entity.Property;
import com.example.datn_realeaste_crm.entity.PropertyImage;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyImageService {

    private final PropertyImageRepository propertyImageRepository;
    private final PropertyRepository propertyRepository;
    private final S3Service s3Service;

    public List<PropertyImageResponse> getPropertyImages(Integer propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }

        return propertyImageRepository.findByPropertyPropertyId(propertyId)
                .stream()
                .map(this::convertToPropertyImageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PropertyImageResponse addPropertyImage(Integer propertyId, MultipartFile file) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        // Upload file to S3
        String folderPath = "properties/" + propertyId + "/images";
        String s3Url = s3Service.uploadFile(file, folderPath);

        PropertyImage propertyImage = PropertyImage.builder()
                .property(property)
                .imageUrl(s3Url)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();

        PropertyImage savedImage = propertyImageRepository.save(propertyImage);
        log.info("Property image added successfully for property ID: {}, Image URL: {}", propertyId, s3Url);

        return convertToPropertyImageResponse(savedImage);
    }
    
    @Transactional
    public PropertyImageResponse addPropertyImageByUrl(Integer propertyId, PropertyImageRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        PropertyImage propertyImage = PropertyImage.builder()
                .property(property)
                .imageUrl(request.getImageUrl())
                .build();

        PropertyImage savedImage = propertyImageRepository.save(propertyImage);

        return convertToPropertyImageResponse(savedImage);
    }

    @Transactional
    public void deletePropertyImage(Integer propertyId, Integer imageId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }

        PropertyImage propertyImage = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Property image not found with id: " + imageId));

        if (!propertyImage.getProperty().getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Property image with id: " + imageId +
                    " does not belong to property with id: " + propertyId);
        }

        // Delete from S3 if it's an S3 URL
        try {
            s3Service.deleteFile(propertyImage.getImageUrl());
            log.info("Image deleted from S3: {}", propertyImage.getImageUrl());
        } catch (Exception e) {
            log.warn("Failed to delete image from S3: {}, Error: {}", propertyImage.getImageUrl(), e.getMessage());
            // Continue with database deletion even if S3 deletion fails
        }

        propertyImageRepository.delete(propertyImage);
        log.info("Property image deleted successfully: {}", imageId);
    }

    @Transactional
    public PropertyImageResponse updatePropertyImage(Integer propertyId, Integer imageId, MultipartFile file) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }

        PropertyImage propertyImage = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Property image not found with id: " + imageId));

        if (!propertyImage.getProperty().getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Property image with id: " + imageId +
                    " does not belong to property with id: " + propertyId);
        }

        // Delete old image from S3
        String oldImageUrl = propertyImage.getImageUrl();
        try {
            s3Service.deleteFile(oldImageUrl);
            log.info("Old image deleted from S3: {}", oldImageUrl);
        } catch (Exception e) {
            log.warn("Failed to delete old image from S3: {}, Error: {}", oldImageUrl, e.getMessage());
        }

        // Upload new image to S3
        String folderPath = "properties/" + propertyId + "/images";
        String newS3Url = s3Service.uploadFile(file, folderPath);

        // Update property image
        propertyImage.setImageUrl(newS3Url);
        propertyImage.setOriginalFilename(file.getOriginalFilename());
        propertyImage.setFileSize(file.getSize());
        propertyImage.setContentType(file.getContentType());

        PropertyImage updatedImage = propertyImageRepository.save(propertyImage);
        log.info("Property image updated successfully: {}, New URL: {}", imageId, newS3Url);

        return convertToPropertyImageResponse(updatedImage);
    }
    
    @Transactional
    public PropertyImageResponse updatePropertyImageByUrl(Integer propertyId, Integer imageId, PropertyImageRequest request) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }

        PropertyImage propertyImage = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Property image not found with id: " + imageId));

        if (!propertyImage.getProperty().getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Property image with id: " + imageId +
                    " does not belong to property with id: " + propertyId);
        }

        propertyImage.setImageUrl(request.getImageUrl());

        PropertyImage updatedImage = propertyImageRepository.save(propertyImage);

        return convertToPropertyImageResponse(updatedImage);
    }

    private PropertyImageResponse convertToPropertyImageResponse(PropertyImage propertyImage) {
        return PropertyImageResponse.builder()
                .id(propertyImage.getId())
                .propertyId(propertyImage.getProperty().getPropertyId())
                .imageUrl(propertyImage.getImageUrl())
                .originalFilename(propertyImage.getOriginalFilename())
                .fileSize(propertyImage.getFileSize())
                .contentType(propertyImage.getContentType())
                .uploadedAt(propertyImage.getUploadedAt())
                .build();
    }
}