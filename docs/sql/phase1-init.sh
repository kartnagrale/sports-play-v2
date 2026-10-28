#!/bin/sh
(
set -eu
checksum=$(sha256sum /opt/play-neml-migrations/2026-10-27-phase1-account-security.sql | cut -d ' ' -f 1)
psql -X --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -v ON_ERROR_STOP=1 \
  -v "checksum=$checksum" -v version=2026-10-27-phase1-account-security \
  -v migration_file=2026-10-27-phase1-account-security.sql -f /opt/play-neml-migrations/phase1-apply.psql
for version in 2026-10-28-phase4-player-import 2026-10-28-phase4-player-photo 2026-10-28-phase5-certificates 2026-10-28-phase5-certificate-navigation 2026-10-28-phase5-certificate-batches; do
 checksum=$(sha256sum /opt/play-neml-migrations/$version.sql | cut -d ' ' -f 1)
 psql -X --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -v ON_ERROR_STOP=1 \
  -v "checksum=$checksum" -v "version=$version" -v "migration_file=$version.sql" -f /opt/play-neml-migrations/phase1-apply.psql
done
)
