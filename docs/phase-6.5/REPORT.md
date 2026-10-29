# Play-Neml — Phase 6.5 Dockerization

Date: 2026-10-29 (Asia/Calcutta). Repository: `kartnagrale/sports-play-v2`. Branch: `feature/dockerization`. Base: `bd710dbc36955f81bea155eea67bfe061f92463d` (sanitized Phases 0–6 integration).

**Status: Phase 6.5 complete; actual containers verified.** Container verification performed no deployment or Phase 7 work. The user subsequently authorized a safe fast-forward publication to main after finalization. CVE-2026-47890 and CVE-2026-47884 remain unresolved production security blockers.

## Implementation

The frontend builds Next.js 16.4.3 / React 19.3.1 using Node 22 and runs standalone output as the non-root node user. The runtime contains traced dependencies and static assets rather than the development dependency tree. Empty public API/WebSocket build settings now deliberately select relative `/api` and `/ws` routes; undefined settings retain the manual-development fallback.

The backend packages Spring Boot 3.5.16 with Maven/Java 17, then runs in a separate Java 17 JRE image as UID/GID 10001. Headless Java, fontconfig and DejaVu fonts support actual photograph processing and PDF/image certificate rendering. Application sources, dependency versions and business rules are unchanged. All five base image references are pinned by digest in the Dockerfiles.

Compose provides PostgreSQL, a one-shot migration job, backend, frontend and an unprivileged nginx gateway. Only the gateway publishes a host port, restricted to loopback. It joins a separate edge network; the application and database networks are internal. Database/backend/frontend are never directly published. Gateway upstreams resolve through Docker DNS so recreated backend addresses can recover without rebuilding the frontend.

Cookies and CSRF remain enforced by Spring Security. The gateway preserves the browser Host and overwrites forwarded headers with trusted operator settings. Exact PUBLIC_ORIGIN applies to CORS and native/SockJS WebSockets. Secure cookies default on; HTTP/false is an explicit disposable loopback override. TLS termination/DNS remain operator responsibilities, documented in [operations](OPERATIONS.md); the future production domain is not assumed deployed.

The app containers run with read-only filesystems, dropped capabilities and no-new-privileges. JVM/frontend memory and temporary files are bounded. Logs rotate, and application file logs live in bounded ephemeral tmpfs. Private photos use a separate project-scoped named volume, writable only by the backend and accessible to browsers only through authorized APIs. No photo directory is served by nginx. PostgreSQL uses its own persistent named volume.

## Database and secrets

The database image includes the tracked complete schema plus exactly the six existing account-security, player-import, player-photo, certificates, certificate-navigation and certificate-batches migrations. Historical SQL bytes are unchanged. Empty PGDATA bootstraps the schema and ledger, then creates a separate application login with DML/schema-usage/sequence permissions. The runtime role has no superuser, database/role creation, application-schema CREATE or migration-ledger modification privileges. Hibernate remains validate-only.

The migration job uses owner credentials, the existing advisory lock and checksum driver. It must complete successfully before the backend starts. Existing databases receive pending migrations rather than schema bootstrap. The optional legacy credential-quarantine migration is deliberately excluded from automatic execution. Owner and application passwords, JWT secret and exact browser origin are required environment inputs; bootstrap and public certificate verification default off. The production image excludes the development local-profile configuration.

The [environment example](../../.env.docker.example) contains no secret values. The [operations guide](OPERATIONS.md) covers bootstrap removal, TLS/proxy settings, checksum failures, Windows line-ending compatibility for existing ledgers, safe upgrades, coordinated password rotation, matched private backups and recovery into new storage. It prohibits volume/prune/reset commands for existing data.

## Verification

Environment: Windows, PowerShell 7.6.5, Docker Desktop 4.87.0 / Engine 29.7.2, Compose 5.4.0, Linux amd64 containers. Runtime versions checked in actual containers: Node 22.23.4, Java 17.0.20.1, PostgreSQL 15.19, nginx 1.30.6. Browser runner: existing Playwright 1.64.1 with installed Microsoft Edge on the host, against the container gateway at localhost:53165.

| Check | Result |
| --- | --- |
| Actual Docker builds | All four images built; frontend production build and Maven packaging succeeded. Digest-pinned rebuild reused the same application layers. |
| Frontend checks | ESLint and TypeScript passed; 18/18 auction reconciliation, refresh and presentation tests passed. |
| Browser workflows | Nine distinct journeys passed: three auction/portal journeys plus six certificate/analytics/scoring journeys. Actual containers handled Excel import, photograph upload, purchases, squad locking, completed scoring, PDF previews/downloads, ZIP batches, verification/revocation, four-role restrictions and reconnect recovery. |
| Container security | Non-root/read-only app containers, internal application/database networks, only the loopback gateway published, no runtime secrets baked into images, restricted DB role, rejected CSRF/foreign origins and authenticated private photos all passed. |
| Migrations | Fresh six-migration initialization, repeated application without duplication, populated earlier-schema upgrade with sentinel preservation and deliberate checksum rejection all passed. The primary ledger remained at six entries. |
| Restart and recreation | Every row in all 43 tables and both stored JPEGs matched the saved hashes after service restart, backend recreation and full Compose down/up. Valid/revoked certificate states and PDF downloads survived each comparison. |
| Backup and restore | Custom pg_dump copied without binary shell redirection, restored into a separate empty database in the isolated PostgreSQL container, and all 43 restored table hashes matched. Both copied photograph hashes matched. |
| Production cookie flag | AUTH_COOKIE_SECURE=true verified Secure, HttpOnly and SameSite=Lax on actual login cookies, with Secure on the CSRF cookie. Transport remained isolated HTTP; real TLS was not tested. |
| Missing secrets | Empty example configuration rejected before container creation. |
| Build contexts | Four actual BuildKit context exports passed private canary/exclusion checks. No env files, runtime credentials, player photos, demos, logs, generated Next output or unnecessary test evidence are admitted. |

The initial browser invocation passed six tests, then failed a certificate assertion hardcoded to the previous demo port. The harness now uses the configured browser origin and evidence directory. All six affected journeys passed on rerun; the previously passing three auction journeys were reused. No application fix or authentication bypass was needed. Raw initial/final logs, synthetic screenshots, accounts and credentials remain under ignored tmp; existing Phase 0–6 evidence was not overwritten. The actual container-generated certificate preview was visually inspected with readable fonts and a QR image.

The initial context probe exposed parent-directory unignore behavior, which admitted unwanted docs/test paths. Explicit intermediate exclusions now restrict the root database context to tracked SQL and four wrapper files, the backend to production sources, and the frontend to build configuration/production source. Context canaries are removed individually after each probe. Images use explicit COPY paths as an additional boundary.

The Windows Invoke-WebRequest readiness poll stalled after a successful restart even though an independent gateway request returned 200 and container health checks passed. Only the identified verification helper was stopped. The poll now uses bounded curl requests, and the remaining checks resumed from the saved snapshot without reinitializing data. The continuation exercised the current runner's comparison, recreation, backup, secure-cookie and shutdown steps. Security probes accept the existing 401/403 CSRF rejection behavior and use a loopback hostname alias to exercise actual cross-origin preflight; same-origin traffic correctly needs no CORS response header. Photo persistence follows the API's versioned photoUrl.

The committed [sanitized verification record](verification.json) contains only counts, versions and results. Raw diagnostic logs and private workflow artifacts remain ignored. No live certificate references, viewer room codes, passwords or access tokens are published as evidence.

Final publication check scanned all 26 staged files against thirteen generated secret/private access values and found zero matches. Git ignore checks confirmed the runtime environment, account state and screenshots are excluded. The original 40 containers and 40 volumes remain present; the two new test data volumes are retained separately.

The Java source/dependency tree is unchanged from the sanitized baseline. The existing [Phase 6 regression](../phase-6/REPORT.md) recorded 96 passed / 3 skipped backend tests; those historical results are reused, not represented as newly executed tests. This phase newly packaged and exercised the Java application inside its container.

## Isolation and publication

Work is in `C:/Users/karti/.codex/worktrees/play-neml-dockerization/sports-play-main`. The original main checkout and its unrelated modifications are preserved. The Dockerization branch starts directly at the requested sanitized commit; the original historical feature commits are not ancestors and remain local recovery history.

The runner uses a random `playneml65-` Compose project, generated private credentials, project-scoped networks and new named volumes. It records pre-existing container/volume identities, never adopts an existing project during initialization, and only controls that test project. Final inventory checks confirmed every pre-existing container and volume remains. The test stack is stopped and its two named data volumes are retained. No existing user/company database was reset, migrated or attached. Private access files, photographs, database backups and raw diagnostic logs are excluded from commits and build contexts.

Final commit: resolve with `git log -1 --format=%H -- docs/phase-6.5/REPORT.md`. Changes are committed on feature/dockerization after final verification. The verification JSON records the pre-publication test state. Subsequent publication is authorized only as a normal fast-forward to the unchanged sanitized main, after checking deployment triggers. The original main checkout remains untouched; all 619 hashes match its historical preservation manifest. GitHub rechecks found no branch protection/rules, webhooks, deployments, environments or Pages site. The sole active workflow runs tests/build/audit and contains no deployment step. The final chat records the verified remote SHA and publication outcome.

## Remaining limits

**CVE-2026-47890 and CVE-2026-47884 remain unresolved Spring MVC production security blockers.** Existing SSE-fragment/XSLT restrictions and [dependency triage](../phase-1/DEPENDENCY-TRIAGE.md) remain in force. No advisory suppression, Boot 4 migration or production-readiness approval occurred.

Local verification does not establish public TLS/DNS operation, production-scale performance, cross-host disaster recovery or image vulnerability clearance. Secure-cookie flags can be tested on the isolated loopback transport; real HTTPS deployment must be verified against the operator's actual TLS proxy. The existing in-process broker and local photo-volume model remain single-host designs. Rehearse real database adoption and matched photo ownership/restore with the database owner before any live rollout. Phase 7 has not begun.
