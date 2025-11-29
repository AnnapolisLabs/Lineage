-- Migration: V16__Fix_team_members_role_constraint.sql
-- Description: Fix team_members role constraint to match the TeamRole enum values

-- ===============================================
-- 1. BACKUP CURRENT TEAM MEMBERS DATA
-- ===============================================

-- Create a backup of current team_members for safety
CREATE TABLE IF NOT EXISTS team_members_backup AS SELECT * FROM team_members;

-- ===============================================
-- 2. DROP EXISTING CONSTRAINT
-- ===============================================

-- Drop the old constraint that only allows lowercase values
ALTER TABLE team_members DROP CONSTRAINT IF EXISTS team_members_role_check;

-- ===============================================
-- 3. CLEAN UP ANY INVALID ROLE VALUES
-- ===============================================

-- Handle any NULL or empty values first
UPDATE team_members SET role = 'MEMBER' 
WHERE role IS NULL OR role = '' OR role = 'null';

-- Handle any invalid role values by mapping them to valid ones and normalizing to uppercase
UPDATE team_members SET role = CASE 
    WHEN LOWER(role) IN ('owner', 'owners') THEN 'OWNER'
    WHEN LOWER(role) IN ('admin', 'administrator') THEN 'ADMIN'
    WHEN LOWER(role) IN ('member', 'members') THEN 'MEMBER'
    WHEN LOWER(role) = 'viewer' THEN 'VIEWER'
    ELSE 'MEMBER'  -- Default unknown roles to MEMBER
END
WHERE role NOT IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER');

-- ===============================================
-- 4. ADD NEW CONSTRAINT WITH MATCHING ENUM VALUES
-- ===============================================

-- Add constraint that matches the TeamRole enum values exactly
-- Note: TeamRole enum uses uppercase values, but database stored lowercase
-- This handles the case conversion for existing data while maintaining compatibility
ALTER TABLE team_members ADD CONSTRAINT team_members_role_check 
CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'));

-- ===============================================
-- 5. FIX COLUMN DEFAULT VALUE
-- ===============================================

-- Ensure the column default matches the Java code default
ALTER TABLE team_members ALTER COLUMN role SET DEFAULT 'MEMBER';

-- ===============================================
-- 6. VERIFICATION AND AUDIT
-- ===============================================

DO $$
DECLARE
    member_count INTEGER;
    invalid_roles INTEGER;
    constraint_exists BOOLEAN;
    backup_count INTEGER;
BEGIN
    -- Check team_members count
    SELECT COUNT(*) INTO member_count FROM team_members;
    
    -- Check backup
    SELECT COUNT(*) INTO backup_count FROM team_members_backup;
    
    -- Check for any invalid roles after our fixes
    SELECT COUNT(*) INTO invalid_roles FROM team_members 
    WHERE role NOT IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER');
    
    -- Check if constraint exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'team_members_role_check' 
        AND table_name = 'team_members'
    ) INTO constraint_exists;
    
    RAISE NOTICE 'Team member role constraint fix completed:';
    RAISE NOTICE 'Total team members: %', member_count;
    RAISE NOTICE 'Backup created: % records', backup_count;
    RAISE NOTICE 'Invalid roles after fix: %', invalid_roles;
    RAISE NOTICE 'Constraint exists: %', constraint_exists;
    
    -- Verify no team members violate the constraint
    IF invalid_roles > 0 THEN
        RAISE EXCEPTION 'Found % team members with invalid roles after constraint update', invalid_roles;
    END IF;
    
    IF NOT constraint_exists THEN
        RAISE EXCEPTION 'Failed to create team_members_role_check constraint';
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
    'V016_CONSTRAINT_VIOLATION',
    jsonb_build_object(
        'migration_version', 'V016',
        'description', 'Fixed team_members_role_check constraint violation',
        'constraint_updated', true,
        'roles_allowed', ARRAY['OWNER', 'ADMIN', 'MEMBER', 'VIEWER'],
        'roles_mapped', 'Normalized case sensitivity',
        'default_role', 'MEMBER',
        'timestamp', NOW()
    ),
    'INFO',
    NOW()
);

-- ===============================================
-- 8. CLEANUP BACKUP (optional - keep for safety)
-- ===============================================

-- Comment this out if you want to keep the backup for recovery
-- DROP TABLE team_members_backup;