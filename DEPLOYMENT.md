# Play-Neml container deployment contract

Use the [Phase 6.5 operations guide](docs/phase-6.5/OPERATIONS.md) for environment configuration, image builds, initialization, tracked migrations, same-origin API/WebSocket routing, TLS integration, private photo storage, matched backups and safe restart/upgrade commands.

Copy [.env.docker.example](.env.docker.example) into a protected external file. Select a unique Compose project explicitly for every command. The stack requires supplied secrets, uses a restricted application database role, disables demo seeding and publishes only the loopback gateway.

The [Phase 6.5 report](docs/phase-6.5/REPORT.md) records actual container verification. Reproduce isolated checks with tools/verify-dockerization.ps1. Private generated credentials, photos and raw evidence remain Git-ignored.

**CVE-2026-47890 and CVE-2026-47884 remain unresolved production security blockers.** Phase 6.5 does not authorize production deployment. Phase 7 has not begun. DNS, trusted TLS certificates and production rollout require separate configuration and authorization.
