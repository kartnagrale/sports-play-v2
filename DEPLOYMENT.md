# Docker Deployment Guide

The repository includes working Dockerfiles and a Compose stack for local development. A fresh clone can be started with:

```bash
docker compose up --build
```

See `README.md` for the local URLs, seeded accounts, database reset command, and manual setup.

## Production requirements

- Linux server with Docker Engine and the Compose plugin
- PostgreSQL 15+ (managed service recommended)
- TLS termination through a reverse proxy or load balancer
- DNS names for the frontend and backend
- Secrets supplied by the deployment environment, never committed to Git

Required backend variables:

```text
DB_URL=jdbc:postgresql://host:5432/database?currentSchema=play_neml
DB_USER=application_user
DB_PASSWORD=strong-secret
JWT_SECRET=long-random-secret-at-least-32-characters
CORS_ORIGINS=https://app.example.com
AUTH_COOKIE_SECURE=true
DATA_SEED_ENABLED=false
LOG_PATH=/app/logs
```

Frontend build arguments:

```text
NEXT_PUBLIC_BACKEND_URL=https://api.example.com
NEXT_PUBLIC_WS_URL=https://api.example.com
```

The `NEXT_PUBLIC_*` values are embedded during `npm run build`; rebuild the frontend image when they change.

## Database initialization

For a new empty database, a database owner executes:

```bash
psql "$DATABASE_URL" -f docs/sql/sports-play-complete-schema.sql
```

For an existing database, review and apply the dated scripts under `docs/sql/` in chronological order. The application runs Hibernate in `validate` mode and will not create or migrate tables.

## Image builds

```bash
docker build -t sports-play-backend ./backend
docker build \
  --build-arg NEXT_PUBLIC_BACKEND_URL=https://api.example.com \
  --build-arg NEXT_PUBLIC_WS_URL=https://api.example.com \
  -t sports-play-frontend ./frontend
```

The backend image exposes port 8001. The frontend image exposes port 3000.

## Production Compose or orchestrator

Use `docker-compose.yml` as a local reference, not as a production secret store. In production:

1. Replace the local PostgreSQL service with the approved managed database or a separately secured database service.
2. Inject backend variables from a secret manager.
3. Build the frontend with public API/WebSocket URLs reachable by the user's browser.
4. Persist logs and database backups outside disposable containers.
5. Terminate HTTPS before the services and keep `AUTH_COOKIE_SECURE=true`.
6. Keep `DATA_SEED_ENABLED=false`.

## Verification

```bash
curl -f https://api.example.com/api/health
```

Then open the frontend, authenticate, verify the active championship selector, and confirm WebSocket-driven auction or match updates.

## Updates

```bash
git pull --ff-only
docker compose build --pull
docker compose up -d
```

Review database migration scripts before deploying application changes that introduce new entities or columns.
