-- Add level_prefixes to projects for custom requirement ID prefixes per level
ALTER TABLE projects ADD COLUMN level_prefixes JSONB DEFAULT '{}'::jsonb;

-- Add section column to requirements for optional hierarchical organization (e.g., "1.1.1", "2.3.4")
ALTER TABLE requirements ADD COLUMN section VARCHAR(50);

-- Create index on section for filtering/sorting
CREATE INDEX idx_requirements_section ON requirements(section) WHERE section IS NOT NULL;
