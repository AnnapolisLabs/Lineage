-- Fix level_prefixes column type conversion to JSONB
-- This migration explicitly handles the type conversion that Hibernate is failing to perform automatically

-- Check if the level_prefixes column exists and is not already JSONB
DO $$
BEGIN
    -- Only proceed if the column exists and is not JSONB
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'projects' 
        AND column_name = 'level_prefixes' 
        AND data_type != 'jsonb'
    ) THEN
        -- Convert using explicit casting
        ALTER TABLE projects ALTER COLUMN level_prefixes TYPE JSONB USING level_prefixes::jsonb;
        
        -- Set default if not already set
        ALTER TABLE projects ALTER COLUMN level_prefixes SET DEFAULT '{}'::jsonb;
        
        RAISE NOTICE 'Successfully converted level_prefixes column to JSONB type';
    ELSE
        RAISE NOTICE 'level_prefixes column is already JSONB type or does not exist';
    END IF;
END $$;