-- Migration: V15__Fix_user_role_constraint.sql
-- Description: Fix constraint violation by updating the global_role constraint to match the updated UserRole enum

-- ===============================================
-- 1. BACKUP CURRENT USERS DATA
-- ===============================================

-- Create a backup of current users for safety
CREATE TABLE IF NOT EXISTS users_backup AS SELECT * FROM users;

-- ===============================================
-- 2. DROP EXISTING CONSTRAINT
-- ===============================================

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_global_role_check;

-- ===============================================
-- 3. CLEAN UP ANY INVALID ROLE VALUES
-- ===============================================

-- Handle any NULL or empty values first
UPDATE users SET global_role = 'USER' 
WHERE global_role IS NULL OR global_role = '' OR global_role = 'null';

-- Handle any invalid role values by mapping them to valid ones
UPDATE users SET global_role = CASE 
    WHEN global_role IN ('ADMIN', 'VIEWER', 'EDITOR') THEN 'USER'  -- Map legacy roles to USER
    WHEN global_role IN ('PROJECT_MANAGER', 'PM') THEN 'PROJECT_MANAGER' 
    WHEN global_role IN ('DEVELOPER', 'DEV') THEN 'DEVELOPER'
    WHEN global_role IN ('OWNER') THEN 'OWNER'
    WHEN global_role IN ('ADMINISTRATOR') THEN 'ADMINISTRATOR'
    ELSE 'USER'  -- Default unknown roles to USER
END
WHERE global_role NOT IN ('USER', 'PROJECT_MANAGER', 'DEVELOPER', 'OWNER', 'ADMINISTRATOR');

-- ===============================================
-- 4. ADD NEW CONSTRAINT WITH CLEAN ROLES
-- ===============================================

-- Add constraint that matches the clean UserRole enum values
ALTER TABLE users ADD CONSTRAINT users_global_role_check 
CHECK (global_role IN (
    'USER',            -- Standard user level 1
    'PROJECT_MANAGER', -- Project manager level 2  
    'DEVELOPER',       -- Developer level 1
    'OWNER',          -- Super-user level 3
    'ADMINISTRATOR'   -- Admin level 2
));

-- ===============================================
-- 5. FIX COLUMN DEFAULT VALUE
-- ===============================================

-- Ensure the column default matches the Java code default
ALTER TABLE users ALTER COLUMN global_role SET DEFAULT 'USER';

-- ===============================================
-- 6. VERIFICATION AND AUDIT
-- ===============================================

DO $$
DECLARE
    role_count INTEGER;
    invalid_roles INTEGER;
    constraint_exists BOOLEAN;
    backup_count INTEGER;
BEGIN
    -- Check users count
    SELECT COUNT(*) INTO role_count FROM users;
    
    -- Check backup
    SELECT COUNT(*) INTO backup_count FROM users_backup;
    
    -- Check for any invalid roles after our fixes
    SELECT COUNT(*) INTO invalid_roles FROM users 
    WHERE global_role NOT IN ('USER', 'PROJECT_MANAGER', 'DEVELOPER', 'OWNER', 'ADMINISTRATOR');
    
    -- Check if constraint exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'users_global_role_check' 
        AND table_name = 'users'
    ) INTO constraint_exists;
    
    RAISE NOTICE 'User role constraint fix completed:';
    RAISE NOTICE 'Total users: %', role_count;
    RAISE NOTICE 'Backup created: % records', backup_count;
    RAISE NOTICE 'Invalid roles after fix: %', invalid_roles;
    RAISE NOTICE 'Constraint exists: %', constraint_exists;
    
    -- Verify no users violate the constraint
    IF invalid_roles > 0 THEN
        RAISE EXCEPTION 'Found % users with invalid roles after constraint update', invalid_roles;
    END IF;
    
    IF NOT constraint_exists THEN
        RAISE EXCEPTION 'Failed to create users_global_role_check constraint';
    END IF;
END $$;

-- ===============================================
-- 7. LOG THE FIX
-- ===============================================

INSERT INTO audit_logs (user_id, action, resource, resource_id, details, severity, created_at)
VALUES (
    NULL,
    'CONSTRAINT_FIX',
    'USER_ROLES',
    'V015_CONSTRAINT_VIOLATION',
    jsonb_build_object(
        'migration_version', 'V015',
        'description', 'Fixed users_global_role_check constraint violation',
        'constraint_updated', true,
        'roles_allowed', ARRAY['USER', 'PROJECT_MANAGER', 'DEVELOPER', 'OWNER', 'ADMINISTRATOR'],
        'roles_mapped', 'Legacy roles mapped to new hierarchy',
        'default_role', 'USER',
        'timestamp', NOW()
    ),
    'INFO',
    NOW()
);

-- ===============================================
-- 8. CLEANUP BACKUP (optional - keep for safety)
-- ===============================================

-- Comment this out if you want to keep the backup for recovery
-- DROP TABLE users_backup;