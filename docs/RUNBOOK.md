# Play-Neml fresh-environment runbook

All database work is restricted to `play_neml`. The application uses Hibernate `validate`; it does not create or alter tables.

## 1. Database owner step

For an empty database, run `docs/sql/sports-play-complete-schema.sql` once as its owner, then run `tools/apply-migrations.ps1` with standard PostgreSQL connection environment variables. Docker Compose performs both steps only when its volume is empty.

For a populated database, inspect its actual schema first and apply only the reviewed Phase 1 migration through that wrapper. Never replay the full schema or historical constraint scripts blindly. The wrapper uses a transactional checksum ledger and an advisory lock; repeated applications are skipped and checksum mismatches fail. See [Phase 1 operations](phase-1/OPERATIONS.md), including the opt-in legacy credential quarantine procedure.

## 2. First super-admin bootstrap

Bootstrap is opt-in and inserts only the missing super-admin user. In PowerShell, set strong values for the first start:

```powershell
$env:APP_BOOTSTRAP_ENABLED = "true"
$env:APP_BOOTSTRAP_EMAIL = "super.admin@your-company.com"
$env:APP_BOOTSTRAP_PASSWORD = "replace-with-a-strong-unique-password"
$env:APP_BOOTSTRAP_FULL_NAME = "Platform Super Admin"
```

After the first successful start, stop the backend and remove these variables before restarting:

```powershell
Remove-Item Env:APP_BOOTSTRAP_ENABLED, Env:APP_BOOTSTRAP_EMAIL, Env:APP_BOOTSTRAP_PASSWORD, Env:APP_BOOTSTRAP_FULL_NAME
```

## 3. Start backend and frontend

Provide `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, and explicit production `CORS_ORIGINS` through the environment. Then:

```powershell
cd backend
mvn spring-boot:run
```

In a second terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`.

## 4. Presentation flow

1. Log in as the bootstrapped super admin.
2. Create a championship from **Launch Championship**. An expiring administrator setup link is shown once for secure handoff; only a non-secret notice is queued in `play_neml.message_tracker`.
3. Log out and sign in as that championship admin.
4. Configure purse, squad composition, player base price, bid increment, and timer.
5. Create at least two teams/captains. Share each captain’s expiring setup link securely; the queue contains no password or setup token.
6. Add players and their auction order.
7. Open separate browser tabs/profiles for captains. Championship and auction context are isolated per tab.
8. Start the auction; captains can bid only for their assigned team.
9. Complete the auction, then show Teams and Auction History.
10. Create and score matches; Scoreboard and analytics update within that championship only.

Do not enable `DATA_SEED_ENABLED` against company or production databases.
