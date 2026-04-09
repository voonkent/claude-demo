-- V1: Create tasks table
-- Generated: 2026-04-08

CREATE TABLE IF NOT EXISTS tasks (
    id          UUID        NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    status      VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    PRIMARY KEY (id)
);
