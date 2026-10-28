# Play-Neml — isolated leadership demonstration

Use only `feature/play-neml-upgrade` in its existing worktree. This demonstration creates synthetic local records through authorized APIs. It does not enable production seeding or public verification, push code, deploy, or touch an existing company database.

## Prepare

Requirements: Windows/PowerShell, Java 17 (the runner defaults to `%USERPROFILE%/.jdks/ms-17.0.20.1`), Node 22.20.x/npm 10.x, Docker Desktop, installed Microsoft Edge, and the repository's existing Maven/npm dependencies. The tested application is Spring Boot 3.5.16, Next 16.4.3/React 19.3.1 and PostgreSQL 15. Existing production blockers **CVE-2026-47890 and CVE-2026-47884 remain unresolved**.

From the worktree root, run:

```powershell
./tools/verify-phase6.ps1 -KeepRunning
```

This creates a uniquely named PostgreSQL container with `purpose=play-neml-phase6`, initializes the existing tracked migrations, runs backend/frontend/browser checks and prepares two synthetic experiences. It explicitly sets demo-only `CERTIFICATES_PUBLIC_ENABLED=true` and `CERTIFICATES_PUBLIC_BASE_URL=http://localhost:53006`. Production defaults remain disabled. `DATA_SEED_ENABLED=false` remains set. Database/JWT/bootstrap credentials are generated randomly and saved only under ignored `tmp/`; no demo password is committed or printed.

Open `http://localhost:53006`. Read local account details from `tmp/phase6-demo-access.json` privately; do not project, email, commit or paste that file. It contains separate administrator/captain accounts, live viewer room code and completed-championship certificate URLs. The platform administrator's private account is in `tmp/phase6-runtime.json`. Each account uses normal login, HttpOnly cookies, activation and tenant rules. Use separate Edge profiles/InPrivate windows for staff roles so cookie changes do not replace another demonstrator's session. Viewer bearer state is tab-local.

The runner uses backend `58096`, frontend `53006` and PostgreSQL `55446`, all bound to loopback. It copies the backend JAR into ignored runtime storage so packaging does not collide with Windows' lock on a running JAR. If already running, stop the previous demo first. `-SkipBuild`/`-SkipBackendTests` are for a matching verified build only; the default executes checks.

## The two experiences

**Live arena** — `Phase 6 Leadership Live Arena`: two teams, two imported referenced players, one synthetic player portrait and one default avatar. Auction configured but not started. Small valid badminton rules: Open Singles, one player per side, a two-team league and top-two final. Prices start at 100 with increments of 10 and a 300-second demonstration timer. These synthetic settings are not recommended production tournament rules.

**Completed archive** — `Phase 6 Sports Championship`: real imported registrations, auction purchases, locked squads, completed league/final, standings, history and freshly approved Winner, Runner-up and player Participation certificates. Existing revoked records remain available to demonstrate audit history. QR URLs use the isolated host, never an assumed deployed `play.neml.in`.

## Demonstrate the complete workflow

1. **Platform administrator:** open Championships. Explain total/active/completed counts, registered participant records, sport/lifecycle charts and recent competition activity. Launch a separate championship through Launch Championship if demonstrating provisioning live. Use synthetic names and an `example.invalid` email. Keep its activation setup link private and activate its administrator normally. Platform access does not grant tenant administration.
2. **Championship administrator:** use Players in the live arena. Download the Excel template and explain stable Player Reference values and preview/confirmation. The provided two-row workbook is `docs/phase-5/fixtures/players.xlsx`; it is already imported into the prepared arena. Re-import it to demonstrate duplicate reference skips, or use a newly provisioned championship for a fresh import. Preview does not create players; confirm only after reviewing validation. Do not add extra players to the prepared one-player-per-team fixture unless you also adapt the roster/auction plan.
3. **Photographs:** use the photo action on Asha Rao, select `docs/phase-6/fixtures/portrait.png`, preview and save. This is a generated geometric test portrait. The other player demonstrates the default avatar. Explain private authenticated retrieval; never use an executive's photo or signature without authorization.
4. **Auction:** open the viewer room and the two captain windows. The administrator starts the auction. Captain A bids 100; Captain B bids 110. Observe the authoritative leader/rank changes and confirm the SOLD celebration only after the administrator sells. Verify the winning roster and purse. The second player must be purchased by the other team at 100 so each one-player roster is complete. Complete the second sale. The timer is live: pause the auction before speaking for longer than its 300-second deadline. Reconnect a viewer to show authoritative state recovery without replaying a historic celebration.
5. **Squads and competition:** each captain opens Squad Confirmation and confirms their one-player roster. The administrator locks all squads. Competition configuration is already prepared; review it, generate the league, and open Matches. Assign one player per side to Open Singles and confirm a valid score such as 21–9. Observe viewer results/standings. Generate the top-two final and complete it with another valid result. Do not manually declare winners from an incomplete table. In a new championship, configure roster limits/sport rules/scheduling before fixtures; these settings lock as play progresses.
6. **Certificates:** in Certificates approve finalized results with an explicit justification. Select Winner or Runner-up, the approved event and eligible team, inspect the PDF-derived preview, confirm and generate. Download/open the vector PDF. For Participation choose players who actually played. Bulk generation accepts up to 25 and confirms the selected award/recipients before issuance. Existing active awards reject accidental duplicates; use the completed archive or the freshly finished arena appropriately rather than issuing the same award twice.
7. **Authenticity and correction:** open a printed QR URL or use `http://localhost:53006/verify`. Show Valid. Revoke one synthetic certificate with a reason; reload the public page and show Revoked. Reissue with a new reference if appropriate. Explain that correcting authoritative match results revokes affected awards and requires a fresh result approval. Use the archive when time is short; its private access file lists prepared verification URLs.

Narration checkpoints: “register once”, “authorize every action”, “one committed sale”, “results establish awards”, “history survives correction”. All figures come from the backend; an empty chart means there are no records, not a fabricated zero-success demo.

## Checklist and recovery

- Confirm login/activation, both themes, server connection label, portrait/default avatar, captain restrictions and read-only viewer navigation before presenting.
- Confirm the final winner/runner-up, PDF readability, ZIP contents and public QR status. A local QR URL works only on the demo host; a phone cannot reach its `localhost`. Open the link on the presenting machine, or configure a separately approved reachable HTTPS demo host before issuance.
- If disconnected, show the visible interrupted state, restore the transport and wait for resynchronization. Match pages also perform a bounded 30-second fallback refresh. Do not announce a successful bid or certificate before its server response.
- To restart retained services, run `./tools/phase6-runtime.ps1 -Action Start`, then in a separate terminal: `node frontend/node_modules/next/dist/bin/next start -p 53006 -H 127.0.0.1`. Stop an already-running frontend first. The build must target backend/WS `http://localhost:58096`.
- Re-run `node tools/seed-phase6-demo.mjs` after successful workflow tests to restore valid archive certificates. It preserves an existing live arena rather than resetting its data. A completed auction is not reset automatically. Provision a fresh synthetic cohort for another live presentation.
- Stop the helper-owned frontend process/terminal and run `./tools/phase6-runtime.ps1 -Action Stop`. The helper checks the exact backend command and container label; PostgreSQL is retained, not deleted. No volume reset command is needed.
- For a fresh disposable environment: stop it, move `tmp/phase6-runtime.json`, `tmp/phase6-demo-accounts.json` and `tmp/phase6-demo-access.json` to a uniquely named private subfolder under `tmp/`, then rerun the full verifier. This creates a new container and keeps the previous one recoverable. Never point these helpers at existing user/company resources. No `docker compose down -v`, database drop or checkout reset is part of this runbook.

Known limits: local single-process/simple-broker tests are not production-scale certification; external notification delivery is not demonstrated; the complete live scoring walkthrough uses badminton. Cricket scoring rules retain regression coverage and its standings display net run rate, but no new cricket functionality is invented. All accounts/data are synthetic. Phase 7 and production release are outside this demonstration.
