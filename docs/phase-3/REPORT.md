# Play-Neml — Phase 3 completion report

Date: 2026-10-28 (Asia/Calcutta). Branch: `feature/play-neml-upgrade`. Baseline: `e32244f`.

**Status: COMPLETE.** Phase 4 has not started. No production readiness is claimed.

## Git and scope

Stage A: `018e167` — role-specific portals and shared page improvements. Stage B: `1c2f9eb7b31e389f738cb808c42a2ef8bcb32c53`. The final evidence/report commit SHA is listed in the completion message and `git log e32244f..HEAD`. Full changed-file inventory: `git diff --name-only e32244f..HEAD`.

Work remained in `C:/Users/karti/.codex/worktrees/play-neml-upgrade/sports-play-main`. The original main checkout and unrelated files remain preserved. No push, merge, repository rename, deployment, existing database reset or schema change occurred. Runtime verification creates new PostgreSQL 15 test containers only. No dependency was installed or upgraded.

## Portals

- Super Admin: championship portfolio, actual total/active/inactive counts, sport distribution, search/status/sport filters, alphabetical cards and championship detail dialog. Platform home resolves to the portfolio. Tenant management remains with the championship administrator.
- Championship Admin: operations dashboard with actual championship counts, phase, squad progress, blockers, fixtures/results, standings and authorized management shortcuts. Existing configuration, team provisioning, competition, match and squad handlers are preserved.
- Captain: assigned team, actual purse, points, squad status, player identities, roster and own upcoming/recent matches. Own matches are filtered from the complete championship match list, avoiding the five-fixture dashboard window.
- Spectator: read-only championship dashboard, fixtures, results, standings, team directory and live auction. No management or bid actions.
- Shared pages: semantic table headers, horizontally scrollable tables, consistent page spacing/heading scale, player status/sort/search controls, roster selection/search/sort, standings search and retry, analytics retry and history search/status/sort. Existing feature forms inherit Phase 2 tokens and components.
- PlayerIdentity/PlayerAvatar now appear in rosters, dashboards, squad confirmation, performers, activity/history and the spotlight. Images remain optional; player uploads/storage/import were not added.

## Auction experience and event correctness

The arena combines a player spotlight, highest confirmed amount and leading team, independent timer, animated bidding leaderboard, purse/squad cards, role-aware bid panel/control desk and recent bid activity. Waiting, running, paused, unsold, completed and connection-interrupted states are distinct. Bids and management commands use the existing CSRF-protected REST endpoints. Super Admin no longer appears to manage tenant auctions.

`rankTeams` uses only authoritative ACTIVE current-player team bids. It selects the highest valid amount per team, sorts descending, breaks equal amounts deterministically by name/ID and gives no-bid teams no fabricated rank. The leaderboard resets per player and reconnect epoch. Phase 2 ReorderGroup now supports interruptible FLIP transforms that start from the currently displayed position during rapid changes; geometry reads are batched and native WAAPI handles the movement. UpdatedValue highlights committed amounts. No animation library was added.

`AuctionReconciler` rejects older state revisions. An initial connection or reconnect synchronizes a fresh REST snapshot, buffers newer arriving STATE frames and suppresses historic sale effects. A sale is accepted only on a live, reconciled connection for the observed player/current transition. Winning bid IDs are stable sale IDs; repeated events are deduplicated. Late previous-player events after subsequent state changes are ignored. REST command responses may reconcile state but never trigger celebrations themselves.

The SOLD native modal shows the exact player, team, price and avatar with emerald/gold entrance, stamp and a short deterministic particle burst. It dismisses through Escape, close, Continue, auto timeout or a later auction revision. It traps focus and restores it through the Phase 2 Dialog. UNSOLD uses neutral/warning feedback rather than a success celebration. Reduced motion suppresses transforms/particles while retaining readable states. Bid announcements are debounced to reduce screen-reader noise; controls are disabled while disconnected or reconciling.

`useLiveAuction` owns subscription lifecycle, reconnect state, cancellation, stale-response rejection and periodic authoritative refresh. Domains never publish STOMP SEND frames. A connection error disables bidding until a fresh baseline is established.

## Minimal backend additions

No routes, authorization expressions, bid validation rules, database columns or migrations changed.

- Existing AuctionStateDto adds `revision` and `teamBids`.
- A championship/auction/player-scoped query returns one highest active bid per team, independent of the recent 50-row history window.
- State reads acquire a shared lock for consistent snapshots. Existing blocking write locks remain; after acquisition, force-increment advances the existing optimistic-lock version even for paused undo/player-only changes. The timer path uses the same revision guarantee. Flush occurs before constructing the versioned DTO/event.
- PLAYER_SOLD retains prior fields and adds winning bid `saleId`, `playerId`, `teamId`, `revision`; PLAYER_UNSOLD adds player ID/revision. Broadcasts still use Phase 1 after-commit dispatch and do not emit rolled-back results.

The standalone supplemental suite was also made self-contained: it provisions its own foreign championship rather than depending on fixtures from the account suite. No application defect was involved.

The first concurrent test exposed NOWAIT contention from directly force-incrementing in the repository lock. This was corrected by acquiring the original blocking write lock before incrementing. Final live concurrent bids have exactly one winner and a controlled rejection.

## Verification

| Check | Final result / evidence |
|---|---|
| Stage A role journeys | 4/4 passed; `evidence/stage-a-browser.log` |
| Frontend ESLint | Passed, zero warnings; `evidence/lint.log` |
| TypeScript | Passed; `evidence/types.log` |
| Next production build | Passed, 24 static pages; `evidence/build.log` |
| Ranking/reconciliation tests | 12/12 passed; `evidence/auction-model-tests.log` |
| Java 17 affected tests/package | 12 passed, zero failures/errors/skips; AuctionPresentationTest, AuctionBroadcasterTest, StompAuthorizationInterceptorTest; `evidence/backend-tests.log` |
| Full Edge browser/integration | 12/12 passed; `evidence/browser-tests.log` |

The final evidence includes 96 screenshots. The full browser run covers all four roles, account activation, CSRF/cookie/session and STOMP boundaries, component states, real player save, tenant boundaries, real multi-session auction bids, upward/downward ranks and actual animated transforms, concurrent equal bids, paused undo with revision advancement, pending/rejected sale suppression, SOLD delivery to four sessions, duplicate delivery of a real committed frame, reconnect, old-frame replay rejection, UNSOLD, next-player reset, reduced motion, roster persistence and >50-bid ranking correctness. The history window case submits 55 later bids and verifies an older team's valid bid remains in `teamBids` while `bidHistory` stays capped at 50.

A supplemental targeted run captures the interrupted-connection screen and rechecks the auction/history/portal journeys. It is recorded separately in `evidence/auction-browser-tests.log` so the successful full-run evidence is retained. No page exceptions or hydration errors were reported in monitored journeys. No full unrelated backend security suite was repeated.

Commands: `npm run lint`, `npm run typecheck`, `node --test frontend/e2e/auction-model.test.cjs`, Java 17 Maven `-Dtest=AuctionPresentationTest,AuctionBroadcasterTest,StompAuthorizationInterceptorTest package`, `tools/verify-phase3-ui.ps1 -Auction`. Reuse the matching build with `-SkipBuild`; `-AuctionOnly` runs the targeted suite. Tests use API/WS localhost:58093, Next localhost:53002 and disposable PostgreSQL localhost:55442. Rebuild with intended environment URLs before a later deployment.

## Visual evidence and accessibility

Representative screenshots in `evidence/`:

- `portfolio-desktop-light.png`, `portfolio-mobile-dark.png`, `admin-desktop-dark.png`, `captain-mobile-light.png`, `viewer-desktop-light.png`.
- `auction-ranking-frame-1.png` through `-3.png`: actual server-driven bid changes; `auction-ranking-frames.json` records sampled positions, transforms and active animations.
- `auction-desktop-light.png`, `auction-desktop-dark.png`, `auction-tablet-dark.png`, `auction-mobile-light.png` include UNSOLD feedback and next player.
- `auction-sold-desktop-dark.png`, `auction-sold-mobile-dark.png`, `auction-sold-reduced-motion.png`, `auction-paused-admin.png`, `auction-completed.png`.
- `auction-disconnected-mobile.png`: interrupted connection.
- `portal-winning-roster.png` and `portal-*-desktop.png`: management, standings, analytics, competition, fixtures, squads and history.

Desktop 1440×960, tablet 768×1024 and mobile 375×812 were exercised with both themes. Representative auction, sale, roster and portal captures were manually inspected. Overflow assertions pass. Native dialogs, visible focus, associated bid labels, 44px shared buttons, keyboard dismissal and focus return use the Phase 2 accessibility foundation. A SHA256 manifest identifies the final evidence files.

## Limitations and next phase

Verification uses Windows Edge, not Safari/Firefox, a physical mobile device or screen-reader certification. Ranking frame telemetry and PNG sequences are provided rather than a video recording. Synthetic championship records were created through real authorized application APIs; they are test data, not production activity. The complete tournament lifecycle, external notification delivery, load/fanout performance and timed automatic auction expiry were not exhaustively retested. Broader Phase 1 evidence remains historical evidence. Existing configuration/match workflows retain their architecture; this is not a new routing or business-rule system.

Phase 4 can supply real image URLs through the existing avatar API and build Excel registration on these player/table patterns. No Excel import, upload/storage, certificates or Phase 4 work began.

**CVE-2026-47890 and CVE-2026-47884 remain unresolved Spring MVC advisory matches and production security blockers.** SSE-fragment/XSLT features remain unused; no suppression, protection bypass or Spring Boot 4 migration was introduced. See `docs/phase-1/SUMMARY.md` and `DEPENDENCY-TRIAGE.md`. Phase 3 completion does not imply production readiness.
`evidence/auction-browser-tests.log`: supplemental 3/3 passed (53.2 seconds), including standalone history-window/tenant checks. All seven disposable Phase 3 containers and their test volumes were removed after verification; existing databases were untouched.
