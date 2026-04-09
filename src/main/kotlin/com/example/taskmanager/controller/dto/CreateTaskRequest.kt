package com.example.taskmanager.controller.dto

import com.example.taskmanager.domain.TaskPriority
import com.example.taskmanager.domain.TaskStatus
import jakarta.validation.constraints.NotBlank

data class CreateTaskRequest(
    @field:NotBlank(message = "Title must not be blank")
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority? = null,
)
