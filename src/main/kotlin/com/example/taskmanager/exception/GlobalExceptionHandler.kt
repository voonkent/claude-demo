package com.example.taskmanager.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException::class)
    fun handleTaskNotFound(ex: TaskNotFoundException): ResponseEntity<Map<String, String>> {
        val body = mapOf(
            "error" to "Not Found",
            "message" to "${ex.message}"
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val details = ex.bindingResult.fieldErrors.associate { it.field to "${it.defaultMessage}" }
        val body = mapOf<String, Any>(
            "error" to "Validation Failed",
            "details" to details
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }
}
