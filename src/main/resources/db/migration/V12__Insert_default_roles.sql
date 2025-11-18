-- Migration: V12__Insert_default_roles.sql
-- Description: Insert default roles and initial role assignments for existing users

-- ===============================================
-- 1. INSERT DEFAULT GLOBAL ROLES
-- ===============================================

INSERT INTO roles (id, name, description, type, is_system, is_active, display_order, permissions) VALUES
-- Admin Role
(gen_random_uuid(), 'ADMIN', 'System administrator with full access to all features', 'GLOBAL', true, true, 1, '[
    "user.create", "user.read", "user.update", "user.delete", "user.manage",
    "role.create", "role.read", "role.update", "role.delete", "role.manage",
    "project.create", "project.read", "project.update", "project.delete", "project.manage",
    "requirement.create", "requirement.read", "requirement.update", "requirement.delete", "requirement.manage",
    "audit.read", "audit.manage",
    "system.configure", "system.monitor"
]'),
-- Project Manager Role  
(gen_random_uuid(), 'PROJECT_MANAGER', 'Project manager with project and team management capabilities', 'GLOBAL', true, true, 2, '[
    "user.read", "user.invite", 
    "project.create", "project.read", "project.update", "project.manage",
    "requirement.create", "requirement.read", "requirement.update", "requirement.delete", "requirement.manage",
    "team.manage"
]'),
-- Developer Role
(gen_random_uuid(), 'DEVELOPER', 'Developer with requirement creation and editing capabilities', 'GLOBAL', true, true, 3, '[
    "project.read", 
    "requirement.create", "requirement.read", "requirement.update", "requirement.delete"
]'),
-- Viewer Role
(gen_random_uuid(), 'VIEWER', 'Read-only access to projects and requirements', 'GLOBAL', true, true, 4, '[
    "project.read", 
    "requirement.read"
]')
ON CONFLICT (name) DO NOTHING;

-- ===============================================
-- 2. INSERT DEFAULT PROJECT ROLES
-- ===============================================

INSERT INTO roles (id, name, description, type, is_system, is_active, display_order, permissions) VALUES
-- Project Admin Role
(gen_random_uuid(), 'PROJECT_ADMIN', 'Project administrator with full project access', 'PROJECT', true, true, 1, '[
    "project.read", "project.update", "project.manage",
    "requirement.create", "requirement.read", "requirement.update", "requirement.delete", "requirement.manage",
    "team.manage", "team.invite", "team.remove"
]'),
-- Project Editor Role
(gen_random_uuid(), 'PROJECT_EDITOR', 'Project editor with requirement management capabilities', 'PROJECT', true, true, 2, '[
    "project.read", 
    "requirement.create", "requirement.read", "requirement.update", "requirement.delete"
]'),
-- Project Viewer Role
(gen_random_uuid(), 'PROJECT_VIEWER', 'Project viewer with read-only access', 'PROJECT', true, true, 3, '[
    "project.read", 
    "requirement.read"
]')
ON CONFLICT (name) DO NOTHING;

-- ===============================================
-- 3. ASSIGN GLOBAL ROLES TO EXISTING USERS
-- ===============================================

-- Create a mapping of old roles to new global roles
WITH role_mapping AS (
    SELECT 
        u.id as user_id,
        CASE 
            WHEN u.role = 'ADMIN' THEN 'ADMIN'
            WHEN u.role = 'EDITOR' THEN 'DEVELOPER' 
            ELSE 'VIEWER'
        END as new_global_role
    FROM users u
    WHERE u.global_role = 'VIEWER' OR u.global_role IS NULL
)
INSERT INTO user_roles (user_id, role_id, scope, granted_at, granted_by, is_active)
SELECT 
    rm.user_id,
    r.id as role_id,
    'GLOBAL'::varchar(20) as scope,
    NOW() as granted_at,
    NULL as granted_by,
    true as is_active
FROM role_mapping rm
JOIN roles r ON r.name = rm.new_global_role AND r.type = 'GLOBAL'
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = rm.user_id 
    AND ur.role_id = r.id 
    AND ur.scope = 'GLOBAL'
);

-- ===============================================
-- 4. CREATE DEFAULT PROJECT MEMBERSHIPS
-- ===============================================

-- Create ProjectRole enum mapping for existing project_members
-- This assumes there's already a project_members table or will be added
-- For now, we'll prepare the data structure for future integration

-- Create a view to map existing ProjectRole to new role system
CREATE OR REPLACE VIEW project_role_mapping AS
SELECT 
    'PROJECT_ADMIN' as project_role_new,
    'ADMIN' as project_role_old
UNION ALL
SELECT 
    'PROJECT_EDITOR',
    'EDITOR'
UNION ALL  
SELECT 
    'PROJECT_VIEWER',
    'VIEWER';

-- ===============================================
-- 5. AUDIT LOG FOR MIGRATION
-- ===============================================

-- Log the role migration in audit logs
INSERT INTO audit_logs (user_id, action, resource, resource_id, details, severity, created_at)
SELECT 
    NULL as user_id,
    'SYSTEM_MIGRATION' as action,
    'ROLE_SYSTEM' as resource,
    'V002_DEFAULT_ROLES' as resource_id,
    jsonb_build_object(
        'migration_version', 'V002',
        'description', 'Default role system initialized',
        'global_roles_created', (SELECT COUNT(*) FROM roles WHERE type = 'GLOBAL'),
        'project_roles_created', (SELECT COUNT(*) FROM roles WHERE type = 'PROJECT'),
        'users_migrated', (SELECT COUNT(*) FROM user_roles WHERE scope = 'GLOBAL'),
        'timestamp', NOW()
    ) as details,
    'INFO'::varchar(20) as severity,
    NOW() as created_at;

-- ===============================================
-- 6. VERIFICATION QUERIES
-- ===============================================

-- Verify role creation
DO $$
DECLARE
    global_role_count INTEGER;
    project_role_count INTEGER;
    user_role_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO global_role_count FROM roles WHERE type = 'GLOBAL';
    SELECT COUNT(*) INTO project_role_count FROM roles WHERE type = 'PROJECT';
    SELECT COUNT(*) INTO user_role_count FROM user_roles WHERE scope = 'GLOBAL';
    
    RAISE NOTICE 'Migration V002 completed successfully:';
    RAISE NOTICE 'Global roles created: %', global_role_count;
    RAISE NOTICE 'Project roles created: %', project_role_count;
    RAISE NOTICE 'User roles assigned: %', user_role_count;
    
    -- Verify expected counts
    IF global_role_count < 4 THEN
        RAISE EXCEPTION 'Expected at least 4 global roles, found %', global_role_count;
    END IF;
    
    IF project_role_count < 3 THEN
        RAISE EXCEPTION 'Expected at least 3 project roles, found %', project_role_count;
    END IF;
    
END $$;

-- ===============================================
-- 7. CLEANUP (if needed)
-- ===============================================

-- Optional: Clean up the old role column after migration is verified
-- This should only be done after application code is updated to use the new role system
-- ALTER TABLE users DROP COLUMN role;

-- Optional: Remove the role constraint if it exists
-- ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- ===============================================
-- 8. COMMENTS FOR ROLES (Table Comments)
-- ===============================================

COMMENT ON TABLE roles IS 'Application roles for RBAC system';
COMMENT ON COLUMN roles.permissions IS 'Role permissions in JSON array format';
COMMENT ON COLUMN roles.type IS 'GLOBAL: system-wide role, PROJECT: project-specific role';