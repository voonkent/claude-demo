package com.example.taskmanager.repository

import com.example.taskmanager.domain.Task
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TaskRepository : JpaRepository<Task, UUID>
