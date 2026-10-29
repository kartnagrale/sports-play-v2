# Phase 7 and NPL development-source integration

Date: 2026-10-29 (Asia/Calcutta). Production readiness: **BLOCKED**.
This is source integration under explicit user authorization to publish development
source despite the documented security findings. It is not release approval.
Historical Phase 7/NPL reports retain the publication restrictions in force when
those reports were written; the subsequent integration authorization permits this
source push without changing their vulnerability dispositions.

## History and preservation

- Starting GitHub main: `3c0aab0cbff94fa5b3445b1947e890cc0a0a62a7`.
- Phase 7: `d523147d9ce79ec6f053f2bc104f47bd69fc1bf0`, merged normally in `0a70d55`.
- NPL: `9edc968ba272c928fa614f64daed23b4d40c8862`, merged normally in `7ac3ef5`.
- Both merges were conflict-free and preserve the source commits and sanitized ancestry.
- Integration branch: `integration/phase7-npl-final`, in a new isolated managed worktree.
- All 619 original checkout file hashes, its HEAD and local main branch are unchanged.
- Both feature branch heads and historical `feature/play-neml-upgrade` remain unchanged.
  The historical upgrade branch is not an ancestor of this integration and is not published.

The final local/source SHA is the commit containing this report and the migration
adjustments; resolve it with `git log -1 --format=%H -- docs/integration/PHASE7-NPL.md`.
The final response records the actual remote main SHA and hosted Actions status
after the non-force push. Publication must stop if a protection check rejects it.

## Integration corrections

CI now applies all seven migrations in order. The PowerShell owner's migration
allowlist includes NPL. Its former `$migration` path variable collided with the
case-insensitive validated `$Migration` parameter, causing ValidateSet rejection;
it is now `$migrationPath`. All seven exact CI invocations were replayed against
the dedicated PostgreSQL fixture successfully. Docker verification assertions now
expect seven ledger entries. Existing migration SQL bytes and checksums are unchanged.

Frontend settings and auction model tests were added to the existing CI verification
job. Maven advisory checks, HIGH/CRITICAL image scans (including unfixed findings),
new-history secret checks and their failure behavior are preserved. No deployment,
registry publication, security suppression or workflow disablement was added.

## Combined verification

| Area | Result |
| --- | --- |
| Backend Java 17 Maven verify/package | 120 passed, 0 failed, 0 errors, 0 skipped; executable JAR produced |
| Dedicated database fixture | PostgreSQL 15.19 on localhost:55441 inside an isolated network namespace; all database test flags enabled |
| Frontend | ESLint, TypeScript and production Next.js build passed |
| Frontend models | 3 settings tests and 12 auction reconciliation tests passed |
| Compose | Configuration valid; database health requires final localhost TCP server |
| Migrations | Fresh seven-entry initialization; populated six-to-seven upgrade; replay; all seven source checksums; tampered NPL checksum rejected transactionally |
| Populated preservation | Synthetic users, existing SOLD player, team and purse hashes retained |
| Settings gateway API | Valid rupees, persistence/reload, cross-screen versions, simultaneous saves, quota ETag/If-Match and rollback passed |
| Auction gateway API | Four independent captain sessions, idempotent preassignment, authorization, forty-player Excel import, forty-four identities, ceiling, four-premium cap, purse reserve, gender feasibility, concurrent bids/sales and eleven-member squads passed |
| WebSockets | Two authenticated subscribers received forty distinct committed sales and monotonic state revisions; reconnection and photo updates passed |
| Cricket | Six unique league pairings, three fixtures/team, actual delivery scoring, expected points and NRR, top-two final, no semifinals, champion and runner-up passed |
| Certificates/photos | Captain participation eligibility and PDF generation passed; real delivery correction invalidated stale approval; explicit revocation passed; private photo bytes persisted |

Application/runtime Docker build inputs are byte-identical to the verified NPL
source. Existing matching images were reused rather than rebuilt: backend
`53555e8ae241`, frontend `737163502939`, database `72b1a49b63e7`. The combined
Compose configuration adds Phase 7's TCP readiness fix. Tests use new isolated
projects/volumes and private generated configuration; no company data or existing
test volumes were reset or deleted. Raw logs, credentials and runtime state remain
outside Git.

The authorized in-app browser reached the real login/dashboard using a disposable
administrator. On resumption, browser tooling reported **“Local browser use is
disabled in Work settings.”** The settings UI submission, reload, responsive/themes
and full browser auction journey therefore remain unverified. No workaround or full
browser pass is claimed. API integration and model tests do not replace that gap.

## Publication and security review

Reviewed newly reachable history, changed files, configuration templates, fixtures,
documentation and all four exported BuildKit contexts. Credential-pattern and exact
generated-private-value scans found no matches. Private build-context canaries were
excluded. No new runtime environment files, database backups, private photos, raw
private logs, actual room codes or account activation links are published. Synthetic
test strings and localhost-only disposable fixture defaults are not real credentials.

GitHub's authenticated API reported no main branch protection, no applicable rules,
no webhooks, no Pages configuration, no deployments and one active verification-only
workflow. The final remote checks are repeated before pushing. No repository policy
or required check is bypassed. Local original main is deliberately not reset to the
remote result, preserving its unrelated checkout.

CVE-2026-47890, CVE-2026-47884 and the critical runtime-image findings remain open.
The original six SBOMs and security inventory are preserved as historical scans,
not mislabeled as fresh scans of the NPL images. Hosted security jobs may fail and
their actual status must be reported. No production deployment, security-owner
acceptance, Spring Boot 4 migration or dependency remediation is part of this task.

## Safe company-PC update procedure

This source publication does not authorize a production upgrade. For an approved
test installation, back up PostgreSQL and player photos and verify restoration into
separate new volumes. Fetch the published commit into a separate clean checkout.
Retain the installation's existing private environment file, exact Compose project
name and volumes. Run `docker compose --env-file <private.env> -p <existing-project> build`,
then `docker compose --env-file <private.env> -p <existing-project> up -d`.
Check the migration job, seven matching ledger checksums, all health checks, login,
settings, photos, auctions and fixtures through the gateway. If application addresses
changed, recreate only the gateway after application health is established.
Never run `down -v`, reset volumes or enable demo seeding. Stop and investigate any
migration/checksum failure. Rollback across schema changes requires a tested
compatible image or coordinated database/photo restore, never edited checksums.
