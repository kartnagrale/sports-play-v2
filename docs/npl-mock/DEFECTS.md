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
