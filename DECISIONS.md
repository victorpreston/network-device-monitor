# Network Device Monitoring Service
## Database Architecture & Design Decisions
 
**Version:** 1.0 &nbsp;·&nbsp; **Target:** PostgreSQL 15 / Spring Boot 3 / Java 17
 
## Scope
 
This schema is designed to serve exactly four operations:
 
- Register a network device
- Submit a status report for a device (`ONLINE` / `DEGRADED` / `OFFLINE`)
- List all devices - current status, last report timestamp, stale flag (no report in 15 minutes)
- View a single device with its 20 most recent status reports
Nothing beyond this scope is encoded in the schema. Extension points are called out explicitly where they exist.

 
## 1. Schema Overview
 
Five tables. Every table name is a plain English noun for the thing it stores. No abbreviations, no `tbl_` prefixes, no Hungarian notation.
 
```sql
-- Reference table: valid device categories
CREATE TABLE device_types (
    id    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name  VARCHAR(50) NOT NULL UNIQUE   -- e.g. Router, Switch, Firewall
);
 
-- Reference table: physical deployment locations
CREATE TABLE sites (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL UNIQUE,
    address    TEXT,
    created_at TIMESTAMPTZ  NOT NULL
);
 
-- Core entity: the registered asset
CREATE TABLE devices (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255) NOT NULL,
    device_type_id UUID         NOT NULL REFERENCES device_types(id),
    hostname       VARCHAR(255) NOT NULL,
    site_id        UUID         NOT NULL REFERENCES sites(id),
    registered_at  TIMESTAMPTZ  NOT NULL
);
 
-- Append-only event log: one row per status submission
CREATE TABLE reports (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id   UUID        NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL CHECK (status IN ('ONLINE','DEGRADED','OFFLINE')),
    message     TEXT,
    reported_at TIMESTAMPTZ NOT NULL
);
 
-- Current-state read model: one row per device, updated on every report
CREATE TABLE current_status (
    device_id   UUID        PRIMARY KEY REFERENCES devices(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL CHECK (status IN ('ONLINE','DEGRADED','OFFLINE')),
    message     TEXT,
    reported_at TIMESTAMPTZ NOT NULL
);
 
-- Indexes
CREATE INDEX idx_reports_device_time ON reports(device_id, reported_at DESC);
CREATE INDEX idx_devices_site        ON devices(site_id);
CREATE INDEX idx_devices_type        ON devices(device_type_id);
```
 
### Entity-Relationship Summary
 
```
device_types ──────────────────────< devices >───────────────── current_status
                                         │
                 sites ──────────────────┘
                                         │
                                      reports
```
 
## 2. Table-by-Table Decisions
 
### 2.1 `device_types`
 
The brief names six device categories - CPE, Router, Switch, Access Point, Firewall, ONT. These are not arbitrary strings entered by operators. They are a closed, business-defined taxonomy that every device must belong to.
 
**Why a table and not a `VARCHAR` column on `devices`?**
 
- A `VARCHAR` column has no referential integrity. A device can be registered as `"router"`, `"Router"`, or `"rtr"` - three different strings, zero database-level enforcement.
- A reference table makes adding a new type (e.g. OLT, GPON) a **data change - a single `INSERT`** - not a code change requiring a deployment and migration.
- It is the natural place to attach type-level metadata later (icon slug, default monitoring interval) without altering the `devices` table.
**Why not a PostgreSQL `ENUM` or an application-level enum stored as `VARCHAR`?**
 
- PostgreSQL `ENUM` requires `ALTER TYPE ... ADD VALUE` to extend. This acquires a `ShareRowExclusiveLock` on the type for the duration of the statement. On a live system with active connections, this blocks. A reference table extends with a plain `INSERT` - no lock, no downtime.
- An application-level enum validated only in Java has no enforcement at the database layer. Direct database access from migrations, ops tooling, or a bug bypasses the application entirely. **The database is the last line of defence.**
 
### 2.2 `sites`
 
A site is a real-world entity - a data centre, a branch office, a POP. Multiple devices are deployed at the same site. Storing it as `location VARCHAR(255)` on the `devices` row would mean:
 
- `"London-01"`, `"london-01"`, and `"London 01"` become three different sites with no database-level way to detect the inconsistency.
- Querying all devices at a site requires a fragile `LIKE` or exact string match.
- Site metadata (address, city) would be duplicated across every device row at that site, with no single source of truth.
- There is nowhere to hang site-level attributes in the future without adding columns to the `devices` table.
Extracting `sites` gives referential integrity, consistency, and a clean extension point.

 
### 2.3 `devices`
 
The core entity. Represents a network asset registered in the system. **Identity only** - name, type, hostname, site, registration timestamp. No operational state lives here.
 
Keeping `devices` as a clean identity registry and pushing operational state into `current_status` is a deliberate separation of concerns. A `devices` row answers *"what is this asset?"*. The `current_status` row answers *"what state is it in right now?"*.

 
### 2.4 `reports`
 
An **append-only event log**. Every status submission by a device produces exactly one new row. Rows are never updated, never deleted (except on cascade when the parent device is removed).
 
**Why keep full history?**
 
- The brief explicitly requires the last 20 reports per device. Full history must be retained.
- An event log is the ground truth. It answers not just *"what is the current state?"* but *"when did it last recover?"*, *"how often does it degrade?"*, *"what was its state at 03:00 last Tuesday?"*
**Why the composite index on `(device_id, reported_at DESC)`?**
 
Both queries against this table - the latest report per device (list view) and the last 20 reports (detail view) - filter by `device_id` and order by `reported_at` descending. The composite index covers both with a **pure index scan**. PostgreSQL never touches the heap pages. One index, two queries, zero full table scans.
 
> A standalone index on `reports(device_id)` alone is not created because the composite index subsumes it - PostgreSQL can use `idx_reports_device_time` for any query that filters by `device_id` regardless of whether it references `reported_at`.

 
### 2.5 `current_status`
 
One row per device. Updated in the **same transaction** as every `INSERT` into `reports`. The row either reflects the latest report or does not exist (the device has never reported).
 
**Why not derive current status by querying `reports` on every request?**
 
| Option | Assessment |
|---|---|
| Query `reports` for latest per device | Requires a subquery or window function aggregating over a table that grows without bound. Acceptable at hundreds of devices, measurably expensive at tens of thousands. |
| Store status on the `devices` row | Conflates device identity with operational state. The `devices` table changes for two different reasons - asset registration and status updates. |
| **Separate `current_status` table** ✓ | O(1) lookup by primary key. The list view becomes a straight `JOIN` between `devices` and `current_status` with no aggregation. Performance is independent of how large `reports` grows. |
 
This is **CQRS at the data layer**: `reports` is the write model - the immutable historical record. `current_status` is the read model - the table optimised for the query the list endpoint actually runs.
 
Both writes happen inside a single transaction. Either both succeed or both fail. There is no window in which `reports` and `current_status` can be inconsistent.

 
## 3. Cross-Cutting Decisions
 
### 3.1 Primary Keys - UUID v4
 
| Option | Assessment |
|---|---|
| `BIGSERIAL` / auto-increment | Sequential integers expose record counts and enable enumeration attacks. The ID is only known after the database round-trip, complicating optimistic inserts and pre-generated correlation IDs. |
| UUID v7 / ULID | Time-ordered UUIDs improve b-tree index locality on sequential inserts. Meaningful at high insert throughput. Requires an extension or application-side generation. |
| **UUID v4 via `gen_random_uuid()`** ✓ | Built into PostgreSQL 13+ with no extensions. Non-guessable. Can be generated at the application layer before the `INSERT` - the ID exists before the row does. The insert locality trade-off is negligible at this scale. |

 
### 3.2 Timestamps - `TIMESTAMPTZ` everywhere
 
`TIMESTAMPTZ` stores values in UTC internally and converts correctly on retrieval regardless of server locale. Bare `TIMESTAMP` is interpreted relative to the database server's timezone setting - a silent source of incorrect data if the server locale ever changes, or if operators and devices span timezones.
 
Infrastructure monitoring timestamps are operationally critical. The stale detection boundary (now minus 15 minutes) only produces correct results if all timestamps share a consistent reference frame. **`TIMESTAMPTZ` is non-negotiable.**
 
**Timestamps are set by the application, not by `DEFAULT NOW()`.**
 
A `DEFAULT NOW()` cannot be overridden in tests without workarounds - the database sets it at insert time regardless of what the test supplies. Application-controlled timestamps are explicit, fully testable, and semantically accurate: `reported_at` records when the device reported, not when the database processed the row, which may differ under load.

 
### 3.3 Status Values - `VARCHAR(20)` with `CHECK` constraint
 
Valid values: `'ONLINE'`, `'DEGRADED'`, `'OFFLINE'`.
 
| Option | Assessment |
|---|---|
| PostgreSQL native `ENUM` | `ALTER TYPE ... ADD VALUE` acquires an exclusive lock on the type. Adding a fourth status requires a deployment window on a live system. |
| Unconstrained `VARCHAR` | No database-level enforcement. Any string can be inserted. Application validation is the only barrier. |
| **`VARCHAR(20)` + `CHECK` constraint** ✓ | Enforces the closed set at the database layer. Adding a new status is `ALTER TABLE ... DROP CONSTRAINT / ADD CONSTRAINT` - DDL, but not the locking minefield of `ALTER TYPE`. The application enum validates at the service layer; the `CHECK` is the final guard. |

 
### 3.4 Referential Integrity - enforced at the database layer
 
All foreign keys are declared and enforced by PostgreSQL. Application-level validation alone is insufficient: direct database access from migrations, admin scripts, or bugs bypasses the application entirely.
 
`ON DELETE CASCADE` is applied to both `reports.device_id` and `current_status.device_id`. A deleted device has no meaningful orphaned reports or status rows. Cascade deletes them atomically within the same statement - no application logic required, no orphaned rows possible.

 
### 3.5 Stale Detection - computed at read time in the service layer
 
A device is stale if `current_status.reported_at < NOW() - INTERVAL '15 minutes'` - or if no row exists in `current_status` (the device has never reported).
 
| Option | Assessment |
|---|---|
| Scheduled job sets an `is_stale` flag | The flag is itself stale between runs. A device that stops reporting at 14:59 and is checked at 15:01 shows as fresh until the next job run. Complexity with no accuracy improvement. |
| Computed column in SQL | Pushes business logic into the database. Cannot be unit-tested without a live database connection. The 15-minute threshold becomes a schema constant rather than application configuration. |
| Per-device event-driven timer | Requires a scheduler with one timer per device. Significant infrastructure overhead for a simple boolean computation. |
| **Computed at read time in the service layer** ✓ | Always accurate to the millisecond. Zero background infrastructure. The threshold is application configuration - changeable without a migration. Fully unit-testable without a database. |

 
### 3.6 Indexes
 
Three indexes, each justified by a specific query pattern.
 
**`idx_reports_device_time`** - composite on `(device_id, reported_at DESC)`
 
- Covers the detail-view query: `WHERE device_id = ? ORDER BY reported_at DESC LIMIT 20` - pure index scan, zero heap access.
- Also covers the subquery inside the list view that retrieves the latest report per device. One index serves both read paths.
**`idx_devices_site`** - on `devices(site_id)`
 
- PostgreSQL does not automatically index foreign key columns. Without this, a query filtering devices by site performs a sequential scan of the entire `devices` table.
**`idx_devices_type`** - on `devices(device_type_id)`
 
- Same rationale. The device list endpoint will filter by type. Without an index, that filter is a full table scan.
