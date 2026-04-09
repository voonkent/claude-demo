-- V2: Create projects table and add project_id FK column to tasks
-- Generated: 2026-04-08

CREATE TABLE IF NOT EXISTS projects (
    id         UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,

    PRIMARY KEY (id)
);

-- Add nullable project_id FK column to tasks
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS project_id UUID;

-- Foreign key constraint: tasks.project_id -> projects.id
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id);

-- Index on FK column for efficient join/lookup
CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks (project_id);
