package com.example.taskmanager.service

import com.example.taskmanager.controller.dto.CreateTaskRequest
import com.example.taskmanager.controller.dto.UpdateTaskRequest
import com.example.taskmanager.domain.Task
import com.example.taskmanager.domain.TaskStatus
import com.example.taskmanager.exception.TaskNotFoundException
import com.example.taskmanager.repository.TaskRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class TaskServiceTest {

    @MockK
    lateinit var taskRepository: TaskRepository

    private lateinit var taskService: TaskService

    private val taskId = UUID.randomUUID()
    private val now = LocalDateTime.now()

    private fun createTask(
        id: UUID = taskId,
        title: String = "Test Task",
        description: String? = "Description",
        status: TaskStatus = TaskStatus.TODO
    ): Task = Task(
        id = id,
        title = title,
        description = description,
        status = status,
        createdAt = now,
        updatedAt = now
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        taskService = TaskService(taskRepository)
    }

    @Test
    fun `should return all tasks`() {
        val tasks = listOf(createTask(), createTask(id = UUID.randomUUID(), title = "Task 2"))
        every { taskRepository.findAll() } returns tasks

        val result = taskService.findAll()

        assertThat(result).hasSize(2)
        verify { taskRepository.findAll() }
    }

    @Test
    fun `should return task when id exists`() {
        every { taskRepository.findById(taskId) } returns Optional.of(createTask())

        val result = taskService.findById(taskId)

        assertThat(result.title).isEqualTo("Test Task")
        verify { taskRepository.findById(taskId) }
    }

    @Test
    fun `should throw TaskNotFoundException when id does not exist`() {
        every { taskRepository.findById(taskId) } returns Optional.empty()

        assertThatThrownBy { taskService.findById(taskId) }
            .isInstanceOf(TaskNotFoundException::class.java)
            .hasMessageContaining(taskId.toString())
    }

    @Test
    fun `should create task`() {
        val request = CreateTaskRequest(title = "New Task", description = "New Description", status = TaskStatus.TODO)
        val taskSlot = slot<Task>()
        every { taskRepository.save(capture(taskSlot)) } answers {
            taskSlot.captured.apply {
                // Simulate JPA setting the id
            }
            createTask(title = "New Task", description = "New Description")
        }

        val result = taskService.create(request)

        assertThat(result.title).isEqualTo("New Task")
        assertThat(result.description).isEqualTo("New Description")
        verify { taskRepository.save(any()) }
    }

    @Test
    fun `should update existing task`() {
        val existing = createTask()
        val request = UpdateTaskRequest(title = "Updated", description = "Updated Desc", status = TaskStatus.DONE)
        every { taskRepository.findById(taskId) } returns Optional.of(existing)
        every { taskRepository.save(any()) } answers {
            createTask(title = "Updated", description = "Updated Desc", status = TaskStatus.DONE)
        }

        val result = taskService.update(taskId, request)

        assertThat(result.title).isEqualTo("Updated")
        assertThat(result.status).isEqualTo(TaskStatus.DONE)
        verify { taskRepository.findById(taskId) }
        verify { taskRepository.save(any()) }
    }

    @Test
    fun `should throw TaskNotFoundException when updating non-existent task`() {
        val request = UpdateTaskRequest(title = "Updated", description = null, status = TaskStatus.DONE)
        every { taskRepository.findById(taskId) } returns Optional.empty()

        assertThatThrownBy { taskService.update(taskId, request) }
            .isInstanceOf(TaskNotFoundException::class.java)
    }

    @Test
    fun `should delete task`() {
        val existing = createTask()
        every { taskRepository.findById(taskId) } returns Optional.of(existing)
        every { taskRepository.delete(existing) } returns Unit

        taskService.delete(taskId)

        verify { taskRepository.findById(taskId) }
        verify { taskRepository.delete(existing) }
    }

    @Test
    fun `should throw TaskNotFoundException when deleting non-existent task`() {
        every { taskRepository.findById(taskId) } returns Optional.empty()

        assertThatThrownBy { taskService.delete(taskId) }
            .isInstanceOf(TaskNotFoundException::class.java)
    }
}
