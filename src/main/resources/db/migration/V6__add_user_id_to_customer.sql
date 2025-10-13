-- Add user_id field to customer table for ownership tracking
ALTER TABLE customer ADD COLUMN user_id int NULL;

-- Add foreign key constraint
ALTER TABLE customer ADD CONSTRAINT customer_user_fk 
    FOREIGN KEY (user_id) REFERENCES user(user_id);

-- Create index for better query performance
CREATE INDEX idx_customer_user_id ON customer(user_id);

-- Update existing customers to assign them to the first admin user
-- This ensures no orphaned customers after migration
UPDATE customer 
SET user_id = (
    SELECT u.user_id 
    FROM user u 
    JOIN user_role ur ON u.user_id = ur.user_id 
    JOIN role r ON ur.role_id = r.role_id 
    WHERE r.role_name = 'ADMIN' 
    AND u.is_active = 1
    ORDER BY u.user_id ASC
    LIMIT 1
) 
WHERE user_id IS NULL;

-- Verify that all customers have been assigned a user
-- If any customer still has NULL user_id, assign to first active user
UPDATE customer 
SET user_id = (
    SELECT user_id 
    FROM user 
    WHERE is_active = 1 
    ORDER BY user_id ASC 
    LIMIT 1
) 
WHERE user_id IS NULL;

-- Make user_id NOT NULL after updating existing records
ALTER TABLE customer MODIFY COLUMN user_id int NOT NULL;
