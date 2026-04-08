---
name: flyway-create-migration
description: Scan JPA entities, diff against existing Flyway migrations, and generate the next versioned SQL migration file. Must only be invoked by the flyway-agent subagent -- never call this skill directly from the main assistant thread.
---

> **Invocation policy:** This skill is owned by `flyway-agent`. The main assistant must delegate Flyway work to that agent, which then invokes this skill. If you reach this skill without going through `flyway-agent`, stop and ask the user to rerun via the agent.

Scan JPA entities, compare against existing Flyway migrations, and write the next versioned SQL migration file. The caller (`flyway-agent`) will run `flyway-migration-check` once after generation to verify the result.

---

## Step 1 -- Scan JPA Entities

Search for entity classes:
- Kotlin: `src/main/kotlin/**/*.kt` containing `@Entity`
- Java: `src/main/java/**/*.java` containing `@Entity`

Parse these annotations to build the desired schema:

| JPA Annotation | Schema Meaning |
|---------------|----------------|
| `@Entity` | Table definition |
| `@Table(name="...")` | Custom table name (default: class name in snake_case) |
| `@Id` | Primary key column |
| `@GeneratedValue(strategy=GenerationType.UUID)` | UUID primary key |
| `@GeneratedValue(strategy=GenerationType.IDENTITY)` | Auto-increment primary key |
| `@Column(name, nullable, length, unique, columnDefinition)` | Column definition |
| `@ManyToOne` / `@JoinColumn` | Foreign key column |
| `@OneToMany(mappedBy)` | Inverse -- no DDL |
| `@ManyToMany` / `@JoinTable` | Join table |
| `@Enumerated(EnumType.STRING)` | VARCHAR for enum |
| `@Lob` | TEXT |
| `@CreationTimestamp` / `@CreatedDate` | TIMESTAMPTZ default `now()` |
| `@UpdateTimestamp` / `@LastModifiedDate` | TIMESTAMPTZ updated on write |
| `@Version` | BIGINT optimistic lock column |
| `@Index` (inside `@Table`) | CREATE INDEX |
| `@UniqueConstraint` | UNIQUE constraint |

## Step 2 -- JPA Type to PostgreSQL Type Mapping

| JPA/Kotlin/Java Type | PostgreSQL Type |
|---------------------|-----------------|
| `String` | `VARCHAR(255)` (or length from `@Column`) |
| `Long` / `long` | `BIGINT` |
| `Int` / `int` / `Integer` | `INTEGER` |
| `Short` / `short` | `SMALLINT` |
| `Boolean` / `boolean` | `BOOLEAN` |
| `Double` / `double` | `DOUBLE PRECISION` |
| `Float` / `float` | `REAL` |
| `BigDecimal` | `NUMERIC(19,2)` (or precision/scale from `@Column`) |
| `LocalDateTime` | `TIMESTAMP` |
| `Instant` / `ZonedDateTime` / `OffsetDateTime` | `TIMESTAMPTZ` |
| `LocalDate` | `DATE` |
| `LocalTime` | `TIME` |
| `UUID` | `UUID` |
| `@Lob String` | `TEXT` |
| `@Enumerated(STRING)` enum | `VARCHAR(50)` |
| `ByteArray` / `byte[]` | `BYTEA` |

**Nullability rules:**
- Kotlin nullable type (`String?`, `Long?`) OR `@Column(nullable = true)` -> column is nullable
- Everything else -> `NOT NULL`
- `@Id` is always `NOT NULL PRIMARY KEY`

## Step 3 -- Analyze Existing Migrations

Read all files in `src/main/resources/db/migration/` in version order. For each:
- Parse `CREATE TABLE`, `ALTER TABLE`, `DROP` statements to build the current known schema
- Track the highest version number `N` (use `N=0` if no migrations exist)

## Step 4 -- Diff Desired vs Current

For each entity, determine required changes:

| Situation | SQL |
|-----------|-----|
| Entity has no table | `CREATE TABLE` |
| Entity has new field | `ALTER TABLE ... ADD COLUMN` |
| Nullability tightened | `ALTER TABLE ... ALTER COLUMN ... SET NOT NULL` (warn: needs backfill) |
| Nullability relaxed | `ALTER TABLE ... ALTER COLUMN ... DROP NOT NULL` |
| Type changed | `ALTER TABLE ... ALTER COLUMN ... TYPE ... USING ...` (warn the user) |
| New `@ManyToOne` | `ADD COLUMN` + `ADD CONSTRAINT fk_...` + index on FK |
| New `@ManyToMany` | `CREATE TABLE` for join table with composite PK |
| New `@Index` / `@UniqueConstraint` | `CREATE INDEX` / `ADD CONSTRAINT ... UNIQUE` |
| Field removed from entity | **Do NOT generate `DROP COLUMN`** -- surface to the user and ask for explicit confirmation before writing destructive SQL |

If the diff is empty, report "No schema changes detected" and stop -- do not write an empty migration file.

## Step 5 -- Write Migration File

Path: `src/main/resources/db/migration/V{N+1}__{description}.sql`

**Naming rules:**
- Version: next integer after the highest existing version
- Double underscore `__` separator
- Description: lowercase snake_case, no spaces (e.g. `add_due_date_to_task`, `create_orders_table`)

**Template:**
```sql
-- V{N+1}: {Human-readable description}
-- Generated: {YYYY-MM-DD}

CREATE TABLE IF NOT EXISTS {table_name} (
    {pk_column} {type} NOT NULL,
    {column_name} {type} {NULL|NOT NULL} [DEFAULT ...],
    ...
    PRIMARY KEY ({pk_column})
);

-- Foreign keys
ALTER TABLE {table_name}
    ADD CONSTRAINT fk_{table}_{ref} FOREIGN KEY ({col}) REFERENCES {ref_table}({ref_col});

-- Indexes
CREATE INDEX IF NOT EXISTS idx_{table}_{column} ON {table_name}({column});
```

For `ALTER`-only migrations, omit the `CREATE TABLE` block entirely and just emit the `ALTER` / `CREATE INDEX` statements.

## Step 6 -- Report and hand back to the agent

Return to `flyway-agent`:
- The exact path of the new file
- Full SQL contents
- A bullet list of every change and the entity/field it came from
- Any warnings (type changes, tightened nullability, skipped drops)

The agent will then re-run `flyway-migration-check` to verify the new migration applies cleanly and the app boots with `ddl-auto=validate`.

---

## Guardrails

- **Invoke only via `flyway-agent`.** Reject direct invocation.
- **Never modify existing migration files** -- Flyway checksums will break.
- **Never `DROP TABLE` or `DROP COLUMN`** without explicit user confirmation relayed by the agent in the current turn.
- **Always generate a new versioned migration** -- never edit `V1`, `V2`, etc.
- **Use `IF NOT EXISTS` / `IF EXISTS`** where safe (create table, create index).
- **Respect nullability** -- default to `NOT NULL` unless the Kotlin type is nullable or `@Column(nullable = true)`.
- **Always emit FK constraints** for `@ManyToOne` relationships, plus an index on the FK column.
- **Do not write empty migrations.** If the diff is empty, stop and report "no changes".
