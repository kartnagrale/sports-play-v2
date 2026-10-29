# Play-Neml container operations

Phase 6.5 uses Next.js 16 / Node 22, Spring Boot 3.5 / Java 17 and PostgreSQL 15. **CVE-2026-47890 and CVE-2026-47884 remain unresolved production security blockers.** These instructions describe the container contract; they do not approve a production release. Phase 7 has not begun.

## Configuration and startup

Install Docker Engine/Desktop with Linux containers and Compose v2.20+ (tested with the versions in REPORT.md). PowerShell examples and the runner require PowerShell 7. Run commands from the repository root. Keep a protected environment file outside the repository. Copy [.env.docker.example](../../.env.docker.example) there, fill every required secret, and select a unique lowercase project name. The project controls image, network and volume names; using another application's project name can reconnect to its data.

Use independent cryptographically random owner and application database passwords and a JWT secret of at least 64 random characters. A PowerShell generator is `[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`; do not paste generated secrets into commits, tickets or reports. Environment values are visible to administrators through Docker inspect; protect the Docker socket and host. Do not print expanded `docker compose config` or attach raw logs/access files as evidence.

```powershell
$config = 'C:/private/playneml.env'
$project = 'playneml-staging' # Match COMPOSE_PROJECT_NAME in the private file.
docker compose --env-file $config -p $project config --quiet
docker compose --env-file $config -p $project build --pull
docker compose --env-file $config -p $project up -d --wait --wait-timeout 240
docker compose --env-file $config -p $project ps
```

Fresh installations require `APP_BOOTSTRAP_ENABLED=true`, a supplied email and a strong password (minimum twelve characters). After the first sign-in, set it to false, remove bootstrap credentials from the environment file and recreate the backend. Bootstrap is idempotent and does not change an existing password. Demo seeding remains disabled.

Only the gateway publishes a port, on `127.0.0.1:HTTP_PORT`. PostgreSQL, backend and frontend have no host ports. The database and application networks are internal. Backend and frontend run as non-root users with read-only filesystems, dropped capabilities and bounded temporary/log space. The backend's only persistent write mount is the private photo volume. Container stdout logs rotate at 3 × 10 MiB per service; application rolling files are ephemeral and bounded by a 64 MiB tmpfs.

## Browser origin, TLS and WebSocket contract

Place a trusted host TLS reverse proxy in front of the loopback gateway. Configure actual DNS/certificates separately. `https://play.neml.in` is a future target, not a deployed domain. Set `PUBLIC_ORIGIN` to the exact externally visible origin, `PUBLIC_SCHEME=https` and `AUTH_COOKIE_SECURE=true`. Forward the original Host to the gateway and support HTTP/1.1 WebSocket Upgrade, a read timeout above the ten-second heartbeat, and uploads up to 6 MiB. Never expose the loopback HTTP listener directly as a public endpoint.

The gateway overwrites forwarded host/protocol/client headers and strips Forwarded. PUBLIC_SCHEME is an operator-controlled fixed value. Do not accept a caller-supplied protocol header. A proxy on another machine/container needs an explicitly reviewed private-network connection; the default listener only accepts host loopback traffic.

Browser builds use relative `/api` and `/ws` URLs. A single image works across origins without rebuilding public URLs. Container DNS names never enter browser API URLs. CORS and both native/SockJS WebSockets use the exact PUBLIC_ORIGIN allowlist. Existing cookie authentication, HttpOnly/SameSite=Lax cookies, per-mutation CSRF tokens, viewer bearer authentication, tenant authorization and revocation checks remain enabled. Photos are served only through authorized API routes; no storage directory is exposed by nginx.

For an explicitly disposable HTTP loopback test only, set PUBLIC_ORIGIN to `http://localhost:53165`, PUBLIC_SCHEME=http and AUTH_COOKIE_SECURE=false. Return to secure cookies for TLS. The supplied verification runner makes this override only in its newly generated private test configuration.

Public certificate verification defaults off. To enable deliberately, set CERTIFICATES_PUBLIC_ENABLED=true and CERTIFICATES_PUBLIC_BASE_URL to the trusted externally visible origin. URLs/QR codes are persisted at issuance; choose the correct base before issuing certificates. The certificate API/PDF engine runs in the Java container with installed fonts and headless Java.

## Database initialization and upgrades

The database image contains only the tracked complete schema, the six existing migrations, their checksum driver and the Docker wrapper/grants scripts. It contains no demo accounts/data or legacy password-reset migration. Fresh PGDATA bootstraps the complete schema, then applies account-security, player-import, player-photo, certificates, certificate-navigation and certificate-batches migrations in that order. Historical SQL bytes are unchanged.

Initialization creates a separate login role for the application with schema USAGE and table DML/sequence access, without superuser, database creation, role creation, schema creation or migration-ledger access. Hibernate only validates the schema. The migration job uses the owner credentials; the application receives only its restricted credentials.

On each new Compose startup the one-shot migrate service validates tracked checksums under the existing advisory lock and applies pending migrations before backend startup. A successful job exits zero; it is expected to appear exited in `compose ps -a`. A checksum mismatch blocks the job/backend. Never edit recorded checksums, delete the ledger, or disable Hibernate validation to force startup.

For an upgrade, first stop writes, back up the database and photos together, review new migrations and build the new images. Then:

```powershell
docker compose --env-file $config -p $project stop gateway frontend backend
docker compose --env-file $config -p $project up -d --wait db
docker compose --env-file $config -p $project run --rm --no-deps migrate
if ($LASTEXITCODE -ne 0) { throw 'Migration failed; keep application stopped' }
docker compose --env-file $config -p $project up -d --wait --wait-timeout 240
```

The bootstrap is for empty volumes only. To adopt an existing database, rehearse a backup restore into a NEW project first and have the owner review schema/migration history and create/grant the application role. Do not mount an existing company volume into this setup without that review. Existing Windows-created migrations may have CRLF checksums: preserve the exact historical bytes matching their ledger when adopting an existing database. A mismatch is a stop condition, not permission to rewrite history.

Changing a password in the env file does not rotate an existing PostgreSQL role. Perform owner-reviewed role password rotation and coordinate the application configuration; init scripts intentionally do not reset existing passwords. Keep the JWT secret stable across routine restarts; rotating it invalidates sessions.

## Persistence, backup and recovery

Project-scoped `postgres_data` and `player_photos` named volumes survive restart, recreate and `down`. They are never host source directories. Retain and back up both: database rows reference immutable photo keys; certificate snapshots and import/auction/scoring state are stored in PostgreSQL, and PDFs are rendered from those snapshots. Capture a matched backup with writers stopped.

```powershell
docker compose --env-file $config -p $project stop gateway frontend backend
# Write inside the DB container, then docker cp; avoid binary shell redirection on Windows.
$backupDirectory = Join-Path 'C:/private/backups' ('playneml-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $backupDirectory -ErrorAction Stop | Out-Null
$dumpName = 'playneml-' + [guid]::NewGuid().ToString('N') + '.dump'
docker compose --env-file $config -p $project exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f "$1"' sh "/tmp/$dumpName"
$db = docker compose --env-file $config -p $project ps -q db
docker cp "${db}:/tmp/$dumpName" (Join-Path $backupDirectory 'database.dump')
# The stopped backend container retains its named-volume mount.
$backend = docker compose --env-file $config -p $project ps -aq backend
docker cp "${backend}:/app/var/player-photos" (Join-Path $backupDirectory 'player-photos')
docker compose --env-file $config -p $project up -d --wait --wait-timeout 240
```

Protect backups as private data. Test restoration only into a NEW project with new volumes: start its database alone, restore using `pg_restore --no-owner --no-privileges` into a separate empty database as its owner, review the ledger, run grants/migrations, then restore the matching photos with UID/GID 10001 ownership. Rehearse with a database administrator before replacing any live installation. Never restore over existing user data or mix independent database/photo snapshots.

Safe routine controls:

```powershell
docker compose --env-file $config -p $project restart backend
docker compose --env-file $config -p $project stop
docker compose --env-file $config -p $project start
docker compose --env-file $config -p $project down # Preserves named volumes.
```

Do not use `down -v`, `docker volume prune`, `docker system prune`, blanket container removal, or a shared project name. The verification runner only stops its own project and retains its test volumes. Capture diagnostic logs privately; do not publish tokens, room codes, account files, photos or unrestricted request evidence.

## Reproducible isolated verification

On Windows with Node 22/npm 10 and installed Edge:

```powershell
./tools/verify-dockerization.ps1
```

The runner creates a random, previously nonexistent Compose project and private ignored state, builds/runs actual containers, and exercises existing browser workflows plus security/persistence checks. A repeat invocation resumes that same explicitly labeled test project; use a separate worktree for an independent environment. Test artifacts and raw logs stay under ignored tmp. See REPORT.md for the actual verification achieved in this run.

Production rollout remains blocked by both documented CVEs and still requires the separately authorized Phase 7 verification. No push, merge or deployment is part of this phase.

The frontend follows [Next.js standalone deployment](https://nextjs.org/docs/app/getting-started/deploying) and [self-hosting proxy guidance](https://nextjs.org/docs/app/guides/self-hosting). Network separation follows [Docker Compose networking](https://docs.docker.com/compose/how-tos/networking/). Base image digests are pinned to this verified build; update them deliberately, rebuild and repeat verification when adopting security patches. A digest pin is not a vulnerability clearance.
