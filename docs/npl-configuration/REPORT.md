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

## Verification (in progress)

- Targeted backend contract, parsing, initialization, fixture and locking tests: 17 passed.
- Frontend settings model: 3 passed, including empty keyboard drafts and payload whitelist.
- Real original gateway request: HTTP 400 reproduced with the response-shaped payload.
- Browser journeys: blocked by local browser/computer-use policy; no browser success claimed.
- ESLint and TypeScript: passed.
- Rebuilt backend on disposable PostgreSQL through Docker gateway: valid save,
  reload, cross-screen stale version rejection, policy synchronization, and invalid
  transaction rollback passed.
- Production frontend image build: pending at this checkpoint.

## Remaining feature stages

Captain preassignment, configurable bid ceiling/premium purchase cap, purchase
reservation changes, expanded quota/readiness UX, and complete NPL cricket and
certificate regression verification remain pending at this checkpoint.

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

The Stage A checkpoint commit is the commit containing this report's initial
version. Final commit IDs and remaining verification will be recorded after checks.
