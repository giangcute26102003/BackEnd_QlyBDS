-- Manual SQL Script để cập nhật bảng property_images cho S3 integration
-- Chạy script này nếu không sử dụng Flyway migration

-- Kiểm tra cấu trúc bảng hiện tại
DESCRIBE property_images;

-- Thêm các trường metadata cho S3 (chỉ chạy nếu chưa có)
ALTER TABLE property_images 
ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255) NULL AFTER image_url,
ADD COLUMN IF NOT EXISTS file_size BIGINT NULL AFTER original_filename,
ADD COLUMN IF NOT EXISTS content_type VARCHAR(100) NULL AFTER file_size,
ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER content_type;

-- Cập nhật timestamp cho các records hiện có
UPDATE property_images 
SET uploaded_at = CURRENT_TIMESTAMP 
WHERE uploaded_at IS NULL;

-- Thêm indexes để cải thiện performance (chỉ thêm nếu chưa có)
CREATE INDEX IF NOT EXISTS idx_property_images_uploaded_at ON property_images(uploaded_at);
CREATE INDEX IF NOT EXISTS idx_property_images_content_type ON property_images(content_type);

-- Hiển thị cấu trúc bảng sau khi cập nhật
DESCRIBE property_images;

-- Hiển thị indexes
SHOW INDEX FROM property_images;

-- Kiểm tra dữ liệu mẫu
SELECT * FROM property_images LIMIT 5;

-- Thông báo hoàn thành
SELECT 'Property images table updated successfully for S3 integration!' as status;