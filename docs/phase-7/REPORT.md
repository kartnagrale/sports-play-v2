# Play-Neml Phase 7 — security and release assessment

Date: 2026-10-29, Asia/Calcutta. Branch: feature/release-readiness. Starting main: `3c0aab0cbff94fa5b3445b1947e890cc0a0a62a7`.

**Phase 7 verification/finalization: complete with unresolved release gates. Production readiness: BLOCKED / PENDING SECURITY AND OPERATIONS APPROVAL.** No production deployment, DNS, real certificates, company database or notification consumer was modified. Both Spring CVEs remain unresolved. Additional image findings are retained. Critical security gates fail, so this branch must not be pushed under the user's explicit stop-before-push instruction. No production approval or risk acceptance is claimed.

## Security disposition and framework compatibility

See [security decision](SECURITY-DECISION.md) for official advisories, source evidence and the coordinated migration plan. The current Boot 3.5.16 / Framework 6.2.19 / Java 17 stack is unchanged. The resolved Maven tree contains 122 packages; fresh OSV queries match spring-webmvc to GHSA-j9f9-w8pj-32f8 and GHSA-pc63-qcmh-9cmg (CVE-2026-47890 and CVE-2026-47884).

Spring's official severities are LOW and MEDIUM respectively; Trivy/GHSA report CRITICAL package findings. Both classifications are preserved rather than reconciled by suppression. No affected SSE-fragment or XSLT view path was found. REST controller mappings and STOMP JSON events do not demonstrate reachability of those features. Unused features do not remove vulnerable code or authorize risk acceptance.

Official documentation confirms Boot 4.1.2 is stable and Java 17 compatible, with Framework 7.0.10 minimum; Framework 7.0.9 fixes both advisories. A compatible 6.2.20 fix requires legitimate Enterprise access, which was not supplied. No unauthorized artifacts were obtained.

No major upgrade was committed. Concrete prerequisites include Jackson 3 compatibility for persisted certificate/import/configuration snapshots and notification payloads, replacement of Boot test modules/@MockBean, removal of incompatible Tomcat 10 overrides, and proof of Security 7/JPA 3.2/Hibernate locking behavior. Those upgraded-stack contracts are not verified here. This is a deferred migration, not a claim that Boot 4 is impossible. The existing coherent application is preserved without mixing Spring 7 into Boot 3.

## Dependency and image scans

Fresh npm package-lock audit reports zero findings. That does not clear the Node image's bundled tooling or Debian packages. Maven OSV scan exits nonzero for the two retained advisories. [Security inventory](evidence/security-inventory.json) records package versions, IDs, severity, available fixes, scan identity and applicability status for every image finding. Six CycloneDX SBOMs cover Maven, npm and all four built runtime images. Raw scanner logs stay ignored under tmp.

Trivy 0.76-family scanner image is pinned by digest in CI and the inventory. It scanned the actual Phase 6.5 image IDs, including runtime OS packages and Java/Node/Go artifacts. Application/base images were not changed or rebuilt. Scanner DB initialization exceeded the first timeout; a retry with cached databases and a 15-minute timeout completed. A concurrent OS-only attempt hit a cache lock and is not counted as successful evidence.

| Runtime image | CRITICAL | HIGH | MEDIUM | LOW | UNKNOWN |
| --- | ---: | ---: | ---: | ---: | ---: |
| Java backend | 2 | 0 | 27 | 16 | 0 |
| Node frontend | 1 | 66 | 97 | 84 | 1 |
| PostgreSQL | 1 | 25 | 31 | 3 | 1 |
| nginx gateway | 1 | 2 | 3 | 44 | 2 |

Counts are package/advisory occurrences, not independently reachable exploits. Trivy's status `fixed` means a fix exists upstream, not that the installed image is fixed. Highlights requiring owner remediation/triage:

- Backend: Framework 6.2.19, both named CVEs; OSS fix 7.0.9 requires coordinated Boot migration.
- Frontend: Debian zlib1g CVE-2023-45853 marked will_not_fix; bundled npm dependency findings include brace-expansion, ip-address, pacote, picomatch and sigstore. The lockfile application audit does not cover those base-image copies. No runtime reachability clearance is claimed.
- PostgreSQL: Go stdlib v1.24.6 in gosu, including CVE-2025-68121; inventory lists fixed versions. Presence in a privilege-switching binary is not proof that the reported TLS path is invoked; owner triage remains necessary.
- Gateway: brotli-libs 1.2.0-r1 CVE-2026-111497 has fix 1.2.1-r0; curl/libcurl 8.22.0-r0 findings list 8.23.0-r0. Update the pinned base/package set only with a new build, scan and regression. No untested image patch is represented as complete.

## Implementation and CI

A fresh startup exposed a readiness race: pg_isready could observe PostgreSQL's initialization Unix socket while the final TCP server was unavailable. Compose health now requires localhost TCP, preventing the migrate dependency from relying on the temporary socket. No schema or migration checksum changed.

CI now initializes all six migrations, enables Phase 4–6 database tests plus the dedicated Phase 1 fixture, keeps frontend lint/type/build/audit, queries resolved Maven advisories, scans all four built images for HIGH/CRITICAL findings including unfixed ones, and scans newly reachable commit blobs for credential patterns. Permissions are read-only and duplicate runs cancel by ref. No publish/deploy step exists. Current known findings intentionally fail security gates. Workflow YAML was parsed locally; hosted CI has not run because publication is blocked. Repository administrators must configure required branch checks; a workflow file alone does not enforce branch protection.

The secret-pattern gate is defense in depth, not a guarantee of credential absence. Generated private values require an additional exact-value scan before any publication. New SBOMs/inventory contain package metadata, not runtime env files or private workflow evidence.

## Verification ledger

Fresh Java 17 Maven verify/package passed: **99 tests, zero failures/errors, zero skips**, including all three previously skipped DatabaseSecurityIntegrationTest cases. The six-migration PostgreSQL 15.19 fixture supports the import/photo/certificate/analytics database tests; cricket engine/rule regressions are included. The emitted JAR was packaged successfully. Prior evidence remains available in [Phase 6](../phase-6/REPORT.md) and [Phase 6.5](../phase-6.5/REPORT.md), without counting those older runs as newly executed tests.

Frontend lint, typecheck and 18 model tests passed. Nine distinct container browser journeys passed: three auction/portal journeys in the initial run and all six affected certificate/analytics/reconnect journeys on rerun. The initial certificate preview PDF response succeeded, but PNG preview visibility exceeded the default five-second assertion under concurrent build/scan load. The bounded expectation timeout is now twenty seconds; the affected journeys passed without application changes or mocked APIs. A production Next build is reused from the unchanged Phase 6.5 image.

Fresh-volume verification of the health fix passed: final TCP readiness, migration-job success and exactly six ledger records. Fresh container checks passed CSRF, origin/WebSocket rejection, restricted credentials, private-photo authorization, non-root/read-only containers and build-context private canaries.

A full cricket delivery-to-final browser journey was not performed; engine/rule tests and cricket presentation checks are narrower evidence. Real HTTPS simulation, physical-phone QR scanning, external notification delivery and multi-host certification also remain unverified. These gaps must not be hidden by the passing badminton journeys.

The Phase 1 test port is reserved by Windows. Instead of changing the test's isolation guard, its PostgreSQL fixture runs on port 55441 inside a dedicated container network namespace shared only with the Maven test container. Thus localhost/phase1 URL restrictions remain intact; no existing database was repurposed. All generated credentials remain ignored. The initially unsuccessful host-port fixture was retained without starting or attaching user data.

Docker regression uses new random project-scoped volumes and credentials, existing verified images retagged locally, and explicit disposable-only QR verification/HTTP settings. No original Phase 6.5 volume is mounted. Reusing existing application layers avoids rebuilding identical images.

Full Compose down/up, backend recreation and service restart all passed saved comparisons: every row in all 43 tables and both stored JPEGs matched, valid/revoked certificate records survived, and authenticated PDFs still downloaded. Migration replay, populated upgrade and tampered-checksum rejection passed. The custom database dump restored into a separate empty database with all table hashes matching; both copied photo hashes matched.

The additional recovery harness then restored that matched backup into another isolated database and NEW photo volume, set ownership to UID/GID 10001, and started a separate read-only backend with bootstrap/demo/public verification disabled. All 43 restored table hashes and both photo hashes matched. Real login, authenticated user/photo requests, valid/revoked certificate history and PDF download passed. Public verification returned INVALID as configured. Recovered backend startup and clean shutdown passed. Test volumes/backups are retained privately; all disposable services are stopped. This is same-host recovery evidence, not off-host disaster-recovery certification. [Sanitized verification](evidence/verification.json) records the results.

Trivy executable version was verified as 0.76.1. Scan coverage does not independently certify every embedded JDK implementation path; package scans and SBOM inventories are not runtime reachability proofs. The new secret gate was tested on a separate synthetic Git fixture: clean commits passed and a generated credential canary in new history was rejected. Workflow YAML parses; remote execution remains untested because this branch is not published.

## Operations and approval gates

See [release operations](RELEASE-OPERATIONS.md) for backup schedule proposals, rotation, monitoring, database-owner procedures, rollback, notification-consumer compatibility and required approvals. The in-process broker/private local-volume design remains single-host. Resource limits, bounded ephemeral logging, non-root permissions, restricted DB credentials, photo privacy, exact origins and cookie/CSRF controls retain the Phase 6.5 contract.

No real TLS/DNS endpoint was available. Secure/HttpOnly/SameSite behavior can be checked locally, but no public TLS or multi-node certification is claimed. An HTTPS staging simulation was not performed. Real certificate-verification privacy settings, notification delivery, central error monitoring, off-host backup retention and RPO/RTO still require operations/security/data-owner approval.

## Git and preservation

Work is isolated in the release-readiness worktree. The original main checkout's 619 file hashes match the historical preservation manifest; its HEAD, branch and unrelated modifications are preserved. Historical feature/play-neml-upgrade remains at `917885ec52eb1b103eddcc75a9f5a57b8dd37c18` and is not an ancestor of this branch. Sanitized history is not rewritten.

Local verified changes may be committed with the release blocked. Main publication is prohibited while critical security checks fail. GitHub main remains the starting Phase 6.5 SHA unless explicitly verified otherwise in the final response. No claim of Phase 7 success or production release approval is made while these gates are open.

Local commit: resolve with `git log -1 --format=%H -- docs/phase-7/REPORT.md`. Final review includes all intended staged files, six SBOMs and the complete newly reachable commit objects; exact private values and credential patterns must have zero matches. Runtime configs, snapshots, backups, private photos and raw logs remain ignored. GitHub main was verified at the starting SHA and is deliberately left unchanged.

Before production: resolve or obtain explicit security-owner disposition for both Spring findings and the remaining image findings, rebuild/rescan/retest any changed images, complete the unverified cricket/HTTPS/consumer checks, obtain security/database/operations approvals and configure required branch gates. No approval is fabricated and no deployment is scheduled.
