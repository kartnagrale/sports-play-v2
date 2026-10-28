# Play-Neml — Phase 5 certificate management

Date: 2026-10-28 (Asia/Calcutta). Repository: `kartnagrale/sports-play-v2`. Branch: `feature/play-neml-upgrade`. Baseline: `e4ecb5c`.

**Status: COMPLETE for the supported championship certificate workflows. No production readiness is claimed.** Phase 6 was not started. Nothing was pushed, merged or deployed. Existing databases and volumes were preserved.

## Commits and continuity

| Stage | Local commit | Delivered |
| --- | --- | --- |
| A | `29d478b` | Eligibility, approval/issuance snapshots, secure APIs, revocation, PDF templates and migration |
| B | `c320574` | Certificate workspace, wizard, exact PDF previews, history and reissue |
| C | `29ad42e` | Atomic batches, ZIP delivery, opt-in verification, QR codes and final integration evidence |
| D | The commit containing this report; resolve with `git log -1 --format=%H -- docs/phase-5/REPORT.md` | Final review, evidence manifest and completion report |

Work continued in `C:/Users/karti/.codex/worktrees/play-neml-upgrade/sports-play-main`. The app could not attach this already-owned worktree, so commands used its explicit path. The original `main` checkout and its unrelated changes were preserved. The continuation resumed unfinished Stage C; it did not recreate Stage A/B. File inventory: `git diff --name-only e4ecb5c..HEAD`.

## Eligibility and result approval

The existing cricket and badminton engines complete a decisive final, update the authoritative champion/runner-up configuration and complete the final stage. Certificates consume this state rather than ranking an incomplete leaderboard. Both sports support **team Winner, team Runner-up, team Participation and individual player Participation**. Individual winner/runner-up and additional award categories are deliberately unavailable.

Before issuance, the assigned championship administrator approves the results with a mandatory 10–1,000 character justification. Approval requires the completed championship lifecycle, every generated fixture completed, exactly one decisive final, a completed final stage, matching authoritative champion/runner-up values and finalists belonging to the championship. Approval snapshots retain all match IDs, result revisions, stage types, finalist/winner IDs, eligible recipient IDs/names, approver and timestamp. There is no manual winner declaration or eligibility bypass.

Participation policy is fixed and explicit: teams must appear in completed matches; badminton players must have an assigned side in a completed rubber of a completed match; cricket players must belong to the locked playing XI of a completed match with a completed innings. Registration, Excel import, auction purchase or unused roster membership alone does not establish participation. Participation certificates become available after the same championship approval checkpoint as other awards.

Issuance rechecks the approval against current match revisions and participation identities. Current authorized recipient names are copied into each new issuance; subsequent name changes never rewrite existing PDFs. A correction/reissue can therefore use a corrected name while preserving the original certificate. All source data remains championship-scoped.

## Issuance, history and corrections

Each record contains an internal UUID, random public `PN-` reference, championship/approval IDs, category/type/recipient ID, immutable recipient and championship display snapshots, achievement, event/sport, issue date, administrator name/role, accent, template version and optional verification URL. Actor IDs, request payloads, revocation details and replacement relationships remain private administrative data.

Transactions lock the championship and validate locked source matches. Database constraints enforce tenant ownership of approval and recipient references, unique public references, per-tenant request IDs and one active certificate per category/type/recipient. Same-request retries return the original record; conflicting reuse returns a conflict. Concurrent identical requests issue once. A new request for an already active award fails instead of silently creating another certificate.

Revocation requires a reason and records the administrator/time without deleting history. Database triggers make issuance snapshots and audit records immutable. Match result/revision changes or deletion revoke affected championship issuances and invalidate approval in the same transaction; adding a fixture invalidates the previously approved competition scope. This conservative policy revokes all active certificates in that championship. Obtain a new approval and issue corrections with new IDs/references linked to the revoked originals. Reusing an old request returns its original, possibly revoked record, never a new valid award.

## PDFs and certificate designs

Backend generation uses [Apache PDFBox 3.0.8](https://pdfbox.apache.org/download.cgi), compatible with Java 17 and distributed under [Apache License 2.0](https://pdfbox.apache.org/). It produces vector A4 landscape pages with an ivory background, navy identity strip, restrained border geometry, serif award heading and prominent recipient typography. Winner uses gold, Runner-up silver and Participation a print-legible accent. Participation can use the existing championship primary color, darkened when necessary for paper contrast. The existing branding display name is snapshotted.

Recipient/body text uses the bundled embedded DejaVu Sans font, with its license included. Fixed headings use a standard PDF serif font. Text is wrapped and fitted in bounded regions; long names and unsupported characters are checked before committing issuance. Unsupported glyphs return a controlled error rather than silently changing names. No HTML, JavaScript, remote image fetch or filesystem path enters rendering. The issuer is the real administrator and the title is “Championship Administrator”; no executive identity, signature or corporate endorsement is fabricated.

PDFs are regenerated from immutable snapshots with versioned template/font resources and fixed document metadata. Identical snapshot rendering and repeated ZIP downloads were byte-identical in tests. The browser preview uses a backend PNG rendered from the actual PDF and also provides the PDF itself, avoiding dependence on an embedded browser PDF viewer. Previews are labeled and create no permanent issuance. Revoked downloads retain the historical original; the administrative view displays the revoked status and public verification reports it.

No durable PDF file or PDF blob is stored. Database snapshots, migration history and the matching template/font application version must be included in backup/recovery. Normal requests close PDF documents, streams and archive resources; browser blob URLs are revoked. Retain old template versions when introducing future layouts.

## Certificate management UI

The existing server screen mapping grants `/certificates` only to `CHAMPIONSHIP_ADMIN`. The workspace uses the shared PageHeader, cards, metrics, filters, tables, pagination, dialogs, notifications and player identity components. It shows actual category totals, revoked counts and issuance history, searchable by recipient/reference and filterable by category/status. Totals include historical issued records, including later revocations.

The five-step wizard selects award, approved championship event, eligible recipient, actual PDF preview and final confirmation. Generation reports success only after the server confirms it. History supports view/download/revocation and linked correction/reissue. Native dialogs provide keyboard focus containment, Escape and focus return. Desktop and mobile layouts and both themes were exercised; the history table scrolls within its own region. Existing private photograph retrieval and default avatars are reused in selection without embedding photographs into certificates.

## Bulk issuance and ZIP delivery

Bulk selection supports eligible players, a roster filter, eligible teams, winners and runners-up. The confirmation lists the actual selected recipients and award. A request accepts 1–25 recipients, deduplicates IDs and normalizes their order for idempotency. All issuance records/audits and the batch record commit together. Invalid eligibility or a later database failure rolls back earlier members. An unchanged retry returns the original batch and its current certificate statuses. No percentage or partial success is invented while the synchronous request is pending.

Issuance and ZIP download are separate operations: an archive failure cannot hide already-confirmed issuance. The successful dialog retains a download retry action; individual certificates also remain in history. Limits are 16 KiB certificate POST bodies, 25 recipients, a 90-second batch transaction timeout, two simultaneous batch operations and four simultaneous PDF/image render operations per process. ZIP generation processes one PDF at a time and caps total source PDF bytes at 8 MiB, plus bounded ZIP metadata/compression overhead. Larger requests must be divided into smaller batches; there is no unbounded asynchronous job queue.

ZIP filenames use only immutable server-generated references, never recipient names or uploaded paths. Tests opened the archives and each PDF, checked two unique safe filenames and the correct recipient/reference text, and proved repeat-download equality. A separate 25-PDF maximum-batch test passed. The browser archive was 28,977 bytes and contained exactly two valid PDFs; details are in `bulk-archive-validation.json` and `bulk-pdf-inspection.json`. Archives/PDFs are authenticated and returned with private/no-store cache controls.

## Public verification and QR codes

Public verification is **disabled by default**. An operator can explicitly set `CERTIFICATES_PUBLIC_ENABLED=true` and `CERTIFICATES_PUBLIC_BASE_URL=https://<approved-public-host>`. The URL must be HTTPS, except HTTP on loopback for local testing, and rejects credentials, query strings and fragments. No destination is inferred from a request Host header, and `play.neml.in` is not assumed deployed. Each issuance freezes its own URL; changing deployment configuration does not rewrite earlier certificates.

The PDF contains a vector QR code and clickable URI when enabled. [ZXing core 3.5.4](https://github.com/zxing/zxing/releases) creates the code locally. A test rendered the PDF at print resolution, decoded the actual QR bitmap and checked the exact configured URL/reference and PDF link annotation. A real issued bulk PDF was separately rendered with Poppler and its link inspected with pypdf. The live destination page was exercised anonymously.

`GET /api/certificate-verification/{reference}` returns `VALID`, `REVOKED` or `INVALID`. Malformed, unknown and disabled lookups return the same minimal invalid body with HTTP 200. Public references contain a random UUID rather than a sequence. Valid/revoked responses expose only reference, recipient display name, championship display name, sport, category and issue date. They do not expose player IDs, internal championship records, emails, photographs, issuer identity, approval metadata, credentials or revocation justification. The public page is marked noindex and responses are not cached. Enabling verification is an operator decision to publish these limited award details. Authentication/CSRF policy for every other API remains unchanged.

## Migrations and authorization

Three additive migrations use the existing checksummed/advisory-locked owner-run driver:

1. `2026-10-28-phase5-certificates.sql`: approval, issuance and audit tables; tenant/uniqueness constraints; source-result invalidation and immutable issuance triggers.
2. `2026-10-28-phase5-certificate-navigation.sql`: administrator-only server screen mapping.
3. `2026-10-28-phase5-certificate-batches.sql`: batch records, new-fixture invalidation and append-only approval/audit protections.

The fresh initializer and `tools/apply-migrations.ps1 -Migration <version>` include these versions in order. Historical migration contents/checksums were not changed. Fresh initialization, populated upgrades, repeat application and deliberate Phase 5 checksum mismatch rejection passed. The populated fixture retained its existing sold player, team, price, purse and version.

All administrative operations, including previews, history and PDF/ZIP downloads, require the existing `championshipSecurity.canManage` boundary. Super Admin does not inherit tenant management. Anonymous users, captains, viewers and foreign tenant administrators are denied. Only the minimal opt-in authenticity lookup is anonymous. Database composite foreign keys provide additional tenant protection. Existing Excel import, photo, auction and scoring service implementations were not modified.

## Executed verification

| Check | Actual result / evidence |
| --- | --- |
| Stage A | 9 certificate tests, PostgreSQL rollback/concurrency/auth, migrations and packaging passed before `29d478b` |
| Stage B | 3 API/Edge journeys, lint, TypeScript and production build passed before `c320574` |
| Final certificate backend | **18 distinct tests passed, 0 failures/errors/skips**: 12 database/API, 2 PDF, 4 verification/resource tests; `backend-test-summary.json` |
| Java 17 verify/package | Full 17-test `verify` succeeded in `stage-c-tests.log`; test-only additions then passed in `final-targeted-tests.log`, yielding 18 distinct tests. Production application code was unchanged between those successful runs |
| Bulk integrity | Normal and winner batches, duplicate IDs, same-key/concurrent retries, invalid eligibility, corrected sources, rollback after an earlier insert, foreign access and 26-recipient rejection passed |
| PDF/ZIP/QR | PDF parsing/text/dimensions, Unicode examples, very long/unbroken names, deterministic bytes, 25-PDF ZIP, actual QR decoding, archive filenames/counts and repeated archive equality passed |
| Public verification | Disabled policy performs no lookup; trusted URL validation, minimal fields, valid/revoked/unknown/malformed references and anonymous browser journey passed |
| Frontend | ESLint zero warnings; TypeScript passed; production build generated **26 pages** including `/certificates` and `/verify`; `stage-c-lint.log`, `stage-c-types.log`, `stage-c-build.log` |
| Final API/Edge journeys | **4/4 passed in 27.5 seconds**; `stage-c-browser.log` |
| Database safety | Fresh full initializer, populated upgrade, repeat application, preserved data and Phase 5 checksum rejection passed; `final-fresh-migrations.log`, `final-migrations.log`, `migration-validation.log`, `migration-checksum-rejection.log` |
| Dependency audit | 122 resolved Maven packages; only the existing Spring MVC advisory matches; `backend-dependency-tree.txt`, `backend-osv-audit.json` |

The real end-to-end fixture provisioned a championship/admin, imported two referenced Excel players, purchased both through real auction bids/sales, activated captains, confirmed/locked squads, generated league/playoff fixtures and completed genuine badminton rubber results. It then approved results, previewed/issued/downloaded a winner PDF, revoked and reissued it, issued runner-up/player participation awards, issued a two-team batch, downloaded its ZIP and verified public validity/revocation. Correcting the actual final through the result-correction API revoked every active certificate and changed public status immediately. Captain/viewer screen mappings and cross-championship denials were asserted. No success API was mocked. See `workflow.json`.

Tests used a newly created PostgreSQL 15 container labeled `purpose=play-neml-phase5` and synthetic persistent data. It was stopped and retained after verification. No existing database was reset. The initial continuation packaging failure was a Windows lock on the running test JAR; stopping that test process resolved it. It was not a remaining application build failure. No unchanged Phase 1–4 suite was unnecessarily rerun.

## Evidence and reproduction

Evidence is under [evidence/](evidence/), indexed by [SHA256SUMS.txt](evidence/SHA256SUMS.txt). Actual rendered outputs were manually inspected:

- [Winner](evidence/WINNER.png), [Runner-up](evidence/RUNNER_UP.png), [Participation](evidence/PARTICIPATION.png), [long name](evidence/long-name.png) and [QR award](evidence/qr-certificate.png).
- [Actual bulk PDF](evidence/bulk-1.pdf) and [its Poppler rendering](evidence/bulk-1.png); [second bulk PDF](evidence/bulk-2.pdf).
- [Wizard preview](evidence/wizard-preview.png), [history](evidence/certificate-history.png), [revocation](evidence/certificate-revoked.png), [mobile light](evidence/certificates-mobile-light.png) and [mobile dark](evidence/certificates-mobile-dark.png).
- [Bulk confirmation](evidence/bulk-confirmation.png), [bulk success](evidence/bulk-success.png), [valid verification](evidence/verification-valid.png) and [revoked verification](evidence/verification-revoked.png).

Reproduce with `tools/verify-phase5.ps1` on Java 17, Docker, Node/npm and installed Windows Edge. The runner creates only a new disposable database, enables local public verification explicitly, applies migrations, runs focused certificate verification/package, lint/typecheck/build and the four journeys, then stops its processes/container. Default test ports are PostgreSQL 55445, backend 58095 and frontend 53005. Run without already-bound test ports. `-SkipBuild` reuses a matching verified production build only. Runtime credentials and temporary configuration remain ignored and are absent from tracked evidence.

## Limitations and Phase 6 readiness

- Core certificate workflows are complete for cricket/badminton championships with a decisive finalized final. League-only/no-final championships, incomplete/tied finals and unsupported sports cannot receive approved awards. A full cricket delivery-to-final browser demonstration was not repeated; cricket participation is covered by real PostgreSQL sport-specific fixtures.
- Participation is completed-match participation at championship completion. Registration-only policy, unused substitutes and additional individual awards are unsupported. No discretionary manual eligibility override exists.
- Certificate title/accent reuse existing branding. Logo/signature uploads and configurable issuer titles are not implemented; arbitrary existing URL references are not fetched or trusted as approved assets. Photographs remain selection aids only. Certificates are authenticity records, not cryptographically signed/PAdES documents.
- The embedded font supports its actual glyph set; complete multilingual shaping and every script/emoji are not supported. Unsupported text is rejected before issuance. Physical printing, phone-camera QR scanning, Safari/Firefox, screen-reader certification and production load were not tested.
- Batches are deliberately synchronous and limited to 25, with per-process capacity limits rather than a distributed queue/shared limiter. Edge-wide rate limits, operator verification privacy policy, production DNS, backup restore and multi-instance capacity remain deployment work. Original ZIPs can contain later-revoked historical PDFs; verification remains authoritative.
- Further separately authorized Phase 6 development can build on these APIs and snapshots. Phase 6 was not started, and no production release is approved.

**CVE-2026-47890 and CVE-2026-47884 remain unresolved Spring MVC production security blockers.** The existing affected SSE-fragment/XSLT features remain unused; no suppression, security weakening or Spring Boot 4 migration occurred. See the preserved [Phase 1 dependency triage](../phase-1/DEPENDENCY-TRIAGE.md). Phase 5 does not establish production readiness.
