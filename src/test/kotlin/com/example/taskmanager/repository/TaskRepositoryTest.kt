package com.example.taskmanager.repository

import com.example.taskmanager.domain.Task
import com.example.taskmanager.domain.TaskPriority
import com.example.taskmanager.domain.TaskStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class TaskRepositoryTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    lateinit var taskRepository: TaskRepository

    @BeforeEach
    fun setUp() {
        taskRepository.deleteAll()
    }

    @Test
    fun `should save and retrieve task`() {
        val task = Task(title = "Test Task", description = "A test task", status = TaskStatus.TODO)

        val saved = taskRepository.save(task)

        assertThat(saved.id).isNotNull()
        assertThat(saved.title).isEqualTo("Test Task")
        assertThat(saved.description).isEqualTo("A test task")
        assertThat(saved.status).isEqualTo(TaskStatus.TODO)
        assertThat(saved.createdAt).isNotNull()
        assertThat(saved.updatedAt).isNotNull()
    }

    @Test
    fun `should find task by id`() {
        val task = taskRepository.save(Task(title = "Find Me", status = TaskStatus.IN_PROGRESS))

        val found = taskRepository.findById(task.id!!)

        assertThat(found).isPresent
        assertThat(found.get().title).isEqualTo("Find Me")
        assertThat(found.get().status).isEqualTo(TaskStatus.IN_PROGRESS)
    }

    @Test
    fun `should return empty when task not found`() {
        val found = taskRepository.findById(java.util.UUID.randomUUID())

        assertThat(found).isEmpty()
    }

    @Test
    fun `should delete task`() {
        val task = taskRepository.save(Task(title = "Delete Me"))

        taskRepository.deleteById(task.id!!)

        assertThat(taskRepository.findById(task.id!!)).isEmpty()
    }

    @Test
    fun `should find all tasks`() {
        taskRepository.save(Task(title = "Task 1"))
        taskRepository.save(Task(title = "Task 2"))

        val tasks = taskRepository.findAll()

        assertThat(tasks).hasSize(2)
    }

    @Test
    fun `should update task`() {
        val task = taskRepository.save(Task(title = "Original", status = TaskStatus.TODO))

        task.title = "Updated"
        task.status = TaskStatus.DONE
        val updated = taskRepository.save(task)

        assertThat(updated.title).isEqualTo("Updated")
        assertThat(updated.status).isEqualTo(TaskStatus.DONE)
    }

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
}
