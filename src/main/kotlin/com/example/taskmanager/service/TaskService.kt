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
        if (priority != null) {
            taskRepository.findByPriority(priority)
        } else {
            taskRepository.findAll()
        }

    @Transactional(readOnly = true)
    fun findById(id: UUID): Task =
        taskRepository
            .findById(id)
            .orElseThrow { TaskNotFoundException(id) }

    fun create(request: CreateTaskRequest): Task {
        val task =
            Task(
                title = request.title,
                description = request.description,
                status = request.status,
                priority = request.priority,
            )
        return taskRepository.save(task)
    }

    fun update(
        id: UUID,
        request: UpdateTaskRequest,
    ): Task {
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
