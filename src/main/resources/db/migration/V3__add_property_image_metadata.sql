-- Add metadata columns to property_images table for S3 integration
-- Based on existing table structure: id (PK, AUTO_INCREMENT), property_id (FK), image_url

ALTER TABLE property_images 
ADD COLUMN original_filename VARCHAR(255) NULL AFTER image_url,
ADD COLUMN file_size BIGINT NULL AFTER original_filename,
ADD COLUMN content_type VARCHAR(100) NULL AFTER file_size,
ADD COLUMN uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER content_type;

-- Update existing records with default timestamp
UPDATE property_images 
SET uploaded_at = CURRENT_TIMESTAMP 
WHERE uploaded_at IS NULL;

-- Add indexes for better query performance
CREATE INDEX idx_property_images_uploaded_at ON property_images(uploaded_at);
CREATE INDEX idx_property_images_content_type ON property_images(content_type);

-- Note: Primary key, foreign key constraint, and AUTO_INCREMENT already exist in the original table structure