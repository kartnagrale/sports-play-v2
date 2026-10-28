# Phase 4 operations

Keep the Phase 1 production security blocker: CVE-2026-47890 and CVE-2026-47884 remain unresolved. This phase does not approve deployment.

## Database

Existing databases require the owner-run tracked migrations, in order, after the Phase 1 account-security migration:

```powershell
./tools/apply-migrations.ps1 -Migration 2026-10-28-phase4-player-import
./tools/apply-migrations.ps1 -Migration 2026-10-28-phase4-player-photo
```

Use the existing PG connection environment or `.pgpass`; never put passwords in committed scripts. The shared driver checks checksums and serializes migrations. Do not baseline an unknown database or reset volumes. The fresh Docker initializer runs the same migrations after the existing base schema; historical SQL checksums remain unchanged.

## Excel registration

Download the championship-qualified template from Player Management. Enter records in `Players`; examples and guidance live on a separate sheet so example players are never accidentally imported. All six headers must remain present; Full Name and Gender are the required values. Actual gender values are MALE and FEMALE. Blank price uses the current championship base price; blank order assigns automatically. Currency values support 17 whole digits and two decimal places. Skill/name limits are 255 characters.

References are optional, stable uppercase IDs of 1–64 ASCII letters/digits/underscores/hyphens. Same-name people with different references are separate players. Existing references are shown as skipped and never overwritten, including sold players and roster assignments. Duplicate references within one workbook are errors. Missing references require an explicit confirmation acknowledgement: changed files can create additional players, because matching by name is unsafe.

The server retains bounded parsed rows only until confirmation or expiry, never the workbook bytes. Every minute, expired payloads are scrubbed; metadata, counts and status remain for auditing. Parsed rows are necessary temporary personal information and require the same database/backups access restrictions as player records. Confirmation purges the payload immediately. An exact checksum identifies an import within its championship. Concurrent confirmations return the same committed result. Concurrent different workbooks serialize on the championship, then recheck references, orders, lifecycle and roster mutability. A conflict rolls back all inserts. Completed import results are available through the authorized, actor-bound status endpoint. Browser previews can be recovered after refresh by reopening Import Players.

Preview defaults: 15 minutes; 2,000 rows; 2 MiB compressed file; 16 MiB expanded ZIP content; 128 ZIP entries; six worksheet columns. Worksheet parsing uses SAX and never evaluates formulas. Macros, embedded content, external workbook links, malformed archives, formula/error/boolean cells and unknown workbook types are rejected. Config keys: `app.players.import.max-rows`, `max-bytes`, `max-expanded-bytes`, `preview-lifetime`. The UI advertises the default limits; synchronize guidance if deployment limits change. Servlet multipart defaults are 5 MiB per file and 6 MiB per request.

Uploads have a bounded per-instance, per-peer limit of 30 requests/minute (`app.players.upload-attempts-per-minute`). Tune a shared edge limit before scaling. CSRF, cookie/bearer authentication, account revocation and championship authorization retain the existing application conventions.

Apache POI 6.0.0 supports Java 17 ([official release](https://poi.apache.org/changes.html)); it is the only new application dependency. No frontend dependency was added.
The newly introduced Commons Compress path also brings Commons Lang. Boot's managed 3.17.0 matches CVE-2025-48924, so this phase pins the compatible patched 3.18.0 release. The resolved 117-package OSV scan then returns only the two pre-existing Spring MVC matches; see `evidence/backend-osv-audit.json`.

## Player photographs

Authorized championship administrators can upload, replace or remove photographs, including after roster confirmation/locking and after a sale. These operations only change `photo_key`, `photo_size`, `photo_updated_at` and `photo_updated_by`. They do not change auction state, money, team ownership, fixtures, identity, references or squad confirmation. Upload/removal actor IDs and timestamps remain in the player metadata; application logs record the corresponding championship/player/actor IDs without image contents.

JPEG and PNG are decoded with Java ImageIO, bounded to 5 MiB, 4096 pixels on either side and 12 million pixels before allocating the full raster. File extensions, declared content types and original names are never trusted for storage. SVG, GIF and undecodable formats are rejected. WebP is intentionally unsupported because this Java 17 runtime has no installed WebP decoder. Accepted pixels are centre-cropped and resampled to a 512 × 512 JPEG at 85% quality, with a white background for transparency. Original image metadata and appended content are discarded. The preview explains the square crop. Automatic EXIF-orientation correction and an interactive crop/rotate editor are future enhancements; use an upright, prepared image when source camera orientation requires it.

`PlayerPhotoStorage` is the storage boundary. The default local implementation writes immutable UUID objects under `PLAYER_PHOTO_DIR` (default `./var/player-photos`, gitignored). Keep this directory outside static/document roots, restrict OS permissions to the application operator, and persist/back it up alongside the database. A writable filesystem supporting atomic rename is required. A failed write cannot commit its database pointer. Rollbacks delete newly written objects; committed replacements/removals delete the old object. Deleting a permitted player also removes its photograph after commit. Cleanup failures log the opaque object key for operator reconciliation. A process crash between the filesystem operation and transaction completion can leave an inaccessible orphan; reconcile unreferenced UUID files against `players.photo_key` after a conservative grace period. Do not delete live or in-flight objects. There is no distributed file/database transaction or automatic crash-orphan collector.

For production, provision a private persistent volume for a single application instance. For multiple instances, implement a private object-storage adapter at the same interface (put/read/delete), provide it as a Spring bean, and retain private authorization through the controller. The conditional local bean backs off. No cloud SDK or public bucket is introduced in this phase. Object retention, retries/reconciliation and backup recovery remain deployment responsibilities.

Image retrieval is championship-qualified and requires the existing `canView` authorization on every request. URLs carry a UUID version that remains valid only until replacement/removal. Old versions return 404. Responses are JPEG with `Cache-Control: no-store, private` and a generic inline filename. No filesystem path or original filename is exposed. The existing PlayerAvatar retrieves private image bytes through Axios with HttpOnly staff cookies or the current tab's viewer bearer token, then uses an ephemeral blob URL with Next Image optimization disabled. Blob URLs are revoked on replacement/unmount, and failed/removed photos fall back to the existing initials/default avatar. Private images never pass through a public Next image optimizer.

PlayerDto, match/performer player references, bid activity and SOLD events carry the versioned photo URL. Existing PlayerAvatar/PlayerIdentity consumers cover management, rosters, captain dashboard, squad confirmation, performers, auction history, spotlight and celebration. Committed `PLAYER_PHOTO_UPDATED` events refresh live auction snapshots immediately; the existing 15-second authoritative refresh also reconciles photos. Event transport retains Phase 1 tenant authorization and after-commit delivery, and changes no auction revision/bidding rules. Profile updates do not trigger a SOLD celebration.

## Optional ZIP photo import

Deferred. Core Excel registration and individual photographs are the Phase 4 deliverable. Stable references and the reusable photo service support a later bounded ZIP preview/match/confirm workflow. No ZIP photo upload endpoint is exposed. Never match by player name or silently overwrite existing photos.

## Reproducible verification

`tools/verify-phase4.ps1` creates a newly named PostgreSQL container on localhost:55444, applies and re-applies migrations to a separate populated fixture database, runs Java 17 verification, starts the real backend on localhost:58094 and Next production build on localhost:53004, and executes the Edge integration suite. It stops its application processes and retains its own container stopped for troubleshooting. It never starts or modifies an existing database. Test accounts/passwords are generated in memory and are absent from committed evidence. Raw request bodies, cookies and tokens are not captured. Screenshots show synthetic players only.
