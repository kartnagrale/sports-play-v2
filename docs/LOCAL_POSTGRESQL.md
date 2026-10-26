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

1. Run the backend once with the additional argument `--spring.jpa.hibernate.ddl-auto=update --app.seed.enabled=false` to create the JPA tables.
2. Execute `docs/sql/2026-08-22-strict-multi-championship.sql` against the `sports_play` database.
3. Execute `docs/sql/2026-08-22-championship-admin-configuration.sql` against the `sports_play` database.
4. Remove the one-time arguments and start with the `local` profile. Hibernate then validates the schema and demo data is seeded.

Never run the local rebuild sequence against the company database.
