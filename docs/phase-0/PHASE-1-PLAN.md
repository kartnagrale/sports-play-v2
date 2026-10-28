# Play-Neml — Phase 1 implementation plan

Status: proposed, not started. Execute only after an explicit Phase 1 request. Baseline: `636f30d11ae50b300f32d96f6a901c908dfb2433`; see [REPORT.md](REPORT.md) and saved evidence. The audit commit contains documentation and evidence only.

Backend paths below are relative to `backend/src/main/java/com/neml/badminton/`; frontend paths start at the repository root. New paths are proposals. Preserve existing REST payloads, tenant roles, sport behavior, PostgreSQL schema name and Java package names.

## 1. Close the reproduced WebSocket publication vulnerability

**Priority P0; severity High; confirmed at runtime.** `config/StompAuthorizationInterceptor.java` permits unauthenticated CONNECT when no bearer header is supplied and only authorizes SUBSCRIBE. `config/WebSocketConfig.java` exposes the simple `/topic` broker. An anonymous client sent a synthetic event to a championship matches topic; an authorized viewer received it. This demonstrates forged event delivery, not a database mutation or reading private messages.

Modify `config/StompAuthorizationInterceptor.java` to reject unauthenticated CONNECT after accounting for the authenticated HTTP handshake principal, reject client SEND to `/topic/**`, and default-deny client application messages unless an actual handler requires them. Keep UUID/auction-ownership checks on SUBSCRIBE. Review `config/WebSocketConfig.java`, `security/JwtAuthFilter.java`, and `frontend/src/lib/ws.ts` together to preserve staff HttpOnly cookie and viewer bearer authentication. Do not fix this by making the handshake authenticated for all clients: viewer credentials arrive in the STOMP frame.

Add **new** `backend/src/test/java/com/neml/badminton/config/StompAuthorizationInterceptorTest.java` and a real broker integration test. Acceptance: anonymous CONNECT and SEND rejected; staff and viewers can subscribe only as permitted; viewers/captains cannot publish forged messages; wildcard and cross-tenant subscriptions denied; spoofed auction ownership denied; legitimate after-commit events and reconnect work. Include token-expiry and changed-membership behavior in the socket lifecycle policy. No database migration required.

## 2. Correct viewer principal handling

**Priority P1; severity Medium; confirmed at runtime.** GET `/api/championships` with a valid viewer token returns 500. `controller/ChampionshipController.java` binds only `User`; `service/ChampionshipService.java:listFor` dereferences the null argument. Saved server logs identify the null `User.getRole()` call.

Modify those two files to handle authenticated principal types explicitly. Recommended contract: the global staff-list route rejects viewer principals with 403; viewers continue using the already tenant-qualified championship route. Check `frontend/src/lib/championship.ts` so joining/reloading a viewer room does not depend on the staff list. Preserve Super Admin's global read and staff membership-scoped lists.

Add **new** HTTP security regression tests covering anonymous, staff A/B, Super Admin and viewer A/B. Acceptance: no 500 for any supported principal; no championship leakage; valid viewer join/reload still works. No migration required. Do not simply return all championships when the principal is absent.

## 3. Triage and remediate dependency findings with pinned tooling

**Priority P0 for production critical/high matches; exploitability not yet established.** npm reports 11 affected package entries overall and 5 with dev omitted. OSV matches 20/105 resolved Maven dependencies, including 3 test dependencies. Counts are not exploit counts.

Modify `backend/pom.xml`, `frontend/package.json`, `frontend/package-lock.json`, `frontend/next.config.js`, `frontend/.eslintrc.json` as applicable, `backend/Dockerfile`, and `frontend/Dockerfile`. Add **new** Maven wrapper files and `.github/workflows/ci.yml`; pin Java 17 and a supported Node/npm runtime consistently across local and CI builds. The existing Java 25 test failure must not be hidden with an experimental Byte Buddy flag.

Create a triage record per advisory: package/version/scope, aliases, vulnerable configuration, application reachability, severity, fixed release, proposed target, test evidence and disposition. Inspect actual Next features before attributing middleware or image-processing advisories to this app. Include backend build plugins and runtime container/JDK scans, which Phase 0 did not scan.

Upgrade frontend and backend in separate reviewable changes. Choose supported Next/React/ESLint versions together and a supported Spring Boot BOM; stage major migrations following official guides. Do not cherry-pick incompatible Spring jar versions or use automatic force-upgrade commands. Exact target versions remain an implementation-time compatibility decision, not an unresolved audit execution step.

Acceptance: Java 17 clean verify, frontend lint/build and complete security/role/auction/scoring regressions pass; no unreviewed production critical/high advisory remains. Any unresolved finding needs explicit documented risk acceptance and mitigation, not a clean-audit claim. Dependency changes alone should require no domain DDL; review Hibernate mapping/DDL expectations before deployment.

## 4. Secure provisioned credentials and browser authentication

**Priority P1; plaintext credentials confirmed in code; external compromise not demonstrated.** `service/MessageTrackerGateway.java` places the temporary password in `msg_parameters`. `mustChangePassword` is only notification metadata; no corresponding enforced user state or password-change API was found. Review queue access/retention with its owner before changing this integration.

Modify `service/ChampionshipService.java`, `service/MessageTrackerGateway.java`, `service/AuthService.java`, `controller/AuthController.java`, `dto/AuthDtos.java`, `entity/User.java`, `repository/UserRepository.java`, `security/JwtService.java`, `security/JwtAuthFilter.java`, and `config/SecurityConfig.java`. Add **new** activation-token service/repository/entity and password setup/change endpoints if using the recommended single-use invitation design. Update `frontend/src/lib/auth.ts`, `frontend/src/lib/api.ts`, login and **new** activation UI; update admin/captain provisioning responses and pages together.

Recommended migration: additive invitation/token-hash, expiry, used/revoked timestamps and user credential-state/version fields in `play_neml`; validate/backfill existing accounts deliberately. Tokens should be short-lived and single-use. Delivery payloads still contain sensitive activation links: restrict queue readers, avoid logs and define retention. Do not delete or rewrite historical queue records without an approved retention/migration plan. Never weaken authentication to make initial setup easier.

Define CSRF protection for cookie-authenticated mutations and explicit allowed origins; wildcard credentialed CORS must fail production configuration validation. Add bounded login/room-join throttling with distributed behavior documented. Review login JSON token return: remove unnecessary staff token exposure only with a compatible client/API transition. Preserve legitimate viewer bearer behavior.

Acceptance: expired/replayed invitations fail; password change is enforced; old credentials/tokens are invalidated as designed; cross-origin mutations fail; valid login/logout, viewer join, captain bidding and SockJS still work. Tests must include form/multipart requests in anticipation of import. This is security stabilization, not implementation of the import feature.

## 5. Establish safe bootstrap and schema evolution

**Priority P1; code/schema finding.** `sports-play-complete-schema.sql` creates tables but not reference navigation data. `seed/DataSeeder.java` mixes navigation with fixed demo accounts and does not include the competition screen. `SuperAdminBootstrap.java` only creates the user. The prior runtime test asserted navigation HTTP 200, not populated usable menus.

Modify `seed/DataSeeder.java`, `seed/SuperAdminBootstrap.java` as necessary; add **new** `seed/ReferenceDataInitializer.java` or a versioned reference-data migration. Review `service/NavigationService.java`, `entity/AppScreen.java`, `entity/RoleScreenMapping.java` and repositories against all current routes. Preserve intentional role grants and existing customized records; do not overwrite them silently.

Update `README.md`, `docs/RUNBOOK.md`, `docs/LOCAL_POSTGRESQL.md`, `DEPLOYMENT.md` and `DEPLOYMENT_MANUAL.md`. Separate empty bootstrap from populated-database upgrade. Remove the recommendation to use `ddl-auto=update`. Adopt a checksummed owner-run migration ledger or a reviewed migration-tool baseline after actual schema drift is known. Keep runtime credentials DDL-free and Hibernate validation enabled.

Acceptance: a new disposable database with demo seeding disabled supports login and correct menus for all four portals; existing configured championships remain usable after additive migrations. Rehearse on an isolated populated copy, validate tenant composite FKs/triggers and rollback strategy. No existing database may be altered merely to prepare tests.

## 6. Resolve correctness debt and establish repeatable regression gates

**Priority P2, with any reproduced data-integrity defect promoted.** Resolve hook warnings in `frontend/src/app/(authed)/admin/championship-settings/page.tsx`, `admin/team-management/page.tsx`, `players/page.tsx`, `frontend/src/components/AppShell.tsx`, and font loading in `frontend/src/app/layout.tsx`. Align player management UI with `ChampionshipSecurity`: Super Admin must not appear able to edit tenant players. Guard/cancel stale requests during tenant switches.

Review builder defaults in `entity/Announcement.java`, `MatchFormat.java`, `Match.java`, `Player.java`, `Team.java`, `Season.java`, `Bid.java`, and `AuctionState.java`; test intended persisted defaults before adding annotations. Map expected optimistic locking/data conflicts in `controller/ApiExceptionHandler.java` to stable 409 responses without leaking SQL. Keep changes targeted; broad competition-page and design-system refactoring belongs to later phases.

Before Phase 4, plan extraction from `controller/TenantPlayerAdminController.java` into **new** `service/PlayerManagementService.java`, preserving manual CRUD behavior and establishing consistent championship locking/mutation guards. Player parsing, uploads and import APIs are explicitly out of Phase 1 scope. Complete import/certificate integration designs are in [REPORT.md](REPORT.md).

Add **new** PostgreSQL-backed integration tests and `frontend/playwright.config.ts`/`frontend/e2e/` with isolated test data. Recover/replace the historical 50-check harness with versioned tests rather than citing its old results. Test captains, tenant A/B, public/private viewers, simultaneous bids, squad locks, both sports, result corrections and socket reconnect. Remove the 171 tracked generated build files only in a dedicated reviewed housekeeping commit; preserve local files and source. Re-run checks affected by each change.

## Exit criteria and outstanding decisions

Phase 1 is complete only when the two reproduced defects have regression tests and fixes, production dependency findings are resolved or explicitly accepted, credential/origin protections are verified, safe fresh provisioning works, and the above validation has fresh evidence. Full visual/browser coverage remains required before UI modernization acceptance and final release.

No execution blocker remains for Phase 0. Outstanding Phase 1 inputs are the notification queue contract/retention owner, supported-version compatibility choices, and the actual deployed schema inventory before an upgrade rehearsal. Future feature decisions include player identity/deduplication policy and certificate eligibility/download access. DNS/TLS/hosting for `play.neml.in` remain Phase 7 work; they do not block this documentation or isolated development.
