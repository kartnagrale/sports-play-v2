# Release operations and human gates

Production is BLOCKED pending the security decision. No workflow deploys. CI advisories and image scans are intended to fail on remaining findings, including unfixed HIGH/CRITICAL image findings. Do not use continue-on-error, ignore-unfixed or CVE exclusions to clear this release.

Use [Phase 6.5 operations](../phase-6.5/OPERATIONS.md) for environment inputs and database-owner migration commands. Never auto-baseline a company database. The six tracked migrations and private photo directory remain unchanged. Coordinate one writer host with the in-process broker; no clustered broker or shared filesystem certification is claimed.

Before deployment, named owners must record:

- Security: signed CVE decision, current dependency/image scans and reviewed secret injection. Docker env is visible to host administrators; protect socket/config/backup access and use the company's approved secret manager.
- Operations: actual DNS/TLS proxy verification, HTTP-to-HTTPS handling, exact browser origin, Upgrade timeouts, fixed trusted forwarded protocol/host, resource budgets and graceful shutdown. Local Secure-cookie flag checks are not public TLS verification.
- Database: rehearsal on a restored copy, migration checksum review, least-privilege grants and a coordinated database/photo recovery point. Capture matched snapshots with writers stopped.
- Recovery: approved RPO/RTO, daily minimum backup proposal (tighten to business needs), backup before every schema/release change, encrypted off-host retention, access review and periodic restore drills. No schedule has been installed by this task.
- Monitoring: collect bounded container logs into approved storage, alerts for health/restart loops, storage exhaustion, auth rejection spikes and database waits. Never log cookies, activation links or room codes. The bounded /app/logs tmpfs is ephemeral; central retention and alert ownership remain operational work.
- Messaging: validate the real message_tracker consumer against activationRequired/setupInstructions payloads; no company notification service was contacted. Synthetic notification secrecy tests do not certify that consumer.
- Rotation: coordinate owner/application DB role passwords explicitly (env edits alone do not rotate roles); rotate JWT deliberately to revoke sessions, test activation expiry and invalidate bootstrap access after onboarding.

Rollback: stop ingress/writes, preserve both current volumes and diagnostic evidence, redeploy the previously recorded image digests only if schema compatibility is verified. Never run downgrade SQL speculatively. If schema/snapshot compatibility fails, restore a matched backup into NEW database/photo storage and validate before routing traffic. Preserve the failed installation for investigation. No production rollback was exercised here.

Demo accounts, public QR verification and HTTP cookie overrides belong only to labeled disposable environments. Production defaults keep public certificate verification disabled; enabling it requires an explicit trusted base URL and owner privacy review. No production account, DNS or certificate is modified.

Phase 7's additional restore harness restores a matched synthetic database/photo backup into a new database and named volume, assigns UID/GID 10001 and starts a separate backend with public verification disabled. Retained volumes and dump files are private. See REPORT.md for whether this run actually completed; a script's presence is not evidence of success.
