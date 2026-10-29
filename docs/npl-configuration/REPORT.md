# NPL configuration implementation report

Status: **PARTIAL overall verification**. Stages A–F are implemented and verified through the real gateway/PostgreSQL. Required browser and visual journeys remain blocked by local browser policy. Production release remains BLOCKED.

Branch: `feature/npl-configuration-fixes`. Base: verified fetched `origin/main` at
`3c0aab0cbff94fa5b3445b1947e890cc0a0a62a7`. Work is isolated in a managed worktree.
The original main checkout and Phase 7 branch at `d523147d` are untouched.

## Stage A: settings corrections

The original page assigns the entire GET settings response to form state and PUTs it
back. The response contains `championshipId`, `status`, and `auctionId`; these do not
belong to `ConfigureChampionshipRequest`. The application's custom Jackson mapper
uses strict unknown-property parsing. It rejects this response echo before Bean
Validation and before the service transaction. The generic malformed-request
handler hides the field. This was reproduced using actual requests through the
isolated Docker gateway with authentication, CSRF, and disposable PostgreSQL.
Browser reproduction could not run: browser access is disabled in local Work
settings and Windows computer use stopped because it could not verify Chrome's URL.

The frontend now constructs an explicit write DTO. It includes the current policy
revision as `expectedVersion`, excludes response metadata, and adopts the saved
response after success. Money inputs use required positive rupee amounts and
`step=0.01`, replacing the invalid `min=1, step=100000` lattice. Empty keyboard edits
remain empty drafts and cannot submit; nonfinite, negative, overprecision, and
out-of-range values are rejected. No currency unit conversion is performed.
Backend money remains BigDecimal with two-decimal validation.

Configuration writers and settings snapshot reads serialize on the championship row. Quota GET responses include their own ETag revision; quota writes use that exact snapshot via If-Match. This avoids pairing older field values with a newer revision from a separate read. Stale requests receive HTTP 409, including simultaneous writers. Championship switches cancel outstanding settings reads.

Configuration writers serialize on the championship row. Settings updates mirror
money, timer, roster limits, team purses, and gender quota rules into the existing
policy records. Competition roster changes update operational and auction limits.
Lowering the maximum roster also caps the existing minimum at that maximum; the
configured playing side must still fit. Quota updates mirror gender minimums back
to operational settings. Policy revisions invalidate stale settings/policy/quota
forms; quota writes require `If-Match`. Auction/roster changes are rejected after
auction actions, including historical bids that were later undone. Existing tables
and records remain readable. No migration is required for Stage A.

Malformed JSON responses identify a bounded field name without echoing the input
value, request body, parser internals, or confidential data. Invalid changes roll
back transactionally.

## Verification

- Targeted backend contract, parsing, initialization, fixture and locking checkpoint tests: 17 passed.
- Frontend settings model: 3 passed, including empty keyboard drafts and payload whitelist.
- Real original gateway request: HTTP 400 reproduced with the response-shaped payload.
- Browser journeys: blocked by local browser/computer-use policy; no browser success claimed.
- ESLint and TypeScript: passed. Frontend settings model: 3 passed; existing auction model/revision/deduplication tests: 12 passed.
- Rebuilt backend on disposable PostgreSQL through Docker gateway: valid save,
  reload, cross-screen stale version rejection, policy synchronization, and invalid
  transaction rollback passed.
- Production frontend, backend and database images: rebuilt successfully. Containers healthy.
- Backend: 120 distinct tests in combined current reports, 98 passed, 22 database-gated tests skipped, no failures/errors. The complete suite ran with 119 tests (97 passed, 22 skipped), followed by the added deterministic tie-break regression. The skipped tests require the separate localhost:55441/phase1 environment; they were not represented as PostgreSQL passes. New integration tests below use actual PostgreSQL 15 in Docker.
- Real gateway integration: four independent captain sessions, preassignment replay and authorization, 40-player XLSX import, 44 identities, bid ceiling, premium cap, losing/undone bids, concurrent bids/sales, 40 purchases, 11-member squad confirmation, and purse reconciliation passed.
- Cricket integration: six unique league pairings, three matches per team, completed delivery scoring, accumulated points, NRR checked against actual runs/legal balls with the engine’s three-decimal rounding, two qualifiers, no semifinals, one final, champion/runner-up, and all 44 participant identities eligible for certificates passed. Equal-points/equal-NRR ordering has a separate deterministic name-fallback regression.
- The cricket integration uses a custom one-over format and entirely synthetic disposable data. Scores come from actual delivery requests; no historical results or direct database result edits are used.

## Captain player preassignment

An authorized championship admin can link an available existing player (preserving
photos and identity) or create a player for the team's actual captain login. The
new PREASSIGNED status has no auction, sale price, bid, or purse deduction. It is
eligible for squad confirmation and the cricket playing XI. Repeat identical
requests return the same identity. Championship-scoped database uniqueness and a
composite captain-membership foreign key prevent duplicate/team-crossing
assignments. Assignment events are audited. Excel reference matches skip existing
captains; ambiguous captain-name duplicates are flagged instead of creating players.

## Maximum bids and premium purchases

AuctionConfiguration owns optional maximumBid and maximumPremiumPurchases, plus a
captainPreassignmentRequired readiness flag. Both configuration screens expose
these fields. A proposal button fills 100,000 / 5,000 / 15,000 / 4 premium / 11 squad /
0 male minimum / 3 female minimum / 1,000 increment. These remain editable values;
no behavior depends on the championship name. The increment and example 2/0 points
are proposals, not historical claims.

One evaluator enforces bid and sale eligibility. Premium counts query committed
SOLD players whose sale price equals the configured ceiling; captains and other
bid history do not count. Opening-price edits, manual registration, workbook
confirmation, and configuration saves reject inconsistent above-ceiling prices.
Championship and auction-state locks serialize bids/sales, including across auctions.
Sale deduction occurs only in the existing committed-sale transaction. General player edits and auction opening-price edits both enforce the ceiling; active bids lock opening-price changes. Purchased players and preassigned captains cannot be deleted through the generic player endpoint. A persisted configuration-section lock survives auction resets even if bid history is later removed. Resume and status controls cannot bypass team/captain readiness.

## Purse and gender quotas

Reserve = configured minimum player bid × max(0, required roster size − current
roster count − candidate purchase). The actual roster includes preassigned
captains. Four 15,000 purchases and six 5,000 purchases cost 90,000; all four test
teams retained 10,000. Pending, losing, rejected and undone bids never deduct purse.
Gender feasibility uses remaining slots versus remaining gender deficits; a zero
male minimum imposes no male quota. Captains count according to their player gender.
Squad confirmation validates the final configured size and quotas again.

## Admin and auction interfaces

Team Management lists captain-to-player assignments and supports association with
existing registered players. Auction and Teams display maximum bid, increment,
premium counts, remaining squad slots, female progress and authoritative blocking
reasons. Illegal controls are disabled; server checks remain authoritative. Existing
bid ranking, sale animation, state revision and reconnection components are retained.
Competition includes an explicit editable four-team custom cricket proposal: roster
11/11, XI 11, single round robin, top-two final, two qualifiers, example win/loss 2/0.

## Migration

`2026-10-29-npl-auction-rules.sql` adds optional auction constraints and captain
identity metadata, extends player status, and adds captain integrity/uniqueness/FK
constraints. Historical purchases and tables remain intact. It is registered in the
existing checksum migration driver and database image's explicit context allowlist.
The isolated database was upgraded in place from the original image successfully. A second, fresh database initialized successfully. Both migration ledgers match the source SHA-256: `c853ecfd4133d905788197c909338cb692d4e4b233683c582fff9c818d90edca`. No existing volume was deleted.

## Docker, photos, WebSockets and certificates

The isolated Compose project is `playneml-npl-test`; fresh initialization used
`playneml-npl-fresh`. Only their new volumes were used. Valid authentication and
CSRF were exercised through the gateway. Invalid settings rolled back without
partially updating purse, limits or configuration revisions.

Two independent authenticated STOMP subscriptions received authoritative auction
states and forty unique sale IDs each, with monotonic revisions. Separate
reconnection and multi-client photo-update checks passed. Photo JPEG bytes, all
44 identities, team balances and seven fixtures survived container recreation.
A preassigned captain received a real certificate PDF. Reversing an actual final
delivery invalidated the previous approval for new issuance, and explicit
certificate revocation remained functional. These are synthetic test championships,
not fabricated historical competitions or manually inserted completed scores.

Final image IDs:

- Backend: `sha256:53555e8ae2410b8a2548d6a540ef75d2175c4ef06b650491bd4d1e1e5db75bb5`
- Frontend: `sha256:7371635029391bdc90e1ad1e91aabc59dad3e5823e0f3c74f81568c8913cecac`
- Database: `sha256:72b1a49b63e778a6f63f39ce5bd54dd91d88ab06386b037c7ccd1b3722544b9f`

## Reproducible regression entry points

Use a disposable stack on localhost:18090 only, with private test environment
values outside Git. After installing frontend dependencies and generating fixtures
with `python tests/npl-workbook.py <private-fixture-directory>`, set
`NPL_TEST_EMAIL`, `NPL_TEST_PASSWORD`, `NPL_FIXTURE_DIR`, and `NPL_TEST_STATE`.

- `node tests/npl-settings-api.mjs`: valid money, reload, cross-screen and simultaneous saves, quota ETag/If-Match, rollback.
- `node tests/npl-reserve-api.mjs`: exact-minimum-purse championship, rejected/pending balances, one deduction, readiness bypass and opening-price locks.
- `node tests/npl-championship-api.mjs`: captain sessions, Excel import, auction, two live subscribers, squads, all seven real-scored fixtures, standings and certificates.
- `node tests/npl-persistence-api.mjs`: uses the saved private integration state and `NPL_TEST_PHOTO`; certificate correction and photo/WebSocket checks. Set `NPL_PERSISTENCE_MODE=after` after container recreation to check retained bytes/data.
- Backend: Maven `test`; database-gated JUnit tests need their own dedicated environment.
- Frontend: `npm run lint`, `npm run typecheck`, `node --experimental-strip-types --test tests/championship-settings.test.mjs`, and `node e2e/auction-model.test.cjs`. Production builds were verified inside the frontend Docker image.

## Known limitations and outstanding verification

Actual browser submission/keyboard editing, visual light/dark/mobile checks,
animation screenshots and browser reconnection journeys were **not run**. Local
browser access is disabled in Work settings, and Windows computer use stopped
because it could not verify Chrome's URL. No browser success is claimed. The new
`frontend/e2e/npl-settings.spec.ts` test is saved, opt-in with
`NPL_BROWSER_ENABLED=true`, for an authorized browser run. Historical browser
fixtures were adapted to the versioned contract and explicit miniature roster/team
configuration; they type-check but were not re-executed.

The 22 environment-gated database JUnit tests remain skipped in this run. Real
PostgreSQL gateway integration passed independently. Company backup restoration,
production load and company-PC installation were not executed. Phase 7's existing
verification evidence remains separate and unchanged.

Captain gender and auction exclusion are immutable after preassignment through the
generic player editor; names, skills and photos remain editable. An audited
reassignment/gender-correction workflow is not included. Ambiguous imported
captain-name collisions are flagged for identity review, never silently merged.

NPL values are proposed through two explicit save steps (auction settings and
competition configuration); defaults for existing championships are not silently
converted. Tie/no-result points remain configurable; configurable loss points now
apply during both delivery completion and administrative result handling.

## Existing installation update procedure

Production remains blocked. Test upgrades in a separate environment first.

1. Retain the installation's private environment file outside Git. Record its exact
   Compose project name, volume names, image IDs, and deployed commit.
2. Back up PostgreSQL and the player photo volume; verify both backups can restore
   into new, separately named test volumes before updating the installation.
3. Check out the approved local commit on a separate checkout. Keep the same private
   environment configuration and the same existing Compose project name for an
   authorized installation upgrade; use a different name for testing.
4. Run `docker compose --env-file <private.env> -p <existing-project> build`.
5. Run `docker compose --env-file <private.env> -p <existing-project> up -d`.
   If gateway upstream addresses changed during recreation, recreate the gateway
   after application health checks: `docker compose --env-file <private.env> -p <existing-project> up -d --force-recreate gateway`.
   The migration service runs the repository's checksum-verified migrations before
   the backend starts. Inspect its exit code and all container health checks.
6. Verify login/CSRF, settings save and reload, photographs, stored data, and live
   auction connections through the gateway. Do not run `down -v`, remove existing
   volumes, substitute empty volumes, or run seeded demo initialization.
7. If an additive migration was applied, rollback requires a tested compatible
   image or full coordinated database/photo restore; do not edit migration checksums.

## Security release status

Production readiness remains BLOCKED. CVE-2026-47890, CVE-2026-47884, and additional
critical container findings remain unresolved. No security dependency remediation,
Spring Boot 4 migration, weakened scan, production deployment, push, or merge is
performed by this implementation. No dependency manifests were changed. No fresh security clearance is claimed; the existing critical publication gates remain failing. GitHub main was not changed by this task.

## Local commits

Stage A: `981306ed26c15539b2584dfd42cca7073123e7e0`.
Feature checkpoint: `6eecb518a1529aa34da488b86e9a3e9e753759f5`.
Final verified implementation: `c0adadff837674c036a70f3c9f16ff49e9840f46`.
A final documentation-only commit records this SHA and the retained test resources.
No push, merge or production deployment was performed.

## Test resource disposition

The two isolated test Compose projects were stopped after verification. Their PostgreSQL and photo volumes were retained for inspection; no `down -v` or volume deletion was run. Private test configuration, logs and synthetic fixtures remain outside Git under the local Codex temporary directory. The managed feature worktree remains attached for review.
