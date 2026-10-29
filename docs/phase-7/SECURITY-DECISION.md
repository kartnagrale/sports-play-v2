# Security decision — 2026-10-29

Decision: **PENDING security-owner review. No risk acceptance granted.** Retain the coherent Boot 3.5.16 / Framework 6.2.19 / Java 17 implementation. Both package findings remain release blockers; no Enterprise artifact was downloaded and no Spring 7 override was added to Boot 3.

| Finding | Official severity | Affected installed package | Supported fix | Application evidence |
| --- | --- | --- | --- | --- |
| CVE-2026-47890 / GHSA-j9f9-w8pj-32f8 | Spring LOW; historical OSV/GHSA classification CRITICAL | spring-webmvc 6.2.19 | Framework 7.0.9 OSS or 6.2.20 Enterprise | No SseEmitter, FragmentsRendering or text/event-stream usage in production source. STOMP/SockJS transports JSON events, not SSE view fragments. |
| CVE-2026-47884 / GHSA-pc63-qcmh-9cmg | Spring MEDIUM; historical OSV/GHSA classification CRITICAL | spring-webmvc 6.2.19 | Framework 7.0.9 OSS or 6.2.20 Enterprise | No XsltView, ModelAndView or standalone @Controller usage. Controllers use @RestController and return DTOs, JSON, PDFs or images. Security's /** matchers do not create MVC view-rendering mappings. |

Primary sources: [SSE advisory](https://spring.io/security/cve-2026-47890/), [XSLT advisory](https://spring.io/security/cve-2026-47884/). The former requires SSE view fragments and attacker-controlled streamed data; the latter requires XSLT view rendering with a broad mapping and implicit view name. These paths were not found in this application. Source inspection reduces observed applicability; it does not remove the vulnerable code from the package or authorize acceptance. Keep those features absent and reassess when controller/view configuration changes.

No legitimate Enterprise entitlement or authenticated repository configuration was supplied. The owner may supply licensed 6.2.20 access; do not fetch Enterprise binaries through unofficial mirrors. Revalidate its Boot compatibility and test before using it.

## Coordinated Boot 4 path

Official [requirements](https://docs.spring.io/spring-boot/system-requirements.html) list stable Boot 4.1.2, Java 17 minimum, Framework 7.0.10 minimum and Servlet 6.1/Tomcat 11. [Managed coordinates](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html) list Security 7.1.2 and Hibernate 7.4.13.Final at review time. Use the complete Boot BOM, remove incompatible Tomcat 10/Jackson 2 overrides, and resolve the actual tree before claiming a fix.

The [Boot migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) describes modular production/test starters and moved test packages. Existing WebMvcTest, DataJpaTest and AutoConfigureMockMvc imports need conversion; @MockBean usage needs replacement. [JSON documentation](https://docs.spring.io/spring-boot/4.0/reference/features/json.html) makes Jackson 3 the default; Jackson 2 support is transitional/deprecated. Play-Neml explicitly injects a Jackson 2 ObjectMapper into certificate snapshots, import previews, sport configuration, scoring, squads and notifications. Its JavaTimeModule/NON_NULL contract must remain compatible with already persisted snapshots, PDF payload hashes and external notification consumers. JJWT's Jackson adapter must also be accounted for independently of Spring's active JSON mapper.

[Security migration](https://docs.spring.io/spring-security/reference/6.5/migration-7/configuration.html) requires the Lambda DSL, already used by SecurityConfig. [Servlet migration](https://docs.spring.io/spring-security/reference/7.0/migration/servlet/index.html) still requires explicit authorization review. Preserve CSRF cookie/token handling, JWT credential-version revocation, exact origins, matcher semantics and inbound/outbound STOMP checks. Do not assume compiling means identical security behavior.

[Hibernate migration](https://docs.hibernate.org/orm/7.0/migration-guide/) introduces JPA 3.2 and changes domain validation, query and locking behavior. Revalidate the actual managed Hibernate version, entity mappings, native SQL, optimistic revisions and PostgreSQL locks against the six unchanged migrations, never use ddl-auto=update to hide incompatibility.

No major migration is committed: old/new snapshot compatibility, external notification-consumer compatibility and the entire upgraded persistence/security suite have not been demonstrated. These are acceptance prerequisites, not a claim that Boot 4 cannot support the application. The safe release decision is to preserve the verified stack, leave production approval pending, and obtain either licensed compatible patch access or complete a coordinated migration with these acceptance checks. No partially upgraded stack is left behind.

Security owner must choose and sign the fix or explicit, time-bounded risk acceptance with compensating controls, expiry and a remediation owner. Operations and database owners must separately approve TLS, backup/restore, secrets, monitoring and rollout. None of these approvals is inferred from merging code or successful local tests.
