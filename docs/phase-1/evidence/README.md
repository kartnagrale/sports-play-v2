# Phase 1 evidence

Final claims use these outputs from 2026-10-27, produced in the isolated upgrade worktree:

- `backend-database-verify.log`: Java 17 Maven verify/package, 57 passing tests with the dedicated PostgreSQL integration environment enabled.
- `maven-java17-version.log`: executed wrapper/runtime versions.
- `final-targeted-security-tests.log`, `final-targeted-java-version.log`: final JWT/STOMP test run on Java 17, including a well-formed JWT signed with an untrusted key. The earlier full 57-test run is retained unchanged; it predates this additional test-only case.
- `npm-ci.log`, `frontend-lint-final.log`, `frontend-typecheck-final.log`: clean locked install and warning-free lint/types.
- `frontend-runtime-build.log`, `frontend-runtime*.log`: production build and local Next startup using test URLs.
- `runtime-security.json`: 83 passing API/STOMP assertions, including signed expiry, both Phase 0 defects, tenant denials, concurrent bids, purse consistency, broadcasts and revoked subscriptions.
- `browser-tests.log`: three passing headless Edge journeys. Browser tracing/video/screenshots are disabled so setup secrets are not captured in artifacts.
- `database-bootstrap.log`, `migration-*.log`, `upgrade-*.log`: fresh schema, populated synthetic upgrade, repeats, checksum rejection, opt-in legacy quarantine and preservation assertions.
- `docker-fresh-init*.log`: a separate temporary PostgreSQL container executed the Compose initialization mounts and shared migration driver.
- `database-validation.log`: zero new application-owned queue secret fields and zero malformed activation hashes.
- `compose-validation.log`: successful configuration validation; not an application-image build.
- `npm-audit*.json`: zero final affected package entries.
- `backend-dependency-tree.txt`, `backend-osv-audit.json`, `backend-final-advisory-details.json`: resolved Maven graph and the two remaining advisory matches.
- `security-review.json`: JWT-pattern review of saved logs and a static check for configured affected SSE/XSLT features. This supplements, not replaces, manual reachability analysis.

Files named `initial`, `current`, `refined`, `intermediate` or `stabilization`, and the earlier `backend-verify.log`/`frontend-build.log`, retain development checkpoints. They may include failures corrected later or fewer tests. `backend-advisory-details.json` describes the intermediate post-Boot-upgrade matches, before the compatible maintenance overrides; use the final scan for current findings. Do not treat any intermediate checkpoint as the final result.

`SHA256SUMS.txt` covers the raw output bytes. Local `.gitattributes` disables line-ending conversion for those outputs. Tool-generated trailing whitespace is retained intentionally; authored documentation and source were checked separately.

All runtime accounts/data are synthetic. PostgreSQL was confined to the labeled test container on localhost:55441 and new test databases. Backend/Next services used 58092/53001 and were stopped by the runner. The test PostgreSQL containers were stopped after evidence collection. No user/company database, main checkout content, DNS/TLS or production hosting was modified. CI was configured but never remotely run; broader tournament/mobile/accessibility/load/image-OS coverage remains outside these results.

See [operations](../OPERATIONS.md) for reproduction commands and [report](../REPORT.md) for completion status and outstanding risks.
