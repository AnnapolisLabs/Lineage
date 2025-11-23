-- V8: Fix Requirement ID Length Constraints
-- Migration to resolve "value too long for type character varying(255)" errors
-- caused by generated requirement IDs exceeding the current VARCHAR(100) constraint

-- Update the req_id column to VARCHAR(255) to accommodate longer generated IDs
ALTER TABLE requirements 
    ALTER COLUMN req_id TYPE VARCHAR(255);

-- Add a check constraint to ensure req_id values don't exceed 200 characters for safety
-- This prevents extremely long requirement IDs while allowing sufficient flexibility
-- for hierarchical numbering with custom prefixes (e.g., "USER_CUSTOM_PREFIX-001")
ALTER TABLE requirements 
    ADD CONSTRAINT chk_requirements_req_id_length 
    CHECK (LENGTH(req_id) <= 200);

-- Add comment explaining the constraint rationale
COMMENT ON COLUMN requirements.req_id IS 
    'Requirement identifier with hierarchical numbering. Limited to 200 characters to prevent database errors. 
    Format typically follows "PREFIX-###" where PREFIX can be customized per level (e.g., CR-001, SYS-001, REQ-L1-001). 
    Previous VARCHAR(100) constraint caused failures with long custom prefixes.';

COMMENT ON CONSTRAINT chk_requirements_req_id_length ON requirements IS 
    'Ensures requirement IDs remain within reasonable length limits (200 chars max) to maintain database performance 
    and prevent constraint violations. Allows flexibility for custom hierarchical prefixes while maintaining safety margins.';