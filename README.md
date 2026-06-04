# Network Device Monitoring Service

A backend service for registering and monitoring network infrastructure devices. Devices periodically report their operational status, and the system tracks their current state, flags stale devices, and maintains a full report history.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Documentation | SpringDoc OpenAPI (Swagger UI) |
| Containerisation | Docker + Docker Compose |

---

## Database Schema

Five tables, each with a single responsibility:

| Table | Purpose |
|-------|---------|
| `device_types` | Reference table — CPE, Router, Switch, Access Point, Firewall, ONT |
| `sites` | Physical deployment locations |
| `devices` | Registered network assets |
| `reports` | Append-only status event log |
| `current_status` | Current operational state per device (read model) |

---

## Prerequisites

- Java 21
- Maven 3.9+ (or use `./mvnw`)
- PostgreSQL 15 (or Docker)

---

## Running with Docker (Recommended)

Starts both PostgreSQL and the application:

```bash
cp .env.example .env        # fill in your credentials
docker-compose up --build
```

The API will be available at `http://localhost:8080`.

To stop and remove volumes:

```bash
docker-compose down -v
```

---

## Running Locally

**1. Create the database:**

```sql
CREATE DATABASE netdevmon;
```

**2. Configure credentials** (optional — defaults to `postgres/postgres`):

```bash
export DB_USERNAME=your_user
export DB_PASSWORD=your_password
```

**3. Start the application:**

```bash
./mvnw spring-boot:run
```

Flyway will run all migrations and seed the device types on startup.

---

## API Endpoints

### Devices

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/devices` | Register a new device |
| `GET` | `/api/v1/devices` | List all devices with current status and stale flag |
| `GET` | `/api/v1/devices/{id}` | Get a device with its 20 most recent reports |
| `POST` | `/api/v1/devices/{id}/reports` | Submit a status report for a device |

### Sites

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/sites` | Register a site |
| `GET` | `/api/v1/sites` | List all sites |

### Device Types

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/device-types` | List all device types |

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/api-docs
```

---

## Example Requests

**Register a site:**
```json
POST /api/v1/sites
{
  "name": "London-01",
  "address": "1 Tech Street, London"
}
```

**Register a device:**
```json
POST /api/v1/devices
{
  "name": "Core Router 01",
  "deviceTypeId": "<uuid-of-Router>",
  "hostname": "core-rtr-01.london-01.corp",
  "siteId": "<uuid-of-site>"
}
```

**Submit a status report:**
```json
POST /api/v1/devices/{id}/reports
{
  "status": "ONLINE",
  "message": "All interfaces up"
}
```

---

## Stale Detection

A device is considered **stale** if it has not submitted a report within the last **15 minutes**, or if it has never submitted a report. The stale flag is computed at read time and returned on every device response.

---

## Running Tests

```bash
./mvnw test
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/netdevmon` | Full JDBC URL |
