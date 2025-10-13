-- Migration to change property availability from VARCHAR to INT with new status values
-- 0: Not Available, 1: Pending Approval, 2: Available, 3: Deposited, 4: Sold

-- Step 1: Add new column with int type
ALTER TABLE `property` 
ADD COLUMN `availability_new` int DEFAULT 1 COMMENT '0: Not Available, 1: Pending Approval, 2: Available, 3: Deposited, 4: Sold';

-- Step 2: Migrate existing data
UPDATE `property` 
SET `availability_new` = CASE 
    WHEN `availability` = 'PENDING' THEN 1
    WHEN `availability` = 'APPROVED' THEN 2
    WHEN `availability` = 'AVAILABLE' THEN 2
    WHEN `availability` = 'DRAFT' THEN 1
    WHEN `availability` = 'REJECTED' THEN 0
    WHEN `availability` = 'DEPOSITED' THEN 3
    WHEN `availability` = 'SOLD' THEN 4
    WHEN `availability` = 'NOT_AVAILABLE' THEN 0
    ELSE 1
END;

-- Step 3: Drop old column
ALTER TABLE `property` DROP COLUMN `availability`;

-- Step 4: Rename new column to original name
ALTER TABLE `property` 
CHANGE COLUMN `availability_new` `availability` int DEFAULT 1 NOT NULL COMMENT '0: Not Available, 1: Pending Approval, 2: Available, 3: Deposited, 4: Sold';

-- Step 5: Add constraint to ensure valid values
ALTER TABLE `property` 
ADD CONSTRAINT `chk_property_availability` 
CHECK (`availability` IN (0, 1, 2, 3, 4));
