package com.example.taskmanager.controller

import com.example.taskmanager.controller.dto.CreateTaskRequest
import com.example.taskmanager.controller.dto.UpdateTaskRequest
import com.example.taskmanager.domain.Task
import com.example.taskmanager.domain.TaskPriority
import com.example.taskmanager.domain.TaskStatus
import com.example.taskmanager.exception.GlobalExceptionHandler
import com.example.taskmanager.exception.TaskNotFoundException
import com.example.taskmanager.service.TaskService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(TaskController::class)
@Import(GlobalExceptionHandler::class)
class TaskControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var taskService: TaskService

    private val taskId = UUID.randomUUID()
    private val now = LocalDateTime.now()

    private fun createTask(
        id: UUID = taskId,
        title: String = "Test Task",
        description: String? = "Description",
        status: TaskStatus = TaskStatus.TODO,
        priority: TaskPriority? = null,
    ): Task =
        Task(
            id = id,
            title = title,
            description = description,
            status = status,
            priority = priority,
            createdAt = now,
            updatedAt = now,
        )

    @Test
    fun `should return all tasks`() {
        val tasks = listOf(createTask(), createTask(id = UUID.randomUUID(), title = "Task 2"))
        every { taskService.findAll(null) } returns tasks

        mockMvc
            .perform(get("/api/tasks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].title").value("Test Task"))

        verify { taskService.findAll(null) }
    }

    @Test
    fun `should return tasks filtered by priority`() {
        val highTask = createTask(priority = TaskPriority.HIGH)
        every { taskService.findAll(TaskPriority.HIGH) } returns listOf(highTask)

        mockMvc
            .perform(get("/api/tasks").param("priority", "HIGH"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].priority").value("HIGH"))

        verify { taskService.findAll(TaskPriority.HIGH) }
    }

    @Test
    fun `should return 400 for invalid priority value`() {
        mockMvc
            .perform(get("/api/tasks").param("priority", "URGENT"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `should return task by id`() {
        every { taskService.findById(taskId) } returns createTask()

        mockMvc
            .perform(get("/api/tasks/$taskId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(taskId.toString()))
            .andExpect(jsonPath("$.title").value("Test Task"))
            .andExpect(jsonPath("$.status").value("TODO"))
    }

    @Test
    fun `should return 404 when task not found`() {
        every { taskService.findById(taskId) } throws TaskNotFoundException(taskId)

        mockMvc
            .perform(get("/api/tasks/$taskId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
    }

    @Test
    fun `should create task and return 201`() {
        val request = CreateTaskRequest(title = "New Task", description = "Desc", status = TaskStatus.TODO)
        every { taskService.create(any()) } returns createTask(title = "New Task", description = "Desc")

        mockMvc
            .perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("New Task"))
    }

    @Test
    fun `should create task with priority and return 201`() {
        val request = CreateTaskRequest(title = "High Priority Task", priority = TaskPriority.HIGH)
        every { taskService.create(any()) } returns
            createTask(title = "High Priority Task", priority = TaskPriority.HIGH)

        mockMvc
            .perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("High Priority Task"))
            .andExpect(jsonPath("$.priority").value("HIGH"))

        verify { taskService.create(any()) }
    }

    @Test
    fun `should update task`() {
        val request = UpdateTaskRequest(title = "Updated", description = "Updated Desc", status = TaskStatus.DONE)
        every { taskService.update(taskId, any()) } returns
            createTask(
                title = "Updated",
                description = "Updated Desc",
                status = TaskStatus.DONE,
            )

        mockMvc
            .perform(
                put("/api/tasks/$taskId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Updated"))
            .andExpect(jsonPath("$.status").value("DONE"))
    }

    @Test
    fun `should delete task and return 204`() {
        every { taskService.delete(taskId) } returns Unit

        mockMvc
            .perform(delete("/api/tasks/$taskId"))
            .andExpect(status().isNoContent)

        verify { taskService.delete(taskId) }
    }

    @Test
    fun `should return 400 for invalid create request`() {
        val invalidRequest = mapOf("title" to "", "status" to "TODO")

        mockMvc
            .perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Failed"))
    }

    @Test
    fun `should return 400 for invalid update request`() {
        val invalidRequest = mapOf("title" to "", "status" to "TODO")

        mockMvc
            .perform(
                put("/api/tasks/$taskId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Failed"))
    }
}
