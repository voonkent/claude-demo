# Task Priority with Filtering — Design Spec

**Date:** 2026-04-09
**Feature:** Add LOW/MEDIUM/HIGH priority to tasks; filter `GET /api/tasks` by priority.

---

## Overview

Tasks gain an optional, nullable `priority` field. The `GET /api/tasks` endpoint accepts an optional `?priority=` query parameter to filter results. All other endpoints accept and return priority where relevant.

---

## Data Model

### New enum: `domain/TaskPriority.kt`

```kotlin
enum class TaskPriority { LOW, MEDIUM, HIGH }
```

### `Task` entity — new field

```kotlin
@Enumerated(EnumType.STRING)
@Column(nullable = true)
var priority: TaskPriority? = null
```

Schema migration is handled automatically by `ddl-auto: update`. Hibernate adds a nullable `priority` column to the `tasks` table. Existing rows get `NULL`.

---

## API Contract

### `POST /api/tasks` — Create

`priority` is optional; omitting it stores `null`.

```json
{ "title": "Fix bug", "priority": "HIGH" }
{ "title": "Fix bug" }
```

### `PUT /api/tasks/{id}` — Update

`priority` is optional; omitting it or sending `null` clears/leaves the priority as null.

```json
{ "title": "Fix bug", "status": "IN_PROGRESS", "priority": "LOW" }
{ "title": "Fix bug", "status": "IN_PROGRESS" }
```

### `GET /api/tasks` — List (with optional filter)

| Request | Behaviour |
|---|---|
| `GET /api/tasks` | Returns all tasks regardless of priority |
| `GET /api/tasks?priority=HIGH` | Returns only tasks with priority = HIGH |
| `GET /api/tasks?priority=URGENT` | `400 Bad Request` (invalid enum value) |

Spring's `MethodArgumentTypeMismatchException` handler produces the 400 automatically — no custom error handling needed.

### `GET /api/tasks/{id}` and `DELETE /api/tasks/{id}` — Unchanged

### `TaskResponse` — Updated

Gains a `priority: TaskPriority?` field in all responses.

---

## Architecture & Layer Changes

| Layer | Change |
|---|---|
| `domain/TaskPriority.kt` | **New file** — enum `LOW, MEDIUM, HIGH` |
| `domain/Task.kt` | Add nullable `priority: TaskPriority?` field |
| `repository/TaskRepository.kt` | Add `findByPriority(priority: TaskPriority): List<Task>` |
| `service/TaskService.kt` | `findAll(priority: TaskPriority?)` — delegates to `findByPriority` or `findAll` |
| `controller/dto/CreateTaskRequest.kt` | Add `val priority: TaskPriority? = null` |
| `controller/dto/UpdateTaskRequest.kt` | Add `val priority: TaskPriority? = null` |
| `controller/dto/TaskResponse.kt` | Add `val priority: TaskPriority?`; update `from()` |
| `controller/TaskController.kt` | `getAllTasks(@RequestParam(required = false) priority: TaskPriority?)` |

No new layers. No new files beyond `TaskPriority.kt`.

---

## Testing Plan

### `TaskServiceTest` (unit, MockK)

- `findAll(null)` → calls `taskRepository.findAll()`
- `findAll(HIGH)` → calls `taskRepository.findByPriority(HIGH)`
- `create` with priority set → saved with correct priority
- `create` with priority null → saved with null priority
- `update` sets priority correctly

### `TaskControllerTest` (`@WebMvcTest`)

- `GET /api/tasks` → 200, all tasks returned
- `GET /api/tasks?priority=HIGH` → 200, filtered results
- `GET /api/tasks?priority=INVALID` → 400

### `TaskRepositoryTest` (`@DataJpaTest` + Testcontainers)

- Seed tasks with mixed priorities (LOW, HIGH, null)
- `findByPriority(HIGH)` returns only HIGH tasks
- `findAll()` returns all tasks

All existing tests remain passing — `priority` nullable default is backwards compatible.

---

## Out of Scope

- Multi-value priority filter (`?priority=LOW,HIGH`)
- Priority-based sorting
- Default priority value (null is the default)
- Flyway/Liquibase migration (project uses `ddl-auto: update`)
