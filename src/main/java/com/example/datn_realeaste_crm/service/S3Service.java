package com.example.datn_realeaste_crm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String folderPath) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            // Get and validate content type
            String contentType = determineContentType(file);
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Only image files are allowed. Detected type: " + contentType);
            }

            // Generate unique file name
            String fileName = generateFileName(file.getOriginalFilename());
            String key = folderPath + "/" + fileName;

            log.debug("Uploading file: {} with Content-Type: {} to S3 key: {}", 
                    file.getOriginalFilename(), contentType, key);

            // Upload file to S3 with proper content type and metadata
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .contentDisposition("inline") // Display in browser instead of download
                    .cacheControl("max-age=31536000") // Cache for 1 year
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Return the public URL
            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
            log.info("File uploaded successfully: {} with Content-Type: {}", fileUrl, contentType);
            
            return fileUrl;

        } catch (IOException e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        } catch (S3Exception e) {
            log.error("S3 error uploading file: {}", e.getMessage());
            throw new RuntimeException("S3 error uploading file", e);
        }
    }
    
    /**
     * Determine content type from file with fallback to extension-based detection
     */
    private String determineContentType(MultipartFile file) {
        // First, try to get content type from the file
        String contentType = file.getContentType();
        
        // If content type is missing or generic, detect from file extension
        if (contentType == null || contentType.equals("application/octet-stream") || contentType.isEmpty()) {
            String filename = file.getOriginalFilename();
            if (filename != null) {
                contentType = getContentTypeFromExtension(filename.toLowerCase());
            }
        }
        
        // Normalize content type
        if (contentType != null) {
            contentType = contentType.toLowerCase().trim();
        }
        
        log.debug("Determined Content-Type: {} for file: {}", contentType, file.getOriginalFilename());
        return contentType;
    }
    
    /**
     * Get content type based on file extension
     */
    private String getContentTypeFromExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "application/octet-stream";
        }
        
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "ico":
                return "image/x-icon";
            case "tif":
            case "tiff":
                return "image/tiff";
            default:
                log.warn("Unknown image extension: {}, defaulting to application/octet-stream", extension);
                return "application/octet-stream";
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            // Extract key from URL
            String key = extractKeyFromUrl(fileUrl);
            if (key == null) {
                log.warn("Invalid S3 URL format: {}", fileUrl);
                return;
            }

            // Delete object from S3
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", key);

        } catch (S3Exception e) {
            log.error("S3 error deleting file: {}", e.getMessage());
            throw new RuntimeException("S3 error deleting file", e);
        }
    }

    public boolean doesFileExist(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key == null) {
                return false;
            }

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Error checking file existence: {}", e.getMessage());
            return false;
        }
    }

    private String generateFileName(String originalFilename) {
        // Get file extension
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generate unique filename with timestamp and UUID
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        return String.format("%s_%s%s", timestamp, uuid, extension);
    }

    private String extractKeyFromUrl(String fileUrl) {
        try {
            // Expected format: https://bucket-name.s3.region.amazonaws.com/key
            String expectedPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            if (fileUrl.startsWith(expectedPrefix)) {
                return fileUrl.substring(expectedPrefix.length());
            }
            return null;
        } catch (Exception e) {
            log.error("Error extracting key from URL: {}", e.getMessage());
            return null;
        }
    }
}