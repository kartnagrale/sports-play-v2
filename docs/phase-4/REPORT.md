# Play-Neml — Phase 4 completion report

Date: 2026-10-28 (Asia/Calcutta). Repository: `kartnagrale/sports-play-v2`. Branch: `feature/play-neml-upgrade`. Baseline: `eab7a5d`.

**Status: COMPLETE for the core Excel import and individual photograph workflows. Optional ZIP photograph import is deferred.** Phase 5 and certificate generation were not started. Nothing was pushed, merged or deployed. Production readiness remains blocked by the existing Spring MVC advisories.

## Commits and workspace

| Stage | Local commit | Result |
| --- | --- | --- |
| A — Excel registration | `6c606bc` | Implemented, verified and committed before photograph work |
| B — Private photographs | `47c26d7` | Implemented, reviewed, verified and committed |
| D — Final documentation | The subsequent documentation commit, discoverable with `git log --oneline -3` | This report, inventory and final evidence manifest |

Work used `C:/Users/karti/.codex/worktrees/play-neml-upgrade/sports-play-main`. The original `main` checkout and its unrelated changes were preserved. The app could not attach this already-owned worktree to this chat, so commands used its explicit path. No checkout reset, repository rename, existing database reset or production migration occurred.

The complete file inventory is [CHANGED-FILES.md](CHANGED-FILES.md). Source areas include the shared registration service, workbook/import services and controller, player reference/metadata mappings, photo processor/storage/service/controller, migration initializer/wrapper, existing player dialogs/page, existing identity consumers, auction photo events, focused tests and verification tools. No second avatar component or frontend component library was introduced.

## Excel import and template

Player Management offers Add Player, Import Players and Download Excel Template. The accessible import dialog supports file selection/drop, name/size guidance, row-level preview, validation summary, destination confirmation and persisted results. Failed files can be replaced. A preview can be recovered after refresh by reopening the dialog. Successful imports refresh the existing table/count while preserving search/filter state. Imported players have the normal default avatar until a photograph is added.

The `.xlsx` template contains a `Players` sheet and a separate instructions/examples sheet. Example rows are deliberately outside the import sheet. All six headers must be retained:

| Column | Values |
| --- | --- |
| Full Name | Required; trimmed; maximum 255 characters |
| Gender | Required; actual backend values `MALE` or `FEMALE`; Excel dropdown supplied |
| Skill Level | Optional text; maximum 255 characters |
| Base Price | Optional positive decimal; maximum 17 whole digits and two decimal places; blank uses championship settings at confirmation |
| Auction Order | Optional positive integer; blank assigns an unused order automatically |
| Player Reference | Recommended stable identifier; 1–64 ASCII letters/digits/underscores/hyphens, normalized uppercase |

The server checks actual OOXML package contents instead of trusting MIME type/extensions. Default limits are 2 MiB compressed, 16 MiB expanded content, 128 ZIP entries, 2,000 data rows and six columns. ZIP preflight and SAX worksheet parsing bound file processing; formula evaluation and workbook DOM loading are avoided. Unsupported/malformed files, macros, embedded content, external workbook links, missing/duplicate headers, formula/error/boolean cells, invalid fields and duplicate references produce controlled errors. Blank rows within the scan limit are ignored. Limits and preview lifetime are configurable; deployment changes should also update the default-limit UI guidance.

`PlayerRegistrationService` is authoritative for manual creation and bulk confirmation. It preserves championship/auction associations, configured BigDecimal prices, AVAILABLE status, automatic order assignment and roster mutability. Explicit orders are reserved before automatic assignment. Both paths reject closed registration phases and confirmed/locked rosters. Existing auction-order uniqueness remains enforced by the database. Manual create/edit was exercised through real APIs, and Add Player/save was exercised in the browser.

## Duplicates, confirmation and audit

References are unique within a championship. Same-name players with distinct references are separate people. Existing references are previewed as skipped and are never overwritten, including sold players and existing team/roster assignments. Duplicate references within the same workbook invalidate it. Missing references require explicit acknowledgement: exact-file retries are protected, but a changed/re-saved workbook without stable references can create additional people. Names are never used as identifiers.

An import has a server-generated ID, championship, authorized actor, SHA-256 checksum, sanitized file metadata, timestamps, expiry, status and counts. The server stores parsed rows temporarily, never raw workbook bytes. Successful confirmation immediately clears the payload; expiry cleanup scrubs it every minute while preserving audit metadata/counts. Preview defaults to 15 minutes.

Confirmation accepts the import ID/checksum, not client-supplied player rows. It binds the actor and championship, locks the championship, rechecks expiry, roster/lifecycle state, references and orders, then commits all additions together. Any failure rolls back the whole batch. Repeated and concurrent confirmations return the same completed result. Different concurrent imports serialize; a reference committed by the first is skipped by the second. A changed/conflicting preview is rejected for review. Completed status remains available through the authorized actor-bound endpoint.

## Player photographs and private delivery

Championship administrators can add, preview, replace and remove a photograph from Player Management. The dialog explains the square crop, validates file guidance and shows upload/processing status and errors. The table includes a Players without photographs filter. Existing PlayerAvatar/PlayerIdentity provide initials/default fallback when no image is present or retrieval fails.

The backend decodes JPEG/PNG, checks file and pixel bounds before full raster allocation, centre-crops/resamples to 512 × 512 pixels and writes a fresh JPEG at 85% quality. Source metadata/appended bytes are discarded; transparent areas use a white background. SVG, GIF, unverified content and unavailable decoders are rejected. WebP is not enabled because the runtime has no WebP decoder.

`PlayerPhotoStorage` separates processing/business logic from storage. The default implementation stores UUID-named immutable objects under configurable `PLAYER_PHOTO_DIR`, outside public assets; the default directory is gitignored. Only an opaque key, normalized byte size, update time and actor are stored on the player. No large image binary or client path is stored in the player table. Failed writes cannot commit a pointer. Rollbacks remove the newly written object; committed replacement/removal/player deletion removes the old object. A future private object-storage adapter can replace the conditional local bean without adding a cloud dependency now.

Retrieval uses `/api/championships/{championshipId}/players/{id}/photo?version=...`, checks `canView` and the player/championship association, and returns `Cache-Control: no-store, private`. Replaced/removed versions return 404. Filenames and filesystem paths are never exposed. The client fetches bytes through the existing Axios authentication path, supporting HttpOnly staff cookies and tab-local viewer bearer tokens, then renders an ephemeral blob URL through the existing unoptimized Next Image component. Blob URLs are revoked on replacement/unmount. Private images do not pass through a public optimizer or unauthenticated static route.

Cosmetic changes remain permitted after a player is sold or a roster is locked. Only photo metadata changes: auction status, sold/base price, team ownership, references, auction order, squad lock and match-related data are preserved. Unit/database tests verify replacement/removal and rollback cleanup against a sold player on a locked roster in the league phase.

## Avatar and auction integration

Photo URLs flow through PlayerDto, match/performer player references, bid DTOs and confirmed SOLD events. Existing identity consumers now pass them in Player Management, rosters, captain dashboard, squad confirmation, top performers, auction history/activity, spotlight and SOLD celebration. No additional player registration model was created.

Committed `PLAYER_PHOTO_UPDATED` events refresh connected auction snapshots; the existing authoritative periodic refresh remains. Photo changes do not change auction revisions or fabricate sale events. Phase 3 revision reconciliation, stable sale IDs, duplicate suppression, bid rules and STOMP authorization are preserved.

The real browser scenario imported a referenced player, uploaded an image, started an auction, placed legitimate captain/admin bids, recorded actual ranking animation, and sold that exact player to Phase Blazers for **₹60,000**. The spotlight and celebration displayed the current photo version. Replaying the actual committed sale frame, including after dismissal, did not replay the celebration. The next player without a photograph showed the default avatar. Post-sale cosmetic replacement preserved the winning team and selling price; rosters/history displayed the image.

## Migrations and database safety

Two additive tracked migrations use the existing checksummed/advisory-locked Phase 1 driver:

- `2026-10-28-phase4-player-import.sql`: stable reference column/check/tenant-unique index and import audit/preview table.
- `2026-10-28-phase4-player-photo.sql`: nullable photo key/size/time/actor columns, actor foreign key and metadata consistency check.

The fresh initializer applies these after the historical base schema and account-security migration. Historical migration SQL/checksums were not rewritten. Existing databases require explicit owner-run migration commands; unknown databases are not automatically baselined.

Fresh initialization, a separate populated upgrade, repeat application and deliberate checksum mismatch rejection passed. The populated fixture retained its sold player, team association, selling price, purse and version. Test runs created newly named disposable PostgreSQL 15 containers only, stopped them afterward and retained them for troubleshooting. Existing company/user databases and volumes were not reset.

## Security verification

Actual API/browser checks cover administrator-only imports/photo writes; anonymous, captain, viewer and foreign-championship denials; actor/tenant-bound confirmations; unchanged CSRF enforcement; private reads; missing/stale photo versions; and staff logout/revocation. Both a logged-out cookie context and a copied, revoked pre-logout cookie context receive 401 for protected image retrieval. Staff authentication is restored through a normal login for the remaining tests. A legitimate viewer bearer token reads its own room's image, receives 403 for a foreign championship and cannot upload. No credentials, tokens, original camera files, raw workbooks or temporary normalized image files are tracked in the evidence.

Uploads retain the bounded per-process limiter (default 30 requests per peer/minute), body/pixel/row/archive limits and safe error responses. Multi-instance deployments still need shared edge limits. Existing authentication/account revocation, origin policy, CSRF and STOMP boundaries were not weakened.

The initial Phase 4 new-dependency check found Boot-managed Commons Lang 3.17.0 through the new POI dependency path; pinning compatible 3.18.0 removed that additional advisory. The saved resolved 117-package audit then matches only the two existing Spring MVC advisories. No Phase 0/1/2/3 audit or framework migration was repeated during continuation.

## Executed verification and evidence

| Verification | Actual result | Evidence |
| --- | --- | --- |
| Java 17 Maven verify/package | 76 passed, 0 failures/errors; 3 unrelated Phase 1 DB tests skipped because their separate fixture environment was absent | `evidence/backend-tests.log`, `evidence/backend-test-summary.json` |
| Workbook/shared registration | Valid files, missing headers, invalid fields, empty/malformed/oversized files, formulas, defaults, order reservation, 2,000 rows | `PlayerWorkbookServiceTest`, `PlayerRegistrationServiceTest`, targeted log |
| PostgreSQL transactions | Mid-batch database rejection rolls back earlier inserts; reference races; expiry; actor/tenant binding; lifecycle and roster lock | `PlayerImportDatabaseTest` and full test log |
| Image/file handling | JPEG/PNG normalization, 512-square output, trailing metadata removal, SVG/GIF/forged content rejection, byte/dimension/pixel limits, private UUID storage/deletion | `PlayerImageProcessorTest` |
| Photo persistence/lifecycle | Actual files, replacement/removal, rollback compensation, sold/team/price/reference and locked-squad preservation | `PlayerImportDatabaseTest` |
| Frontend ESLint | Passed, zero warnings | `evidence/lint.log` |
| TypeScript | Passed | `evidence/types.log` |
| Next production build | Passed, 24 static pages | `evidence/build.log` |
| Auction model/reconciliation | 12/12 passed | `evidence/auction-model-tests.log` |
| Full real API/Edge browser journeys | 4/4 passed; Excel/manual/2,000-row flows plus photo security and auction | `evidence/browser-tests.log` |
| Supplemental photo/privacy/auction journeys | 2/2 passed; cookie revocation, bearer scope, mobile themes, ranking animation and duplicate committed-frame replay | `evidence/photo-browser-tests.log`, `photo-privacy-browser.log`, `photo-ranking-motion.json` |
| Migration checks | Fresh/populated/repeat/checksum rejection passed | `evidence/migration-*.log`, `upgrade-*.log` |

The full browser run used persistent test data and real APIs/backend/storage; no mocked successful import, image upload, bid or sale was used. Duplicate-event verification replays the actual committed server frame as fault injection. Screenshots use safe synthetic portrait bitmaps rather than random athlete photographs. Monitored admin/viewer pages reported no uncaught page errors. The full run and supplemental run use separate disposable fixtures; supplemental screenshots show the smaller two-player photo cohort.

Representative screenshots, manually inspected:

- [Invalid workbook](evidence/excel-invalid.png), [2,000-row preview](evidence/excel-preview.png), [committed result](evidence/excel-result.png).
- [Photo preview](evidence/photo-preview.png), [player table](evidence/photo-player-table.png), [mobile light](evidence/photo-players-mobile-light.png), [mobile dark](evidence/photo-players-mobile-dark.png).
- [Spotlight](evidence/auction-photo-spotlight.png), [mobile spotlight light](evidence/auction-photo-spotlight-mobile-light.png), [mobile spotlight dark](evidence/auction-photo-spotlight-mobile-dark.png).
- [SOLD with photo](evidence/auction-photo-sold.png), [mobile SOLD](evidence/auction-photo-sold-mobile.png), [team roster](evidence/photo-team-roster.png), [auction history](evidence/photo-auction-history.png).

`evidence/SHA256SUMS.txt` identifies the saved evidence. Reproduction and storage/migration commands are in [OPERATIONS.md](OPERATIONS.md). The targeted continuation command is `tools/verify-phase4.ps1 -SkipBuild -SkipBackendTests -PhotoOnly`; reuse only a matching verified build. Final application code did not change after the full backend/build checks; continuation extended verification and documentation.

## Deferred work, limitations and Phase 5 readiness

ZIP photo import remains a documented enhancement and has no exposed endpoint. It should later match stable references with bounded archive validation, preview/confirmation and explicit replacement consent, reusing the photo service. It is optional and does not block core Phase 4 completion.

Photograph storage currently requires an operator-managed private persistent filesystem with atomic rename. File/DB crash windows can leave inaccessible orphan files; normal rollback/commit cleanup is tested, but automatic crash-orphan collection and distributed object-store operations are not implemented. Coordinate database/file backups and reconcile orphans conservatively. WebP, automatic EXIF-orientation correction and an interactive crop/rotate editor are deferred; current server processing uses a centre square of the decoded pixels. Prepared upright JPEG/PNG images are supported.

Verification uses installed Windows Edge and synthetic data, not physical mobile devices, Safari/Firefox, screen-reader certification or production load. The broader tournament/match lifecycle, deployment, container/JDK scanning, backup restore and external notification delivery were not recertified. Existing frontend monetary formatting remains the application's existing Number-based presentation; persisted registration/auction money remains BigDecimal. Deployment-specific rate limiting, storage permissions/retention and privacy policy remain operator responsibilities.

**CVE-2026-47890 and CVE-2026-47884 remain unresolved Spring MVC dependency matches and production security blockers.** Affected SSE-fragment/XSLT features remain unused; no advisory suppression or Boot 4 migration was introduced. See the existing Phase 1 summary/triage. Core Phase 4 is complete for further separately authorized development; production release is not approved. Phase 5 and certificate generation have not begun.
