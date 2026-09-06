-- Migration: V18__Fix_task_assignments_status_priority_constraint.sql
-- Description: Fix task_assignments status/priority constraints to match the TaskStatus/TaskPriority enum values
-- (same lowercase-vs-uppercase mismatch already fixed for team_members in V16/V17)

-- ===============================================
-- 1. BACKUP CURRENT TASK ASSIGNMENTS DATA
-- ===============================================

CREATE TABLE IF NOT EXISTS task_assignments_backup AS SELECT * FROM task_assignments;

-- ===============================================
-- 2. DROP EXISTING CONSTRAINTS
-- ===============================================

ALTER TABLE task_assignments DROP CONSTRAINT IF EXISTS task_assignments_status_check;
ALTER TABLE task_assignments DROP CONSTRAINT IF EXISTS task_assignments_priority_check;

-- ===============================================
-- 3. CLEAN UP ANY INVALID / LOWERCASE VALUES
-- ===============================================

UPDATE task_assignments SET status = 'ASSIGNED'
WHERE status IS NULL OR status = '' OR status = 'null';

UPDATE task_assignments SET status = CASE
    WHEN UPPER(status) = 'ASSIGNED' THEN 'ASSIGNED'
    WHEN UPPER(status) = 'IN_PROGRESS' THEN 'IN_PROGRESS'
    WHEN UPPER(status) = 'COMPLETED' THEN 'COMPLETED'
    WHEN UPPER(status) = 'CANCELLED' THEN 'CANCELLED'
    ELSE 'ASSIGNED'
END
WHERE status NOT IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');

UPDATE task_assignments SET priority = 'MEDIUM'
WHERE priority IS NULL OR priority = '' OR priority = 'null';

UPDATE task_assignments SET priority = CASE
    WHEN UPPER(priority) = 'LOW' THEN 'LOW'
    WHEN UPPER(priority) = 'MEDIUM' THEN 'MEDIUM'
    WHEN UPPER(priority) = 'HIGH' THEN 'HIGH'
    WHEN UPPER(priority) = 'CRITICAL' THEN 'CRITICAL'
    ELSE 'MEDIUM'
END
WHERE priority NOT IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');

-- ===============================================
-- 4. ADD NEW CONSTRAINTS WITH MATCHING ENUM VALUES
-- ===============================================

ALTER TABLE task_assignments ADD CONSTRAINT task_assignments_status_check
CHECK (status IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

ALTER TABLE task_assignments ADD CONSTRAINT task_assignments_priority_check
CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

-- ===============================================
-- 5. FIX COLUMN DEFAULT VALUES
-- ===============================================

ALTER TABLE task_assignments ALTER COLUMN status SET DEFAULT 'ASSIGNED';
ALTER TABLE task_assignments ALTER COLUMN priority SET DEFAULT 'MEDIUM';

-- ===============================================
-- 6. VERIFICATION AND AUDIT
-- ===============================================

DO $$
DECLARE
    task_count INTEGER;
    invalid_statuses INTEGER;
    invalid_priorities INTEGER;
    status_constraint_exists BOOLEAN;
    priority_constraint_exists BOOLEAN;
    backup_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO task_count FROM task_assignments;
    SELECT COUNT(*) INTO backup_count FROM task_assignments_backup;

    SELECT COUNT(*) INTO invalid_statuses FROM task_assignments
    WHERE status NOT IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');

    SELECT COUNT(*) INTO invalid_priorities FROM task_assignments
    WHERE priority NOT IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');

    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'task_assignments_status_check'
        AND table_name = 'task_assignments'
    ) INTO status_constraint_exists;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'task_assignments_priority_check'
        AND table_name = 'task_assignments'
    ) INTO priority_constraint_exists;

    RAISE NOTICE 'Task assignments status/priority constraint fix completed:';
    RAISE NOTICE 'Total tasks: %', task_count;
    RAISE NOTICE 'Backup created: % records', backup_count;
    RAISE NOTICE 'Invalid statuses after fix: %', invalid_statuses;
    RAISE NOTICE 'Invalid priorities after fix: %', invalid_priorities;
    RAISE NOTICE 'Status constraint exists: %', status_constraint_exists;
    RAISE NOTICE 'Priority constraint exists: %', priority_constraint_exists;

    IF invalid_statuses > 0 THEN
        RAISE EXCEPTION 'Found % tasks with invalid statuses after constraint update', invalid_statuses;
    END IF;

    IF invalid_priorities > 0 THEN
        RAISE EXCEPTION 'Found % tasks with invalid priorities after constraint update', invalid_priorities;
    END IF;

    IF NOT status_constraint_exists THEN
        RAISE EXCEPTION 'Failed to create task_assignments_status_check constraint';
    END IF;

    IF NOT priority_constraint_exists THEN
        RAISE EXCEPTION 'Failed to create task_assignments_priority_check constraint';
    END IF;
END $$;

-- ===============================================
-- 7. LOG THE FIX
-- ===============================================

INSERT INTO audit_logs (user_id, action, resource, resource_id, details, severity, created_at)
VALUES (
    NULL,
    'CONSTRAINT_FIX',
    'TASK_ASSIGNMENTS',
    'V018_CONSTRAINT_VIOLATION',
    jsonb_build_object(
        'migration_version', 'V018',
        'description', 'Fixed task_assignments_status_check and task_assignments_priority_check constraint violations',
        'constraint_updated', true,
        'statuses_allowed', ARRAY['ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'],
        'priorities_allowed', ARRAY['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'],
        'timestamp', NOW()
    ),
    'INFO',
    NOW()
);
