# Play-Neml — Phase 1 implementation report

Date: 2026-10-27 (Asia/Calcutta). Branch: `feature/play-neml-upgrade`. Starting audit commit: `5963f77`; original main baseline: `636f30d11ae50b300f32d96f6a901c908dfb2433`.

See [finalization summary](SUMMARY.md) for the concise final status. Finalization reused the successful 57-test backend and 83-check runtime evidence and added a targeted wrong-signing-key JWT regression; no application implementation changed after those full verification runs.

**Phase 1 Status: PARTIALLY COMPLETE.** The confirmed defects and the implemented security flows pass the verification below. The dependency backlog remains open because two CRITICAL-labeled Spring MVC advisory matches require a compatible upstream backport or a tested Boot 4/Spring 7 migration. They concern features not configured by this application; no exploit of those dependencies was demonstrated. This report does not silently accept them or declare production readiness.

No work was pushed or merged. Main and the original checkout's unrelated modifications were preserved. No existing user/company database was changed. Phase 2, Excel import and certificates were not started.

## Implemented security fixes

| Finding / root cause | Implementation | Verification |
|---|---|---|
| HIGH: anonymous STOMP clients could forge server events because inbound SEND was unchecked | Deny every client SEND, including `/topic`, `/queue`, `/app` and unknown destinations. There are no legitimate client message handlers; domain commands remain tenant-authorized REST requests. Require authenticated CONNECT and exact tenant/auction subscriptions; preserve cookie staff and bearer viewer connections. | Anonymous CONNECT and pre-connect SEND, viewer/staff publication and cross-tenant subscriptions rejected in live tests. Authorized subscribers receive committed auction broadcasts. |
| MEDIUM: viewer GET `/api/championships` dereferenced a null `User` | Staff-list method accepts only global USER/SUPER_ADMIN roles; service additionally rejects a missing user. Viewer access uses the tenant-qualified route and tab context. | Viewer list returns controlled 403; own-room reads/reloads work, foreign access fails; admin/captain/Super Admin reads retain their intended scope. |
| HIGH code-confirmed risk: recoverable temporary passwords were stored in queued JSON and returned during provisioning | Disabled accounts receive a 256-bit, 30-minute default setup token. Only its SHA-256 hash, expiry, issuance version and single-use timestamp are persisted. The authorized provisioner gets a one-time fragment link; the notice queue gets no password or setup token. Password setup hashes the owner's password and enables the account atomically. | Hash-only database assertions; invalid/expired/replayed/superseded links rejected; two concurrent activations have one winner; real browser activates and signs in. New application-owned queue notices contain no credential fields. |
| Staff logout did not invalidate previously issued JWTs | JWT credential-version claim checked against enabled account state; logout increments that version under a row lock. Login JSON contains user data only; JWT stays in HttpOnly cookie. | Signed expired and invalid JWTs rejected. Logged-out token replay fails and an existing socket stops receiving later server events. |
| Cookie auth had no CSRF protection and wildcard credentialed origins were configurable | Enable CSRF cookie/header validation for unsafe HTTP operations, including login/activation/join. Refresh the client token across authentication changes. Validate explicit HTTP(S) origins at startup and use them for HTTP and WebSockets. Secure, HttpOnly, SameSite=Lax cookie defaults retained. | Missing/forged CSRF rejected, exact allowed origin accepted, foreign HTTP/WebSocket origins rejected, wildcard/path/user-info origin configurations rejected. |
| Essential navigation depended on demo seed accounts | Independent idempotent reference initializer adds missing screens/mappings, including competition, without overwriting existing labels/visibility. Demo seeding remains optional. | Fresh application with demos disabled has platform and tenant menus. Database tests preserve custom reference data on repeat initialization. |

Setup renewal is role- and tenant-restricted, rejects active accounts, and invalidates older unused setup links through an issuance version. Passwords require at least 12 characters and at most 72 UTF-8 bytes. Provisioning no longer gives anyone a usable initial password. Setup expiry is shown from the server-provided timestamp. Manual secure handoff is the supported onboarding path; automated delivery of setup links is not configured.

The new per-process login/join/activation limiter defaults to 300 attempts per peer/route in 15 minutes and bounds its in-memory map. It needs deployment-specific tuning and a shared edge/proxy policy before scaling. Outbound authorization adds database reads to preserve revocation correctness; high-fanout performance has not been certified.

## Dependencies and framework compatibility

| Area | Phase 0 | Phase 1 |
|---|---|---|
| Full npm audit | 11 affected package entries: 1 critical, 8 high, 2 moderate | **0** |
| npm audit excluding dev | 5 affected entries: 1 critical, 4 high | **0** |
| Maven OSV scan | Matches in 20/105 dependencies; 94 distinct advisory IDs | Matches in **1/105** dependencies; **2** advisory IDs |
| Backend | Boot 3.2.5; Tomcat 10.1.20; Jackson 2.15.4; JDBC 42.6.2; Log4j 2.21.1 | Boot **3.5.16**; Tomcat **10.1.61**; aligned Jackson BOM **2.21.8**; JDBC **42.7.14**; aligned Log4j **2.25.5** |
| Frontend | Next/config 14.2.15; React/DOM 18.3.1; vulnerable Axios 1.x; ESLint 8.57.1 | Next/config **16.4.3**; React/DOM **19.3.1**; Axios **1.21.0**; ESLint **9.39.5** |
| Tooling | Unpinned local Maven; Node 20 Docker image | Maven wrapper **3.9.11**, Java **17**, Node **22** Docker/engine family; executed frontend checks on Node **22.20.0**, npm **10.9.3** |

The Next migration was evaluated against actual usage: no middleware, server actions or server cookie/route-parameter APIs required conversion. The build explicitly uses webpack; ESLint's flat CLI replaces removed `next lint`. React hooks/ref issues exposed by the newer rules were corrected without disabling rules. Compatible transitive npm packages were updated deliberately and the lockfile regenerated; no force audit-fix command was used. Boot remains in its compatible Java 17/Jackson 2/Hibernate 6 family with aligned maintenance overrides, rather than mixing Spring 7 jars into Boot 3.

Remaining package matches are `spring-webmvc:6.2.19`:

- `GHSA-j9f9-w8pj-32f8` / `CVE-2026-47890`: SSE fragment rendering.
- `GHSA-pc63-qcmh-9cmg` / `CVE-2026-47884`: XSLT view path handling.

Both carry CRITICAL advisory metadata. Source/configuration review found neither affected feature in use. Keep those features disabled; complete a supported compatible fix or a Boot 4 migration before closing the backlog. The published fix identified by the advisories is Spring 7.0.9; no Spring 6.2 release newer than 6.2.19 was found in Maven Central during this work. Package matching does not establish application exploitability. See [dependency triage](DEPENDENCY-TRIAGE.md) for baseline IDs, fixed package paths, conditions and primary advisory links.

Registry output also labels ESLint 9 deprecated. It currently has no npm audit finding, but ESLint 10 compatibility remains toolchain maintenance. Maven plugins, JDK/container OS and external delivery systems were not vulnerability-scanned.

## Database changes and compatibility

The additive account migration introduces enabled/version fields and `account_activations`. Existing passwords and memberships survive; old staff JWTs without a version claim require sign-in again. Hibernate remains in validation mode. Essential reference initialization performs inserts only for missing reference records and preserves existing customization.

`tools/apply-migrations.ps1` and the shared psql driver implement an owner-run, checksummed transaction ledger with an advisory lock. The same driver runs during empty Docker-volume initialization. Existing volumes require the reviewed owner step; there is no automatic baseline of unknown schemas.

The separate **opt-in** legacy migration removes old password fields only from application-owned queue JSON, cancels pending delivery and quarantines affected non-super-admin accounts until they receive new setup links. Its repeat is skipped by the ledger. Unrelated applications' messages, users, rosters, sold status and purse/version values were preserved in a synthetic populated upgrade. A mismatch checksum rejects the transaction. This migration was never applied to an existing user database. Historical backups and external notification archives remain an operational credential-retention concern.

See [operations](OPERATIONS.md) for ordering, queue-consumer coordination, recovery endpoints, local/HTTPS settings and exact verification commands. The external notification consumer must support the new account-notice contract before rollout; it was not connected or tested here.

## Frontend corrections

The original six lint warnings are resolved. Settings/team/player requests use stable dependencies, explicit tenant URLs and cancellation/stale-tenant guards. Navigation effects depend on stable store members. Refs update outside render; derived bid values reset when their minimum changes. Cricket's server-suggested batters reconcile at the response boundary only when the suggestion changes. Loading flags update on async completion instead of redundant synchronous effect resets. Theme hydration uses an external-store snapshot. Next-managed fonts preserve the existing font families while removing the custom-font warning. Super Admin no longer appears able to edit tenant players.

Changes support the new security flows and framework version; no premium design system or portal redesign was implemented. Player CRUD business logic remains in its controller pending the planned shared service before Phase 4.

## Executed verification

| Check | Actual result | Evidence |
|---|---|---|
| Java 17 wrapper/version | Maven 3.9.11, Microsoft Java 17.0.20.1 | `evidence/maven-java17-version.log` |
| Final Maven verify/package with dedicated PostgreSQL | **57 passed, 0 failures/errors/skips**, packaged JAR | `evidence/backend-database-verify.log` |
| Original backend tests | All original 32 remain passing within that 57 | Same log and existing test classes |
| Final test-only JWT/STOMP verification | **8 passed**, including untrusted signing-key rejection before account lookup | `evidence/final-targeted-security-tests.log` |
| PostgreSQL integration | Concurrent activation, renewal invalidation, reference-data idempotency/customization passed | Same log, `DatabaseSecurityIntegrationTest` |
| Targeted API/STOMP suite | **83/83 passed**, including both Phase 0 failures | `evidence/runtime-security.json` |
| Browser security journeys | **3/3 passed**, installed Edge, headless | `evidence/browser-tests.log` |
| Clean frontend install | `npm ci` succeeded from regenerated lockfile | `evidence/npm-ci.log` |
| Frontend lint | Passed with **0 warnings** | `evidence/frontend-lint-final.log` |
| TypeScript | Passed | `evidence/frontend-typecheck-final.log`; production build also checks types |
| Production build and startup | Passed with test API/WS build URLs; application pages served | `evidence/frontend-runtime-build.log`, frontend runtime logs |
| Backend startup | Validated migrated schema; fresh synthetic bootstrap; demos disabled | `evidence/backend-runtime.log` |
| Fresh PostgreSQL Docker initialization | Full schema plus shared tracked migration succeeded | `evidence/docker-fresh-init.log`, startup log |
| Populated upgrade/repeat/quarantine/checksum rejection | Passed in a new synthetic database | `evidence/migration-validation.log` and migration logs |
| Compose configuration | `docker compose config --quiet` passed | `evidence/compose-validation.log` |
| Dependency audits | npm zero; OSV two remaining contextual matches | Final audit JSON files |
| Credential persistence/log review | New queue contains no secret fields; token hashes have expected format; tested passwords/setup tokens/JWTs absent from runtime log | Runtime result, `NotificationSecrecyTest`, database validation log |

New automated coverage includes the inbound/outbound STOMP boundary, viewer principal handling, browser cookie/CSRF/origin behavior, activation races/replay/renewal, JWT revocation/expiry, queued notice contents, rate limiting, after-commit broadcasts and rollback suppression. Live checks exercise captain ownership, simultaneous equal bids with one winner, sale/purse consistency, cross-tenant denials, server broadcasts and delivery suppression after logout. Browser checks cover staff login, password setup/sign-in, viewer join/reload and anonymous socket rejection.

The old ad hoc 50-check result was not reused as new evidence. The new 83-check suite is versioned. Coverage still does not establish every cricket/badminton tournament, correction/playoff path, full mobile/accessibility behavior or production load. CI was configured but not remotely executed. Full backend/frontend container-image builds, image OS scans, DNS/TLS, hosting, backup restore and production rollout were not executed.

## Commits and changed modules

- `501e82e` — initial STOMP publication and viewer-list fixes with nine focused tests.
- `943af21` — compatible backend dependency updates and pinned Maven tooling.
- `830c8f2` — activation/session/origin/CSRF protections, onboarding contract, reference initialization, migrations and backend regression tests.
- `633dc8e` — web dependency migration, reactive-state/font fixes, browser tests and CI configuration.
- `9dad3d2` — explicit wrong-signing-key JWT regression; final eight targeted JWT/STOMP tests passed.
- The final documentation/tooling commit preserves this report, operation procedures, audits and runtime/migration verification scripts; obtain its SHA with `git log -1` after checkout.

Important modules are `config/{StompAuthorizationInterceptor,SecurityConfig,WebSocketConfig,AllowedOrigins,AuthRateLimitFilter}`, `security/{JwtAuthFilter,TokenAuthenticationService}`, auth/championship controllers/services/DTOs, activation entity/repository/service, `ReferenceDataInitializer`, the SQL driver/migrations, frontend auth/API/activation/provisioning pages and touched hook consumers. See [changed-file inventory](CHANGED-FILES.md) for exact paths and the evidence directory for raw outputs.

## Outstanding work and Phase 2 recommendation

1. Finish the compatible Spring security dependency remediation, preferably through a tested Boot 4/Spring 7 migration. Port Jackson/security/test configuration deliberately and rerun the tenant, scoring, auction, database and socket suites. Do not mask the current critical advisory labels with an audit suppression.
2. Before a shared rollout, review actual schema drift, apply owner-approved migrations, coordinate the notification consumer and legacy account reactivation, and review credential copies in backups/delivery archives. These operations require real environment ownership; no production state was assumed.
3. Complete broader browser/tournament/concurrency/load coverage, proxy/rate-limit topology and container/JDK scanning. Upgrade deprecated dev tooling after compatibility validation. Existing builder/default/deprecation warnings and legacy tracked build artifacts remain documented technical debt.

Keep Phase 2 unstarted until the open Phase 1 dependency work has a verified resolution or explicit risk decision. No execution blocker prevents further local development, but the security backlog is not closed and this branch is not certified for production.

**Phase 1 Status: PARTIALLY COMPLETE.**
