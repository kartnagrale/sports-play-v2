# Universal Sports Auction & Championship Platform

Multi-tenant, sport-agnostic live auctions and tournament management with championship-scoped roles and public viewer rooms.

- **Frontend:** Next.js 14 (App Router, TypeScript, Tailwind) — `/app/frontend`
- **Backend:** Spring Boot 3.2.5 (Java 17, JPA, Spring Security, WebSockets/STOMP) — `/app/backend`
- **Database:** PostgreSQL 15 in production; in-memory H2 by default for local development

## Quick Start (local machine)

### 1. Prerequisites
```
Java 17 · Maven 3.8+ · Node 18+ / Yarn · PostgreSQL 15
```

### 2. Backend
```bash
cd backend
# Optional production-style database configuration:
# export DB_URL=jdbc:postgresql://localhost:5432/sports_platform
# export DB_USER=postgres
# export DB_PASSWORD=postgres
export JWT_SECRET="change-me-256-bit-secret-value-goes-here-please-make-me-very-long"
mvn package
java -jar target/sports-platform-0.0.1-SNAPSHOT.jar
# Backend up on :8001
# Data auto-seeded on first boot
```

### 3. Frontend
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
