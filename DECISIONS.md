# Network Device Monitoring Service
## Architecture & Design Decisions

**Version:** 2.0 &nbsp;·&nbsp; **Stack:** PostgreSQL 15 · Spring Boot 3.3.5 · Java 21

---

## Scope

This service is designed to serve exactly four operations:

- Register a network device
- Submit a status report for a device (`ONLINE` / `DEGRADED` / `OFFLINE`)
- List all devices with current status, last report timestamp, and stale indicator
- View a single device with its 20 most recent status reports

Nothing beyond this scope is encoded in the schema or application logic. Extension points are called out explicitly where they exist.

---

## 1. Database Schema

Five tables. Every table name is a plain English noun for the thing it stores. No abbreviations, no `tbl_` prefixes, no Hungarian notation.

### `device_types`
```sql
CREATE TABLE device_types (
    id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);
```
Seeded with: `CPE`, `Router`, `Switch`, `Access Point`, `Firewall`, `ONT`.

### `sites`
```sql
CREATE TABLE sites (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL UNIQUE,
    address    TEXT,
    latitude   NUMERIC(9,6),
    longitude  NUMERIC(9,6),
    created_at TIMESTAMPTZ  NOT NULL
);
```
`latitude` and `longitude` are nullable — a site can be registered without coordinates and updated later. `NUMERIC(9,6)` gives six decimal places of precision (~11 cm at the equator), which is more than sufficient for physical infrastructure location.

### `devices`
```sql
CREATE TABLE devices (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255) NOT NULL,
    device_type_id UUID         NOT NULL REFERENCES device_types(id),
    hostname       VARCHAR(255) NOT NULL,
    site_id        UUID         NOT NULL REFERENCES sites(id),
    registered_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_devices_site ON devices(site_id);
CREATE INDEX idx_devices_type ON devices(device_type_id);
```

### `reports`
```sql
CREATE TABLE reports (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id   UUID        NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL CHECK (status IN ('ONLINE','DEGRADED','OFFLINE')),
    message     TEXT,
    reported_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reports_device_time ON reports(device_id, reported_at DESC);
```

### `current_status`
```sql
CREATE TABLE current_status (
    device_id   UUID        PRIMARY KEY REFERENCES devices(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL CHECK (status IN ('ONLINE','DEGRADED','OFFLINE')),
    message     TEXT,
    reported_at TIMESTAMPTZ NOT NULL
);
```

### Entity Relationship

```
device_types ──────────────────────< devices >──────────── current_status
                                         │
                 sites ──────────────────┘
                                         │
                                      reports
```

---

## 2. Table-by-Table Decisions

### 2.1 `device_types`

The brief names six device categories. These are not free-text entered by operators — they are a closed, business-defined taxonomy every device must belong to.

**Why a reference table and not a `VARCHAR` column on `devices`?**

- A `VARCHAR` column has no referential integrity. A device can be registered as `"router"`, `"Router"`, or `"rtr"` — three strings, zero database-level enforcement.
- A reference table makes adding a new type (e.g. OLT, GPON) a single `INSERT` — a data change, not a code change requiring a deployment.
- It is the natural place to attach type-level metadata later (icon slug, monitoring interval, polling protocol) without altering the `devices` table.

**Why not a PostgreSQL `ENUM`?**

`ALTER TYPE ... ADD VALUE` acquires a `ShareRowExclusiveLock` for the duration of the statement. On a live system with active connections this blocks. A reference table extends with a plain `INSERT` — no lock, no downtime.

### 2.2 `sites`

A site is a real-world physical entity — a data centre, a branch office, a point of presence. Storing it as `location VARCHAR(255)` on the `devices` row would mean:

- `"London-01"`, `"london-01"`, and `"London 01"` become three different sites with no database-level way to detect the inconsistency.
- Querying all devices at a site requires a fragile `LIKE` or exact string match.
- Site metadata (address, coordinates) would be duplicated across every device row, with no single source of truth.

Extracting `sites` gives referential integrity, a single source of truth, and a clean extension point.

**On coordinates:** `latitude NUMERIC(9,6)` and `longitude NUMERIC(9,6)` were added to make sites geographically addressable. Network infrastructure is physical — operators need to know where equipment is deployed. Six decimal places gives ~11 cm precision, which is well beyond what is needed to locate a building or rack. Both columns are nullable: a site can be created without known coordinates and enriched later.

### 2.3 `devices`

The core entity. Represents a registered network asset. **Identity only** — name, type, hostname, site, registration timestamp. No operational state lives here.

Keeping `devices` as a clean identity registry and pushing operational state into `current_status` is a deliberate separation of concerns. A `devices` row answers *"what is this asset?"*. The `current_status` row answers *"what state is it in right now?"*. They change for entirely different reasons at entirely different rates.

### 2.4 `reports`

An **append-only event log**. Every status submission produces exactly one new row. Rows are never updated or deleted (except cascade when the parent device is removed).

**Why retain full history?**

The brief explicitly requires the last 20 reports per device — full history must be retained. An event log is also the ground truth for any operational question beyond the current brief: when did it last recover, how often does it degrade, what was its state at 03:00 last Tuesday.

**Why the composite index on `(device_id, reported_at DESC)`?**

Both queries against this table — latest-per-device for the list view, last-20 for the detail view — filter by `device_id` and order by `reported_at` descending. The composite index covers both with a pure index scan. PostgreSQL never touches the heap pages. One index, two queries, zero full table scans.

A standalone `reports(device_id)` index is not created because the composite index subsumes it — PostgreSQL uses `idx_reports_device_time` for any query filtering only by `device_id`.

### 2.5 `current_status`

One row per device. Updated in the **same transaction** as every `INSERT` into `reports`. The row either reflects the latest report or does not exist (the device has never reported).

**Why not derive current status from `reports` on every request?**

| Option | Assessment |
|---|---|
| Query `reports` for latest per device on every request | Requires a subquery or window function aggregating over a table that grows without bound. Acceptable at hundreds of devices, measurably expensive at tens of thousands. |
| Store status on the `devices` row | Conflates device identity with operational state. The `devices` table changes for two different reasons — asset registration and status updates. |
| **Separate `current_status` table** ✓ | O(1) lookup by primary key. The list view becomes a straight `JOIN` with no aggregation. Performance is independent of how large `reports` grows. |

This is **CQRS at the data layer**: `reports` is the write model — the immutable historical record. `current_status` is the read model — optimised for the query the list endpoint actually executes.

Both writes happen inside a single `@Transactional` method. Either both succeed or both fail. There is no window in which `reports` and `current_status` can be inconsistent.

---

## 3. Cross-Cutting Technical Decisions

### 3.1 Primary Keys — UUID v4

| Option | Assessment |
|---|---|
| `BIGSERIAL` / auto-increment | Sequential integers expose record counts and enable enumeration attacks. The ID is only known after the database round-trip. |
| UUID v7 / ULID | Time-ordered UUIDs improve b-tree index locality on sequential inserts. Requires an extension or application-side generation. Meaningful at very high insert rates. |
| **UUID v4 via `gen_random_uuid()`** ✓ | Built into PostgreSQL 13+, no extensions. Non-guessable. Can be generated at the application layer before the `INSERT`. The insert locality trade-off is negligible at this scale. |

### 3.2 Timestamps — `TIMESTAMPTZ` everywhere

`TIMESTAMPTZ` stores values in UTC internally and converts correctly on retrieval regardless of server locale. Bare `TIMESTAMP` is interpreted relative to the database server timezone setting — a silent source of incorrect data if the server locale changes, or if operators and devices span timezones.

Stale detection (`reported_at < NOW() - INTERVAL '15 minutes'`) only produces correct results if all timestamps share a consistent reference frame. **`TIMESTAMPTZ` is non-negotiable.**

Timestamps are set by the application, not by `DEFAULT NOW()`. A database default cannot be overridden in unit tests — the database sets it at insert time regardless of what the test supplies. Application-controlled timestamps are explicit, fully testable, and semantically accurate: `reported_at` records when the device reported, not when the database processed the row.

### 3.3 Status Values — `VARCHAR(20)` with `CHECK` constraint

Valid values: `'ONLINE'`, `'DEGRADED'`, `'OFFLINE'`.

| Option | Assessment |
|---|---|
| PostgreSQL native `ENUM` | `ALTER TYPE ... ADD VALUE` acquires an exclusive lock. Adding a fourth status requires a deployment window on a live system. |
| Unconstrained `VARCHAR` | No database-level enforcement. Any string can be inserted. |
| **`VARCHAR(20)` + `CHECK` constraint** ✓ | Enforces the closed set at the database layer. The application enum validates at the service layer; the `CHECK` constraint is the final guard. Adding a value is a DDL migration, not a locking `ALTER TYPE`. |

### 3.4 Referential Integrity — enforced at the database layer

All foreign keys are declared and enforced by PostgreSQL. Application-level validation alone is insufficient: direct database access from migrations, admin scripts, or a bug bypasses the application entirely.

`ON DELETE CASCADE` is applied to both `reports.device_id` and `current_status.device_id`. A deleted device has no meaningful orphaned reports or status rows. Cascade deletes them atomically — no application logic required, no orphaned rows possible.

### 3.5 Stale Detection — computed at read time in the service layer

A device is stale if `current_status.reported_at < NOW() - 15 minutes`, or if no row exists in `current_status` (the device has never reported). The threshold is a named constant in the service: `STALE_THRESHOLD_MINUTES = 15`.

| Option | Assessment |
|---|---|
| Scheduled job sets an `is_stale` flag | The flag is itself stale between runs. A device that stops reporting is only marked stale after the next job fires. Operational accuracy varies by schedule interval. |
| Computed column in SQL | Pushes business logic into the database. The 15-minute threshold becomes a schema constant rather than application configuration. Not unit-testable without a live database. |
| Per-device event-driven timer | Requires a scheduler with one timer per device. Infrastructure overhead for a single boolean computation. |
| **Computed at read time in the service layer** ✓ | Always accurate to the millisecond. Zero background infrastructure. Threshold is application configuration — changeable without a migration. Fully unit-testable with mocked timestamps. |

### 3.6 N+1 Query Prevention

The list endpoint returns all devices with their current status. A naïve implementation issues one query per device (1 + N). This is prevented at two levels:

**Device fetch** — a single JPQL query with `JOIN FETCH` loads devices, their types, and their sites in one round-trip regardless of device count:
```java
@Query("SELECT d FROM Device d JOIN FETCH d.deviceType JOIN FETCH d.site")
List<Device> findAllWithDetails();
```

**Current status fetch** — a single `findAllById()` call loads all current status rows as a `Map<UUID, CurrentStatus>`. The service then performs an in-memory lookup per device — no per-device query:
```java
Map<UUID, CurrentStatus> statusMap = currentStatusRepository
    .findAllById(deviceIds)
    .stream()
    .collect(toMap(cs -> cs.getDevice().getId(), identity()));
```

The list endpoint always executes exactly **2 queries**, regardless of how many devices exist.

### 3.7 Schema Ownership — Flyway with Hibernate validation only

Flyway owns the schema. Hibernate is set to `ddl-auto: validate` — it validates that entities match the schema at startup but never modifies it.

| Option | Assessment |
|---|---|
| `ddl-auto: create-drop` | Destroys and recreates the schema on every restart. Unusable in production. |
| `ddl-auto: update` | Applies additive changes silently. Does not drop renamed columns, cannot detect drift. Dangerous in production. |
| **Flyway + `ddl-auto: validate`** ✓ | Every schema change is a versioned, reviewable, repeatable SQL file. Flyway runs in CI, in Docker, and in production identically. Hibernate acts as a safety net — if the entity model diverges from the actual schema, the application refuses to start rather than running in a broken state. |

### 3.8 Indexes

Three indexes, each justified by a specific query pattern.

**`idx_reports_device_time`** — composite on `(device_id, reported_at DESC)`
- Covers detail-view query: `WHERE device_id = ? ORDER BY reported_at DESC LIMIT 20` — pure index scan, zero heap access.
- Also covers the latest-report lookup in the list view. One index serves both read paths.

**`idx_devices_site`** — on `devices(site_id)`
- PostgreSQL does not automatically index foreign key columns. Without this, filtering devices by site is a sequential scan of the entire `devices` table.

**`idx_devices_type`** — on `devices(device_type_id)`
- Same rationale. Filtering by device type without an index is a full table scan.

---

## 4. API Design Decisions

### 4.1 Response Envelope — GraphQL-inspired structure

Every API response — success or failure — uses a consistent three-field envelope:

```json
{
  "data": { ... },
  "errors": null,
  "meta": {
    "timestamp": "2026-06-04T01:51:05.035Z",
    "version": "v1",
    "count": 6
  }
}
```

**On success:** `data` carries the payload, `errors` is absent, `meta` carries timestamp, API version, and item count for list responses.

**On error:**
```json
{
  "data": null,
  "errors": [
    {
      "message": "Device not found",
      "code": "NOT_FOUND",
      "field": null
    }
  ],
  "meta": {
    "timestamp": "2026-06-04T01:51:05.035Z",
    "version": "v1"
  }
}
```

**On validation failure:** one error object per invalid field, each with `code: VALIDATION_ERROR` and the `field` name that failed.

**Why not `{ success: true, message: "...", data: {} }`?**

The simple boolean envelope forces the consumer to parse a flag and a string message to understand what went wrong. There is no machine-readable error classification — every error is just a string. The GraphQL-inspired structure gives:

- `errors` as a typed array — a consumer can iterate errors and act on each `code` and `field` without string parsing.
- `meta.count` on list responses — the consumer knows the total without counting array elements.
- `meta.version` — forwards compatibility signal for clients.
- Absence of `errors` is the success signal — no boolean required.

Error codes are intentionally coarse-grained (`NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_ERROR`) — sufficient for client-side branching without leaking internal implementation detail.

### 4.2 API Versioning — `/api/v1/` prefix

All endpoints are prefixed with `/api/v1/`. This reserves the right to introduce `/api/v2/` with breaking changes without removing the v1 contract. The version is also echoed in every response envelope via `meta.version`.

### 4.3 Documentation — `/docs` and `/docs/openapi`

Swagger UI is exposed at `/docs`. The raw OpenAPI JSON is at `/docs/openapi`. `/swagger-ui.html` is not used — it is the old SpringDoc default and carries no semantic meaning. `/docs` is the conventional, human-readable path that modern APIs use.

---

## 5. Testing Strategy

### 5.1 Test layers

The test suite covers two layers only: **service** and **controller**.

```
test/
  service/
    DeviceServiceTest.java     — business logic, stale detection, error paths
    ReportServiceTest.java     — report submission, current_status upsert
    SiteServiceTest.java       — site registration, list
  controller/
    DeviceControllerTest.java  — HTTP status codes, request validation, error response shape
    SiteControllerTest.java    — HTTP status codes, request validation
    DeviceTypeControllerTest.java
```

**Service tests** use `@ExtendWith(MockitoExtension.class)` — pure JVM, no Spring context. All dependencies are mocked. These tests run in milliseconds and can run anywhere without infrastructure.

**Controller tests** use `@WebMvcTest` — loads only the web layer. Services are mocked with `@MockBean`. These tests verify HTTP semantics: status codes, response structure, validation error shape, and 404 handling. No database, no Testcontainers.

### 5.2 Why no repository tests?

Repository tests (`@SpringBootTest` + Testcontainers) would verify the custom JPQL queries against a real database. This was considered and rejected for this scope:

- The two custom queries (`JOIN FETCH`, `findTop20...OrderByReportedAtDesc`) are straightforward enough that any mistake is caught immediately at startup by Hibernate's schema validation or at integration time.
- Testcontainers adds Docker as a hard dependency of the test run. On CI, this means a Docker-in-Docker setup. On developer machines, it means Docker must be running before tests can pass.
- The service tests mock the repositories and assert on the service behaviour. The correctness of the SQL itself is confirmed by running the application against a real database.

The deliberate trade-off: fewer test infrastructure dependencies in exchange for slightly lower coverage of the data access layer.

### 5.3 Test organisation — `@Nested` classes as structure

Every test class uses `@Nested` inner classes to group tests by operation rather than by comments. Comments rot; `@Nested` classes are enforced by the compiler and appear as named suites in test reports.

---

## 6. Infrastructure Decisions

### 6.1 Containerisation — multi-stage Dockerfile

```
Stage 1 (builder): maven:3.9.9-eclipse-temurin-21-alpine
  - COPY pom.xml → RUN mvn dependency:go-offline   ← cached layer
  - COPY src      → RUN mvn package -DskipTests

Stage 2 (runtime): eclipse-temurin:21-jre-alpine
  - COPY --from=builder /app/target/*.jar app.jar
  - ENTRYPOINT ["java", "-jar", "app.jar"]
```

The dependency download step is a **separate layer** from the source copy. Docker caches this layer as long as `pom.xml` does not change. A source-only change rebuilds in ~20 seconds rather than re-downloading the entire dependency tree.

The runtime image contains only the JRE, not the JDK or Maven. The final image is ~180 MB rather than the ~600 MB builder image.

### 6.2 Docker Compose — health-check dependency chain

The `app` service declares:
```yaml
depends_on:
  postgres:
    condition: service_healthy
```

The postgres service has a `pg_isready` healthcheck. The application container does not start until the database is confirmed ready. Without this, the app starts, attempts the Flyway migration, finds no database, and crashes. The health check eliminates the race condition without any retry logic in the application.

### 6.3 Local development defaults

`docker-compose.yaml` and `application.yml` both default `DB_USERNAME` and `DB_PASSWORD` to `postgres` when the environment variable is not set. This means `docker-compose up` works immediately with no `.env` file or pre-configuration.

In production, the variables are set explicitly in the deployment environment. The application will refuse to start if they are not set (no default at the infrastructure level), which is intentional — a misconfigured production deploy fails loudly rather than connecting to a database with default credentials.

### 6.4 Java 21

The brief specifies Java 17+. Java 21 was chosen because it is the current LTS release and is what the CI environment and Docker images target. Java 21 adds virtual threads (Project Loom), records, pattern matching, and sealed classes — all of which are available to this codebase. Records are used extensively for DTOs and response objects.
