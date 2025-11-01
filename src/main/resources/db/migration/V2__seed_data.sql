-- Create default admin user
-- Password: admin123 (BCrypt hash)
-- NOTE: Change this password immediately after first login in production!
INSERT INTO users (id, email, password_hash, name, role, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    'admin@lineage.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Admin User',
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
