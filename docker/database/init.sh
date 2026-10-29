#!/bin/sh
# The official entrypoint calls this only for an empty PGDATA volume.
set -eu
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f /opt/play-neml/schema.sql
sh /opt/play-neml-migrations/phase1-init.sh
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
 -v "app_user=$APP_DB_USER" -v "app_password=$APP_DB_PASSWORD" <<'SQL'
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password' NOSUPERUSER NOCREATEDB NOCREATEROLE;
SQL
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
 -v "app_user=$APP_DB_USER" -f /opt/play-neml/grants.psql
