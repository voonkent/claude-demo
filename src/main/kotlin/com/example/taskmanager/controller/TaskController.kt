package com.example.taskmanager.controller

import com.example.taskmanager.controller.dto.CreateTaskRequest
import com.example.taskmanager.controller.dto.TaskResponse
import com.example.taskmanager.controller.dto.UpdateTaskRequest
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
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/tasks")
class TaskController(private val taskService: TaskService) {

    @GetMapping
    fun getAllTasks(): ResponseEntity<List<TaskResponse>> {
        val tasks = taskService.findAll().map { TaskResponse.from(it) }
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
