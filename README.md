# Task Manager

A REST API and Kanban dashboard for task management, built with Kotlin, Spring Boot 3.4, and PostgreSQL.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9.25 (JVM 21) |
| Framework | Spring Boot 3.4.4 |
| Database | PostgreSQL 16 |
| Build | Gradle 8.12 (Kotlin DSL) |
| Testing | JUnit 5, MockK, SpringMockK, Testcontainers |
| Coverage | JaCoCo (80% min line + branch) |

## Features

- **REST API** — Full CRUD for tasks at `/api/tasks` with Bean Validation and structured error responses
- **Kanban Dashboard** — Thymeleaf-rendered board at `/dashboard` grouping tasks by status (To Do, In Progress, Done) with task counts per column
- **JPA Auditing** — Automatic `createdAt` / `updatedAt` timestamps
- **Global Exception Handling** — `@RestControllerAdvice` returning consistent error payloads (404, 400)

## Prerequisites

- JDK 21
- Docker & Docker Compose

## Quick Start

```bash
# Start PostgreSQL
docker compose up -d

# Build and run tests
./gradlew build

# Start the application
./gradlew bootRun
```

The API is available at http://localhost:8080/api/tasks and the dashboard at http://localhost:8080/dashboard.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/tasks` | List all tasks |
| GET | `/api/tasks/{id}` | Get task by ID |
| POST | `/api/tasks` | Create a task |
| PUT | `/api/tasks/{id}` | Update a task |
| DELETE | `/api/tasks/{id}` | Delete a task |
| GET | `/dashboard` | Kanban board UI |

### Example: Create a Task

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "My task", "description": "Details here", "status": "TODO"}'
```

## Project Structure

```
com.example.taskmanager
├── config/          # JPA auditing
├── domain/          # Task entity, TaskStatus enum
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic
├── controller/      # REST + dashboard controllers
│   └── dto/         # Request/response DTOs
└── exception/       # Exception classes and handler
```

## Testing

```bash
# Run all tests
./gradlew test

# Generate coverage report
./gradlew jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

Tests are organized by layer:

| Layer | Style | Tools |
|-------|-------|-------|
| Repository | `@DataJpaTest` + Testcontainers | PostgreSQL 16 container, `@ServiceConnection` |
| Service | Unit tests (no Spring context) | MockK |
| Controller | `@WebMvcTest` | MockMvc, `@MockkBean` (SpringMockK) |
