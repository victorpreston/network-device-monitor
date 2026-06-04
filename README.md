# Network Device Monitoring Service

A full-stack service for registering and monitoring network infrastructure assets. Devices report their operational status; the system tracks current state, flags stale devices, and maintains a full report history.

For all architectural and design decisions - schema choices, API design, testing strategy, infrastructure - see [DECISIONS.md](./DECISIONS.md).


## Quick Start

The entire stack - database, backend, and frontend - runs with one command from the repo root:

```bash
docker-compose up --build
```

| Service | URL |
|---|---|
| Dashboard | http://localhost:3000 |
| Backend API | http://localhost:8080/api/v1 |
| Swagger UI (API docs) | http://localhost:8080/docs |

No environment variables or configuration are required. The defaults (`DB_USERNAME=postgres`, `DB_PASSWORD=postgres`) are built into the Compose file. Flyway runs automatically on startup - it creates the schema and seeds demo devices covering all four statuses (Online, Degraded, Offline, Stale).

To stop and wipe the database:

```bash
docker-compose down -v
```

## Why `docker-compose up` is all you need

Three services start in a dependency chain, each waiting for the previous one to be healthy before starting:

```
postgres  ──(healthy)──▶  app (Spring Boot)  ──(healthy)──▶  frontend (nginx)
```

**API calls from the browser never hit a CORS issue** because the frontend is not making cross-origin requests. nginx - which serves the React app on port 3000 - also acts as a reverse proxy: any request the browser makes to `/api/...` is forwarded internally to `http://app:8080/api/...` on the Docker network. From the browser's perspective, everything is on the same origin (`localhost:3000`).

```
Browser → localhost:3000/api/v1/devices
                 │
            nginx proxy
                 │
         app:8080/api/v1/devices   (Docker internal network)
```

The React source code only ever references `/api/v1/...` - no hardcoded host or port.


## Repo Structure

```
network-device-monitoring/
├── backend/                  Spring Boot REST API
│   ├── src/
│   ├── pom.xml
│   ├── Dockerfile
│   └── README.md             ← API endpoints, env vars, running tests
├── frontend/                 React + TypeScript dashboard
│   ├── src/
│   ├── nginx.conf
│   ├── Dockerfile
│   └── README.md             ← dev server setup, proxy explanation, project structure
├── docker-compose.yaml       Runs the full stack
├── DECISIONS.md              Architecture and design decisions (start here)
└── README.md                 This file
```

## Running without Docker

For local development (e.g. running the backend in an IDE while hot-reloading the frontend):

- **[backend/README.md](./backend/README.md)** - prerequisites, environment variables, running the API, running the 40-test suite
- **[frontend/README.md](./frontend/README.md)** - `npm install`, `npm run dev`, how the Vite dev proxy replaces nginx for local development


## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21 · Spring Boot 3.3.5 · PostgreSQL 15 · Flyway |
| Frontend | React 19 · TypeScript · Vite · Pure CSS (custom properties) |
| Infrastructure | Docker · Docker Compose · nginx |
| Testing | JUnit 5 · Mockito · MockMvc · 40 tests |
