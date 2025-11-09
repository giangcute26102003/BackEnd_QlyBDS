-- Add action column to review table
ALTER TABLE review ADD COLUMN action VARCHAR(50) NULL;

-- Add comment for documentation
COMMENT ON COLUMN review.action IS 'Action or event type associated with the review (e.g., VISIT, CONTACT, INTERESTED, etc.)';

