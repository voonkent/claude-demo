# Task Manager — Kotlin Spring Boot 3.4

REST API for task management built with Kotlin, Spring Boot, and PostgreSQL.

## Tech Stack

- **Language:** Kotlin 1.9.25, JVM 21
- **Framework:** Spring Boot 3.4.4 (Web, Data JPA, Thymeleaf)
- **Database:** PostgreSQL 16 (via Docker Compose)
- **Build:** Gradle 8.12 (Kotlin DSL)
- **Testing:** JUnit 5, MockK, SpringMockK, Testcontainers

## Project Structure

```
com.example.taskmanager
├── config/          # JPA auditing config
├── domain/          # Entities and enums (Task, TaskStatus)
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic
├── controller/      # REST controllers
│   └── dto/         # Request/response DTOs
└── exception/       # Exception classes and @RestControllerAdvice handler
```

## Build & Run

```bash
# Start PostgreSQL
docker-compose up -d

# Build (compiles, tests, JaCoCo coverage check)
./gradlew build

# Run the application
./gradlew bootRun
```

The API is available at `http://localhost:8080/api/tasks`.

## Testing

```bash
# Run all tests
./gradlew test

# Generate coverage report
./gradlew jacocoTestReport
# HTML report: build/reports/jacoco/test/html/index.html

# Run build with coverage verification (80% min line + branch)
./gradlew build
```

## API Endpoints

| Method | Path              | Description     |
|--------|-------------------|-----------------|
| GET    | /api/tasks        | List all tasks  |
| GET    | /api/tasks/{id}   | Get task by ID  |
| POST   | /api/tasks        | Create task     |
| PUT    | /api/tasks/{id}   | Update task     |
| DELETE | /api/tasks/{id}   | Delete task     |

## Rules & Commands

- [Kotlin conventions](.claude/rules/kotlin-conventions.md)
- [Testing conventions](.claude/rules/testing-conventions.md)
- [Add entity command](.claude/commands/add-entity.md) — `/add-entity <EntityName>`
