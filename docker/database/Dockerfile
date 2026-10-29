FROM postgres:15-alpine@sha256:54e0dd8e8cddcdbb9156c2486eed5a43eccb7214d7c6828097d41ce049fb3984
COPY docs/sql/sports-play-complete-schema.sql /opt/play-neml/schema.sql
COPY docs/sql/phase1-init.sh docs/sql/phase1-apply.psql docs/sql/2026-10-27-phase1-account-security.sql docs/sql/2026-10-28-phase4-player-import.sql docs/sql/2026-10-28-phase4-player-photo.sql docs/sql/2026-10-28-phase5-certificates.sql docs/sql/2026-10-28-phase5-certificate-navigation.sql docs/sql/2026-10-28-phase5-certificate-batches.sql /opt/play-neml-migrations/
COPY docker/database/init.sh /docker-entrypoint-initdb.d/001-play-neml.sh
COPY docker/database/migrate.sh docker/database/grants.psql /opt/play-neml/
# Normalize executable scripts even on Windows; preserve historical migration bytes.
RUN sed -i 's/\r$//' /docker-entrypoint-initdb.d/001-play-neml.sh /opt/play-neml/migrate.sh
