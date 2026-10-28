# Play-Neml — Phase 1 dependency triage

This records package/advisory matches, not independently demonstrated exploits. Baseline evidence is unchanged under `docs/phase-0/evidence/`. Current raw scans are under `docs/phase-1/evidence/`. Public registry versions and OSV details were checked on 2026-10-27.

## Upgrade choices

| Component | Before | After | Compatibility assessment |
|---|---|---|---|
| Spring Boot BOM | 3.2.5 | 3.5.16 | Stays in Boot 3/Jackson 2/Hibernate 6 and Java 17; 57-test target includes real PostgreSQL validation. Boot 4/Spring 7 requires a separate compatibility migration. |
| Tomcat family | 10.1.20 | 10.1.61 | Compatible 10.1 patch family, including servlet/websocket runtime. Advisory-listed 10.1.58 was absent from Maven Central; a later published patch was used. |
| Jackson BOM | 2.15.4 | 2.21.8 | Aligns every Jackson 2 module via the BOM; typed REST DTOs and cricket/configuration JSON paths verified by tests. |
| PostgreSQL JDBC | 42.6.2 | 42.7.14 | Compatible driver patch family; schema, locking and runtime checks exercised. |
| Log4j bridge/API | 2.21.1 | 2.25.5 | Aligned family property; application still uses Logback through SLF4J. No MapMessage JSON layout is configured. |
| Next / eslint-config-next | 14.2.15 | 16.4.3 | Major migration evaluated: no middleware, server actions, route params or server cookie APIs to port; webpack mode preserves the existing build. ESLint CLI replaces removed next lint. |
| React / React DOM | 18.3.1 | 19.3.1 | Matching versions/types; effects/ref issues corrected under the new recommended hooks rules. |
| Axios | locked vulnerable 1.x | 1.21.0 | Existing interceptors retained; CSRF fetching made asynchronous and refreshed across authentication changes. |
| ESLint | 8.57.1 | 9.39.5 | Flat Next configuration; no rule suppressions; zero frontend lint warnings. Registry labels this ESLint 9 release deprecated; moving to ESLint 10 remains toolchain maintenance work. |
| Node Docker base | 20-alpine | 22-alpine | Matches the supported frontend engine family; local executed checks used Node 22.20.0/npm 10.9.3. Container OS/JDK image vulnerability scans were not executed. |

Direct backend dependencies are the Spring starters, JDBC driver and explicitly declared libraries; Spring Framework, Tomcat, Jackson, logging and most test libraries are transitively resolved or BOM-managed. The explicit maintenance-family properties are documented in `backend/pom.xml`; arbitrary individual Spring jars were not overridden.

## Frontend baseline advisories

After upgrading direct dependencies and explicitly updating compatible transitive packages, both full and production-only npm audits report zero affected package entries. This includes browserslist, baseline-browser-mapping, follow-redirects, postcss-selector-parser and source-map-js lockfile updates. No audit-fix force command was used.

| Baseline package | Direct | npm severity | Baseline advisory IDs / source links |
|---|---|---|---|
| @next/eslint-plugin-next | no | high | Inherited from affected dependency paths; see raw audit via/effects fields |
| axios | yes | high | [1240600: Axios: Prototype pollution gadget in fetch adapter can alter outbound requests ](https://github.com/advisories/GHSA-vh66-26gq-q6x8); [1240601: Axios: Prototype-Pollution Gadget in the Default Instance Allows Inherited Object.prototype.method to Override HTTP Method ](https://github.com/advisories/GHSA-9fr6-4gfg-395g); [1240603: Axios: ReDoS in fromDataURI data: URL parser freezes the Node event loop (DoS) ](https://github.com/advisories/GHSA-c29m-xwm3-cm6r); [1240604: Axios: ReDoS (O(N²)) in shouldBypassProxy host normalization, reachable via untrusted redirect Location ](https://github.com/advisories/GHSA-mghh-pgcx-3jjj); [1240605: Axios: Prototype Pollution Gadget in axios toFormData Options ](https://github.com/advisories/GHSA-x97p-jq2g-jp4f); [1240607: Axios: HTTP/2 adapter bypasses configured DNS lookup and proxy controls ](https://github.com/advisories/GHSA-3pq3-5fj3-cg6v); [1240608: Axios: Denial of Service via Unhandled 'error' Event in HTTP/2 ClientHttp2Session Initialization ](https://github.com/advisories/GHSA-542g-h47m-68v8); [1240626: Axios: Header Injection via Inherited headers After Minimal Interceptor ](https://github.com/advisories/GHSA-j8rh-479h-cp32); [1240627: Axios: Fetch Adapter Header Injection via Inherited FormData getHeaders ](https://github.com/advisories/GHSA-4hqw-qxg8-jxx2); [1240628: Axios: Node HTTP adapter prototype-pollution gadget allows request socket hijack via inherited createConnection ](https://github.com/advisories/GHSA-m8m8-qj5v-23w3); [1240629: Axios: CIDR-form NO_PROXY entries are ignored, causing proxy exclusion bypass for internal IP ranges ](https://github.com/advisories/GHSA-44g4-m2mj-wpvx); [1240634: Axios: maxRedirects: 0 is not enforced by the fetch adapter, allowing redirect-based SSRF ](https://github.com/advisories/GHSA-r4gj-5m52-g5wh) |
| baseline-browser-mapping | no | moderate | [1193686: baseline-browser-mapping process termination on invalid input causes denial of service ](https://github.com/advisories/GHSA-w5vr-8v7q-w6rv) |
| browserslist | no | high | [1153171: Browserslist: Unbounded memory growth (no cache eviction) via distinct query results, leading to eventual OOM ](https://github.com/advisories/GHSA-c83g-rgw3-j3cx); [1153172: Browserslist: Uncaught crash / prototype write via untrusted browserslist-stats.json custom stats (normalizeStats) ](https://github.com/advisories/GHSA-73wf-gq98-2v4g) |
| eslint-config-next | yes | high | Inherited from affected dependency paths; see raw audit via/effects fields |
| follow-redirects | no | high | [1242604: follow-redirects array-form headers bypass cross-origin credential stripping ](https://github.com/advisories/GHSA-q3jh-wrc6-jgq8); [1242605: follow-redirects sensitiveHeaders regex escaping can expose credential headers ](https://github.com/advisories/GHSA-8r9p-f939-6h3c) |
| glob | no | high | [1109842: glob CLI: Command injection via -c/--cmd executes matches with shell:true ](https://github.com/advisories/GHSA-5j98-mcp5-4vw2) |
| next | yes | critical | [1101438: Next.js Allows a Denial of Service (DoS) with Server Actions ](https://github.com/advisories/GHSA-7m27-7ghc-44w9); [1105461: Information exposure in Next.js dev server due to lack of origin verification ](https://github.com/advisories/GHSA-3h52-269p-cp9r); [1107226: Next.js Affected by Cache Key Confusion for Image Optimization API Routes ](https://github.com/advisories/GHSA-g5qg-72qw-gw5v); [1107512: Next.js Improper Middleware Redirect Handling Leads to SSRF ](https://github.com/advisories/GHSA-4342-x723-ch2f); [1107513: Next.js Content Injection Vulnerability for Image Optimization ](https://github.com/advisories/GHSA-xv57-4mr9-wg8v); [1108291: Next.js Race Condition to Cache Poisoning ](https://github.com/advisories/GHSA-qpjv-v59x-3qc4); [1111391: Next Vulnerable to Denial of Service with Server Components ](https://github.com/advisories/GHSA-mwv6-3258-q52c); [1112182: Next has a Denial of Service with Server Components - Incomplete Fix Follow-Up ](https://github.com/advisories/GHSA-5j59-xgg2-r9c4); [1112593: Next.js self-hosted applications vulnerable to DoS via Image Optimizer remotePatterns configuration ](https://github.com/advisories/GHSA-9g9p-9gw9-jx7f); [1112653: Next.js HTTP request deserialization can lead to DoS when using insecure React Server Components ](https://github.com/advisories/GHSA-h25m-26qc-wcjf); [1113689: Authorization Bypass in Next.js Middleware ](https://github.com/advisories/GHSA-f82v-jwr5-mffw); [1114897: Next.js: HTTP request smuggling in rewrites ](https://github.com/advisories/GHSA-ggv3-7p47-pfv8); [1114940: Next.js: Unbounded next/image disk cache growth can exhaust storage ](https://github.com/advisories/GHSA-3x4c-7xq6-9pq8); [1116376: Next.js has a Denial of Service with Server Components ](https://github.com/advisories/GHSA-q4gf-8mx6-v5v3); [1117931: Next.js Vulnerable to Denial of Service with Server Components ](https://github.com/advisories/GHSA-8h8q-6873-q5fj); [1118942: Next.js's Middleware / Proxy redirects can be cache-poisoned ](https://github.com/advisories/GHSA-3g8h-86w9-wvmq); [1118944: Next.js vulnerable to cross-site scripting in App Router applications using CSP nonces ](https://github.com/advisories/GHSA-ffhc-5mcf-pf4q); [1118946: Next.js vulnerable to cache poisoning via collisions in React Server Component cache-busting ](https://github.com/advisories/GHSA-vfv6-92ff-j949); [1118948: Next.js has cross-site scripting in beforeInteractive scripts with untrusted input ](https://github.com/advisories/GHSA-gx5p-jg67-6x7h); [1118952: Next.js has a Denial of Service in the Image Optimization API ](https://github.com/advisories/GHSA-h64f-5h5j-jqjh); [1118954: Next.js vulnerable to server-side request forgery in applications using WebSocket upgrades ](https://github.com/advisories/GHSA-c4j6-fc7j-m34r); [1118958: Next.js vulnerable to cache poisoning in React Server Component responses ](https://github.com/advisories/GHSA-wfc6-r584-vfw7); [1118962: Next.js has a Middleware / Proxy bypass in Pages Router applications using i18n ](https://github.com/advisories/GHSA-36qx-fr4f-26g5); [1124172: Next.js: Denial of Service in App Router using Server Actions ](https://github.com/advisories/GHSA-m99w-x7hq-7vfj); [1124185: Next.js: Server-Side Request Forgery in Server Actions on custom servers ](https://github.com/advisories/GHSA-89xv-2m56-2m9x); [1124187: Next.js: Cache confusion of response bodies for requests with bodies ](https://github.com/advisories/GHSA-68g3-v927-f742); [1124189: Next.js: Cache confusion of response bodies for requests with bodies containing invalid UTF-8 byte sequences ](https://github.com/advisories/GHSA-4633-3j49-mh5q); [1124191: Next.js: Unbounded Server Action payload in Edge runtime ](https://github.com/advisories/GHSA-4c39-4ccg-62r3); [1124193: Next.js: Server-Side Request Forgery in rewrites via attacker-controlled destination hostname ](https://github.com/advisories/GHSA-p9j2-gv94-2wf4); [1124197: Next.js: Unauthenticated disclosure of internal Server Function endpoints ](https://github.com/advisories/GHSA-955p-x3mx-jcvp); [1193677: Next.js: Unauthenticated Remote Code Execution on windows-hosted servers ](https://github.com/advisories/GHSA-p293-qw3h-jr36); [1193733: Next.js: Unauthenticated Remote Code Execution in Image Optimization API when AVIF files are used ](https://github.com/advisories/GHSA-2xp9-vwfh-vxw4); [1242817: Next.js: Unauthenticated Remote Code Execution in Image Optimization API when SVG files are used ](https://github.com/advisories/GHSA-2464-fhr4-w9xw); [1242819: Next.js: Denial of Service in App Router using Server Actions ](https://github.com/advisories/GHSA-p3m5-59mp-jv5r) |
| postcss | no | high | [1117015: PostCSS has XSS via Unescaped </style> in its CSS Stringify Output ](https://github.com/advisories/GHSA-qx2v-qp2m-jg93); [1124252: PostCSS: Arbitrary file read and information disclosure via attacker-controlled sourceMappingURL in CSS comments ](https://github.com/advisories/GHSA-6g55-p6wh-862q); [1130709: PostCSS: incomplete fix of GHSA-6g55-p6wh-862q — attacker-controlled sourceMappingURL reads arbitrary .map files when `from` is unset ](https://github.com/advisories/GHSA-fxqj-rqcc-2cmp); [1139510: PostCSS: Path Traversal in Previous Source Map Auto-Loading (sourceMappingURL) leads to Arbitrary .map File Disclosure ](https://github.com/advisories/GHSA-r28c-9q8g-f849) |
| postcss-selector-parser | no | moderate | [1243379: PostCSS: Quadratic complexity in flat selector parsing allows CPU exhaustion ](https://github.com/advisories/GHSA-rj75-hqrm-r3gf) |
| source-map-js | no | high | [1241209: source-map-js allows event-loop denial of service through indexed source-map section offsets ](https://github.com/advisories/GHSA-68fv-2mgg-jv7q) |

## Backend baseline package dispositions

| Baseline package | Scope | Old version | Current version | Baseline advisory IDs | Current disposition |
|---|---|---|---|---|
| org.springframework.boot:spring-boot | compile | 3.2.5 | 3.5.16 | GHSA-rc42-6c7j-7h5r | No current OSV match after upgrade |
| ch.qos.logback:logback-core | compile | 1.4.14 | 1.5.34 | GHSA-25qh-j22f-pwp8, GHSA-6v67-2wr5-gvf4, GHSA-jhq6-gfmj-v8fx, GHSA-p47f-322f-whfh, GHSA-pr98-23f8-jwxv, GHSA-qqpg-mvqg-649v | No current OSV match after upgrade |
| org.apache.logging.log4j:log4j-api | compile | 2.21.1 | 2.25.5 | GHSA-qv9r-c865-cp47 | No current OSV match after upgrade |
| org.apache.tomcat.embed:tomcat-embed-core | compile | 10.1.20 | 10.1.61 | GHSA-23hv-mwm6-g8jf, GHSA-25xr-qj8w-c4vf, GHSA-27hp-xhwr-wr2m, GHSA-3p2h-wqq4-wf4h, GHSA-42wg-hm62-jcwg, GHSA-563x-q5rq-57qp, GHSA-5j33-cvvr-w245, GHSA-5m62-pw8w-7w9f, GHSA-5mp6-jrq3-r938, GHSA-7jqf-v358-p8g7, GHSA-83qj-6fr2-vhqg, GHSA-9m3c-qcxr-9x87, GHSA-9m89-8frq-c98c, GHSA-9xv2-5v5q-p794, GHSA-ff77-26x5-69cr, GHSA-fpj8-gq4v-p354, GHSA-fv25-8xcx-gqjc, GHSA-gcx9-497g-6cp6, GHSA-gqp3-2cvr-x8m3, GHSA-gx5v-xp9w-j4cg, GHSA-h2fw-rfh5-95r3, GHSA-h3gc-qfqq-6h8f, GHSA-h3x4-894j-xpx5, GHSA-h6fc-48rj-7qqh, GHSA-hgrr-935x-pq79, GHSA-mgp5-rv84-w37q, GHSA-r29c-68gh-xp6x, GHSA-rv64-5gf8-9qq8, GHSA-vfww-5hm6-hx2j, GHSA-wc4r-xq3c-5cf3, GHSA-wm9w-rjj3-j356, GHSA-wmwf-9ccg-fff5, GHSA-wr62-c79q-cv37, GHSA-x4m4-345f-5h5g | No current OSV match after upgrade |
| org.springframework:spring-web | compile | 6.1.6 | 6.2.19 | GHSA-2rmj-mq67-h97g, GHSA-4gc7-5j7h-4qph, GHSA-6r3c-xf4w-jxjm | No current OSV match after upgrade |
| org.springframework:spring-webmvc | compile | 6.1.6 | 6.2.19 | GHSA-3chg-m5w7-qfv5, GHSA-4773-3jfm-qmx3, GHSA-6hcq-hmm3-jj3c, GHSA-6p4f-wcwh-5vvm, GHSA-72pg-x5f8-j25j, GHSA-957g-f97v-vppc, GHSA-cjpg-rgq5-fr37, GHSA-cx7f-g6mp-7hqm, GHSA-g5vr-rgqm-vf78, GHSA-h3qp-gqrc-q736, GHSA-mq64-j8f9-9gcj, GHSA-pc63-qcmh-9cmg, GHSA-r936-gwx5-v52f, GHSA-wg35-8jpf-2xv3, GHSA-x23c-287f-qqv5 | Remaining contextual matches below |
| org.springframework:spring-context | compile | 6.1.6 | 6.2.19 | GHSA-4gc7-5j7h-4qph, GHSA-4wp7-92pw-q264 | No current OSV match after upgrade |
| org.springframework:spring-expression | compile | 6.1.6 | 6.2.19 | GHSA-9f52-rjqv-25qv, GHSA-r5w3-xv2f-j59q, GHSA-wxpp-56q6-5pcg | No current OSV match after upgrade |
| org.springframework.data:spring-data-commons | compile | 3.2.5 | 3.5.13 | GHSA-5m4m-73w9-8433, GHSA-5vpf-xvv7-c8vh | No current OSV match after upgrade |
| org.springframework.security:spring-security-core | compile | 6.2.4 | 6.5.11 | GHSA-q3v6-hm2v-pw99 | No current OSV match after upgrade |
| org.springframework.security:spring-security-crypto | compile | 6.2.4 | 6.5.11 | GHSA-mg83-c7gq-rv5c | No current OSV match after upgrade |
| org.springframework.security:spring-security-web | compile | 6.2.4 | 6.5.11 | GHSA-293q-567p-wmwq, GHSA-c4q5-6c82-3qpw, GHSA-mf92-479x-3373 | No current OSV match after upgrade |
| org.springframework:spring-websocket | compile | 6.1.6 | 6.2.19 | GHSA-7fch-4f2f-jcgm, GHSA-q723-847q-5g8g | No current OSV match after upgrade |
| org.postgresql:postgresql | runtime | 42.6.2 | 42.7.14 | GHSA-98qh-xjc8-98pq | No current OSV match after upgrade |
| com.fasterxml.jackson.core:jackson-databind | compile | 2.15.4 | 2.21.8 | GHSA-3pjw-73gf-8qr5, GHSA-5gvw-p9qm-jgwh, GHSA-5jmj-h7xm-6q6v, GHSA-cxp5-3px4-pw24, GHSA-gx83-3vf8-gh7j, GHSA-hgj6-7826-r7m5, GHSA-j3rv-43j4-c7qm, GHSA-q4xh-88c3-wmh7, GHSA-rmj7-2vxq-3g9f, GHSA-vvgp-rfg2-7rr6, GHSA-wjgm-6hv5-3cvf, GHSA-wv8q-qhhj-9h54 | No current OSV match after upgrade |
| com.fasterxml.jackson.core:jackson-core | compile | 2.15.4 | 2.21.8 | GHSA-72hv-8253-57qq, GHSA-7hhh-6rmp-j9qf, GHSA-r7wm-3cxj-wff9 | No current OSV match after upgrade |
| net.minidev:json-smart | test | 2.5.1 | 2.5.2 | GHSA-pq2g-wx69-c263 | No current OSV match after upgrade |
| org.assertj:assertj-core | test | 3.24.2 | 3.27.7 | GHSA-rqfh-9r24-8c9r | No current OSV match after upgrade |
| org.springframework:spring-core | compile | 6.1.6 | 6.2.19 | GHSA-659m-px2c-25wj, GHSA-jmp9-x22r-554x | No current OSV match after upgrade |
| org.xmlunit:xmlunit-core | test | 2.9.1 | 2.10.4 | GHSA-chfm-68vv-pvw5 | No current OSV match after upgrade |

## Remaining findings — no blanket suppression

Current OSV: one affected package, `org.springframework:spring-webmvc:6.2.19`, two CRITICAL advisory labels. These remain unresolved package matches. A source/configuration review found no SSE fragment endpoints, XSLT views or templates. That lowers demonstrated application exposure; it is not proof that the dependency is fixed or an authorization to enable those features.

### GHSA-j9f9-w8pj-32f8

Spring Framework Server Sent Event stream corruption while rendering fragments

Aliases: CVE-2026-47890. OSV/GHSA severity: CRITICAL.

The vulnerable path requires SSE streams that render view fragments. This application serves REST JSON and STOMP topics and defines no such SSE endpoint or rendering path.

- [Primary advisory](https://spring.io/security/cve-2026-47890)

### GHSA-pc63-qcmh-9cmg

Spring Framework Improper Path Limitation in XsltView

Aliases: CVE-2026-47884. OSV/GHSA severity: CRITICAL.

The vulnerable path requires XSLT view rendering with a stylesheet path influenced by untrusted data. This application defines no XSLT view bean, stylesheet template or view controller.

- [Primary advisory](https://spring.io/security/cve-2026-47884)

The current Boot 3.5 BOM uses the newest published Spring 6.2 line found in Maven Central (6.2.19). The two advisories list fixes in Spring 7.0.9; mixing Spring 7 into Boot 3 would be unsafe. Complete a Boot 4/Spring 7 migration, or obtain a verified compatible vendor backport and explicit risk disposition, before declaring the dependency backlog closed. Do not enable SSE fragments/XSLT during that interval. No blanket allowlist or hidden audit suppression was added.

The scans do not cover Maven plugins, operating system/JDK images, external notification infrastructure or deployment configuration. CI was configured locally and was not remotely executed.
