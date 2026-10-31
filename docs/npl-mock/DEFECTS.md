# Full mock defect and repair log

## NPL-MOCK-001: off-increment bids accepted

- Reproduced through the actual authenticated captain API on main `4488316c6b2c8b63cb9d14fe599f947ac44cb4ce`.
- Rules: opening price 5000, increment 1000. Direct opening bid 5500 returned HTTP 200 instead of 400.
- Root cause: `TenantAuctionService.bid` only compared the amount with the next minimum; it did not check divisibility by the configured increment.
- Recovery: paused auction and undid the pending 5500 bid using admin APIs before any sale. All four team purses remained 100000. No database edits.
- Repair: enforce an exact increment multiple relative to the preceding bid (or synthetic preceding amount of opening price minus increment). Legal multiple-increment jumps remain valid.
- Regression: four parameterized service cases cover off-increment opening/follow-up bids, including fractional rupees, and assert no bid or financial write. `AuctionPresentationTest`: 7 tests passed, none skipped.
- Gateway retry and final full-suite evidence are recorded in the full report.

## Execution issues classified separately

- The test harness initially expected 403 where the real gateway returns 401 for unauthenticated/error-dispatched requests. Both deny the request. Invalid import confirmation returns 409, as designed. Test expectations were corrected; security was unchanged.
- Packaged Windows Python redirects LocalAppData file access. XLSX fixtures now use the repository's ignored `tmp` directory; credentials remain in protected external storage.
- An initial backend suite overlapped another compilation in the same host target directory, causing missing-class failures. That run is invalid evidence. Subsequent suites use container-local output directories.
- One legacy supplemental settings test used a fixed fixture password. Its generator now uses cryptographic randomness. Active-account reactivation correctly returned HTTP 409; that policy was preserved. The initial disposable project was stopped with all data intact, and the full workflow was repeated in fresh `playneml-npl-mock-verified` with generated credentials throughout.
- Windows briefly denied atomic checkpoint replacement while a reader/scanner held a file handle. The harness now retries transient sharing errors without truncating the valid checkpoint. The fresh workflow resumed its live innings successfully without resetting any application data.

## NPL-MOCK-002: final correction retains stale champion and completion

- Reproduced by reversing the final's last legal delivery with the authorized correction API.
- Actual before repair: final match LIVE, while championship/status/final stage remained COMPLETED and champion/runner-up remained populated.
- Existing certificate triggers correctly revoked certificates and invalidated approvals. They did not manage competition lifecycle.
- Root cause: cricket correction reset the match but never reversed the championship and stage completion metadata.
- Repair: reopen the stage and atomically clear final awards, return the championship to ACTIVE/FINAL. A restored decisive delivery completes it normally.
- Regression: `CompetitionResultCorrectionTest`, plus the real correction/reapproval/reissue workflow. The harness restores the decisive delivery before asserting observations.

## NPL-MOCK-003: frozen cricket league can be corrected after top-two final generation

- Actual correction returned HTTP 200 after a final already existed. The harness immediately restored the original delivery through scoring.
- Root cause: cricket lacked the downstream-fixture correction guard; the shared guard only recognized semifinals after a league, missing TOP_TWO_FINAL.
- Repair: enforce the guard for cricket delivery corrections and recognize either semifinals or a final as downstream of league results.
- Real retry returns HTTP 409 without modifying the frozen result. The focused regression verifies the no-semifinal case.
- Before/after observations are retained in `evidence/correction-before-repair.json`, `correction-result.json`, `frozen-league-before-repair.json`, and `frozen-league-correction.json`.

## NPL-MOCK-004: re-completing a legacy corrected league loses FROZEN status

- Independent read-only SQL reconciliation caught league status COMPLETED after the pre-repair negative correction had been restored. The frozen timestamp, original standings snapshot and seed rows still existed.
- Root cause: `advanceAfterCompletion` unconditionally wrote COMPLETED for the league stage.
- Repair: retain FROZEN for an already frozen stage. Idempotent playoff generation reconciles legacy stage metadata only when an original frozen timestamp and snapshot already exist; it preserves all seeds, pairings and snapshot contents.
- Regression: an additional focused case verifies re-completion preserves frozen status and the original snapshot. Full suite: 128 passed, no skipped tests. The real idempotent playoff API and independent database assertions verify the retained mock after repair.
