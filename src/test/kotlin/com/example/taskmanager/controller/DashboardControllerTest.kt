package com.example.taskmanager.controller

import com.example.taskmanager.domain.Task
import com.example.taskmanager.domain.TaskStatus
import com.example.taskmanager.service.TaskService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(DashboardController::class)
class DashboardControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var taskService: TaskService

    private val now = LocalDateTime.now()

    private fun createTask(
        title: String = "Test Task",
        description: String? = null,
        status: TaskStatus = TaskStatus.TODO
    ): Task = Task(
        id = UUID.randomUUID(),
        title = title,
        description = description,
        status = status,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `should render dashboard view with empty task list`() {
        every { taskService.findAll() } returns emptyList()

        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk)
            .andExpect(view().name("dashboard"))
            .andExpect(model().attributeExists("columns", "totalCount"))
            .andExpect(model().attribute("totalCount", 0))
    }

    @Test
    fun `should group tasks by status into three columns`() {
        val tasks = listOf(
            createTask(title = "Todo 1", status = TaskStatus.TODO),
            createTask(title = "Todo 2", status = TaskStatus.TODO),
            createTask(title = "In Progress 1", status = TaskStatus.IN_PROGRESS),
            createTask(title = "Done 1", status = TaskStatus.DONE)
        )
        every { taskService.findAll() } returns tasks

        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk)
            .andExpect(model().attribute("totalCount", 4))
            .andExpect(xpath("//div[@class='column']").nodeCount(3))
    }

    @Test
    fun `should display task titles and descriptions`() {
        val tasks = listOf(
            createTask(title = "My Task", description = "A description", status = TaskStatus.TODO)
        )
        every { taskService.findAll() } returns tasks

        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk)
            .andExpect(xpath("//div[@class='card-title' and text()='My Task']").exists())
            .andExpect(xpath("//div[@class='card-description' and text()='A description']").exists())
    }

    @Test
    fun `should show empty state when column has no tasks`() {
        every { taskService.findAll() } returns emptyList()

        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk)
            .andExpect(xpath("//div[@class='empty-state']").nodeCount(3))
    }

    @Test
    fun `should display correct task counts per column`() {
        val tasks = listOf(
            createTask(title = "T1", status = TaskStatus.TODO),
            createTask(title = "T2", status = TaskStatus.IN_PROGRESS),
            createTask(title = "T3", status = TaskStatus.IN_PROGRESS)
        )
        every { taskService.findAll() } returns tasks

        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk)
            .andExpect(xpath("//div[@data-status='TODO']//span[@class='column-count' and text()='1']").exists())
            .andExpect(xpath("//div[@data-status='IN_PROGRESS']//span[@class='column-count' and text()='2']").exists())
            .andExpect(xpath("//div[@data-status='DONE']//span[@class='column-count' and text()='0']").exists())
    }
}
