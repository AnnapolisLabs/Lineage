-- Migration: V13__Remove_seed_admin_user.sql
-- Description: Remove legacy SQL-seeded admin user. Admin is now created programmatically on startup.

-- Remove legacy seeded admin user created by V2__seed_data.sql
-- Admin is now created programmatically on application startup.
DELETE FROM user_roles
WHERE user_id IN (SELECT id FROM users WHERE email = 'admin@lineage.local');

DELETE FROM users
WHERE email = 'admin@lineage.local';