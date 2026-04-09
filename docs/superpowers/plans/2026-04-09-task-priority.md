# Task Priority with Filtering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a nullable `TaskPriority` (LOW/MEDIUM/HIGH) field to tasks and support single-value priority filtering on `GET /api/tasks`.

**Architecture:** A new `TaskPriority` enum is added to the domain. The `Task` entity, all three DTOs, the repository, service, and controller each receive a minimal, focused change. The filter uses Spring Data's derived query method (`findByPriority`) with a conditional branch in the service — if `priority` is null, `findAll()` is called; if non-null, `findByPriority()` is called.

**Tech Stack:** Kotlin 1.9.25, Spring Boot 3.4.4, Spring Data JPA, MockK, SpringMockK (`@MockkBean`), JUnit 5, Testcontainers (PostgreSQL 16), Gradle.

---

## File Map

| Action | File |
|--------|------|
| Create | `src/main/kotlin/com/example/taskmanager/domain/TaskPriority.kt` |
| Modify | `src/main/kotlin/com/example/taskmanager/domain/Task.kt` |
| Modify | `src/main/kotlin/com/example/taskmanager/controller/dto/CreateTaskRequest.kt` |
| Modify | `src/main/kotlin/com/example/taskmanager/controller/dto/UpdateTaskRequest.kt` |
| Modify | `src/main/kotlin/com/example/taskmanager/controller/dto/TaskResponse.kt` |
| Modify | `src/main/kotlin/com/example/taskmanager/repository/TaskRepository.kt` |
| Modify | `src/main/kotlin/com/example/taskmanager/service/TaskService.kt` |
| Modify | `src/main/kotlin/com/example/taskmanager/controller/TaskController.kt` |
| Modify | `src/test/kotlin/com/example/taskmanager/repository/TaskRepositoryTest.kt` |
| Modify | `src/test/kotlin/com/example/taskmanager/service/TaskServiceTest.kt` |
| Modify | `src/test/kotlin/com/example/taskmanager/controller/TaskControllerTest.kt` |

---

## Task 1: Add `TaskPriority` enum and update `Task` entity

These are the foundation types required for all subsequent tests to compile.

**Files:**
- Create: `src/main/kotlin/com/example/taskmanager/domain/TaskPriority.kt`
- Modify: `src/main/kotlin/com/example/taskmanager/domain/Task.kt`

- [ ] **Step 1: Create `TaskPriority.kt`**

```kotlin
package com.example.taskmanager.domain

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}
```

- [ ] **Step 2: Add `priority` field to `Task.kt`**

Add the import and field. The full file after changes:

```kotlin
package com.example.taskmanager.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "tasks")
@EntityListeners(AuditingEntityListener::class)
class Task(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    var title: String,

    @Column
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TaskStatus = TaskStatus.TODO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var priority: TaskPriority? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime? = null
)
```

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/example/taskmanager/domain/TaskPriority.kt \
        src/main/kotlin/com/example/taskmanager/domain/Task.kt
git commit -m "feat: add TaskPriority enum and priority field to Task entity"
```

---

## Task 2: Update DTOs to include priority

**Files:**
- Modify: `src/main/kotlin/com/example/taskmanager/controller/dto/CreateTaskRequest.kt`
- Modify: `src/main/kotlin/com/example/taskmanager/controller/dto/UpdateTaskRequest.kt`
- Modify: `src/main/kotlin/com/example/taskmanager/controller/dto/TaskResponse.kt`

- [ ] **Step 1: Update `CreateTaskRequest.kt`**

```kotlin
package com.example.taskmanager.controller.dto

import com.example.taskmanager.domain.TaskPriority
import com.example.taskmanager.domain.TaskStatus
import jakarta.validation.constraints.NotBlank

data class CreateTaskRequest(
    @field:NotBlank(message = "Title must not be blank")
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority? = null
)
```

- [ ] **Step 2: Update `UpdateTaskRequest.kt`**

```kotlin
package com.example.taskmanager.controller.dto

import com.example.taskmanager.domain.TaskPriority
import com.example.taskmanager.domain.TaskStatus
import jakarta.validation.constraints.NotBlank

data class UpdateTaskRequest(
    @field:NotBlank(message = "Title must not be blank")
    val title: String,
    val description: String? = null,
    val status: TaskStatus,
    val priority: TaskPriority? = null
)
```

- [ ] **Step 3: Update `TaskResponse.kt`**

```kotlin
package com.example.taskmanager.controller.dto

import com.example.taskmanager.domain.Task
import com.example.taskmanager.domain.TaskPriority
import com.example.taskmanager.domain.TaskStatus
import java.time.LocalDateTime
import java.util.UUID

data class TaskResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: TaskPriority?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(task: Task): TaskResponse = TaskResponse(
            id = task.id!!,
            title = task.title,
            description = task.description,
            status = task.status,
            priority = task.priority,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt
        )
    }
}
```

- [ ] **Step 4: Verify existing tests still compile and pass**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL (all existing tests pass — `priority` defaults to `null` everywhere).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/example/taskmanager/controller/dto/CreateTaskRequest.kt \
        src/main/kotlin/com/example/taskmanager/controller/dto/UpdateTaskRequest.kt \
        src/main/kotlin/com/example/taskmanager/controller/dto/TaskResponse.kt
git commit -m "feat: add priority field to DTOs and TaskResponse"
```

---

## Task 3: TDD — `TaskRepository.findByPriority`

**Files:**
- Modify: `src/test/kotlin/com/example/taskmanager/repository/TaskRepositoryTest.kt`
- Modify: `src/main/kotlin/com/example/taskmanager/repository/TaskRepository.kt`

- [ ] **Step 1: Write the failing test**

Add this test to `TaskRepositoryTest`. Also update the `createTask` helper used elsewhere in the class to support `priority`. Add these additions to the existing file:

At the top of the file, add the import:
```kotlin
import com.example.taskmanager.domain.TaskPriority
```

Add this test method to the class:
```kotlin
@Test
fun `should find tasks by priority`() {
    taskRepository.save(Task(title = "Low Task", priority = TaskPriority.LOW))
    taskRepository.save(Task(title = "High Task 1", priority = TaskPriority.HIGH))
    taskRepository.save(Task(title = "High Task 2", priority = TaskPriority.HIGH))
    taskRepository.save(Task(title = "No Priority Task", priority = null))

    val result = taskRepository.findByPriority(TaskPriority.HIGH)

    assertThat(result).hasSize(2)
    assertThat(result.map { it.title }).containsExactlyInAnyOrder("High Task 1", "High Task 2")
}
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew test --tests "com.example.taskmanager.repository.TaskRepositoryTest.should find tasks by priority"
```

Expected: FAIL — compilation error: `findByPriority` does not exist on `TaskRepository`.

- [ ] **Step 3: Add `findByPriority` to `TaskRepository.kt`**

```kotlin
package com.example.taskmanager.repository

import com.example.taskmanager.domain.Task
import com.example.taskmanager.domain.TaskPriority
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TaskRepository : JpaRepository<Task, UUID> {
    fun findByPriority(priority: TaskPriority): List<Task>
}
```

- [ ] **Step 4: Run to verify it passes**

```bash
./gradlew test --tests "com.example.taskmanager.repository.TaskRepositoryTest"
```

Expected: BUILD SUCCESSFUL, all repository tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/example/taskmanager/repository/TaskRepository.kt \
        src/test/kotlin/com/example/taskmanager/repository/TaskRepositoryTest.kt
git commit -m "feat: add findByPriority to TaskRepository with tests"
```

---

## Task 4: TDD — `TaskService.findAll(priority?)`, `create`, and `update`

**Files:**
- Modify: `src/test/kotlin/com/example/taskmanager/service/TaskServiceTest.kt`
- Modify: `src/main/kotlin/com/example/taskmanager/service/TaskService.kt`

- [ ] **Step 1: Write the failing tests**

In `TaskServiceTest.kt`, make these changes:

**a) Add import at the top:**
```kotlin
import com.example.taskmanager.domain.TaskPriority
```

**b) Update the `createTask` helper to support `priority`:**
```kotlin
private fun createTask(
    id: UUID = taskId,
    title: String = "Test Task",
    description: String? = "Description",
    status: TaskStatus = TaskStatus.TODO,
    priority: TaskPriority? = null
): Task = Task(
    id = id,
    title = title,
    description = description,
    status = status,
    priority = priority,
    createdAt = now,
    updatedAt = now
)
```

**c) Update existing `should return all tasks` test** to call `findAll(null)` (the signature is changing):
```kotlin
@Test
fun `should return all tasks`() {
    val tasks = listOf(createTask(), createTask(id = UUID.randomUUID(), title = "Task 2"))
    every { taskRepository.findAll() } returns tasks

    val result = taskService.findAll(null)

    assertThat(result).hasSize(2)
    verify { taskRepository.findAll() }
}
```

**d) Add new tests** for priority filtering and priority in create/update:
```kotlin
@Test
fun `should return tasks filtered by priority`() {
    val highTask = createTask(priority = TaskPriority.HIGH)
    every { taskRepository.findByPriority(TaskPriority.HIGH) } returns listOf(highTask)

    val result = taskService.findAll(TaskPriority.HIGH)

    assertThat(result).hasSize(1)
    assertThat(result[0].priority).isEqualTo(TaskPriority.HIGH)
    verify { taskRepository.findByPriority(TaskPriority.HIGH) }
}

@Test
fun `should create task with priority`() {
    val request = CreateTaskRequest(title = "High Task", priority = TaskPriority.HIGH)
    val taskSlot = slot<Task>()
    every { taskRepository.save(capture(taskSlot)) } answers {
        createTask(title = "High Task", priority = TaskPriority.HIGH)
    }

    val result = taskService.create(request)

    assertThat(result.priority).isEqualTo(TaskPriority.HIGH)
    verify { taskRepository.save(any()) }
}

@Test
fun `should update task priority`() {
    val existing = createTask()
    val request = UpdateTaskRequest(title = "Updated", status = TaskStatus.DONE, priority = TaskPriority.LOW)
    every { taskRepository.findById(taskId) } returns Optional.of(existing)
    every { taskRepository.save(any()) } answers {
        createTask(title = "Updated", status = TaskStatus.DONE, priority = TaskPriority.LOW)
    }

    val result = taskService.update(taskId, request)

    assertThat(result.priority).isEqualTo(TaskPriority.LOW)
    verify { taskRepository.save(any()) }
}
```

- [ ] **Step 2: Run to verify new tests fail**

```bash
./gradlew test --tests "com.example.taskmanager.service.TaskServiceTest"
```

Expected: FAIL — `findAll(null)` does not compile (signature mismatch); new tests reference `findByPriority` on mock which has no stub.

- [ ] **Step 3: Update `TaskService.kt`**

```kotlin
package com.example.taskmanager.service

import com.example.taskmanager.controller.dto.CreateTaskRequest
import com.example.taskmanager.controller.dto.UpdateTaskRequest
import com.example.taskmanager.domain.Task
import com.example.taskmanager.domain.TaskPriority
import com.example.taskmanager.exception.TaskNotFoundException
import com.example.taskmanager.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class TaskService(
    private val taskRepository: TaskRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(priority: TaskPriority? = null): List<Task> =
        if (priority != null) taskRepository.findByPriority(priority)
        else taskRepository.findAll()

    @Transactional(readOnly = true)
    fun findById(id: UUID): Task =
        taskRepository
            .findById(id)
            .orElseThrow { TaskNotFoundException(id) }

    fun create(request: CreateTaskRequest): Task {
        val task = Task(
            title = request.title,
            description = request.description,
            status = request.status,
            priority = request.priority,
        )
        return taskRepository.save(task)
    }

    fun update(id: UUID, request: UpdateTaskRequest): Task {
        val task = findById(id)
        task.title = request.title
        task.description = request.description
        task.status = request.status
        task.priority = request.priority
        return taskRepository.save(task)
    }

    fun delete(id: UUID) {
        val task = findById(id)
        taskRepository.delete(task)
    }
}
```

- [ ] **Step 4: Run to verify all service tests pass**

```bash
./gradlew test --tests "com.example.taskmanager.service.TaskServiceTest"
```

Expected: BUILD SUCCESSFUL, all service tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/example/taskmanager/service/TaskService.kt \
        src/test/kotlin/com/example/taskmanager/service/TaskServiceTest.kt
git commit -m "feat: update TaskService to support priority filtering, create, and update"
```

---

## Task 5: TDD — `TaskController` priority query parameter

**Files:**
- Modify: `src/test/kotlin/com/example/taskmanager/controller/TaskControllerTest.kt`
- Modify: `src/main/kotlin/com/example/taskmanager/controller/TaskController.kt`

- [ ] **Step 1: Write the failing tests**

In `TaskControllerTest.kt`, make these changes:

**a) Add imports at the top:**
```kotlin
import com.example.taskmanager.domain.TaskPriority
```

**b) Update the `createTask` helper to support `priority`:**
```kotlin
private fun createTask(
    id: UUID = taskId,
    title: String = "Test Task",
    description: String? = "Description",
    status: TaskStatus = TaskStatus.TODO,
    priority: TaskPriority? = null
): Task = Task(
    id = id,
    title = title,
    description = description,
    status = status,
    priority = priority,
    createdAt = now,
    updatedAt = now
)
```

**c) Update the existing `should return all tasks` test** — the controller now calls `taskService.findAll(null)`, so the mock must match:
```kotlin
@Test
fun `should return all tasks`() {
    val tasks = listOf(createTask(), createTask(id = UUID.randomUUID(), title = "Task 2"))
    every { taskService.findAll(null) } returns tasks

    mockMvc.perform(get("/api/tasks"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].title").value("Test Task"))
}
```

**d) Add new tests:**
```kotlin
@Test
fun `should return tasks filtered by priority`() {
    val highTask = createTask(priority = TaskPriority.HIGH)
    every { taskService.findAll(TaskPriority.HIGH) } returns listOf(highTask)

    mockMvc.perform(get("/api/tasks").param("priority", "HIGH"))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].priority").value("HIGH"))
}

@Test
fun `should return 400 for invalid priority value`() {
    mockMvc.perform(get("/api/tasks").param("priority", "URGENT"))
        .andExpect(status().isBadRequest)
}
```

- [ ] **Step 2: Run to verify new tests fail**

```bash
./gradlew test --tests "com.example.taskmanager.controller.TaskControllerTest"
```

Expected: FAIL — `should return all tasks` fails (mock expects `findAll(null)` but controller still calls `findAll()`); priority filter tests fail (no `?priority` param handling).

- [ ] **Step 3: Update `TaskController.kt`**

```kotlin
package com.example.taskmanager.controller

import com.example.taskmanager.controller.dto.CreateTaskRequest
import com.example.taskmanager.controller.dto.TaskResponse
import com.example.taskmanager.controller.dto.UpdateTaskRequest
import com.example.taskmanager.domain.TaskPriority
import com.example.taskmanager.service.TaskService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/tasks")
class TaskController(private val taskService: TaskService) {

    @GetMapping
    fun getAllTasks(
        @RequestParam(required = false) priority: TaskPriority?
    ): ResponseEntity<List<TaskResponse>> {
        val tasks = taskService.findAll(priority).map { TaskResponse.from(it) }
        return ResponseEntity.ok(tasks)
    }

    @GetMapping("/{id}")
    fun getTaskById(@PathVariable id: UUID): ResponseEntity<TaskResponse> {
        val task = taskService.findById(id)
        return ResponseEntity.ok(TaskResponse.from(task))
    }

    @PostMapping
    fun createTask(@Valid @RequestBody request: CreateTaskRequest): ResponseEntity<TaskResponse> {
        val task = taskService.create(request)
        val response = TaskResponse.from(task)
        return ResponseEntity.created(URI.create("/api/tasks/${response.id}")).body(response)
    }

    @PutMapping("/{id}")
    fun updateTask(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTaskRequest
    ): ResponseEntity<TaskResponse> {
        val task = taskService.update(id, request)
        return ResponseEntity.ok(TaskResponse.from(task))
    }

    @DeleteMapping("/{id}")
    fun deleteTask(@PathVariable id: UUID): ResponseEntity<Void> {
        taskService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
```

- [ ] **Step 4: Run all controller tests**

```bash
./gradlew test --tests "com.example.taskmanager.controller.TaskControllerTest"
```

Expected: BUILD SUCCESSFUL, all controller tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/example/taskmanager/controller/TaskController.kt \
        src/test/kotlin/com/example/taskmanager/controller/TaskControllerTest.kt
git commit -m "feat: add priority query param to GET /api/tasks with tests"
```

---

## Task 6: Full build verification

- [ ] **Step 1: Run the full build**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL — all tests pass, JaCoCo 80% line and branch coverage thresholds met.

- [ ] **Step 2: If JaCoCo fails**, check the report and add any missing test cases

```bash
# View coverage report
open build/reports/jacoco/test/html/index.html
```

Coverage gaps are most likely in `TaskService.findAll` (both branches) or `TaskController`. Ensure `findAll(null)` and `findAll(HIGH)` tests exist in both the service and controller test suites — they were added in Tasks 4 and 5.

- [ ] **Step 3: Final commit (if any coverage fixes were needed)**

```bash
git add -p
git commit -m "test: improve coverage for priority filtering"
```
