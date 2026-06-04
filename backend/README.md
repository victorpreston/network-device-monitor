# Network Device Monitoring Service

A backend REST API for registering and monitoring network infrastructure. Devices report their operational status periodically; the system tracks current state per device, flags stale devices that have stopped reporting, and maintains a full status report history.

For the reasoning behind every schema, architectural, and API design choice - see [DECISIONS.md](./DECISIONS.md).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Containerisation | Docker + Docker Compose |
| Testing | JUnit 5 · Mockito · MockMvc |

---

## Prerequisites

| Tool | Version |
|---|---|
| Docker & Docker Compose | Any recent version |
| Java | 21 (only needed if running locally without Docker) |
| Maven | 3.9+ (or use the included `./mvnw` wrapper) |

---

## Running with Docker (Recommended)

No `.env` file or configuration needed. The Compose file ships with safe local defaults (`postgres / postgres`).

```bash
docker-compose up --build
```

The API will be available at `http://localhost:8080`.

Flyway runs all migrations and seeds the six device types automatically on startup.

To stop and remove volumes:

```bash
docker-compose down -v
```

---

## Running Locally

**1. Start PostgreSQL and create the database:**

```bash
psql -U postgres -c "CREATE DATABASE netdevmon;"
```

**2. Set credentials** (skip this step if your local Postgres uses `postgres / postgres`):

```bash
export DB_USERNAME=your_user
export DB_PASSWORD=your_password
```

**3. Run the application:**

```bash
./mvnw spring-boot:run
```

Flyway will run all migrations and seed device types on first start.

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/netdevmon` | Full JDBC URL |

---

## Database Schema

Five tables. Each has a single responsibility.

### `device_types`
Reference table. Seeded on startup - do not insert manually.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK, generated |
| `name` | VARCHAR(50) | UNIQUE |

**Seeded values:** `CPE`, `Router`, `Switch`, `Access Point`, `Firewall`, `ONT`

### `sites`
Physical deployment locations.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK, generated |
| `name` | VARCHAR(255) | UNIQUE, required |
| `address` | TEXT | optional |
| `latitude` | NUMERIC(9,6) | optional, ±90 |
| `longitude` | NUMERIC(9,6) | optional, ±180 |
| `created_at` | TIMESTAMPTZ | set by application |

### `devices`
Registered network assets. Identity only - no operational state.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK, generated |
| `name` | VARCHAR(255) | required |
| `device_type_id` | UUID | FK → `device_types` |
| `hostname` | VARCHAR(255) | required |
| `site_id` | UUID | FK → `sites` |
| `registered_at` | TIMESTAMPTZ | set by application |

### `reports`
Append-only event log. Never updated or deleted (except cascade).

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK, generated |
| `device_id` | UUID | FK → `devices` ON DELETE CASCADE |
| `status` | VARCHAR(20) | `ONLINE` / `DEGRADED` / `OFFLINE` |
| `message` | TEXT | optional |
| `reported_at` | TIMESTAMPTZ | set by application |

### `current_status`
One row per device. Updated in the same transaction as every report insert. The read model - O(1) lookup regardless of how large `reports` grows.

| Column | Type | Notes |
|---|---|---|
| `device_id` | UUID | PK + FK → `devices` ON DELETE CASCADE |
| `status` | VARCHAR(20) | `ONLINE` / `DEGRADED` / `OFFLINE` |
| `message` | TEXT | optional |
| `reported_at` | TIMESTAMPTZ | timestamp of the latest report |

---

## API Overview

All endpoints are prefixed with `/api/v1/`. Every response uses the same envelope:

```json
{
  "data": { },
  "errors": null,
  "meta": {
    "timestamp": "2026-06-04T10:30:00.000+00:00",
    "version": "v1",
    "count": null
  }
}
```

`errors` is absent on success. `count` is populated on list responses. On failure, `data` is null and `errors` contains one or more error objects.

### Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/device-types` | List all device types |
| `POST` | `/api/v1/sites` | Register a site |
| `GET` | `/api/v1/sites` | List all sites |
| `GET` | `/api/v1/sites/{id}` | Get a single site by ID |
| `GET` | `/api/v1/sites/{id}/devices` | List all devices at a site |
| `POST` | `/api/v1/devices` | Register a device |
| `GET` | `/api/v1/devices` | List all devices (with optional filters) |
| `GET` | `/api/v1/devices/{id}` | Get a device with its 20 most recent reports |
| `POST` | `/api/v1/devices/{id}/reports` | Submit a status report for a device |

---

## Endpoint Reference

### GET `/api/v1/device-types`

Returns the seeded list of device types. Use the `id` values when registering a device.

**Response `200 OK`:**
```json
{
  "data": [
    { "id": "1a2b3c4d-...", "name": "CPE" },
    { "id": "2b3c4d5e-...", "name": "Router" },
    { "id": "3c4d5e6f-...", "name": "Switch" },
    { "id": "4d5e6f7a-...", "name": "Access Point" },
    { "id": "5e6f7a8b-...", "name": "Firewall" },
    { "id": "6f7a8b9c-...", "name": "ONT" }
  ],
  "errors": null,
  "meta": { "timestamp": "2026-06-04T10:30:00.000+00:00", "version": "v1", "count": 6 }
}
```

---

### POST `/api/v1/sites`

Register a new site. `name` is required. `address`, `latitude`, and `longitude` are optional.

**Request body:**
```json
{
  "name": "London-01",
  "address": "1 Tech Street, London, EC1A 1BB",
  "latitude": 51.517400,
  "longitude": -0.085100
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `name` | string | Yes | Non-blank |
| `address` | string | No | - |
| `latitude` | number | No | Between -90 and 90 |
| `longitude` | number | No | Between -180 and 180 |

**Response `201 Created`:**
```json
{
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "London-01",
    "address": "1 Tech Street, London, EC1A 1BB",
    "latitude": 51.517400,
    "longitude": -0.085100,
    "createdAt": "2026-06-04T10:30:00.000+00:00"
  },
  "errors": null,
  "meta": { "timestamp": "2026-06-04T10:30:00.000+00:00", "version": "v1" }
}
```

---

### GET `/api/v1/sites`

Returns all registered sites.

**Response `200 OK`:**
```json
{
  "data": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "name": "London-01",
      "address": "1 Tech Street, London, EC1A 1BB",
      "latitude": 51.517400,
      "longitude": -0.085100,
      "createdAt": "2026-06-04T10:30:00.000+00:00"
    },
    {
      "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "name": "Manchester-01",
      "address": "5 Digital Way, Manchester, M1 2AB",
      "latitude": 53.480900,
      "longitude": -2.242600,
      "createdAt": "2026-06-04T10:31:00.000+00:00"
    }
  ],
  "errors": null,
  "meta": { "timestamp": "2026-06-04T10:32:00.000+00:00", "version": "v1", "count": 2 }
}
```

---

### GET `/api/v1/sites/{id}`

Returns a single site by ID.

**Response `200 OK`:**
```json
{
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "London-01",
    "address": "1 Tech Street, London, EC1A 1BB",
    "latitude": 51.517400,
    "longitude": -0.085100,
    "createdAt": "2026-06-04T10:30:00.000+00:00"
  },
  "errors": null,
  "meta": { "timestamp": "2026-06-04T10:32:00.000+00:00", "version": "v1" }
}
```

**Response `404 Not Found`:**
```json
{
  "data": null,
  "errors": [{ "message": "Site not found", "code": "NOT_FOUND", "field": null }],
  "meta": { "timestamp": "2026-06-04T10:32:00.000+00:00", "version": "v1" }
}
```

---

### GET `/api/v1/sites/{id}/devices`

Returns all devices registered at a given site. Returns 404 if the site does not exist. Returns an empty list if the site exists but has no devices.

**Response `200 OK`:**
```json
{
  "data": [
    {
      "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
      "name": "Core Router 01",
      "deviceType": "Router",
      "hostname": "core-rtr-01.london-01.corp",
      "site": "London-01",
      "registeredAt": "2026-06-04T09:00:00.000+00:00",
      "currentStatus": "ONLINE",
      "lastReportAt": "2026-06-04T10:28:00.000+00:00",
      "stale": false
    }
  ],
  "errors": null,
  "meta": { "timestamp": "2026-06-04T10:32:00.000+00:00", "version": "v1", "count": 1 }
}
```

---

### POST `/api/v1/devices`

Register a new network device. Use a `deviceTypeId` from `GET /api/v1/device-types` and a `siteId` from `GET /api/v1/sites`.

**Request body:**
```json
{
  "name": "Core Router 01",
  "deviceTypeId": "2b3c4d5e-f6a7-8901-bcde-f12345678901",
  "hostname": "core-rtr-01.london-01.corp",
  "siteId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `name` | string | Yes | Non-blank |
| `deviceTypeId` | UUID | Yes | Must match an existing device type |
| `hostname` | string | Yes | Non-blank |
| `siteId` | UUID | Yes | Must match an existing site |

**Response `201 Created`:**
```json
{
  "data": {
    "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "name": "Core Router 01",
    "deviceType": "Router",
    "hostname": "core-rtr-01.london-01.corp",
    "site": "London-01",
    "registeredAt": "2026-06-04T10:33:00.000+00:00",
    "currentStatus": null,
    "lastReportAt": null,
    "stale": true
  },
  "errors": null,
  "meta": { "timestamp": "2026-06-04T10:33:00.000+00:00", "version": "v1" }
}
```

A newly registered device has no reports yet - `currentStatus` is null and `stale` is `true`.

---

### GET `/api/v1/devices`

Returns all devices with their current status and stale flag. Supports optional query filters.

**Query parameters:**

| Parameter | Type | Description |
|---|---|---|
| `status` | `ONLINE` \| `DEGRADED` \| `OFFLINE` | Filter by current status |
| `stale` | `true` \| `false` | Filter by stale flag |

Both parameters are optional and can be combined.

**Examples:**
```
GET /api/v1/devices
GET /api/v1/devices?status=OFFLINE
GET /api/v1/devices?stale=true
GET /api/v1/devices?status=DEGRADED&stale=false
```

**Response `200 OK`:**
```json
{
  "data": [
    {
      "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
      "name": "Core Router 01",
      "deviceType": "Router",
      "hostname": "core-rtr-01.london-01.corp",
      "site": "London-01",
      "registeredAt": "2026-06-04T09:00:00.000+00:00",
      "currentStatus": "ONLINE",
      "lastReportAt": "2026-06-04T10:28:00.000+00:00",
      "stale": false
    },
    {
      "id": "d4e5f6a7-b8c9-0123-defa-234567890123",
      "name": "Branch Firewall",
      "deviceType": "Firewall",
      "hostname": "fw-01.manchester-01.corp",
      "site": "Manchester-01",
      "registeredAt": "2026-06-04T09:15:00.000+00:00",
      "currentStatus": null,
      "lastReportAt": null,
      "stale": true
    }
  ],
  "errors": null,
  "meta": { "timestamp": "2026-06-04T10:35:00.000+00:00", "version": "v1", "count": 2 }
}
```

---

### GET `/api/v1/devices/{id}`

Returns a single device with its 20 most recent status reports, ordered newest first.

**Response `200 OK`:**
```json
{
  "data": {
    "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "name": "Core Router 01",
    "deviceType": "Router",
    "hostname": "core-rtr-01.london-01.corp",
    "site": "London-01",
    "registeredAt": "2026-06-04T09:00:00.000+00:00",
    "currentStatus": "ONLINE",
    "lastReportAt": "2026-06-04T10:28:00.000+00:00",
    "stale": false,
    "recentReports": [
      {
        "id": "e5f6a7b8-c9d0-1234-efab-345678901234",
        "status": "ONLINE",
        "message": "All interfaces up, CPU 12%, MEM 34%",
        "reportedAt": "2026-06-04T10:28:00.000+00:00"
      },
      {
        "id": "f6a7b8c9-d0e1-2345-fabc-456789012345",
        "status": "DEGRADED",
        "message": "High packet loss on eth2",
        "reportedAt": "2026-06-04T10:13:00.000+00:00"
      },
      {
        "id": "a7b8c9d0-e1f2-3456-abcd-567890123456",
        "status": "ONLINE",
        "message": "Recovered",
        "reportedAt": "2026-06-04T09:58:00.000+00:00"
      }
    ]
  },
  "errors": null,
  "meta": { "timestamp": "2026-06-04T10:35:00.000+00:00", "version": "v1" }
}
```

**Response `404 Not Found`:**
```json
{
  "data": null,
  "errors": [{ "message": "Device not found", "code": "NOT_FOUND", "field": null }],
  "meta": { "timestamp": "2026-06-04T10:35:00.000+00:00", "version": "v1" }
}
```

---

### POST `/api/v1/devices/{id}/reports`

Submit a status report for a device. This updates `current_status` in the same transaction - the list endpoint immediately reflects the new state.

**Request body:**
```json
{
  "status": "ONLINE",
  "message": "All interfaces up, CPU 12%, MEM 34%"
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `status` | `ONLINE` \| `DEGRADED` \| `OFFLINE` | Yes | Must be one of the three values |
| `message` | string | No | Free text |

**Response `201 Created`:**
```json
{
  "data": null,
  "errors": null,
  "meta": { "timestamp": "2026-06-04T10:36:00.000+00:00", "version": "v1" }
}
```

**Response `404 Not Found` (device does not exist):**
```json
{
  "data": null,
  "errors": [{ "message": "Device not found", "code": "NOT_FOUND", "field": null }],
  "meta": { "timestamp": "2026-06-04T10:36:00.000+00:00", "version": "v1" }
}
```

---

## Error Responses

All errors use the same envelope. `data` is always null on an error response.

### Validation error (400 Bad Request)

One error object per invalid field:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Device name is required",
      "code": "VALIDATION_ERROR",
      "field": "name"
    },
    {
      "message": "Site is required",
      "code": "VALIDATION_ERROR",
      "field": "siteId"
    }
  ],
  "meta": { "timestamp": "2026-06-04T10:36:00.000+00:00", "version": "v1" }
}
```

### Not found (404)

```json
{
  "data": null,
  "errors": [{ "message": "Device not found", "code": "NOT_FOUND", "field": null }],
  "meta": { "timestamp": "2026-06-04T10:36:00.000+00:00", "version": "v1" }
}
```

### Error codes

| Code | HTTP Status | Meaning |
|---|---|---|
| `VALIDATION_ERROR` | 400 | A request field failed validation |
| `NOT_FOUND` | 404 | The requested resource does not exist |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## Stale Detection

A device is flagged `stale: true` if:

- it has **never submitted a report**, or
- its last report was **more than 15 minutes ago**

The flag is computed at read time on every list and detail response. There is no background job - the value is always accurate to the millisecond. The threshold is a named constant in the service layer (`STALE_THRESHOLD_MINUTES = 15`).

---

## API Documentation

Interactive Swagger UI:
```
http://localhost:8080/docs
```

Raw OpenAPI JSON spec:
```
http://localhost:8080/docs/openapi
```

---

## Running Tests

```bash
./mvnw test
```

40 tests across two layers. No Docker or database required - all tests run against mocked dependencies.

```
Service layer  (Mockito):   DeviceServiceTest, SiteServiceTest, ReportServiceTest
Controller layer (MockMvc): DeviceControllerTest, SiteControllerTest, DeviceTypeControllerTest
```

Test output shows each `@Nested` suite individually with pass/fail counts.
