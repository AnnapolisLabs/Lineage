-- Migration: V14__Enhanced_RBAC_Schema.sql
-- Description: Create comprehensive RBAC schema with role hierarchy, collaboration features, and permission management
-- Version: 14
-- Date: 2025-11-23
-- Author: Architecture Team

-- ===============================================
-- 1. ENHANCED ROLES TABLE WITH HIERARCHY
-- ===============================================

-- Add new columns to existing roles table
ALTER TABLE roles 
ADD COLUMN IF NOT EXISTS hierarchy_level INTEGER DEFAULT 1,
ADD COLUMN IF NOT EXISTS inheritance_path UUID[],
ADD COLUMN IF NOT EXISTS escalation_rules JSONB DEFAULT '{}',
ADD COLUMN IF NOT EXISTS collaboration_permissions JSONB DEFAULT '{}',
ADD COLUMN IF NOT EXISTS temporary_permissions JSONB DEFAULT '{}',
ADD COLUMN IF NOT EXISTS resource_scopes JSONB DEFAULT '[]';

-- Update existing roles with new hierarchy levels
UPDATE roles SET hierarchy_level = CASE
    WHEN name = 'OWNER' THEN 3
    WHEN name = 'ADMIN' THEN 2  
    WHEN name = 'ADMINISTRATOR' THEN 2
    WHEN name = 'PROJECT_MANAGER' THEN 2
    WHEN name = 'DEVELOPER' THEN 1
    WHEN name = 'USER' THEN 1
    WHEN name = 'VIEWER' THEN 1
    ELSE hierarchy_level
END;

-- Add inheritance paths for role hierarchy
UPDATE roles SET inheritance_path = CASE
    WHEN name = 'OWNER' THEN ARRAY[(SELECT id FROM roles WHERE name = 'ADMINISTRATOR')]
    WHEN name = 'ADMINISTRATOR' THEN ARRAY[(SELECT id FROM roles WHERE name = 'USER')]
    ELSE inheritance_path
END;

-- Add collaboration permissions for different role types
UPDATE roles SET collaboration_permissions = CASE
    WHEN name = 'OWNER' THEN '[
        "team.manage", "team.invite", "team.remove", "team.configure",
        "task.assign", "task.manage", "task.complete", "task.reassign",
        "peer.review", "peer.approve", "peer.reject",
        "comment.create", "comment.moderate", "comment.delete",
        "workflow.manage", "workflow.execute", "workflow.configure",
        "notification.manage", "notification.send", "notification.configure"
    ]'::jsonb
    WHEN name = 'ADMINISTRATOR' THEN '[
        "team.manage", "team.invite", "team.remove",
        "task.assign", "task.complete", "task.reassign",
        "peer.review", "peer.approve", "peer.reject",
        "comment.create", "comment.moderate",
        "workflow.execute",
        "notification.send"
    ]'::jsonb
    WHEN name = 'USER' THEN '[
        "team.participate", "team.join", "team.leave",
        "task.complete", "task.update",
        "peer.review", "peer.comment",
        "comment.create", "comment.read",
        "workflow.participate",
        "notification.read"
    ]'::jsonb
    ELSE collaboration_permissions
END;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_roles_hierarchy_level ON roles(hierarchy_level);
CREATE INDEX IF NOT EXISTS idx_roles_type_hierarchy ON roles(type, hierarchy_level);
CREATE INDEX IF NOT EXISTS idx_roles_resource_scopes ON roles USING GIN(resource_scopes);

-- ===============================================
-- 2. PERMISSION DEFINITIONS TABLE
-- ===============================================

CREATE TABLE IF NOT EXISTS permission_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_key VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    requires_confirmation BOOLEAN DEFAULT FALSE,
    audit_required BOOLEAN DEFAULT TRUE,
    is_system BOOLEAN DEFAULT FALSE,
    risk_level VARCHAR(20) DEFAULT 'LOW' CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Insert comprehensive permission definitions
INSERT INTO permission_definitions (permission_key, resource, action, description, category, risk_level) VALUES
-- Project Management Permissions
('project.create', 'project', 'create', 'Create new projects', 'project_management', 'MEDIUM'),
('project.read', 'project', 'read', 'View project details', 'project_management', 'LOW'),
('project.update', 'project', 'update', 'Modify project properties', 'project_management', 'MEDIUM'),
('project.delete', 'project', 'delete', 'Delete projects', 'project_management', 'CRITICAL'),
('project.manage', 'project', 'manage', 'Full project administration', 'project_management', 'HIGH'),
('project.transfer_ownership', 'project', 'transfer_ownership', 'Transfer project ownership', 'project_management', 'CRITICAL'),

-- User Management Permissions  
('user.create', 'user', 'create', 'Create user accounts', 'user_management', 'HIGH'),
('user.read', 'user', 'read', 'View user information', 'user_management', 'LOW'),
('user.update', 'user', 'update', 'Modify user profiles', 'user_management', 'MEDIUM'),
('user.delete', 'user', 'delete', 'Delete user accounts', 'user_management', 'CRITICAL'),
('user.manage', 'user', 'manage', 'Full user administration', 'user_management', 'HIGH'),

-- Role Management Permissions
('role.create', 'role', 'create', 'Create new roles', 'role_management', 'HIGH'),
('role.read', 'role', 'read', 'View role definitions', 'role_management', 'LOW'),
('role.update', 'role', 'update', 'Modify role permissions', 'role_management', 'HIGH'),
('role.delete', 'role', 'delete', 'Delete role definitions', 'role_management', 'CRITICAL'),

-- Team Collaboration Permissions
('team.manage', 'team', 'manage', 'Manage team structures', 'collaboration', 'MEDIUM'),
('team.invite', 'team', 'invite', 'Invite team members', 'collaboration', 'LOW'),
('team.participate', 'team', 'participate', 'Participate in teams', 'collaboration', 'LOW'),

-- Task Management Permissions
('task.assign', 'task', 'assign', 'Assign tasks to team members', 'task_management', 'LOW'),
('task.complete', 'task', 'complete', 'Complete assigned tasks', 'task_management', 'LOW'),
('task.manage', 'task', 'manage', 'Manage task assignments', 'task_management', 'MEDIUM'),
('task.read', 'task', 'read', 'View task information', 'task_management', 'LOW'),

-- Peer Review Permissions
('peer.review', 'peer', 'review', 'Conduct peer reviews', 'collaboration', 'LOW'),
('peer.approve', 'peer', 'approve', 'Approve peer reviews', 'collaboration', 'MEDIUM'),
('peer.reject', 'peer', 'reject', 'Reject peer reviews', 'collaboration', 'MEDIUM'),

-- System Administration Permissions
('system.configure', 'system', 'configure', 'Configure system settings', 'system_administration', 'CRITICAL'),
('system.monitor', 'system', 'monitor', 'Monitor system performance', 'system_administration', 'HIGH'),

-- Audit and Compliance Permissions
('audit.read', 'audit', 'read', 'Read audit logs', 'audit_compliance', 'MEDIUM'),
('audit.manage', 'audit', 'manage', 'Manage audit settings', 'audit_compliance', 'HIGH')

ON CONFLICT (permission_key) DO UPDATE SET
    description = EXCLUDED.description,
    category = EXCLUDED.category,
    risk_level = EXCLUDED.risk_level,
    updated_at = NOW();

-- Create indexes for permission definitions
CREATE INDEX IF NOT EXISTS idx_permission_definitions_resource ON permission_definitions(resource);
CREATE INDEX IF NOT EXISTS idx_permission_definitions_category ON permission_definitions(category);
CREATE INDEX IF NOT EXISTS idx_permission_definitions_risk_level ON permission_definitions(risk_level);
CREATE INDEX IF NOT EXISTS idx_permission_definitions_key ON permission_definitions(permission_key);

-- ===============================================
-- 3. TEAM MANAGEMENT TABLES
-- ===============================================

CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id),
    is_active BOOLEAN DEFAULT TRUE,
    settings JSONB DEFAULT '{}',
    auto_assign_reviewers BOOLEAN DEFAULT FALSE,
    require_peer_review BOOLEAN DEFAULT TRUE,
    max_members INTEGER DEFAULT 50,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(project_id, name)
);

CREATE TABLE IF NOT EXISTS team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) DEFAULT 'member' CHECK (role IN ('owner', 'admin', 'member', 'viewer')),
    permissions JSONB DEFAULT '{}',
    joined_at TIMESTAMP DEFAULT NOW(),
    invited_by UUID REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'pending')),
    last_activity_at TIMESTAMP,
    contribution_score INTEGER DEFAULT 0,
    UNIQUE(team_id, user_id)
);

-- ===============================================
-- 4. TASK MANAGEMENT TABLES
-- ===============================================

CREATE TABLE IF NOT EXISTS task_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_title VARCHAR(255) NOT NULL,
    task_description TEXT,
    assigned_by UUID NOT NULL REFERENCES users(id),
    assigned_to UUID NOT NULL REFERENCES users(id),
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    requirement_id UUID REFERENCES requirements(id) ON DELETE SET NULL,
    status VARCHAR(20) DEFAULT 'assigned' CHECK (status IN ('assigned', 'in_progress', 'completed', 'cancelled')),
    priority VARCHAR(20) DEFAULT 'medium' CHECK (priority IN ('low', 'medium', 'high', 'critical')),
    due_date TIMESTAMP,
    completed_at TIMESTAMP,
    estimated_hours INTEGER,
    actual_hours INTEGER,
    notes TEXT,
    completion_notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Task tags and blockers collections
CREATE TABLE IF NOT EXISTS task_tags (
    task_id UUID REFERENCES task_assignments(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (task_id, tag)
);

CREATE TABLE IF NOT EXISTS task_blockers (
    task_id UUID REFERENCES task_assignments(id) ON DELETE CASCADE,
    blocker VARCHAR(200) NOT NULL,
    PRIMARY KEY (task_id, blocker)
);

-- ===============================================
-- 5. PEER REVIEW TABLES
-- ===============================================

CREATE TABLE IF NOT EXISTS peer_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id UUID NOT NULL REFERENCES requirements(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id),
    author_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'revision_requested')),
    review_type VARCHAR(50) DEFAULT 'code' CHECK (review_type IN ('code', 'design', 'documentation', 'process', 'requirements')),
    comments TEXT,
    review_details JSONB DEFAULT '{}',
    effort_rating INTEGER CHECK (effort_rating >= 1 AND effort_rating <= 5),
    quality_rating INTEGER CHECK (quality_rating >= 1 AND quality_rating <= 5),
    priority_suggestion VARCHAR(20) CHECK (priority_suggestion IN ('maintain', 'increase', 'decrease')),
    review_deadline TIMESTAMP,
    reviewed_at TIMESTAMP,
    time_spent_minutes INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(requirement_id, reviewer_id)
);

-- Peer review files and tags
CREATE TABLE IF NOT EXISTS review_files (
    review_id UUID REFERENCES peer_reviews(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    PRIMARY KEY (review_id, file_path)
);

CREATE TABLE IF NOT EXISTS review_tags (
    review_id UUID REFERENCES peer_reviews(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (review_id, tag)
);

-- ===============================================
-- 6. PERMISSION AUDIT TABLES
-- ===============================================

CREATE TABLE IF NOT EXISTS permission_changes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    changed_by UUID NOT NULL REFERENCES users(id),
    permission_key VARCHAR(100) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    resource_id UUID,
    change_type VARCHAR(20) DEFAULT 'grant' CHECK (change_type IN ('grant', 'revoke', 'modify', 'extend', 'suspend')),
    reason TEXT,
    approved BOOLEAN DEFAULT TRUE,
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP,
    effective_from TIMESTAMP DEFAULT NOW(),
    effective_until TIMESTAMP,
    expires_at TIMESTAMP,
    is_temporary BOOLEAN DEFAULT FALSE,
    auto_expire BOOLEAN DEFAULT FALSE,
    context_data JSONB DEFAULT '{}',
    request_source VARCHAR(100),
    risk_level VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ===============================================
-- 7. CREATE INDEXES FOR PERFORMANCE
-- ===============================================

-- Team management indexes
CREATE INDEX IF NOT EXISTS idx_teams_project_id ON teams(project_id);
CREATE INDEX IF NOT EXISTS idx_teams_created_by ON teams(created_by);
CREATE INDEX IF NOT EXISTS idx_teams_active ON teams(is_active);
CREATE UNIQUE INDEX IF NOT EXISTS uk_teams_project_name ON teams(project_id, name);

CREATE INDEX IF NOT EXISTS idx_team_members_team_id ON team_members(team_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user_id ON team_members(user_id);
CREATE INDEX IF NOT EXISTS idx_team_members_status ON team_members(status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_team_members_team_user ON team_members(team_id, user_id);

-- Task management indexes
CREATE INDEX IF NOT EXISTS idx_task_assignments_assigned_to ON task_assignments(assigned_to);
CREATE INDEX IF NOT EXISTS idx_task_assignments_assigned_by ON task_assignments(assigned_by);
CREATE INDEX IF NOT EXISTS idx_task_assignments_project_id ON task_assignments(project_id);
CREATE INDEX IF NOT EXISTS idx_task_assignments_requirement_id ON task_assignments(requirement_id);
CREATE INDEX IF NOT EXISTS idx_task_assignments_status ON task_assignments(status);
CREATE INDEX IF NOT EXISTS idx_task_assignments_priority ON task_assignments(priority);
CREATE INDEX IF NOT EXISTS idx_task_assignments_due_date ON task_assignments(due_date);

-- Peer review indexes
CREATE INDEX IF NOT EXISTS idx_peer_reviews_requirement_id ON peer_reviews(requirement_id);
CREATE INDEX IF NOT EXISTS idx_peer_reviews_reviewer_id ON peer_reviews(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_peer_reviews_author_id ON peer_reviews(author_id);
CREATE INDEX IF NOT EXISTS idx_peer_reviews_status ON peer_reviews(status);
CREATE INDEX IF NOT EXISTS idx_peer_reviews_reviewed_at ON peer_reviews(reviewed_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_peer_reviews_requirement_reviewer ON peer_reviews(requirement_id, reviewer_id);

-- Permission audit indexes
CREATE INDEX IF NOT EXISTS idx_permission_changes_user_id ON permission_changes(user_id);
CREATE INDEX IF NOT EXISTS idx_permission_changes_changed_by ON permission_changes(changed_by);
CREATE INDEX IF NOT EXISTS idx_permission_changes_permission_key ON permission_changes(permission_key);
CREATE INDEX IF NOT EXISTS idx_permission_changes_resource_id ON permission_changes(resource_id);
CREATE INDEX IF NOT EXISTS idx_permission_changes_change_type ON permission_changes(change_type);
CREATE INDEX IF NOT EXISTS idx_permission_changes_effective_from ON permission_changes(effective_from);
CREATE INDEX IF NOT EXISTS idx_permission_changes_effective_until ON permission_changes(effective_until);

-- ===============================================
-- 8. VERIFICATION
-- ===============================================

DO $$
DECLARE
    permission_count INTEGER;
    role_count INTEGER;
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO permission_count FROM permission_definitions;
    SELECT COUNT(*) INTO role_count FROM roles;
    SELECT COUNT(*) INTO table_count FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name IN ('teams', 'team_members', 'task_assignments', 'peer_reviews', 'permission_changes', 'permission_definitions');
    
    RAISE NOTICE 'RBAC schema migration completed:';
    RAISE NOTICE 'Permissions: %, Roles: %, Tables: %', permission_count, role_count, table_count;
END $$;