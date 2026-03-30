package com.example.taskmanager.exception

import java.util.UUID

class TaskNotFoundException(id: UUID) : RuntimeException("Task not found with id: $id")
