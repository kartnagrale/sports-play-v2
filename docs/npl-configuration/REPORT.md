# NPL configuration implementation report

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

- Targeted backend contract, parsing, initialization, fixture and locking tests: 17 passed.
- Frontend settings model: 3 passed, including empty keyboard drafts and payload whitelist.
- Real original gateway request: HTTP 400 reproduced with the response-shaped payload.
- Browser journeys: blocked by local browser/computer-use policy; no browser success claimed.
- ESLint and TypeScript: passed.
- Rebuilt backend on disposable PostgreSQL through Docker gateway: valid save,
  reload, cross-screen stale version rejection, policy synchronization, and invalid
  transaction rollback passed.
- Production frontend, backend and database images: rebuilt successfully. Containers healthy.
- Full backend suite: 116 discovered, 94 passed, 22 database-gated tests skipped, no failures/errors.
- Real gateway integration: four independent captain sessions, preassignment replay and authorization, 40-player XLSX import, 44 identities, bid ceiling, premium cap, losing/undone bids, concurrent bids/sales, 40 purchases, 11-member squad confirmation, and purse reconciliation passed.
- Cricket integration: six unique league pairings, three matches per team, completed delivery scoring, accumulated points, finite NRR, two qualifiers, no semifinals, one final, champion/runner-up, and all 44 participant identities eligible for certificates passed.
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
Sale deduction occurs only in the existing committed-sale transaction.

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
The isolated database was upgraded in place from the original image successfully.

## Remaining verification

Actual browser submission/keyboard editing, visual light/dark/mobile checks and
browser reconnection journeys remain blocked by local browser policy. Additional
photo/restart, WebSocket and certificate correction checks are being finalized.

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
authorized by this implementation. GitHub main has not changed.

## Local commits

Stage A: `981306ed26c15539b2584dfd42cca7073123e7e0`.
The feature checkpoint is the next commit containing this updated report.
Final verification commit IDs will be recorded after remaining checks.
