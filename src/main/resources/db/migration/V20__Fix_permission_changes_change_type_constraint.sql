-- Migration: V20__Fix_permission_changes_change_type_constraint.sql
-- Description: Fix permission_changes change_type constraint to match the PermissionChange.ChangeType
-- enum values (same lowercase-vs-uppercase mismatch already fixed for team_members in V16/V17,
-- task_assignments in V18, and peer_reviews in V19)

-- ===============================================
-- 1. BACKUP CURRENT PERMISSION CHANGES DATA
-- ===============================================

CREATE TABLE IF NOT EXISTS permission_changes_backup AS SELECT * FROM permission_changes;

-- ===============================================
-- 2. DROP EXISTING CONSTRAINT
-- ===============================================

ALTER TABLE permission_changes DROP CONSTRAINT IF EXISTS permission_changes_change_type_check;

-- ===============================================
-- 3. CLEAN UP ANY INVALID / LOWERCASE VALUES
-- ===============================================

UPDATE permission_changes SET change_type = 'GRANT'
WHERE change_type IS NULL OR change_type = '' OR change_type = 'null';

UPDATE permission_changes SET change_type = CASE
    WHEN UPPER(change_type) = 'GRANT' THEN 'GRANT'
    WHEN UPPER(change_type) = 'REVOKE' THEN 'REVOKE'
    WHEN UPPER(change_type) = 'MODIFY' THEN 'MODIFY'
    WHEN UPPER(change_type) = 'EXTEND' THEN 'EXTEND'
    WHEN UPPER(change_type) = 'SUSPEND' THEN 'SUSPEND'
    ELSE 'GRANT'
END
WHERE change_type NOT IN ('GRANT', 'REVOKE', 'MODIFY', 'EXTEND', 'SUSPEND');

-- ===============================================
-- 4. ADD NEW CONSTRAINT WITH MATCHING ENUM VALUES
-- ===============================================

ALTER TABLE permission_changes ADD CONSTRAINT permission_changes_change_type_check
CHECK (change_type IN ('GRANT', 'REVOKE', 'MODIFY', 'EXTEND', 'SUSPEND'));

-- ===============================================
-- 5. FIX COLUMN DEFAULT VALUE
-- ===============================================

ALTER TABLE permission_changes ALTER COLUMN change_type SET DEFAULT 'GRANT';

-- ===============================================
-- 6. VERIFICATION AND AUDIT
-- ===============================================

DO $$
DECLARE
    change_count INTEGER;
    invalid_types INTEGER;
    constraint_exists BOOLEAN;
    backup_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO change_count FROM permission_changes;
    SELECT COUNT(*) INTO backup_count FROM permission_changes_backup;

    SELECT COUNT(*) INTO invalid_types FROM permission_changes
    WHERE change_type NOT IN ('GRANT', 'REVOKE', 'MODIFY', 'EXTEND', 'SUSPEND');

    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'permission_changes_change_type_check'
        AND table_name = 'permission_changes'
    ) INTO constraint_exists;

    RAISE NOTICE 'Permission changes change_type constraint fix completed:';
    RAISE NOTICE 'Total permission changes: %', change_count;
    RAISE NOTICE 'Backup created: % records', backup_count;
    RAISE NOTICE 'Invalid change types after fix: %', invalid_types;
    RAISE NOTICE 'Constraint exists: %', constraint_exists;

    IF invalid_types > 0 THEN
        RAISE EXCEPTION 'Found % permission changes with invalid change_type after constraint update', invalid_types;
    END IF;

    IF NOT constraint_exists THEN
        RAISE EXCEPTION 'Failed to create permission_changes_change_type_check constraint';
    END IF;
END $$;

-- ===============================================
-- 7. LOG THE FIX
-- ===============================================

INSERT INTO audit_logs (user_id, action, resource, resource_id, details, severity, created_at)
VALUES (
    NULL,
    'CONSTRAINT_FIX',
    'PERMISSION_CHANGES',
    'V020_CONSTRAINT_VIOLATION',
    jsonb_build_object(
        'migration_version', 'V020',
        'description', 'Fixed permission_changes_change_type_check constraint violation',
        'constraint_updated', true,
        'change_types_allowed', ARRAY['GRANT', 'REVOKE', 'MODIFY', 'EXTEND', 'SUSPEND'],
        'timestamp', NOW()
    ),
    'INFO',
    NOW()
);
