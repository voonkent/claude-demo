---
description: Testing conventions and patterns for the task-manager project
globs: "**/*Test.kt"
---

# Testing Conventions

## General Rules

- Every new class must have corresponding unit tests
- No test should depend on another test's state
- Use `@BeforeEach` for setup, not shared mutable state
- Test both happy path and error/edge cases

## Test Naming

Use backtick style with descriptive names:

```kotlin
@Test
fun `should return task when id exists`() { }

@Test
fun `should throw TaskNotFoundException when id does not exist`() { }
```

## Layer-Specific Patterns

### Service Tests (unit tests with MockK)

- No Spring context — pure unit tests
- Use `@MockK` + `MockKAnnotations.init(this)` in `@BeforeEach`
- Instantiate service manually with mocked dependencies
- Verify interactions with `verify { }`

### Controller Tests (`@WebMvcTest`)

- Use `@WebMvcTest(XxxController::class)` — loads only web layer
- Use `@MockkBean` (from springmockk) to mock service dependencies
- Test HTTP status codes, response bodies, and validation errors
- Use `ObjectMapper` to serialize request bodies

### Repository Tests (`@DataJpaTest` + Testcontainers)

- Use `@DataJpaTest` with `@AutoConfigureTestDatabase(replace = NONE)`
- Use Testcontainers with `@ServiceConnection` on `PostgreSQLContainer`
- Use `@ActiveProfiles("test")` to activate test config
- Container declared in `companion object` with `@JvmStatic`

## Coverage

- JaCoCo minimum: **80% line coverage** and **80% branch coverage**
- Build fails if thresholds are not met (`./gradlew build`)
- View HTML report: `build/reports/jacoco/test/html/index.html`
