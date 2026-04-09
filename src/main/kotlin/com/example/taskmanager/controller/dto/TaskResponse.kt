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
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(task: Task): TaskResponse =
            TaskResponse(
                id = task.id!!,
                title = task.title,
                description = task.description,
                status = task.status,
                priority = task.priority,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
            )
    }
}
