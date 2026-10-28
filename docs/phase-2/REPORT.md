# Play-Neml — Phase 2

Date: 2026-10-27 (Asia/Calcutta). Branch: `feature/play-neml-upgrade`. Baseline: `c14e4ed`.

**Status: COMPLETE.** This report covers the shared visual foundation only. Phase 3 has not started. Nothing is approved for production.

## Git and scope

Work continued in `C:/Users/karti/.codex/worktrees/play-neml-upgrade/sports-play-main`, which was clean at the Phase 1 finalization commit. The original main checkout and its unrelated changes were preserved. No push, merge, repository rename, domain cutover, backend code change, API contract change or existing database modification occurred.

Implementation commit: `b3c3a164044f122b5ceb1d94d9f49c94f42cf5e1`. The verification/report commit SHA is supplied in the completion message; `git log --oneline c14e4ed..HEAD` lists this phase. The changed-file inventory below includes source, test tooling and evidence.

## Branding and tokens

- Exact product name **Play-Neml**, original track/play monogram, typographic wordmark and SVG favicon. No official NEML logo or corporate palette is implied.
- Sidebar, persistent header, login, activation, viewer entry, optional splash, page titles and root metadata use the identity. The descriptor appears in the shared account-page presentation.
- Midnight navy / mint dark theme and off-white / deep teal light theme. Semantic background, surface, elevation, foreground, secondary/muted text, border, focus, primary/secondary accent, success, warning, danger, information and live colors live in `globals.css`.
- Tailwind colors expose RGB variables with opacity support. Existing feature utilities inherit the system; the legacy `white` alias remains theme-aware for compatibility. Low-opacity legacy secondary text maps to readable semantic text colors.
- Outfit and JetBrains Mono remain Next-managed fonts; headings use a quieter scale, statistics/tables use tabular numerals. Spacing, page/container widths, radii, shadows, sidebar/header dimensions, table cells and motion timings are centralized. Existing Tailwind breakpoints remain the responsive contract.
- `next-themes` retains persistent selection and its pre-hydration theme script. Toggle rendering is hydration-safe. No extra animation or styling dependency was installed.

## Shared component foundation

`src/components/ui/index.tsx` exports Button, IconButton, Card, MetricCard, StatusBadge, Input, Select, Textarea, Checkbox, Field, SearchInput, Skeleton, LoadingIndicator, EmptyState, ErrorState, SectionHeader, PageHeader, DataTable and Pagination. Native form attributes/refs pass through. Buttons support variants, loading and disabled states; Field associates labels, hints and errors with generated IDs.

`Dialog.tsx` uses native `showModal()` for background inertness, focus trapping, Escape handling, scroll locking and focus restoration. Player, pricing, coin-toss and schedule-match dialogs now use it. `Disclosure.tsx` adds controlled keyboard tabs, focus/hover tooltips and action menus with arrow/Home/End/Escape behavior. Toasts inherit tokens.

Player management consumes the shared heading/table/search/pagination/identity foundation, with visible keyboard/touch actions and associated edit labels. Dashboard metrics reuse MetricCard without changing the dashboard's information architecture. Other portal pages inherit the shell, typography, tokens and existing CSS utility APIs; they have not received a complete portal redesign.

## Motion foundations

- CSS tokens: `--motion-fast`, `--motion-base`, `--motion-slow`, `--ease-out`.
- `MotionReveal`, `UpdatedValue` and revision-driven `ReorderGroup` in `ui/Motion.tsx`. ReorderGroup batches geometry reads and uses native Web Animations for FLIP transforms on stable `data-motion-id` children. No continuous measurement loop or animation package.
- `.motion-enter`, `.motion-highlight`, `.motion-success`, `.motion-error`, `.motion-expand` with `data-open`, and `.motion-celebrate` are reusable entrance, update, confirmation, error, expansion and future celebration hooks. Future-only classes are explicitly retained in the Tailwind output.
- Dialogs and dropdowns have short entrances; navigation uses restrained state transitions. Global reduced-motion rules suppress transitions/keyframes and smooth scrolling. ReorderGroup skips motion when reduced motion is requested. Loading labels remain available without spinning visuals.
- Phase 3 can connect committed bid/value changes, ranking revisions and winner content to these hooks. No SOLD overlay, confetti system, auction-specific sale logic or fake live data was added.

## Player identity

`PlayerAvatar({name?, imageUrl?, size?, shape?, alt?, loading?})` supports `sm/md/lg/xl`, `circle/square`, initials and an abstract athlete fallback. Images are lazy, dimensioned, decoded by Next Image with `unoptimized` and a no-referrer policy; no speculative photo CDN allowlist or server-side image-fetch proxy was introduced. Only HTTP(S) and same-origin root-relative image URLs are accepted. Broken images revert to initials; keyed URL state handles A → B → A replacement. An empty `alt` makes the avatar decorative next to an existing name.

`PlayerIdentity({name, metadata?, imageUrl?, size?, shape?})` combines identity and optional metadata. Player tables and shell account identities already consume these components. Phase 4 can supply an approved URL without changing their UI API. There are no photo model fields, upload endpoints, storage changes or random-person placeholders.

## Shell and authentication

- Server screen mappings remain the exclusive navigation source, grouped by existing sections. Active links use `aria-current`; desktop navigation collapses and mobile navigation opens as a modal drawer. Crossing back to desktop closes the drawer.
- Header shows current page, championship and portal role, plus functional theme/account controls. Championship name, sport/status and the original selection/store behavior remain available. Account details show only existing account information.
- Skip link, focus outlines, 44px shared buttons, scrollable navigation, content sizing, scoped horizontal table scrolling and responsive account layouts improve accessibility and smaller-screen use.
- Login removes the blocking splash delay, adds explicit labels, password visibility, busy/error states and a clear viewer entry point.
- Activation preserves the fragment-only token handoff, immediate history cleanup, single-use POST contract and UTF-8 password bound. It presents missing links, matching requirements, server rejection/recovery and success. Viewer entry shares the branding and inline failure feedback.
- `api.ts`, `auth.ts`, `championship.ts`, `navigation.ts` and `ws.ts` are unchanged. CSRF refresh, HttpOnly cookies, bearer viewer scoping, exact origins and session revocation remain intact.

## Verification and evidence

| Check | Result | Evidence |
|---|---|---|
| ESLint | PASS, zero warnings | [lint.log](evidence/lint.log) |
| TypeScript | PASS | [typecheck.log](evidence/typecheck.log) |
| Next production build | PASS, all 24 static pages generated | [build.log](evidence/build.log) |
| Real-backend Edge journeys | **9/9 PASS**, 49.5 seconds | [browser-tests.log](evidence/browser-tests.log) |
| Component harness build | PASS, bundled from existing dependencies | [harness-build.log](evidence/harness-build.log) |
| Visual evidence | 65 screenshots; representative account, shell, drawer, form and identity captures manually inspected | [SHA256 manifest](evidence/SHA256SUMS.txt) |
| Runtime/hydration | No page exceptions or hydration errors in monitored journeys | Browser assertions |
| Scope/regression | Backend and auth/API/store/socket implementation unchanged; targeted real security boundaries pass | Browser assertions and Git diff |

An additional isolated component capture verified the production font classes and regenerated the four component screenshots in both themes. Test-harness setup/assertion issues were corrected during development; the linked browser log is the final full successful suite. The original Phase 1 evidence was not overwritten.

Commands: `npm run lint`, `npm run typecheck`, `npm run build`; browser/integration checks use `./tools/verify-phase2-ui.ps1`. The runner builds with API/WS `http://localhost:58093`, serves Next on `http://localhost:53002`, and creates a **new** PostgreSQL 15 container on localhost:55442. It initializes the existing schema/migration only in that disposable database. Generated credentials remain in memory/environment; no setup links or cookies are saved to evidence. Rebuild with intended environment URLs before any later deployment.

The browser suite checks component states; real login/activation/player save; exact server-authorized screen sets for all four roles; their linked routes; championship switching; desktop collapse; modal focus/return; menu and tab keys; both themes; persistence; reduced motion; avatar load/failure/replacement; cookie/CSRF enforcement; viewer tenant rejection; authorized STOMP connection and SEND denial; logout replay rejection. It monitors page exceptions and hydration-related console errors. A temporary browser-intercepted component harness bundles existing source into ignored `tmp/`; it introduces no app route or new dependency.

The runner retains its disposable container stopped for troubleshooting. After this successful verification, all seven containers created during this phase and their test volumes were removed. No existing database/container was started or changed.

Screenshots cover desktop **1440×960**, tablet **768×1024**, mobile **375×812**, both themes, four role shells/drawers, account pages and representative shared controls. Account overflow is also checked at **320×740**. Representative evidence:

- [Login dark](evidence/login-desktop-dark.png), [login light](evidence/login-desktop-light.png), [mobile login](evidence/login-mobile-light.png).
- [Administrator shell dark](evidence/admin-desktop-dark.png), [light](evidence/admin-desktop-light.png), [mobile drawer](evidence/admin-drawer-mobile-light.png).
- [Super Admin](evidence/platform-desktop-light.png), [captain](evidence/captain-desktop-dark.png), [viewer](evidence/viewer-mobile-light.png).
- [Activation success](evidence/activation-success.png), [consumed-link recovery](evidence/activation-consumed-link.png).
- [Player identity variants](evidence/components-identity-light.png), [form states](evidence/components-forms-dark.png), [player table](evidence/players-identities-light.png).

Measured representative token contrast: light muted on app background **4.74:1**, light primary/white **6.00:1**, dark muted on elevated surface **6.07:1**, dark primary on surface **9.52:1**. This is targeted contrast verification, not a full WCAG certification.

Limitations: automated browser coverage uses installed Windows Edge, not Safari/Firefox or physical touch devices. No screen-reader user study, full accessibility audit, exhaustive tournament/scoring journey or load test occurred. Expiry UX uses the same server-error recovery as invalid/consumed links; timed expiry, renewal/races and broader backend protections retain their Phase 1 evidence. Complete portal-specific form/heading/accessibility work remains Phase 3. No backend suite was rerun because no backend implementation changed; affected integration boundaries were tested through the real backend instead.

## Next phase and production blocker

Phase 3 can consume the shared UI exports, native modal/menu/tab patterns, semantic tokens, motion hooks, player identity APIs and responsive shell. Complete portal redesigns and auction choreography await the user's next instruction. Phase 4 photo/import work and later certificates/deployment were not started.

**Phase 1 remains PARTIALLY COMPLETE. CVE-2026-47890 and CVE-2026-47884 remain unresolved Spring MVC dependency advisory matches and a documented production security blocker.** The affected SSE fragment/XSLT features remain unused. No suppression, security bypass, Spring Boot 4 migration or production-readiness claim was introduced. See [Phase 1 summary](../phase-1/SUMMARY.md) and [dependency triage](../phase-1/DEPENDENCY-TRIAGE.md).

## Changed files

Source and tooling (plus `docs/phase-2/evidence/*`: 65 PNGs, build/lint/typecheck/browser/runtime logs and the SHA256 manifest):

- `docs/phase-2/REPORT.md`
- `frontend/e2e/build-ui-harness.cjs`
- `frontend/e2e/phase2.spec.ts`
- `frontend/e2e/ts-loader.cjs`
- `frontend/e2e/ui-harness.tsx`
- `frontend/src/app/(authed)/dashboard/page.tsx`
- `frontend/src/app/(authed)/matches/page.tsx`
- `frontend/src/app/(authed)/players/page.tsx`
- `frontend/src/app/activate/layout.tsx`
- `frontend/src/app/activate/page.tsx`
- `frontend/src/app/globals.css`
- `frontend/src/app/icon.svg`
- `frontend/src/app/layout.tsx`
- `frontend/src/app/login/layout.tsx`
- `frontend/src/app/login/page.tsx`
- `frontend/src/app/room/join/layout.tsx`
- `frontend/src/app/room/join/page.tsx`
- `frontend/src/components/AppShell.tsx`
- `frontend/src/components/AuthLayout.tsx`
- `frontend/src/components/Brand.tsx`
- `frontend/src/components/SplashScreen.tsx`
- `frontend/src/components/auction/BasePriceDialog.tsx`
- `frontend/src/components/auction/CoinTossDialog.tsx`
- `frontend/src/components/players/PlayerAvatar.tsx`
- `frontend/src/components/players/PlayerDialog.tsx`
- `frontend/src/components/theme-toggle.tsx`
- `frontend/src/components/ui/Dialog.tsx`
- `frontend/src/components/ui/Disclosure.tsx`
- `frontend/src/components/ui/Motion.tsx`
- `frontend/src/components/ui/index.tsx`
- `frontend/tailwind.config.js`
- `tools/verify-phase2-ui.ps1`
