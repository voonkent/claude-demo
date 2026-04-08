---
name: flyway-migration-check
description: Verify existing Flyway migrations by running them against a temporary PostgreSQL container, then boot the Spring app with hibernate ddl-auto=validate against that same database to confirm the schema matches the JPA entities. Must only be invoked by the flyway-agent subagent -- never call this skill directly from the main assistant thread.
---

> **Invocation policy:** This skill is owned by `flyway-agent`. The main assistant must delegate Flyway work to that agent. If you reach this skill without going through `flyway-agent`, stop and ask the user to rerun via the agent.

Verify all existing Flyway SQL migrations apply cleanly against a fresh PostgreSQL 16 instance, then confirm the resulting schema matches the JPA entity model by booting the Spring Boot app with `hibernate.ddl-auto=validate`.

---

## Important: shell state does NOT persist between Bash tool calls

Each Bash tool invocation runs in a fresh shell. Environment variables like `$CONTAINER_NAME` or `$BOOT_PID` set in one call are **gone** in the next. To avoid this, this skill uses:

- A **fixed, reusable container name**: `flyway-premigration-check` (kept alive across runs; DB schema is reset in place each run)
- A **PID file**: `/tmp/flyway-check-boot.pid`
- A **log file**: `/tmp/flyway-check-boot.log`

Do not invent `$RANDOM` names or rely on variables surviving between steps.

---

## Prerequisites

- Docker Desktop must be installed and running
- Flyway migration files must exist in `src/main/resources/db/migration/`
- Gradle wrapper (`./gradlew`) available in the project root

---

## Steps

### Step 1 -- Pre-flight checks (single Bash call)

```bash
docker info > /dev/null 2>&1 && echo "DOCKER_OK" || echo "DOCKER_FAIL"
ls src/main/resources/db/migration/*.sql 2>/dev/null || echo "NO_MIGRATIONS"
```

- If `DOCKER_FAIL`: stop and tell the user to start Docker Desktop.
- If `NO_MIGRATIONS`: report "No migrations found to validate" and stop successfully.

### Step 2 -- Ensure a reusable Postgres container is running and the DB is clean

The container `flyway-premigration-check` is **long-lived** across skill runs to avoid paying cold-start cost every time. We only (re)create it if it is missing or not running, and we reset the database contents in place otherwise.

```bash
rm -f /tmp/flyway-check-boot.pid /tmp/flyway-check-boot.log

STATE=$(docker inspect -f '{{.State.Running}}' flyway-premigration-check 2>/dev/null || echo "missing")

if [ "$STATE" = "missing" ]; then
  echo "CONTAINER_NEW"
  docker run --name flyway-premigration-check \
    -e POSTGRES_PASSWORD=test \
    -e POSTGRES_DB=flyway_check \
    -p 15432:5432 \
    -d postgres:16-alpine
elif [ "$STATE" = "false" ]; then
  echo "CONTAINER_RESTART"
  docker start flyway-premigration-check
else
  echo "CONTAINER_REUSE"
fi
```

### Step 3 -- Wait for readiness, then reset the schema

Readiness is fast when the container was reused (already up), slow only on the first cold start.

```bash
for i in $(seq 1 30); do
  docker exec flyway-premigration-check pg_isready -U postgres && echo "PG_READY" && break
  sleep 1
done

# Reset DB state in place -- drops all objects from previous runs without recreating the container.
docker exec flyway-premigration-check psql -U postgres -d flyway_check -v ON_ERROR_STOP=1 -c \
  "DROP SCHEMA public CASCADE; CREATE SCHEMA public; DROP TABLE IF EXISTS flyway_schema_history;" \
  && echo "DB_RESET_OK"
```

If `PG_READY` or `DB_RESET_OK` is not printed, report failure and proceed to cleanup (Step 7).

### Step 4 -- Run Flyway migrate

`MSYS_NO_PATHCONV=1` prevents Git Bash on Windows from mangling the volume mount path (without it, MSYS converts both `/`-prefixed segments and joins them with `;`, creating a spurious `migration;C` directory on the host).

```bash
MSYS_NO_PATHCONV=1 docker run --rm \
  --network host \
  -v "$(pwd)/src/main/resources/db/migration:/flyway/sql:ro" \
  flyway/flyway:latest \
  -url="jdbc:postgresql://localhost:15432/flyway_check" \
  -user=postgres \
  -password=test \
  migrate
```

Capture full output. If Flyway fails, report the failing version + SQL error, then jump to Step 7 (cleanup). Do not proceed.

### Step 5 -- Boot Spring app with `ddl-auto=validate`

Start the app in background and **persist the PID to a file** so later steps can find it.

```bash
nohup ./gradlew bootRun \
  --args='--spring.datasource.url=jdbc:postgresql://localhost:15432/flyway_check --spring.datasource.username=postgres --spring.datasource.password=test --spring.jpa.hibernate.ddl-auto=validate --spring.flyway.enabled=false --server.port=0' \
  > /tmp/flyway-check-boot.log 2>&1 &
echo $! > /tmp/flyway-check-boot.pid
cat /tmp/flyway-check-boot.pid
```

### Step 6 -- Poll the log (bounded, single Bash call)

```bash
RESULT="TIMEOUT"
for i in $(seq 1 90); do
  if grep -q "Started TaskManagerApplication" /tmp/flyway-check-boot.log 2>/dev/null; then
    RESULT="BOOT_OK"; break
  fi
  if grep -qE "Schema-validation:|APPLICATION FAILED TO START|BeanCreationException" /tmp/flyway-check-boot.log 2>/dev/null; then
    RESULT="BOOT_FAIL"; break
  fi
  sleep 1
done
echo "RESULT=$RESULT"
tail -n 60 /tmp/flyway-check-boot.log
```

**Do NOT loop this step across multiple Bash calls.** Run it exactly once. The loop inside the single call is the full poll budget (90s). If `RESULT=TIMEOUT`, treat as failure and go to cleanup.

### Step 7 -- Cleanup (ALWAYS run, even on failure)

Kill the Spring process and remove temp files, but **leave the Postgres container running** so the next invocation is fast. The container will be reused and its DB reset in place on the next run (Step 2/3).

```bash
if [ -f /tmp/flyway-check-boot.pid ]; then
  kill "$(cat /tmp/flyway-check-boot.pid)" 2>/dev/null || true
fi
rm -f /tmp/flyway-check-boot.pid /tmp/flyway-check-boot.log
echo "CLEANUP_DONE (container kept alive for reuse: flyway-premigration-check)"
```

> To fully tear down the reusable container (e.g. before rebooting, or to reclaim the port), run `docker rm -f flyway-premigration-check` manually. The next skill invocation will recreate it automatically.

### Step 8 -- Report results

- **Success** (`RESULT=BOOT_OK`): report migrations applied + "schema matches JPA entities".
- **Flyway failure**: report failing migration version and SQL error from Step 4.
- **Validation failure** (`RESULT=BOOT_FAIL`): extract Hibernate `Schema-validation:` lines from the Step 6 log tail. Recommend running `flyway-create-migration`.
- **Timeout** (`RESULT=TIMEOUT`): report that the app did not start within 90s; include tail of log.

---

## Guardrails

- **Read-only** -- never modify migration files or entities, only validate
- **No production connections** -- only the temporary Docker container on port 15432
- **Always cleanup ephemeral state** -- background Spring process (`/tmp/flyway-check-boot.pid`) and temp log (`/tmp/flyway-check-boot.log`) must be removed after every run; the Postgres container is intentionally kept alive
- **Fixed port 15432 and fixed container name** -- avoids conflict with the dev Postgres on 5432 and avoids state loss between Bash calls
- **Flyway disabled during boot** -- migrations applied via Flyway CLI in Step 4; the Spring app must not re-run them
- **Do not repeat the polling loop across multiple Bash calls** -- Step 6 runs exactly once
