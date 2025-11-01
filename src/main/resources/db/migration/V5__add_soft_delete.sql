-- Add soft delete columns to requirements table
ALTER TABLE requirements
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by UUID REFERENCES users(id);

-- Create index on deleted_at for efficient filtering
CREATE INDEX idx_requirements_deleted_at ON requirements(deleted_at);

-- Update requirement_history to keep track of deleted requirements
-- (no schema changes needed, but we'll track DELETED change_type)
