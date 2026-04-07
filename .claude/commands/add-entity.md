Add a new domain entity named `$ARGUMENTS` with full CRUD support. Follow these steps:

## 1. Entity & Enum (if needed)

- Create `src/main/kotlin/com/example/taskmanager/domain/$ARGUMENTS.kt` as a regular class (not data class)
- Add JPA annotations: `@Entity`, `@Table`, `@EntityListeners(AuditingEntityListener::class)`
- Include `id` (UUID, `@GeneratedValue(strategy = GenerationType.UUID)`), audit fields (`@CreatedDate`, `@LastModifiedDate`)
- Add domain-specific fields based on requirements

## 2. Repository

- Create `src/main/kotlin/com/example/taskmanager/repository/${ARGUMENTS}Repository.kt`
- Extend `JpaRepository<$ARGUMENTS, UUID>`

## 3. Exception

- Create `${ARGUMENTS}NotFoundException` in the `exception` package
- Add handler method in `GlobalExceptionHandler` if not already covered by a generic handler

## 4. DTOs

- Create `Create${ARGUMENTS}Request`, `Update${ARGUMENTS}Request`, `${ARGUMENTS}Response` in `controller/dto/`
- Use data classes with `@field:` validation annotations
- Add `companion object` with `from()` factory method in the response DTO

## 5. Service

- Create `${ARGUMENTS}Service` in the `service` package
- Add `@Service` and `@Transactional` annotations
- Implement `findAll`, `findById`, `create`, `update`, `delete`
- Use constructor injection for the repository

## 6. Controller

- Create `${ARGUMENTS}Controller` in the `controller` package
- `@RestController` with `@RequestMapping("/api/{lowercase plural of $ARGUMENTS}")`
- Endpoints: GET all, GET by id, POST (201 with Location header), PUT, DELETE (204)

## 7. Tests

- **Repository test:** `@DataJpaTest` + Testcontainers + `@ServiceConnection`
- **Service test:** MockK (no Spring context), test happy path + error cases
- **Controller test:** `@WebMvcTest` + `@MockkBean` + MockMvc, test all endpoints + validation

## 8. Update CLAUDE.md

- Add the new entity to the project structure section
- Add new API endpoints to the endpoints table
