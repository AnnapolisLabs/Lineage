-- Add level column to requirements table for DOORS-style hierarchical numbering
ALTER TABLE requirements ADD COLUMN level INTEGER NOT NULL DEFAULT 1;

-- Create index on level for performance
CREATE INDEX idx_requirements_level ON requirements(level);
