# Universal Sports Auction & Championship Platform

Multi-tenant, sport-agnostic live auctions and tournament management with championship-scoped roles and public viewer rooms.

- **Frontend:** Next.js 14 (App Router, TypeScript, Tailwind) — `/app/frontend`
- **Backend:** Spring Boot 3.2.5 (Java 17, JPA, Spring Security, WebSockets/STOMP) — `/app/backend`
- **Database:** PostgreSQL 15, isolated in the `play_neml` schema

## Quick Start (local machine)

### 1. Prerequisites
```
Java 17 · Maven 3.8+ · Node 18+ / Yarn · PostgreSQL 15
```

### 2. Company PostgreSQL access

The database and `play_neml` schema are company-managed and must already exist. The application never creates a database, schema, user, role, or extension. Obtain the host, database name, username, and password from the company DBA. The supplied account must be restricted to the permissions approved for `play_neml`.

### 3. Backend
```bash
cd backend
export DB_URL='jdbc:postgresql://<company-host>:5432/<company-database>?currentSchema=play_neml'
export DB_USER='<company-provided-user>'
export DB_PASSWORD='<company-provided-password>'
export JWT_SECRET="change-me-256-bit-secret-value-goes-here-please-make-me-very-long"
mvn package
java -jar target/sports-platform-0.0.1-SNAPSHOT.jar
# Backend up on :8001
# Data auto-seeded on first boot
```

In IntelliJ IDEA, add the same values under **Run → Edit Configurations → Environment variables**. These variables are mandatory. Hibernate's configured default schema and every connection's search path remain fixed to `play_neml`.

Verify isolation after startup:

```sql
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'play_neml'
ORDER BY table_name;

SHOW search_path;
SELECT current_schema();
```

### 4. Frontend
```bash
cd frontend
cp .env.local.example .env.local  # points to http://localhost:8001
yarn install
yarn dev
# Frontend on http://localhost:3000
```

## Seeded Credentials

| Role | Email | Password |
|------|-------|----------|
| Super Admin | `admin@sports.local` | `Admin@123` |
| Cricket Admin | `cricket.admin@sports.local` | `Admin@123` |
| Football Admin | `football.admin@sports.local` | `Admin@123` |
| Team Captain | `captain@sports.local` | `Admin@123` |

Viewer rooms: `CRIC-8821 / cricket123` and `FOOT-9922 / football123`. Each demo championship has four teams and 24 players.

## Core API

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/auth/login` | `{email,password}` → `{token,user}` |
| GET | `/api/auth/me` | requires Bearer JWT |
| POST | `/api/championships/join-room` | `{roomCode,passcode}` → scoped viewer JWT |
| GET | `/api/navigation?championshipId={id}` | database-driven screens for the effective role |
| GET | `/api/championships/{id}/{teams\|players\|matches}` | tenant-scoped reads |
| GET | `/api/championships/{id}/auctions/{auctionId}/state` | tenant-scoped auction state |
| POST | `/api/championships/{id}/auctions/{auctionId}/bid` | role- and team-scoped bidding |
| POST | `/api/championships/{id}/auctions/{auctionId}/admin/{action}` | championship admin controls |
| WS | `/ws`, topic `/topic/championship/{id}/auction/{auctionId}` | isolated live auction events |

Navigation is stored in `app_screens` and `role_screen_mappings`. Update a screen's metadata or a mapping's `visible` flag in the database to change the menu without changing frontend code. Seed defaults are only inserted when a row does not already exist, so database customizations survive application restarts.
