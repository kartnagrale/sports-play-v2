# Play-Neml — Phase 1 finalization

Date: 2026-10-27. Branch: `feature/play-neml-upgrade`.

**Phase 1 Status: PARTIALLY COMPLETE.** Confirmed application defects are fixed and verified. Two unresolved dependency advisories prevent declaring the full Phase 1 security backlog complete. This finalization does not start Phase 2 or approve a production deployment.

## Commits

| Commit | Change |
|---|---|
| `501e82e` | Deny client STOMP publication and control viewer access to staff championship lists |
| `943af21` | Backend dependency updates, Java 17 and Maven 3.9.11 wrapper |
| `830c8f2` | Account setup, session revocation, CSRF/origin checks, reference initialization and migrations |
| `633dc8e` | Frontend dependency migration, state/font correctness and browser coverage |
| `9dad3d2` | Explicit untrusted-signing-key JWT regression; eight final targeted tests passed |

The final documentation/tooling commit follows these on the same branch; `git log -1` provides its SHA. Nothing was pushed or merged. The original main checkout and its unrelated local changes remain preserved.

## Fixes and dependency results

- Anonymous and authenticated clients cannot SEND to broker/application destinations. Authorized, tenant-scoped subscriptions and server broadcasts work; logout revokes later socket delivery.
- Viewer GET `/api/championships` returns controlled 403; scoped viewer room access and reloads work.
- Setup tokens are random, expiring, single-use and stored only as hashes. Password/setup secrets are absent from new queued notices and tested runtime logs. Renewal invalidates older links. Logout invalidates staff sessions; wrong-key, expired and malformed JWTs are rejected.
- Unsafe HTTP operations require CSRF protection, including after login. Cookie defaults remain HttpOnly/Secure/SameSite=Lax; HTTP/WebSocket origins are explicit and wildcard configurations fail closed.
- Essential reference initialization is independent of demo accounts. Legacy credential cleanup is a separate, owner-approved opt-in migration.

Updated versions: Spring Boot **3.5.16**, Tomcat **10.1.61**, Jackson BOM **2.21.8**, PostgreSQL JDBC **42.7.14**, Log4j **2.25.5**; Next/config **16.4.3**, React/DOM **19.3.1**, Axios **1.21.0**, ESLint **9.39.5**, Node **22** image family.

Full npm audit improved from **1 critical / 8 high / 2 moderate** affected entries to **0**; production-only audit is also **0**. Backend OSV matches improved from **20/105 dependencies** to **1/105**, containing two advisories for `spring-webmvc:6.2.19`:

| Advisory | Severity metadata | Applicability and mitigation |
|---|---|---|
| `GHSA-j9f9-w8pj-32f8` / `CVE-2026-47890` | Critical | SSE view fragments are not configured. Keep them disabled pending a compatible fix. |
| `GHSA-pc63-qcmh-9cmg` / `CVE-2026-47884` | Critical | XSLT views/templates are not configured. Keep them disabled pending a compatible fix. |

These remain package matches, not resolved vulnerabilities or demonstrated application exploits. The advisories identify Spring **7.0.9** as fixed; adding Spring 7 jars to Boot 3 is not a safe patch. A compatible backport or separately tested framework migration remains open. No speculative framework upgrade or audit suppression was added during finalization. See [dependency triage](DEPENDENCY-TRIAGE.md).

## Verification

| Check | Final evidence |
|---|---|
| Java 17 complete backend verify/package | **57 passed**, including PostgreSQL integration; zero failures/errors/skips |
| Final focused JWT/STOMP tests | **8 passed**, including an additional well-formed wrong-key JWT test; `evidence/final-targeted-security-tests.log` |
| API/WebSocket checks | **83/83 passed**, including signed expiry, anonymous SEND before CONNECT, authorized broadcasts, post-login mutations, activation and logout revocation |
| Browser security | **3/3 passed** in headless Edge: staff setup/sign-in, viewer reload/socket, anonymous socket denial |
| Frontend | Clean install, zero-warning lint, TypeScript and production build passed |
| Database | Fresh initialization, populated synthetic upgrade, repeat skips, checksum rejection, legacy quarantine and data preservation passed |
| Infrastructure | Compose configuration, backend/Next startup and fresh PostgreSQL Docker initialization passed |
| Evidence integrity | Original Phase 0 evidence hashes preserved; Phase 1 outputs have a hash manifest |

The complete suites were reused because application code has not changed since their successful runs. Finalization adds only a focused test plus documentation/verification tooling and runs the affected tests. No existing database was reset or modified; verification used disposable test databases.

## Remaining risks and Phase 2 readiness

Resolve or explicitly disposition the two Spring MVC advisories before closing Phase 1. Production additionally requires owner-reviewed schema/queue rollout, shared rate-limit/proxy decisions, image/JDK scans and broader tournament/load/browser coverage. ESLint 9 is deprecated; external notification delivery and historical credential copies need operational review. CI is configured but was not remotely executed.

Phase 2 remains unstarted. Recommend addressing the open dependency decision first. Full findings, file inventory, commands and limitations are in [REPORT.md](REPORT.md), [CHANGED-FILES.md](CHANGED-FILES.md), [OPERATIONS.md](OPERATIONS.md), and [evidence provenance](evidence/README.md).
