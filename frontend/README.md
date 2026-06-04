# Frontend — NetWatch Dashboard

React + TypeScript dashboard for the Network Device Monitoring Service, built with Vite and served via nginx inside Docker.

## Tech Stack

| | |
|---|---|
| Framework | React 19 + TypeScript |
| Build tool | Vite 8 |
| Styling | Pure CSS custom properties (no framework) |
| Routing | React Router DOM 7 |
| HTTP client | Axios |
| Container | nginx:alpine (production) |


## Running with Docker Compose (recommended)

The standard way to run the full stack. No local Node.js installation required.

### Prerequisites

- Docker Desktop (or Docker Engine + Compose plugin)
- Ports `3000`, `8080`, and `5432` free on your machine

### Steps

```bash
# from the repo root
docker-compose up --build
```

Docker Compose starts three services in order:

1. **postgres** — waits until `pg_isready` passes
2. **app** (Spring Boot) — waits until postgres is healthy, then runs Flyway migrations (including demo seed data) and starts on port 8080
3. **frontend** (nginx) — waits until the backend health check passes, then serves the built React app on port 3000

| Service | URL |
|---|---|
| Dashboard | http://localhost:3000 |
| Backend API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/docs |

### Environment variables

The Compose file uses two optional variables with defaults. Create a `.env` file in the **repo root** (next to `docker-compose.yaml`) if you want to override them:

```env
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

The frontend container itself has no environment variables — the nginx proxy handles routing API calls to the backend at build time via `nginx.conf`.

### Stopping

```bash
docker-compose down          # stop and remove containers
docker-compose down -v       # also wipe the postgres volume (fresh DB on next start)
```


## Running locally (dev mode)

Use this when actively developing the frontend. Hot-module replacement (HMR) works out of the box.

### Prerequisites

- Node.js 20+
- The backend running separately on `http://localhost:8080` (see `backend/README.md`)

### Steps

```bash
cd frontend
npm install
npm run dev
```

App available at `http://localhost:5173`.

No environment variables or `.env` file needed. Vite proxies all `/api/` requests to `http://localhost:8080` automatically — CORS is not an issue.

### Available scripts

| Command | Description |
|---|---|
| `npm run dev` | Start dev server with HMR |
| `npm run build` | Type-check and build for production into `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | Run ESLint |


## How the API proxy works

**In Docker** — nginx proxies `/api/` → `http://app:8080/api/` (backend on the internal Docker network):

```nginx
location /api/ {
    proxy_pass http://app:8080/api/;
}
```

**In dev mode** — Vite proxies `/api/` → `http://localhost:8080`:

```ts
// vite.config.ts
server: {
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true }
  }
}
```

In both cases the React code just calls `/api/v1/...` — no hardcoded host, no CORS.


## Project structure

```
frontend/
├── public/               # Static assets
├── src/
│   ├── api/              # Axios API functions (devices, sites, deviceTypes)
│   ├── components/       # UI components (Navbar, DeviceCard, DeviceTable,
│   │                     #   DetailDrawer, RegisterModal, SubmitReportModal,
│   │                     #   SummaryStrip, Toast)
│   ├── pages/            # DevicesPage — main dashboard view
│   ├── types/            # Shared TypeScript interfaces
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css         # Design system (CSS custom properties, light mode)
├── Dockerfile            # Multi-stage: node build → nginx serve
├── nginx.conf            # SPA routing + /api/ proxy
└── vite.config.ts
```

## Features

- **Device grid / table view** — toggle between card grid and sortable table
- **Status filtering** — filter by All / Online / Degraded / Offline / Stale
- **Live search** — search by name, hostname, type, or site
- **Summary strip** — clickable stat cards showing counts per status
- **Detail drawer** — slide-in panel with full device info and 20-report timeline
- **Register device** — modal form with live device type and site dropdowns
- **Submit status report** — submit ONLINE / DEGRADED / OFFLINE reports from the drawer
- **Toast notifications** — auto-dismissing success/error feedback
