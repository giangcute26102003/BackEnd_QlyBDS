-- Add encrypted columns and hash columns for User table
ALTER TABLE user
  ADD COLUMN email_enc VARBINARY(768) NULL COMMENT 'AES-GCM encrypted email',
  ADD COLUMN email_hash BINARY(32) NULL COMMENT 'HMAC-SHA256 hash for email lookup',
  ADD COLUMN phone_number_enc VARBINARY(768) NULL COMMENT 'AES-GCM encrypted phone number',
  ADD COLUMN phone_hash BINARY(32) NULL COMMENT 'HMAC-SHA256 hash for phone lookup',
  ADD COLUMN address_enc BLOB NULL COMMENT 'AES-GCM encrypted address';

-- Add encrypted columns and hash columns for Customer table
ALTER TABLE customer
  ADD COLUMN email_enc VARBINARY(768) NULL COMMENT 'AES-GCM encrypted email',
  ADD COLUMN email_hash BINARY(32) NULL COMMENT 'HMAC-SHA256 hash for email lookup',
  ADD COLUMN phone_number_enc VARBINARY(768) NULL COMMENT 'AES-GCM encrypted phone number',
  ADD COLUMN phone_hash BINARY(32) NULL COMMENT 'HMAC-SHA256 hash for phone lookup',
  ADD COLUMN address_enc BLOB NULL COMMENT 'AES-GCM encrypted address';

-- Create indexes for hash-based lookups
CREATE INDEX idx_user_email_hash ON user(email_hash);
CREATE INDEX idx_user_phone_hash ON user(phone_hash);
CREATE INDEX idx_customer_email_hash ON customer(email_hash);
CREATE INDEX idx_customer_phone_hash ON customer(phone_hash);

