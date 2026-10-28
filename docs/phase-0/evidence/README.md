# Phase 0 evidence provenance

These original outputs were produced on 2026-10-27 against `636f30d11ae50b300f32d96f6a901c908dfb2433` in the isolated `feature/play-neml-upgrade` worktree. They were preserved when the audit resumed; successful expensive checks were not repeated. `SHA256SUMS.txt` records hashes of the original 17 output files, not a signature or proof of external verification.

The documentation-local `.gitattributes` preserves raw output bytes across checkout platforms. Tool-emitted trailing whitespace and blank lines in logs are retained intentionally; editorial whitespace checks apply to the authored Markdown, not rewritten evidence.

| Files | Command or scope |
|---|---|
| `maven-version.log`, `backend-verify.log` | Maven 3.9.11 `-version` and `clean verify` with default Java 25; 3 test errors from Byte Buddy incompatibility |
| `maven-java17-version.log`, `backend-java17-verify.log` | Same commands with `JAVA_HOME` set to Microsoft JDK 17.0.20.1; 32 tests passed and JAR packaged |
| `backend-dependencies.log`, `backend-dependency-tree.txt` | `mvn dependency:tree -DoutputFile=../docs/phase-0/evidence/backend-dependency-tree.txt` |
| `backend-osv-audit.json` | Public Maven coordinates/versions from the resolved compile/runtime/test tree submitted in batches to OSV `/v1/querybatch`; no application data sent |
| `npm-ci.log` | `npm ci --no-audit --no-fund` in `frontend/` |
| `frontend-lint.log`, `frontend-build.log` | `npm run lint`, `npm run build`; Node 22.20.0/npm 10.9.3 |
| `npm-audit.json`, `npm-audit-production.json` | `npm audit --json`, `npm audit --omit=dev --json`; nonzero exit from reported findings |
| `database-bootstrap.log` | Existing full-schema SQL applied with `psql -v ON_ERROR_STOP=1` to a new temporary PostgreSQL 15 database |
| `database-validation.log` | Count of schema tables and one player-to-auction tenant consistency query; not an exhaustive integrity audit |
| `backend-runtime.log`, `backend-runtime-stderr.log` | Java 17 packaged backend startup with Hibernate validation; synthetic bootstrap account, demo seed disabled. Empty stderr file is retained intentionally |
| `runtime-smoke.json` | 31 targeted checks: 29 passed, two failures reproduced. Status checks alone do not validate response content or full workflows |

The runtime environment was a dedicated Docker container `play-neml-phase0-20261027`, PostgreSQL on `127.0.0.1:55439`, and backend on `127.0.0.1:58091`. Image digest recorded during the original pull: `sha256:54e0dd8e8cddcdbb9156c2486eed5a43eccb7214d7c6828097d41ce049fb3984`. Backend and container were stopped after checks. No existing/company database was used. Resume did not start or modify any database.

## Reproduced defects

1. Join a synthetic public championship to obtain a scoped viewer token. Connect a viewer STOMP client to `/ws` and subscribe to `/topic/championship/{id}/matches`. Connect a second client without authentication, then SEND a JSON event with type `PHASE0_INJECTION_PROBE` to that same broker destination. The viewer received the forged event. The probe did not issue a score/bid/database-write request. Only synthetic isolated data was involved. Test unauthorized subscription and auction destinations separately in Phase 1; they were not proven by this single probe.
2. GET `/api/championships` with a valid viewer bearer token returned 500. The runtime log records a null `User.getRole()` dereference in `ChampionshipService.listFor`; the controller expects a `User` whereas the token creates a `ViewerPrincipal`.

The original local harnesses remain preserved in `C:/Users/karti/personal_projects/sports-play-main/tmp/phase0-{runtime.ps1,smoke.mjs,osv.mjs}`. They contain environment-specific paths and are not the tracked historical 50-check suite or a production-ready test runner. No executable harness was added to the application in this documentation-only commit. The regression suite proposed in `PHASE-1-PLAN.md` must replace this ad hoc setup with reproducible fixtures and content assertions.

## Interpretation limits

- npm severities are package/advisory metadata, not independently verified exploitability. Production omission still includes dependencies used during builds; assess deployment reachability explicitly.
- OSV stores package matches and advisory IDs, without normalizing aliases or manually triaging reachability/severity. Build plugins, JDK and image OS were not scanned.
- No full browser, accessibility, production container-build, performance, populated-schema migration or exhaustive tenant-constraint suite was run.
- Local filesystem paths, synthetic test IDs and stack traces are present. A pre-commit credential-pattern review found no live JWT, password, private key or secret values in these outputs. Do not add environment files or provisioned credential responses to future evidence.
