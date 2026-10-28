# Play-Neml — Phase 6

Date: 2026-10-28 (Asia/Calcutta). Branch: `feature/play-neml-upgrade`. Baseline: `4a54ebf`. Repository: `kartnagrale/sports-play-v2`.

**Status: Phase 6 complete.** Stages A–C and final regression are locally committed. The isolated demo services were stopped after verification; their disposable database and private access files are retained for the runbook. No production readiness is claimed. Dockerization/Phase 6.5 and Phase 7 have not begun. Nothing was pushed, merged or deployed.

## Commits and scope

| Stage | Local commit | Result |
| --- | --- | --- |
| A | `83b47b7` | Tenant-aware match subscriptions/reconnect recovery, bounded auction reads and measurement harness |
| B | `57a102c` | Authorized aggregate analytics and reduced captain/roster reads |
| C | `ce11945` | Isolated demo helper/runbook, repeatability checks and sport-aware presentation |
| D | The commit containing this report; resolve with `git log -1 --format=%H -- docs/phase-6/REPORT.md` | Final evidence, regression review and report |

Work remained in `C:/Users/karti/.codex/worktrees/play-neml-upgrade/sports-play-main`; the original `main` checkout and its unrelated changes were preserved. Continuations resumed the saved stages. No dependency installation, schema migration, repository rename, auction-engine replacement or animation-framework addition occurred. Inventory: `git diff --name-only 4a54ebf..HEAD`.

## Real-time reliability

Match subscriptions now follow the selected championship, deactivate on tenant changes, refresh authoritative data after reconnect and coalesce event bursts over 150 ms. A bounded 30-second fallback handles missed notifications. Matches displays errors rather than only logging them, cancels obsolete reads and retains selection only when it exists in the returned championship. Dashboard/standings show live/reconnecting feedback. The browser interrupted a real match socket, corrected the actual final, recovered the current champion in approximately 3.09 seconds and observed a new topic subscription after switching championships.

Background resource refreshes preserve visible data for the same endpoint instead of replacing the dashboard with a loading skeleton; changing tenant/path still hides the old resource immediately. Auction roster reads share an in-flight request and are no longer repeated on every periodic state reconciliation. Explicit refresh, player changes, reconnect and photo events still load the roster. Existing revision acceptance, stale-event rejection, stable sale IDs, duplicate suppression, after-commit broadcast behavior, session revocation and tenant authorization remain intact.

Auction snapshots use a database count for remaining players; selecting the next player uses an ordered first-row query. The old path materialized/sorted every available player. With 500 registered players and one on the block, counting avoids hydrating 499 unnecessary player entities per state read. No bid/purse/auction lifecycle rule changed. Existing FLIP/reduced-motion components remain in use.

## Authorized analytics and presentation

`GET /api/platform/analytics` requires Super Admin and aggregates championship status, sport/lifecycle distribution, participant registration counts and five recent competition audit events. The portfolio reuses the shell's existing championship data rather than issuing another list request, and paginates rendered cards in groups of twelve. Participants means registration records across championships, not deduplicated people.

`GET /api/championships/{championshipId}/analytics/summary` enforces existing tenant view authorization and returns a role-specific response:

- Championship administrator: player auction-state distribution, match completion, purse allocation/balances, squad progress and issued/revoked certificate counts.
- Team captain: own team's roster composition, purse, completed-match wins/losses, squad status and at most five upcoming/recent fixtures. The client fetches only that team's roster, without championship-wide player/fixture/squad lists.
- Viewer/platform observer: competition progress only; no private operations, certificate totals, other-team purse analytics or captain details.

Database aggregates replace full player-table reads for dashboard counts. Standings/squad summaries load assigned roster members rather than all unsold/unassigned registrations. Team roster retrieval queries the requested tenant/team directly. Purse queries are bounded to 100 teams; the dashboard charts show eight with a link to the existing full team workspace.

Charts use labeled native meters with visible counts and empty/error states, without a new chart dependency or color-only interpretation. Portal tests cover four roles, desktop 1440×960, tablet 768×1024, mobile 375×812 and both themes. The captain journey explicitly asserts that no championship-wide `/players` request occurs. Cricket standings display server-provided net run rate rather than badminton rubber columns. Completed cricket fixture cards name the actual winning side, or show Completed when no winner exists, instead of implying a 0–0 rubber score. No scoring functionality was invented.

## Measured performance

Environment: Windows desktop, Intel Core i5-8265U, eight logical processors, Java 17.0.20.1 with `-Xmx512m`, PostgreSQL 15.19 in Docker Desktop, Node 22.20.0 and local loopback REST/native WebSocket STOMP. Browser checks use installed Edge against the production Next build. Measurements share this development machine and are not isolated production benchmarks.

Each bounded cohort provisions 500 referenced Excel players and two teams through real APIs; connects twelve viewers; warms each read route three times; measures 40 auction-state reads and twelve dashboard reads with concurrency four; submits four bursts of eight equal authorized bids; verifies exactly one accepted bid per burst and 28 expected rejections; checks committed revision delivery, monotonic accepted revisions and a single winning purse deduction. Event latency is measured from request start to client receipt, including command processing, not from a synchronized server clock. PostgreSQL activity/lock waiters are sampled every 200 ms; short contention can be missed.

The following are actual p95 milliseconds. “Cold” means the first measured cohort after backend restart, still with the stated three warm-up reads. Separate synthetic cohorts accumulate in the same disposable database.

| Cohort | State read | Dashboard read | Bid response | Viewer event receipt |
| --- | ---: | ---: | ---: | ---: |
| Baseline, original backend | 74.71 | 131.18 | 193.57 | 148.49 |
| Stage A, first post-restart | 374.28 | 787.33 | 447.03 | 335.52 |
| Stage A, warmed | 61.24 | 161.90 | 248.55 | 177.19 |
| Final code, first post-restart | 156.18 | 280.58 | 856.93 | 590.20 |
| Final code, warmed | 101.07 | 156.34 | 392.76 | 314.58 |
| Final verified build, warmed after regression | 45.59 | 79.94 | 133.88 | 91.41 |

All saved cohorts recorded zero unexpected errors, correct accepted-bid ordering, delivery to every connected viewer and correct purse accounting. Sampled maximum lock waiters were 7 at baseline, 7/3 in the Stage A first/warmed samples, 2/6 in final first/warmed samples and 4 in the final verified run. These are sparse observations of expected serialized auction contention, not lock-duration guarantees; the last short run captured only three samples. Initial reconnect measurements opened a replacement connection while the original remained open; the final verified harness explicitly closes the original before reconnecting, retaining twelve concurrent viewers and thirteen total connection attempts. Historical measurements retain their original extra connection behavior. Raw results are `performance-*.json`; the last run is `performance-final-verified.json`.

**The timings do not demonstrate a consistent overall latency improvement.** The measured wins are less entity hydration, fewer full roster requests and burst coalescing. A direct PostgreSQL `EXPLAIN ANALYZE` sample after one sale/advance returned 498 player rows and sorted 106 KiB in 0.934 ms for the old query, versus one count result in 0.486 ms without that sort. This small single sample supports the query-work reduction, not a production speed multiplier. CPU snapshots are cumulative process time; working-set/peak snapshots are retained without claiming comparative memory or CPU savings.

Remaining bottlenecks include serialized bids, per-delivery authorization checks, entity-based standings/fixture construction, client full-list screens outside the optimized dashboard paths, JVM/cache warm-up and the local simple broker. No authorization cache or weaker session-revocation policy was introduced to chase a benchmark.

## Isolated demonstration

The [demo runbook](DEMO-RUNBOOK.md) documents prerequisites, environment variables, setup, randomly provisioned accounts, a seven-step leadership walkthrough, known limitations and safe recovery. `tools/phase6-runtime.ps1` creates a uniquely named container with `purpose=play-neml-phase6`; `tools/verify-phase6.ps1` runs checks and prepares the demo; `tools/seed-phase6-demo.mjs` uses normal authorized APIs and refuses an unexpected database-container label.

The prepared completed archive has real imported/sold players, locked squads, completed badminton league/final, history and four valid Winner/Runner-up/player Participation certificates. A separate live arena has two teams, two referenced registrations, one private synthetic portrait and a default avatar, with its auction ready to start. Re-running the helper preserves the live arena and avoids duplicating active archive awards. The dedicated smoke test opened all four public certificate URLs, loaded the private photo, confirmed the default avatar and checked the viewer has no bid/admin controls.

`CERTIFICATES_PUBLIC_ENABLED=true` and the trusted `http://localhost:53006` verification base are explicit isolated-demo settings. Production public verification/seeding defaults remain disabled. Local QR links must be opened on the presenting machine; a phone cannot reach that machine through its own localhost. No deployed `play.neml.in` is assumed. Credentials remain only in ignored local files and are neither committed nor printed in the report. The runbook explains separate profiles for staff cookie sessions and tab-local viewers.

The original database/container/volumes are untouched. Retained disposable data is not reset on repeat runs. Recovery creates a new labeled environment while preserving the previous one; no database drop or volume deletion is required. Backend runtime uses a private JAR copy so Windows file locking does not interfere with packaging.

## Verification and evidence

| Check | Result / evidence |
| --- | --- |
| Stage A targeted backend | 21 affected auction/broadcast/STOMP/cricket/badminton checks passed and packaging succeeded; `stage-a-backend.log` |
| Stage B backend | 10 analytics/authorization/squad/fixture checks passed; `stage-b-backend.log` |
| Final Java 17 Maven verify | **96 passed, 0 failures/errors, 3 skipped**; `final-backend.log`, `final-backend-summary.json`. Skipped tests belong to the unrelated `DatabaseSecurityIntegrationTest` requiring its separate Phase 1 fixture |
| Certificate/import/photo persistence | Included in final verify against disposable PostgreSQL; rollback, idempotency, source correction/revocation and private photograph integrity passed |
| Frontend model/components | **18/18 passed**: twelve auction reconciliation tests, three burst-refresh tests and three sport-presentation checks; `final-model.log` |
| Lint and TypeScript | Passed; `final-lint.log`, `final-types.log` |
| Production build | Passed, 26 pages; `stage-c-build.log`. Final pass reused this unchanged application build |
| Browser | **9 distinct journeys passed**: six Phase 6 journeys in `final-browser.log` and all three legacy auction/portfolio journeys in `final-auction-browser.log` after harness corrections |
| Demo | **1/1 final smoke passed**; seed/repeat checks passed, four valid certificates, ready live arena and read-only viewer. `demo-validation.json`, `final-demo-seed.log`, `final-demo-browser.log`; original seed/repeat logs omitted from publication as described below |
| Database | Fresh uniquely created PostgreSQL initialized the six existing tracked migrations; no Phase 6 migration or historical checksum change; `migrations-present.log` |
| Private configuration and shutdown | Runtime/access files remain Git-ignored; saved text evidence checked against local passwords, JWT secret and activation tokens with zero matches. Only helper-owned frontend/backend/container stopped; database retained. `final-secret-check.json`, `final-runtime-stop.log` |

Real browser paths use actual activation, login, Excel import, private photo upload, captain bidding, squad locking, valid badminton scoring, authoritative final correction, approval, PDF preview/download, ZIP and opt-in QR verification. The auction suite samples actual animated row transforms, tests reduced motion, delays sale requests to reject premature celebration, replays real committed events as fault injection and tests transport interruption. API successes are not mocked. Four-role checks assert screen restrictions, viewer-safe analytics and foreign-tenant rejection; monitored journeys check page exceptions/hydration. The final total is ten distinct browser journeys including the prepared-demo smoke. Backend, application build and successful Phase 6 journeys were reused while the affected three-test serial auction group was rerun.

Final browser harness maintenance scopes sidebar selectors by accessible navigation name (pagination adds a second navigation landmark), gives retained-data auction fixtures unique names and closes routed WebSocket server connections during teardown. Retries exposed an ambiguous selector, a duplicated retained fixture name and a browser teardown timeout; the corrected auction group then passed all three checks in 31.5 seconds. These are test corrections; they do not alter runtime authorization or application behavior. The first aggregate invocation recorded eight passes and the selector failure and remains in `final-browser.log` for provenance. `final-runner.log` records that initial runner stop, not the subsequent successful targeted checks. The demo helper's final console summary also omits the viewer room code; access details remain in the ignored private file.

Representative screenshots manually inspected: [admin analytics](evidence/analytics-admin-desktop-light.png), [captain mobile](evidence/analytics-captain-mobile-dark.png), [viewer mobile](evidence/analytics-viewer-mobile-light.png), [platform analytics](evidence/analytics-platform-desktop-dark.png), [mobile portfolio](evidence/portfolio-mobile-light.png), [committed SOLD](evidence/auction-sold-desktop-dark.png), [demo portrait/default](evidence/demo-private-photo-and-default.png), [ready arena](evidence/demo-ready-auction.png). Auction motion, certificate previews and valid/revoked verification evidence are also retained under `evidence/`. The final checksum manifest identifies the saved files. Prior Phase 1–5 evidence remains available and was not rewritten.

## Publication sanitization

The public squash integration omits `demo-seed.log`, `demo-repeat-first.log` and `demo-repeat-second.log` because they contained a private viewer room code. Their checksum entries were removed from `evidence/SHA256SUMS`, and private seed/repeat logs are now ignored. The manifest hashes retained committed artifact bytes; seven existing entries were corrected for Git's text normalization. The original Phase 6 test results and retained artifact contents are unchanged; these three original logs are unavailable in the published evidence. The original feature commits remain local recovery history and are not ancestors of the public integration commit.

The exposed code was rotated only in the retained, labeled disposable Phase 6 database. A database check confirmed zero matches for the old code, the private access file was updated without publishing the replacement, and the database was returned to its stopped state. Application logic, migrations and executable verification scripts are unchanged; prior application verification is reused. This integration does not approve production deployment or change the unresolved CVE findings.

## Limitations, Phase 6.5 and Phase 7

The complete competition browser workflow uses badminton. Cricket engines/rules, sport-specific data and corrected presentation retain regression coverage, but a complete cricket delivery-to-final multi-viewer demo was not added. No physical-phone QR scan, Safari/Firefox, screen-reader certification, production load, distributed broker, infrastructure recovery, external notification delivery or deployment was certified. Native meters provide numeric accessible labels; this is not a comprehensive accessibility audit.

Phase 6.5 Dockerization can proceed only as a separately authorized phase using the documented environment contract, private photograph persistence, matched frontend API/WS build settings, tracked migrations and secrets management. No Dockerfile/Compose deployment modernization was started here; Docker was used only for the authorized disposable PostgreSQL tests. Revalidate clean startup, storage/backup recovery, runtime health and real origin/TLS settings during that phase.

Phase 7 is not started. Further development can build on the verified code and runbook, with the performance limits above understood. **CVE-2026-47890 and CVE-2026-47884 remain unresolved Spring MVC production release blockers.** No advisory suppression, security weakening, Spring Boot 4 migration or production-readiness approval occurred. See the preserved [Phase 1 dependency triage](../phase-1/DEPENDENCY-TRIAGE.md).
