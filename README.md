# Sports-Play Platform

Sports-Play is a multi-tenant, multi-sport platform for championship provisioning, live player auctions, squad confirmation, league and playoff generation, cricket and badminton scoring, standings, and analytics.

- Frontend: Next.js 14, React, TypeScript, Tailwind
- Backend: Spring Boot 3.2.5, Java 17, Spring Security, JPA, WebSockets
- Database: PostgreSQL 15+, isolated in the `play_neml` schema

## Fastest local setup: Docker

Requirements: Docker Desktop or Docker Engine with the Compose plugin.

```bash
git clone https://github.com/kartnagrale/sports-play-v2.git
cd sports-play-v2
git checkout feature/multi-portal
docker compose up --build
```

Open:

- Frontend: http://localhost:3000
- Backend health: http://localhost:8001/api/health
- PostgreSQL: localhost:5432, database `sports_play`

The first database start runs `docs/sql/sports-play-complete-schema.sql`. The backend waits for PostgreSQL, validates the complete schema, and seeds local demonstration data. Database data persists in the `sports_play_postgres` Docker volume.

To rebuild application images after pulling changes:

```bash
docker compose up --build
```

To stop the stack without deleting data:

```bash
docker compose down
```

To deliberately reset the local database and seed it again:

```bash
docker compose down -v
docker compose up --build
```

## Manual local setup

Requirements: Java 17, Maven 3.8+, Node.js 20+, npm, and PostgreSQL 15+.

### 1. Create and initialize PostgreSQL

Create an empty database and execute the complete schema once:

```bash
createdb -U postgres sports_play
psql -U postgres -d sports_play -f docs/sql/sports-play-complete-schema.sql
```

The application uses Hibernate `validate`; it never creates or alters the application schema at startup.

### 2. Start the backend

The `local` profile defaults to `localhost:5432/sports_play`, user `postgres`, password `password`, and enables demo seeding. Override these values when your local PostgreSQL credentials differ.

PowerShell:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "local"
$env:LOCAL_DB_URL = "jdbc:postgresql://localhost:5432/sports_play?currentSchema=play_neml"
$env:LOCAL_DB_USER = "postgres"
$env:LOCAL_DB_PASSWORD = "your-local-password"
$env:JWT_SECRET = "local-development-secret-at-least-32-characters"
mvn spring-boot:run
```

Bash:

```bash
cd backend
export SPRING_PROFILES_ACTIVE=local
export LOCAL_DB_URL='jdbc:postgresql://localhost:5432/sports_play?currentSchema=play_neml'
export LOCAL_DB_USER=postgres
export LOCAL_DB_PASSWORD='your-local-password'
export JWT_SECRET='local-development-secret-at-least-32-characters'
mvn spring-boot:run
```

The backend listens on http://localhost:8001.

### 3. Start the frontend

In a second terminal:

```bash
cd frontend
cp .env.local.example .env.local
npm ci
npm run dev
```

On Windows Command Prompt, use `copy .env.local.example .env.local`. The frontend listens on http://localhost:3000.

## Local demonstration access

These accounts exist only when `DATA_SEED_ENABLED=true` (the default in the local profile and Docker Compose).

| Role | Email | Password |
|---|---|---|
| Super Admin | `admin@sports.local` | `Admin@123` |
| Cricket Admin | `cricket.admin@sports.local` | `Admin@123` |
| Football Admin | `football.admin@sports.local` | `Admin@123` |
| Team Captain | `captain@sports.local` | `Admin@123` |

Viewer rooms:

- `CRIC-8821` / `cricket123`
- `FOOT-9922` / `football123`

Never enable demonstration seeding in production.

## Verification

Backend tests:

```bash
cd backend
mvn test
```

Frontend production build:

```bash
cd frontend
npm ci
npm run build
```

Docker configuration validation:

```bash
docker compose config
```

## Production configuration

The default backend profile has no embedded database credentials or JWT secret. Supply all secrets through the deployment environment:

```text
DB_URL=jdbc:postgresql://host:5432/database?currentSchema=play_neml
DB_USER=application_user
DB_PASSWORD=strong-secret
JWT_SECRET=long-random-secret-at-least-32-characters
CORS_ORIGINS=https://your-frontend.example
AUTH_COOKIE_SECURE=true
DATA_SEED_ENABLED=false
LOG_PATH=/var/log/sports-play
```

Initialize a new production database with `docs/sql/sports-play-complete-schema.sql`, or apply the dated scripts under `docs/sql/` in order when upgrading an existing installation. Database ownership and privileges remain the operator's responsibility.

Frontend build-time variables:

```text
NEXT_PUBLIC_BACKEND_URL=https://api.example.com
NEXT_PUBLIC_WS_URL=https://api.example.com
```

## Repository layout

```text
backend/                 Spring Boot API and tests
frontend/                Next.js application
docs/sql/                Fresh schema and incremental SQL migrations
docs/architecture/       Architecture assets and complete user guide
docker-compose.yml       Reproducible local stack
```

## Documentation

- `docs/RUNBOOK.md` - operational presentation flow and database rollout notes
- `docs/LOCAL_POSTGRESQL.md` - local PostgreSQL details
- `docs/architecture/Sports-Play-Platform-Complete-User-Guide.pdf` - screen-by-screen user guide
- `DEPLOYMENT.md` - Docker deployment notes

## Core API areas

- Authentication and viewer-room access: `/api/auth`, `/api/championships/join-room`
- Championship navigation and lifecycle: `/api/navigation`, `/api/championships/{id}`
- Teams, players, auction, and squads: `/api/championships/{id}/...`
- Competition configuration, fixtures, playoffs, and audit: `/api/championships/{id}/competition/...`
- Cricket scoring: `/api/championships/{id}/cricket/...`
- Real-time events: `/ws` with championship-scoped topics

All tenant data and live topics are championship-scoped.
