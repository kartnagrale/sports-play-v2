# Play-Neml

Sports-Play is a multi-tenant, multi-sport platform for championship provisioning, live player auctions, squad confirmation, league and playoff generation, cricket and badminton scoring, standings, and analytics.

- Frontend: Next.js 16, React 19, Node 22, TypeScript, Tailwind
- Backend: Spring Boot 3.5, Java 17, Spring Security, JPA, WebSockets
- Database: PostgreSQL 15+, isolated in the `play_neml` schema

## Docker setup (Phase 6.5)

Use Linux containers in Docker Desktop/Engine with Compose. Follow the [container operations guide](docs/phase-6.5/OPERATIONS.md) to create a protected external configuration from [.env.docker.example](.env.docker.example), supply independent random secrets, configure the exact browser origin/TLS settings and bootstrap the initial administrator.

```powershell
$config = 'C:/private/playneml.env'
$project = 'playneml-staging'
docker compose --env-file $config -p $project config --quiet
docker compose --env-file $config -p $project build --pull
docker compose --env-file $config -p $project up -d --wait --wait-timeout 240
```

Only the loopback gateway publishes a port (8080 by default), behind your trusted TLS proxy. Browser API/WebSocket calls share that origin. The database and private photo volumes persist across restart, recreation and down. Demo seeding is disabled; all Docker credentials must be supplied.

Run ./tools/verify-dockerization.ps1 for isolated verification. See the operations guide for safe commands, upgrades, matched database/photo backups and recovery. Never remove volumes or prune existing data.

**CVE-2026-47890 and CVE-2026-47884 remain unresolved production security blockers.** Containerization is not production deployment approval; Phase 7 has not begun.

## Manual local setup

Requirements: Java 17, Maven 3.9.11 (wrapper included), Node.js 22, npm 10, and PostgreSQL 15+.

### 1. Create and initialize PostgreSQL

Create an empty database and execute the complete schema once:

```bash
createdb -U postgres sports_play
psql -U postgres -d sports_play -f docs/sql/sports-play-complete-schema.sql
```

The application uses Hibernate `validate`; it never creates or alters the application schema at startup. After the empty bootstrap, run `tools/apply-migrations.ps1` as the database owner. Essential navigation is independent of demo data. See [Phase 1 operations](docs/phase-1/OPERATIONS.md) for populated upgrades, onboarding and security settings.

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

These legacy accounts apply only to the manual local profile with demo seeding. Docker disables seeding and uses supplied bootstrap credentials. See the separately retained [Phase 6 demo runbook](docs/phase-6/DEMO-RUNBOOK.md) for its isolated environment.

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
docker compose --env-file <private-config-file> -p <unique-project> config --quiet
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
