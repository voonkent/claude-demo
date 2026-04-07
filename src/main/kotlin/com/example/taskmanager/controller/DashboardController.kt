package com.example.taskmanager.controller

import com.example.taskmanager.domain.TaskStatus
import com.example.taskmanager.service.TaskService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class DashboardController(private val taskService: TaskService) {

    @GetMapping("/dashboard")
    fun dashboard(model: Model): String {
        val tasks = taskService.findAll()
        val grouped = tasks.groupBy { it.status }
        model.addAttribute("columns", TaskStatus.entries.map { status ->
            val columnTasks = grouped[status].orEmpty()
            mapOf("status" to status, "tasks" to columnTasks, "count" to columnTasks.size)
        })
        model.addAttribute("totalCount", tasks.size)
        return "dashboard"
    }
}
