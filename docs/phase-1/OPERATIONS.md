# Play-Neml — Phase 1 operations

These are deployment procedures, not evidence of a production deployment. No company database was modified. `play.neml.in` remains a future target with no assumed DNS, TLS or hosting.

## Schema changes

Keep Hibernate `ddl-auto=validate`, SQL auto-init disabled, and runtime database credentials free of DDL privileges. Back up and rehearse a populated installation before deployment. Confirm it already matches the Phase 0 schema; the new runner deliberately does not infer or auto-baseline unknown historical schemas.

For a fresh empty database run `docs/sql/sports-play-complete-schema.sql` once, then `tools/apply-migrations.ps1`. For an existing Phase 0 installation run only the wrapper after schema review. Standard `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` and `.pgpass` or protected `PGPASSWORD` supply the owner connection. Use `-Psql <executable>` if it is not on PATH.

The additive `2026-10-27-phase1-account-security.sql` introduces `users.account_enabled`, `users.credential_version`, and `account_activations` with token hashes, expiry, single-use timestamp and issuance version. Existing users default to enabled with version zero; passwords and memberships are preserved. Old staff JWTs lacking the new version claim must sign in again. Essential navigation now initializes independently of demo users and preserves existing custom labels/visibility.

The runner records version/checksum/time in `play_neml.schema_migrations`, holds a PostgreSQL advisory transaction lock, and commits DDL plus ledger together. Do not edit an applied migration. Add a new migration for later changes. A matching repeat is skipped; a checksum mismatch aborts. Only the reviewed Phase 1 scripts are tracked by this runner; it does not certify the older migrations' execution history.

Docker Compose mounts the same checksum driver through `phase1-init.sh` for **empty volumes only**. Existing volumes require the owner-run wrapper. Compose is a local development configuration: ports bind localhost; local secrets/demo accounts must not be reused in shared deployments. Backend uses Java 17 and the frontend image uses Node 22.

## Legacy plaintext credentials

New provisioning stores no password or setup secret in `message_tracker`. Historical queue entries are not automatically overwritten during application startup or the additive migration.

Before a shared deployment, pause the notification consumer and inventory application-owned queue rows whose JSON contains `temporaryPassword`. Review their affected account IDs with the database owner. Rehearse the following opt-in operation on an isolated copy:

```powershell
./tools/apply-migrations.ps1 -LegacyCredentials
```

This separate migration removes the legacy secret fields from known Play-Neml queue payloads, cancels their pending delivery, disables affected non-super-admin accounts and increments their credential version. It preserves roster/team/auction records and does not touch other applications' messages. A remediation table records affected user IDs without secrets. Malformed historical application JSON causes rollback and requires owner review. Backups and external delivery archives can still contain old credentials and need the organization's retention/access controls.

Distribute fresh setup links to quarantined accounts before restoring their access. A Super Admin can renew setup only for championship admins; a championship admin can renew it only for captains in that championship:

`POST /api/championships/{championshipId}/accounts/{userId}/setup`

An active account cannot be reset through this endpoint. It is not a general password-reset API. Renewing a disabled account invalidates its older setup links. Keep the notification consumer paused until its templates support the new `activationRequired` notice contract; this repository does not operate or verify the external consumer.

## Account onboarding and sessions

Provisioning returns `setupPath` and `setupExpiresAt` once to the authorized administrator. Share that link securely with the intended owner. The server returns a relative `/activate#token=...` link, avoiding dependence on a guessed production host. The token stays in the URL fragment, is removed from browser history on activation-page load, and is submitted in the POST body. The queue contains only a non-secret account notice and instructions to contact the administrator. Automated email/SMS delivery of setup links is not configured.

The default link lifetime is 30 minutes (`ACTIVATION_LIFETIME`, positive and at most 24 hours). Tokens have 256 bits of randomness and only SHA-256 hashes are persisted. Activation atomically checks expiry, issuance version and single use, hashes the chosen password, and enables the account. Passwords need at least 12 characters and at most 72 UTF-8 bytes for BCrypt. Two concurrent activations have one winner. Expired/lost links need renewal; existing active users retain their passwords unless explicitly quarantined above.

Staff login sets only an HttpOnly, SameSite=Lax cookie; JWTs are no longer included in login JSON. Secure defaults to true. `AUTH_COOKIE_SECURE=false` is appropriate only for local HTTP testing. Default JWT lifetime is eight hours (`JWT_EXPIRATION_MS`); logout increments the account version and invalidates all of that staff account's current sessions, including other tabs. Broker delivery revalidates expiry, account version and membership so an existing subscription stops receiving events after revocation. Inbound client SEND is denied for every destination because domain commands use authorized REST APIs.

Viewer tokens remain tab-local, championship-scoped bearer credentials with expiry. Leaving a viewer room clears local state; it does not maintain a server-side per-viewer revocation list. Do not log tokens, activation fragments, password fields or response bodies containing setup links.

Every unsafe HTTP request, including login, activation and room join, requires a CSRF cookie/header pair. GET `/api/auth/csrf` returns the header token and sets its HttpOnly cookie. Refresh that token before mutations because authentication transitions can rotate it. The frontend shares concurrent token fetches and does not cache the token for the life of the tab. SockJS transport is excluded from HTTP CSRF checking; exact origins plus authenticated, read-only STOMP enforcement protect it.

Set `CORS_ORIGINS` to explicit scheme/host/port origins without paths. Wildcards are rejected at startup. Use matching browser API/WebSocket origins and Secure cookies for HTTPS; do not hardcode a future domain. The bounded login/join/activation rate limiter is per process and uses the socket peer address, with 300 attempts per route/peer in 15 minutes by default (`AUTH_RATE_LIMIT_ATTEMPTS`). Account for shared corporate NAT/proxy traffic when setting it. A shared edge limiter and a reviewed trusted-proxy policy are required before scaling or placing many users behind one proxy.

## Repeatable verification

Use Java 17 (`JAVA_HOME`) and Node 22. Backend Maven wrapper pins Maven 3.9.11. Run `backend/mvnw.cmd verify` or `./mvnw verify` from `backend/`. Default runs skip the three PostgreSQL integration cases unless the dedicated database variables below are set; do not report them as passed without executing them.

The test database must be a new disposable PostgreSQL 15 instance, not a company/local user database. The observed run used container `play-neml-phase1-20261027`, label `purpose=play-neml-phase1`, localhost port 55441 and database `phase1`. Bootstrap its schema and apply the tracked migration first. Set `PHASE1_DB_URL=jdbc:postgresql://127.0.0.1:55441/phase1?currentSchema=play_neml` and `PHASE1_DB_PASSWORD` to its test password, then run Maven verification. The integration tests additionally restrict the database URL to that test endpoint.

`tools/verify-phase1-migrations.ps1` creates a new synthetic upgrade database inside the labeled test container and checks upgrade/repeat/checksum/quarantine behavior without dropping existing databases. `tools/verify-phase1-runtime.ps1 -Browser` starts hidden Java/Next test services on 58092/53001, uses a random new bootstrap identity, runs API/STOMP checks and three Playwright tests, and stops only those services. The browser runner currently targets Windows with installed Microsoft Edge; it does not delete worktrees or reset databases. It rebuilds the frontend with test URLs and uses local HTTP cookies deliberately. Production builds must use their intended environment URLs.

Frontend commands are `npm ci`, `npm run lint`, `npm run typecheck`, and `npm run build`. Browser tests use `npx playwright test` through the runtime runner. CI configuration adds Java 17/PostgreSQL and frontend checks, but no remote CI run occurred in this task. Browser coverage is focused on security journeys, not every screen or full tournament progression.
