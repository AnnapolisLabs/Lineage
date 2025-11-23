-- Migration: V11__Create_user_management_tables.sql
-- Description: Create enhanced user management tables and add security fields to existing users table

-- ===============================================
-- 1. ENHANCE EXISTING USERS TABLE
-- ===============================================

-- Add new columns to existing users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS last_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(255),
ADD COLUMN IF NOT EXISTS bio TEXT,
ADD COLUMN IF NOT EXISTS preferences JSONB DEFAULT '{}',
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE',
ADD COLUMN IF NOT EXISTS global_role VARCHAR(20) DEFAULT 'VIEWER',
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255),
ADD COLUMN IF NOT EXISTS email_verification_expiry TIMESTAMP,
ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255),
ADD COLUMN IF NOT EXISTS password_reset_expiry TIMESTAMP,
ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_login_ip INET,
ADD COLUMN IF NOT EXISTS created_by UUID,
ADD COLUMN IF NOT EXISTS updated_by UUID;

-- Add constraint for status enum
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'users_status_check') THEN
        ALTER TABLE users ADD CONSTRAINT users_status_check 
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED', 'PENDING_VERIFICATION'));
    END IF;
END $$;

-- Add constraint for global role enum
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'users_global_role_check') THEN
        ALTER TABLE users ADD CONSTRAINT users_global_role_check 
        CHECK (global_role IN ('ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'VIEWER'));
    END IF;
END $$;

-- Update existing users to have the new role system
UPDATE users SET global_role = CASE 
    WHEN role = 'ADMIN' THEN 'ADMIN'
    WHEN role = 'EDITOR' THEN 'DEVELOPER'
    ELSE 'VIEWER'
END;

-- ===============================================
-- 2. CREATE NEW TABLES
-- ===============================================

-- User Security Table (MFA and security settings)
CREATE TABLE IF NOT EXISTS user_security (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    secret_key VARCHAR(255),
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_backup_codes TEXT,
    mfa_enabled_at TIMESTAMP,
    security_preferences JSONB DEFAULT '{}',
    device_fingerprint VARCHAR(255),
    last_security_check TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id)
);

-- User Invitations Table
CREATE TABLE IF NOT EXISTS user_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invited_by UUID NOT NULL REFERENCES users(id),
    email VARCHAR(255) NOT NULL,
    project_id UUID REFERENCES projects(id),
    project_role VARCHAR(20) CHECK (project_role IN ('ADMIN', 'EDITOR', 'VIEWER')),
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED')),
    expiry_date TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100),
    resource_id VARCHAR(255),
    details JSONB DEFAULT '{}',
    ip_address INET,
    user_agent TEXT,
    severity VARCHAR(20) DEFAULT 'INFO' CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    created_at TIMESTAMP DEFAULT NOW()
);

-- User Sessions Table
CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    token_hash VARCHAR(255) NOT NULL,
    refresh_token_hash VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    refresh_expires_at TIMESTAMP,
    last_activity_at TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    device_info JSONB,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    type VARCHAR(20) NOT NULL CHECK (type IN ('GLOBAL', 'PROJECT')),
    is_system BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INTEGER,
    permissions JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT NOW()
);

-- User Roles Table (Many-to-Many relationship)
CREATE TABLE IF NOT EXISTS user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id),
    scope VARCHAR(20) DEFAULT 'GLOBAL' CHECK (scope IN ('GLOBAL', 'PROJECT')),
    granted_by UUID REFERENCES users(id),
    granted_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, role_id, project_id, scope)
);

-- ===============================================
-- 3. CREATE INDEXES FOR PERFORMANCE
-- ===============================================

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_global_role ON users(global_role);
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);
CREATE INDEX IF NOT EXISTS idx_users_last_login_at ON users(last_login_at);

-- User Security indexes
CREATE INDEX IF NOT EXISTS idx_user_security_user_id ON user_security(user_id);
CREATE INDEX IF NOT EXISTS idx_user_security_mfa_enabled ON user_security(mfa_enabled);

-- User Invitations indexes
CREATE INDEX IF NOT EXISTS idx_user_invitations_token ON user_invitations(token);
CREATE INDEX IF NOT EXISTS idx_user_invitations_email ON user_invitations(email);
CREATE INDEX IF NOT EXISTS idx_user_invitations_project_id ON user_invitations(project_id);
CREATE INDEX IF NOT EXISTS idx_user_invitations_invited_by ON user_invitations(invited_by);
CREATE INDEX IF NOT EXISTS idx_user_invitations_status ON user_invitations(status);
CREATE INDEX IF NOT EXISTS idx_user_invitations_expiry_date ON user_invitations(expiry_date);

-- Audit Logs indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON audit_logs(resource);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource_id ON audit_logs(resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_severity ON audit_logs(severity);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_ip_address ON audit_logs(ip_address);

-- User Sessions indexes
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_session_id ON user_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_token_hash ON user_sessions(token_hash);
CREATE INDEX IF NOT EXISTS idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_revoked ON user_sessions(revoked);

-- Roles indexes
CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);
CREATE INDEX IF NOT EXISTS idx_roles_type ON roles(type);

-- User Roles indexes
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_project_id ON user_roles(project_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_scope ON user_roles(scope);

-- ===============================================
-- 4. INSERT DEFAULT ROLES (compatibility fix)
-- ===============================================

-- Insert default system roles
INSERT INTO roles (id, name, description, type, is_system, is_active, display_order, permissions) VALUES
(gen_random_uuid(), 'ADMIN', 'System Administrator', 'GLOBAL', true, true, 1, '["ALL"]'),
(gen_random_uuid(), 'PROJECT_MANAGER', 'Project Manager', 'GLOBAL', true, true, 2, '["PROJECT_CREATE", "PROJECT_EDIT", "PROJECT_DELETE", "USER_INVITE"]'),
(gen_random_uuid(), 'DEVELOPER', 'Developer', 'GLOBAL', true, true, 3, '["REQUIREMENT_CREATE", "REQUIREMENT_EDIT", "REQUIREMENT_DELETE"]'),
(gen_random_uuid(), 'VIEWER', 'Viewer', 'GLOBAL', true, true, 4, '["READ_ONLY"]')
ON CONFLICT (name) DO NOTHING;

-- ===============================================
-- 5. GRANT PERMISSIONS (for production environments)
-- ===============================================

-- Optional: Grant specific permissions (uncomment for production)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON user_security TO lineage_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON user_invitations TO lineage_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON audit_logs TO lineage_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON user_sessions TO lineage_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON roles TO lineage_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON user_roles TO lineage_app;

-- ===============================================
-- 6. COMMENTS FOR DOCUMENTATION
-- ===============================================

COMMENT ON TABLE user_security IS 'User security settings including MFA and device tracking';
COMMENT ON TABLE user_invitations IS 'User invitation system for project collaboration';
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for security and compliance';
COMMENT ON TABLE user_sessions IS 'User session management with device tracking';
COMMENT ON TABLE roles IS 'Role definitions for RBAC';
COMMENT ON TABLE user_roles IS 'User-role assignments with project scoping';

COMMENT ON COLUMN user_security.mfa_backup_codes IS 'Encrypted backup codes for MFA recovery';
COMMENT ON COLUMN user_invitations.token IS 'Unique token for invitation acceptance';
COMMENT ON COLUMN audit_logs.details IS 'Additional event details in JSON format';
COMMENT ON COLUMN user_sessions.device_info IS 'Device fingerprinting data in JSON format';
COMMENT ON COLUMN roles.permissions IS 'Role permissions in JSON array format';
COMMENT ON COLUMN user_roles.scope IS 'GLOBAL: system-wide role, PROJECT: project-specific role';