# Play-Neml full NPL mock verification

Date: 31 October 2026, Asia/Calcutta.

**FULLY VERIFIED:** the real application completed the primary 40-player auction, squad locking, six league matches, final, certificate workflows, and container-recreation persistence checks. **PARTIALLY VERIFIED:** browser coverage, with specific exclusions below. **BLOCKED:** production publication, owing to the retained Phase 7 security gates.

## 1. Environment and isolation

Final inspection project: **`playneml-npl-mock-verified`**. Application: [http://localhost:18090](http://localhost:18090). The gateway binds only `127.0.0.1:18090`; PostgreSQL, backend and frontend have no published host ports.

- Database: `npl_mock`, PostgreSQL 15.19, volume `playneml-npl-mock-verified_postgres_data`.
- Private photographs: `playneml-npl-mock-verified_player_photos`.
- Networks: project-specific `edge`, internal `application`, and internal `database` networks.
- Credentials, environment and resumable account state: `%LOCALAPPDATA%\PlayNeml\npl-full-mock-verified`, outside Git, protected by a Windows ACL limited to the current account. No values are reproduced here.
- Containers remain running and healthy; migration job exited 0. Bootstrap is now disabled and its password was removed from the Compose environment file.

The first disposable project, `playneml-npl-mock`, is **stopped, with every volume retained**. It also completed the tournament. An older supplemental test had used a fixed fixture password. The generator was corrected; the application properly refused activation reissue for an active account. Rather than bypass that rule, the entire final workflow was repeated with generated credentials in the fresh project. Historical observations are retained in `evidence/first-run/`.

The final project contains one primary NPL championship and six separately labelled auxiliary tenants for settings, reserve, scoring and spectator tests. All success totals below are scoped to the primary championship. No company PostgreSQL service, existing company championship, employee/captain account, old private photograph store, pre-existing Docker volume or unrelated worktree was modified.

## 2. Application version and Git state

Repository: `https://github.com/kartnagrale/sports-play-v2`. Fetch verified `origin/main` exactly matched **`4488316c6b2c8b63cb9d14fe599f947ac44cb4ce`**.

Branch: `feature/npl-full-mock-verification`.

Worktree: `C:\Users\karti\.codex\worktrees\npl-full-mock-verification\sports-play-main`.

The original checkout remains on `636f30d`, with its original modified `.gitignore` and untracked items intact. Application images were built from the isolated worktree, including the three repair commits listed in section 23. Java runtime/tests used Temurin 17.0.20.1. Local Node was 22.20.0; production frontend Node was 22.23.4. Runtime image IDs and mounts are in [environment evidence](evidence/environment.json).

## 3. Setup and exact rules

Primary championship ID: **`98254646-84a2-4c1b-82a0-22779167a7d9`**. Name `NPL MOCK 2026`, sport `CRICKET`, created as DRAFT, activated through the settings API, and now COMPLETED.

Auction `NPL Mock Auction`: purse ₹100,000; opening price ₹5,000; increment ₹1,000; ceiling ₹15,000; at most four ceiling-price purchases; squad 11; minimum male 0; minimum female 3; timer 30 seconds; captain preassignment required.

Competition: CUSTOM_CHAMPIONSHIP, exactly four teams, minimum/maximum roster 11, playing side 11, SINGLE round robin, one leg, TOP_TWO_FINAL, two qualifiers. Win/loss/tie/no-result points are explicitly 2/0/1/1. Cricket uses CUSTOM **one-over innings**, maximum one over per bowler, one required wicketkeeper and a required toss. This is an efficient synthetic verification format, not a claim about last year's NPL overs.

League starts 15 November 2026 at 09:30 IST, at Mock Aurora Cricket Ground. Generated fixtures obey the 09:00–22:00 window, 120-minute slots and at least 24 hours between a team's fixtures. Final: 18 November at 15:30 IST. [Independent schedule and score checks](evidence/scoring-schedule-verification.json).

Settings saved/reloaded successfully; stale versions returned 409, invalid values returned useful errors and rolled back. Auction policy synchronized. Competition configuration preserved auction settings. The two proposed-fill buttons and real save/reload were also exercised in the initial browser run with the unchanged frontend.

## 4–5. Teams and captain activation

| Team | Code | Colour | Fictional captain | Activation / API login / browser login |
|---|---|---|---|---|
| NPL Thunder | NPT | `#2563EB` | Mock Arlo Quill | Passed / passed / passed |
| NPL Warriors | NPW | `#DC2626` | Mock Bram Vireo | Passed / passed / passed |
| NPL Strikers | NPS | `#16A34A` | Mock Cyra Flint | Passed / passed / passed |
| NPL Titans | NTT | `#9333EA` | Mock Daro Wren | Passed / passed / passed |

All captains are MALE for this fixture, with unique generated `example.invalid` addresses. Each has exactly one captain/team relationship. The Championship Admin is Mock Aster Vale; its activation and both login paths passed. The fresh Super Admin used the approved bootstrap and actual login API/page. Activation links were consumed inside the protected harness, without Developer Tools or token/password logging. No Google Workspace, SMTP or SMS service was contacted; onboarding messages remain local queue records. A database domain audit found zero accounts outside `example.invalid`.

## 6. Player import

The harness downloaded the real XLSX template, preserved its headers, styles, validation and instruction sheet, and populated exactly 40 synthetic rows: **12 female, 28 male**. It uploaded the workbook, checked preview validation, confirmed import, retried confirmation idempotently, and verified persistence.

Final population: **44 unique references**, comprising four PREASSIGNED captains and 40 purchased employees. Captains have no auction ID or sale price, occupy one slot, cost ₹0, and left each opening purse unchanged. Duplicate preassignment was idempotent. Separate invalid-row and duplicate-reference workbooks were refused at confirmation. Fixtures remain in ignored `tmp/npl-mock/fixtures/`; account secrets do not.

## 7–12. Auction balances, results, quotas and squad confirmation

Every purchase used authenticated captain bidding and the real sale command. Premium bids followed the legal ₹5,000, ₹6,000, …, ₹15,000 sequence. Losing/pending/undone bids did not spend money. The final sale automatically completed the auction.

| Team | Starting purse | Bought | Captain | Final squad | Female | ₹15,000 buys | Spent | Remaining | Squad |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Thunder | ₹100,000 | 10 | 1 | 11 | 3 | 4 | ₹90,000 | ₹10,000 | LOCKED |
| Warriors | ₹100,000 | 10 | 1 | 11 | 3 | 4 | ₹90,000 | ₹10,000 | LOCKED |
| Strikers | ₹100,000 | 10 | 1 | 11 | 3 | 4 | ₹90,000 | ₹10,000 | LOCKED |
| Titans | ₹100,000 | 10 | 1 | 11 | 3 | 4 | ₹90,000 | ₹10,000 | LOCKED |

Totals: **40 purchases, 16 premium and 24 regular, ₹360,000 spent, ₹40,000 remaining**. There are eight male and three female players per squad. No sale exceeds ₹15,000. Each sale's owner/price matches its final active bid; stored purses independently reconcile with player prices.

Four separate captain API sessions confirmed their own squads. The administrator approved/locked all four through the real squad endpoint. Invalid/incomplete confirmations and premature fixture generation were rejected. Database checks confirm 154 locked playing members across seven matches, 14 preassigned-captain appearances and 14 wicketkeeper assignments.

An admin and four captains maintained separate authenticated WebSocket subscriptions during the fresh auction. Each received 502 frames; authoritative STATE revisions were checked for monotonicity. Concurrent identical bids had one winner; concurrent sales committed one deduction. The photo-update/reconnect test also received the real event on two subscribers.

## 13–15. League, standings, final and champion

Every match was scored through CricketMatchPanel-compatible playing-side, toss, innings and delivery APIs. The lower-ranked team batted first and made **6/0 in 1.0 overs**; the winner chased with **12/0 in 0.2 overs**, winning by ten wickets. All fourteen innings and all seven fixtures are COMPLETED.

| Match | Stage | Pairing | Winner |
|---:|---|---|---|
| 1 | League | Strikers vs Warriors | Warriors |
| 2 | League | Thunder vs Titans | Thunder |
| 3 | League | Titans vs Strikers | Strikers |
| 4 | League | Thunder vs Warriors | Thunder |
| 5 | League | Strikers vs Thunder | Thunder |
| 6 | League | Titans vs Warriors | Warriors |
| 7 | Final | Thunder vs Warriors | **Thunder** |

| Rank | Team | Played | Won | Points | NRR |
|---:|---|---:|---:|---:|---:|
| 1 | Thunder | 3 | 3 | 6 | 30.000 |
| 2 | Warriors | 3 | 2 | 4 | 7.714 |
| 3 | Strikers | 3 | 1 | 2 | -7.714 |
| 4 | Titans | 3 | 0 | 0 | -30.000 |

Six unique league pairings and three league appearances per team were independently checked. Qualification retained its frozen standings snapshot and two seeds. Exactly one final was generated; no semifinal or third-place match exists. **Champion: NPL Thunder. Runner-up: NPL Warriors.** Championship status/lifecycle and final stage are COMPLETED.

The separate scoring scenario also verified real wides, no-balls, a free-hit dismissal rejection, a legal run-out wicket, byes/leg-byes, idempotent duplicate delivery, a completed tie, zero tied NRR, name fallback ordering and explicitly different no-result points. These two-player edge-case rules never changed the NPL configuration. [Edge-case results](evidence/cricket-edge-results.json).

## 16. Certificates

All **44** players qualify for participation because they appeared in locked playing sides in completed matches. Winner and runner-up team eligibility passed. Participant, winner and runner-up PDFs were generated and downloaded through authorized APIs; captain and anonymous administrative downloads were denied.

The final project's ledger has **three active reissued certificates and three revoked originals**. A real final-delivery correction invalidated approval and affected awards; stale issuance was rejected. The correction cleared championship awards and completion metadata, then the decisive delivery was restored through the scoring engine. Fresh approval/reissue completed normally. Frozen league correction returned 409. The tournament was left completed throughout final inspection, not in a deliberately broken test state.

Current PDFs: [participation](evidence/mock-participation.pdf), [champion](evidence/mock-winner.pdf), [runner-up](evidence/mock-runner_up.pdf). Nothing was emailed or sent externally. Public certificate verification remains disabled.

## 17. Browser verification

Real Super Admin, Championship Admin and all four captain logins passed in the final project. Captains saw their own team dashboards and restricted navigation. Completed auction counters, locked squads and champion displays were checked after recreation. Desktop and 390×844 mobile dark/light presentations passed; the mobile document did not overflow horizontally.

Initial-run browser checks verified the proposed-fill controls, save/reload, spotlight, synthetic photograph, leading bid/rankings, disabled ceiling bids and the visible SOLD celebration after a real purchase. Those screenshots are explicitly retained as initial-run evidence; frontend source did not change between runs.

**UNVERIFIED browser-only areas:** four simultaneous isolated browser contexts (the available tool exposes a shared session); visual UNSOLD notice; an injected network outage during a live bid and every resulting disabled control; frame-by-frame/reduced-motion animation behavior. Four concurrent captain REST sessions and five WebSocket subscribers were verified. Browser exclusions are not counted as passed. [Detailed browser disposition](evidence/browser-verification.json).

## 18–20. Defects, repairs and remaining limitations

Four reproduced application defects were fixed, with eight additional backend regression cases:

1. Off-increment opening/follow-up bids were accepted. Exact increment validation now rejects them before any financial write.
2. Correcting a completed cricket final retained its champion, runner-up and COMPLETED lifecycle. Correction now reopens and clears that metadata atomically.
3. Frozen cricket league deliveries could be corrected after a top-two final existed. The downstream guard now covers cricket and finals without semifinals.
4. Re-completing a legacy corrected league replaced FROZEN with COMPLETED. Frozen status is preserved; idempotent playoff generation reconciles legacy metadata without replacing the original snapshot, seeds or fixtures.

Failing observations, precise causes and regression evidence are in [DEFECTS.md](DEFECTS.md). Repairs resumed existing state through actual APIs. No business result, purse, owner or sold price was manually edited in PostgreSQL.

Harness/environment issues were handled separately: expected 401/409 contracts, packaged-Python path redirection, a Windows checkpoint-sharing retry, multiline migration JSON parsing and the retired legacy credential fixture. An initial backend run invalidated by overlapping compilation outputs is excluded from success evidence. No reproducible defect remains in the exercised core paths. Browser exclusions and production security blockers remain as stated.

## 21. Backend and frontend validation

| Check | Actual result |
|---|---|
| Java 17 backend suite against separate PostgreSQL 15 | **128 passed, 0 failures, 0 errors, 0 skipped** |
| ESLint | Exit 0, zero warnings allowed |
| TypeScript | `npm run typecheck`, exit 0 |
| Frontend production build | Successful Node 22 / Next.js production image |
| Settings model tests | 3 passed, 0 skipped |
| Auction/reconciliation/live-refresh/presentation models | 18 passed, 0 skipped |
| Real gateway/authentication/CSRF/tenant roles | Passed |
| Real auction, squads, cricket, standings, final, certificates | Passed |
| Independent SQL and migration checksums | Passed |
| Container recreation and full API-state/photo comparison | Passed |

The backend test database was `phase1` inside a separate disposable Docker container, including its own port-55441 network namespace; it never used the company PostgreSQL listener. That test container is stopped and its volume retained. Final test output is from container-local build directories. Raw reports/logs remain ignored under `tmp/npl-mock/`; [backend summary](evidence/backend-test-summary.json) and [frontend summary](evidence/frontend-validation.json) are sanitized. Opt-in Playwright files were not counted as an executed suite; browser actions were verified through the authorized browser tool.

## 22. Docker, migrations and persistence

All seven tracked migration names and SHA-256 checksums match their source files. The migration job succeeded on initial startup and after recreation. No eighth migration or change to historical SQL was introduced. [Migration evidence](evidence/migrations.json).

PostgreSQL, backend, frontend and gateway are healthy; the migration job exited 0. All five Compose containers were force-recreated with the same dedicated volumes. A SHA-256 fingerprint of players, auction, competition, squad confirmations and certificate history matched before/after, as did photograph bytes. Independent read-only SQL was rerun afterward. [Persistence evidence](evidence/persistence-result.json), [database reconciliation](evidence/database-after-recreation.txt), [Docker status](evidence/docker-status.txt).

## 23. Local commits

- `dcdbaedc148d94e2b1eb8efa197ecc3015b155cb` — enforce auction increments.
- `869dfcf0f3c6b45eb6cf3aef524c0bec0d8f8ab7` — reconcile final corrections and protect frozen qualifiers.
- `988d714d8140049e75d4893ee80c8c19243f29ce` — preserve frozen league metadata.
- `ebb2bb8b0939ba7e500e1f01f2d24b148f9430e7` — resumable automation and sanitized execution evidence.

This report is committed afterward; its final commit SHA is given in the delivery message and `git log -1`. No push or merge was performed.

## 24. Exact rerun/resume commands

From PowerShell:

```powershell
Set-Location C:\Users\karti\.codex\worktrees\npl-full-mock-verification\sports-play-main
.\tests\run-npl-full-mock.ps1 -ProjectName playneml-npl-mock-verified -Start -Build -Supplemental -VerifyWorkflow
```

For a quick read/reconciliation resume while already healthy:

```powershell
.\tests\run-npl-full-mock.ps1 -ProjectName playneml-npl-mock-verified
```

The same run name resumes protected checkpoints. To create another fresh synthetic championship without replacing existing data, add a new `-RunName`, for example `-RunName repeat-20261031`. The runner verifies the project, dedicated volumes and loopback binding, uses the approved bootstrap only for a fresh installation, downloads/fills the template, activates users, conducts all purchases, scores matches and checks certificates. `-VerifyWorkflow` adds corrections, migration checks, SQL assertions, recreation and comparison. `-Supplemental` runs isolated negative scenarios once per run checkpoint.

The original application tests remain available. Backend reproduction on this PC:

```powershell
docker start playneml-npl-mock-regression-db
docker build --target build -t playneml-npl-mock-backend-tests:final ./backend
$testName = 'playneml-npl-mock-tests-' + [guid]::NewGuid().ToString('N').Substring(0,8)
docker run --name $testName --network container:playneml-npl-mock-regression-db --env-file "$env:LOCALAPPDATA/PlayNeml/npl-full-mock/regression.env" --mount "type=bind,source=$PWD/backend/src/test,target=/app/src/test,readonly" playneml-npl-mock-backend-tests:final mvn -B test
```

Frontend commands, from `frontend`: `npm run lint`, `npm run typecheck`, `npm run build`, `node --experimental-strip-types --test tests/championship-settings.test.mjs`, and `node --test e2e/auction-model.test.cjs e2e/live-refresh.test.cjs e2e/portal-presentation.test.cjs`.

## 25. Evidence and screenshots

- [Full final API result and 40-sale ledger](evidence/full-mock-result.json).
- [Workflow checkpoints](evidence/workflow-checkpoints.txt), [configuration/correction failures and repairs](DEFECTS.md).
- [Completed dashboard](evidence/final-dashboard.jpg), [four locked squads](evidence/squads-locked.jpg), [completed competition](evidence/tournament-completed.jpg).
- [Captain login evidence](evidence/captain-browser-results.json), with `captain-NPT/NPW/NPS/NTT.jpg`; [Super Admin](evidence/superadmin-browser.jpg).
- [Mobile dark](evidence/auction-mobile-dark.jpg), [mobile light](evidence/auction-mobile-light.jpg), [responsive measurements](evidence/responsive-browser-results.json).
- Initial-run [settings](evidence/first-run/settings-browser.jpg), [competition preset](evidence/first-run/competition-browser.jpg), [SOLD celebration](evidence/first-run/auction-sold.jpg); live bid/photograph screenshots retain the initial-run provenance described in browser evidence.

Ignored runtime artifacts and XLSX fixtures are under `tmp/npl-mock/`. Private credentials and activation/session state remain outside Git in the protected directories. A staged-file scan found no generated passwords, JWTs, CSRF values or environment secrets in the deliverables.

## 26. Expected versus actual; security/publication status

| Expected primary outcome | Actual | Status |
|---|---|---|
| 1 NPL mock championship | 1, COMPLETED | FULLY VERIFIED |
| 4 teams / activated captains | 4 / 4 | FULLY VERIFIED |
| 4 preassigned + 40 auction players | 4 + 40; 44 distinct references | FULLY VERIFIED |
| 40 confirmed purchases | 40 | FULLY VERIFIED |
| 11 players, at least 3 female per team | 11 and exactly 3 each | FULLY VERIFIED |
| ₹15,000 ceiling / at most 4 premium buys | Enforced; 4 each | FULLY VERIFIED |
| ₹100,000 start / valid final balances | ₹90,000 spent, ₹10,000 left each | FULLY VERIFIED |
| 4 confirmed/locked squads | 4 LOCKED | FULLY VERIFIED |
| 6 league matches + 1 final | 7 COMPLETED; no semifinals | FULLY VERIFIED |
| Correct champion / runner-up | Thunder / Warriors | FULLY VERIFIED |
| Certificates and recreation persistence | Eligibility, PDFs, correction/revocation/reissue, state/photo match | FULLY VERIFIED |
| Full browser feature matrix | Explicit exclusions in section 17 | PARTIALLY VERIFIED |
| Production publication | Existing Phase 7 gates remain unresolved | BLOCKED |

Phase 7 historical reports and security gates, dependency locks, framework versions and migration bytes were preserved. **CVE-2026-47890, CVE-2026-47884 and the retained HIGH/CRITICAL image findings are not cleared by this functional exercise.** No fresh full vulnerability scan or production readiness approval is claimed; initial build output also reported moderate npm audit findings. No security finding was suppressed, no unrelated Spring migration was attempted, and no real external notification or production deployment occurred.

The fresh mock remains available locally for inspection. Its volumes, the stopped first-run volumes and the stopped regression database volume have not been deleted.
