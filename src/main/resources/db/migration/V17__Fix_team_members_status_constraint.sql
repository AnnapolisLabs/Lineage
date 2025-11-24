-- Migration: V17__Fix_team_members_status_constraint.sql
-- Description: Fix team_members status constraint to match the TeamMemberStatus enum values

-- ===============================================
-- 1. BACKUP CURRENT TEAM MEMBERS DATA
-- ===============================================

-- Create a backup of current team_members for safety
CREATE TABLE IF NOT EXISTS team_members_status_backup AS SELECT * FROM team_members;

-- ===============================================
-- 2. DROP EXISTING STATUS CONSTRAINT
-- ===============================================

-- Drop the old constraint that only allows lowercase values
ALTER TABLE team_members DROP CONSTRAINT IF EXISTS team_members_status_check;

-- ===============================================
-- 3. CLEAN UP ANY INVALID STATUS VALUES
-- ===============================================

-- Handle any NULL or empty values first
UPDATE team_members SET status = 'ACTIVE' 
WHERE status IS NULL OR status = '' OR status = 'null';

-- Handle any case variations by normalizing to uppercase
UPDATE team_members SET status = CASE 
    WHEN LOWER(status) = 'active' THEN 'ACTIVE'
    WHEN LOWER(status) = 'inactive' THEN 'INACTIVE'
    WHEN LOWER(status) = 'pending' THEN 'PENDING'
    ELSE 'ACTIVE'  -- Default unknown statuses to ACTIVE
END
WHERE status NOT IN ('ACTIVE', 'INACTIVE', 'PENDING');

-- ===============================================
-- 4. ADD NEW CONSTRAINT WITH MATCHING ENUM VALUES
-- ===============================================

-- Add constraint that matches the TeamMemberStatus enum values exactly
ALTER TABLE team_members ADD CONSTRAINT team_members_status_check 
CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING'));

-- ===============================================
-- 5. FIX COLUMN DEFAULT VALUE
-- ===============================================

-- Ensure the column default matches the Java code default
ALTER TABLE team_members ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- ===============================================
-- 6. VERIFICATION AND AUDIT
-- ===============================================

DO $$
DECLARE
    member_count INTEGER;
    invalid_statuses INTEGER;
    constraint_exists BOOLEAN;
    backup_count INTEGER;
BEGIN
    -- Check team_members count
    SELECT COUNT(*) INTO member_count FROM team_members;
    
    -- Check backup
    SELECT COUNT(*) INTO backup_count FROM team_members_status_backup;
    
    -- Check for any invalid statuses after our fixes
    SELECT COUNT(*) INTO invalid_statuses FROM team_members 
    WHERE status NOT IN ('ACTIVE', 'INACTIVE', 'PENDING');
    
    -- Check if constraint exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'team_members_status_check' 
        AND table_name = 'team_members'
    ) INTO constraint_exists;
    
    RAISE NOTICE 'Team member status constraint fix completed:';
    RAISE NOTICE 'Total team members: %', member_count;
    RAISE NOTICE 'Backup created: % records', backup_count;
    RAISE NOTICE 'Invalid statuses after fix: %', invalid_statuses;
    RAISE NOTICE 'Constraint exists: %', constraint_exists;
    
    -- Verify no team members violate the constraint
    IF invalid_statuses > 0 THEN
        RAISE EXCEPTION 'Found % team members with invalid statuses after constraint update', invalid_statuses;
    END IF;
    
    IF NOT constraint_exists THEN
        RAISE EXCEPTION 'Failed to create team_members_status_check constraint';
    END IF;
END $$;

-- ===============================================
-- 7. LOG THE FIX
-- ===============================================

INSERT INTO audit_logs (user_id, action, resource, resource_id, details, severity, created_at)
VALUES (
    NULL,
    'CONSTRAINT_FIX',
    'TEAM_MEMBERS',
    'V017_STATUS_CONSTRAINT_VIOLATION',
    jsonb_build_object(
        'migration_version', 'V017',
        'description', 'Fixed team_members_status_check constraint violation',
        'constraint_updated', true,
        'statuses_allowed', ARRAY['ACTIVE', 'INACTIVE', 'PENDING'],
        'statuses_mapped', 'Normalized case sensitivity to uppercase',
        'default_status', 'ACTIVE',
        'timestamp', NOW()
    ),
    'INFO',
    NOW()
);

-- ===============================================
-- 8. CLEANUP BACKUP (optional - keep for safety)
-- ===============================================

-- Comment this out if you want to keep the backup for recovery
-- DROP TABLE team_members_status_backup;