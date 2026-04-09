-- V3: Create project_owners table and add owner_id FK column to projects
-- Generated: 2026-04-08

CREATE TABLE IF NOT EXISTS project_owners (
    id         UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,

    PRIMARY KEY (id)
);

-- Add nullable owner_id FK column to projects
ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS owner_id UUID;

-- Foreign key constraint: projects.owner_id -> project_owners.id
ALTER TABLE projects
    ADD CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES project_owners (id);

-- Index on FK column for efficient join/lookup
CREATE INDEX IF NOT EXISTS idx_projects_owner_id ON projects (owner_id);
