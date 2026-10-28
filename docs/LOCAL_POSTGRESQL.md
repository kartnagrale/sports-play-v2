# Local PostgreSQL setup

The local development database is isolated from the company database. All application objects remain inside the `play_neml` schema.

## IntelliJ environment variables

Set these under **Run → Edit Configurations → Environment variables**:

```text
SPRING_PROFILES_ACTIVE=local
LOCAL_DB_PASSWORD=<your-local-postgres-password>
```

Optional overrides:

```text
LOCAL_DB_URL=jdbc:postgresql://localhost:5432/sports_play?currentSchema=play_neml
LOCAL_DB_USER=postgres
DATA_SEED_ENABLED=true
```

The normal `application.yml` remains the company-environment configuration. The local datasource is activated only when `SPRING_PROFILES_ACTIVE=local` is present.

## Local rebuild sequence

For a new empty local database and schema:

1. Execute `docs/sql/sports-play-complete-schema.sql` once against the empty database as its owner.
2. Run `tools/apply-migrations.ps1` using `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and a protected password source such as `.pgpass`.
3. Start the backend with Hibernate `validate`. Essential navigation initializes independently of demo seeding. Enable `DATA_SEED_ENABLED` only for disposable local demonstrations.

For a populated database use the reviewed incremental migration, not this empty-database sequence. Do not use `ddl-auto=update`. See [Phase 1 operations](phase-1/OPERATIONS.md).

Never run the local rebuild sequence against the company database.
