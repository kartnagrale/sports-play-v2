#!/bin/sh
# Explicit upgrade job: no bootstrap, reset, role creation or password rotation.
set -eu
sh /opt/play-neml-migrations/phase1-init.sh
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
 -v "app_user=$APP_DB_USER" -f /opt/play-neml/grants.psql
