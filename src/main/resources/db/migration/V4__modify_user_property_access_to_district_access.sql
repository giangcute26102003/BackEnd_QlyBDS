-- Migration to change user_property_access table to use district_id instead of property_id
-- This allows users to have access to all properties within a district

-- First, drop the existing foreign key constraints
ALTER TABLE `user_property_access` 
DROP FOREIGN KEY `user_property_access_property_fk`;

-- Drop the property_id column
ALTER TABLE `user_property_access` 
DROP COLUMN `property_id`;

-- Add the new district_id column
ALTER TABLE `user_property_access` 
ADD COLUMN `district_id` int NOT NULL AFTER `user_id`;

-- Add foreign key constraint to district table
ALTER TABLE `user_property_access` 
ADD CONSTRAINT `user_property_access_district_fk` 
FOREIGN KEY (`district_id`) REFERENCES `district` (`id`) ON DELETE CASCADE;

-- Add index for district_id
ALTER TABLE `user_property_access` 
ADD KEY `user_property_access_district_fk` (`district_id`);

-- Rename the table to better reflect its new purpose
ALTER TABLE `user_property_access` 
RENAME TO `user_district_access`;
