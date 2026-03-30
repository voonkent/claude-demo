---
description: Kotlin coding conventions for the task-manager project
globs: "**/*.kt"
---

# Kotlin Conventions

## Package & Structure

- Base package: `com.example.taskmanager`
- Layer packages: `controller/`, `service/`, `repository/`, `domain/`, `exception/`, `config/`
- DTOs live under `controller/dto/`

## Kotlin Style

- Prefer `val` over `var` — use `var` only for mutable JPA entity fields
- Use Kotlin null safety: `String?` for nullable fields, avoid `!!` except in `TaskResponse.from()` where id is guaranteed
- Use `when` expressions instead of if-else chains
- Use expression body for single-expression functions
- Explicit return types on all public functions

## JPA Entities

- Use **regular classes** (not data classes) for JPA entities
- Apply `@EntityListeners(AuditingEntityListener::class)` for audit fields
- Use `@GeneratedValue(strategy = GenerationType.UUID)` for IDs
- Use `@Enumerated(EnumType.STRING)` for enum fields

## DTOs

- Use `data class` for request/response DTOs
- Use `@field:` prefix for Bean Validation annotations (e.g., `@field:NotBlank`)
- Provide `companion object` factory methods for entity-to-response mapping

## Dependency Injection

- Use constructor injection via primary constructor
- Never use `@Autowired` annotation
- Mark injected dependencies as `private val`

## Error Handling

- Use `@RestControllerAdvice` for centralized exception handling
- Throw domain-specific exceptions (e.g., `TaskNotFoundException`)
- Return structured error responses with `error` and `message`/`details` fields
