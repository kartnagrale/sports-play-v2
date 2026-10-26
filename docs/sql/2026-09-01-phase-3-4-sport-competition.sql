-- PHASES 3 AND 4: GENERIC COMPETITION + BADMINTON LEAGUE/PLAYOFFS
-- Give this script to the company DBA for manual execution.
-- It is idempotent and every application object is explicitly in play_neml.
-- The application keeps spring.jpa.hibernate.ddl-auto=validate and never executes this script.

BEGIN;
SET LOCAL search_path TO play_neml, pg_catalog;
SET LOCAL lock_timeout TO '10s';
SET LOCAL statement_timeout TO '5min';

ALTER TABLE play_neml.championships
    ADD COLUMN IF NOT EXISTS lifecycle_phase varchar(32);
UPDATE play_neml.championships
SET lifecycle_phase = CASE
    WHEN status = 'ARCHIVED' THEN 'ARCHIVED'
    WHEN status = 'COMPLETED' THEN 'COMPLETED'
    WHEN EXISTS (SELECT 1 FROM play_neml.matches m WHERE m.championship_id = championships.id) THEN 'LEAGUE_STAGE'
    WHEN EXISTS (SELECT 1 FROM play_neml.auctions a WHERE a.championship_id = championships.id AND a.status = 'COMPLETED') THEN 'SQUAD_CONFIRMATION'
    WHEN status = 'ACTIVE' THEN 'TEAM_PROVISIONING'
    ELSE 'CONFIGURATION'
END
WHERE lifecycle_phase IS NULL;
ALTER TABLE play_neml.championships
    ALTER COLUMN lifecycle_phase SET DEFAULT 'CREATION',
    ALTER COLUMN lifecycle_phase SET NOT NULL;

-- PostgreSQL requires a unique key matching every composite tenant foreign key.
-- The primary keys remain unchanged; these keys make ownership enforceable in SQL.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint
                   WHERE conrelid='play_neml.teams'::regclass AND conname='uq_teams_id_championship') THEN
        ALTER TABLE play_neml.teams ADD CONSTRAINT uq_teams_id_championship UNIQUE (id, championship_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint
                   WHERE conrelid='play_neml.players'::regclass AND conname='uq_players_id_championship') THEN
        ALTER TABLE play_neml.players ADD CONSTRAINT uq_players_id_championship UNIQUE (id, championship_id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS play_neml.competition_configurations (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    championship_id uuid NOT NULL,
    sport_code varchar(64) NOT NULL,
    round_robin_mode varchar(16) NOT NULL DEFAULT 'SINGLE',
    points_per_win integer NOT NULL DEFAULT 3,
    points_per_loss integer NOT NULL DEFAULT 0,
    tie_breaker_priority varchar(500) NOT NULL DEFAULT 'HEAD_TO_HEAD,FORMAT_WINS,FORMAT_DIFF,MATCHES_WON,NAME',
    qualifying_team_count integer NOT NULL DEFAULT 4,
    venues text NOT NULL DEFAULT 'Center Court',
    league_start_at timestamptz,
    fixture_spacing_days integer NOT NULL DEFAULT 1,
    manual_date_editing boolean NOT NULL DEFAULT true,
    result_schema_version integer NOT NULL DEFAULT 1,
    champion_team_id uuid,
    runner_up_team_id uuid,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_competition_config_championship UNIQUE (championship_id),
    CONSTRAINT ck_competition_round_robin CHECK (round_robin_mode IN ('SINGLE','DOUBLE')),
    CONSTRAINT ck_competition_points CHECK (points_per_win >= 0 AND points_per_loss >= 0),
    CONSTRAINT ck_competition_qualifiers CHECK (qualifying_team_count >= 2 AND qualifying_team_count % 2 = 0),
    CONSTRAINT ck_competition_spacing CHECK (fixture_spacing_days BETWEEN 0 AND 30),
    CONSTRAINT fk_competition_config_championship FOREIGN KEY (championship_id)
        REFERENCES play_neml.championships(id),
    CONSTRAINT fk_competition_config_champion FOREIGN KEY (champion_team_id, championship_id)
        REFERENCES play_neml.teams(id, championship_id),
    CONSTRAINT fk_competition_config_runner_up FOREIGN KEY (runner_up_team_id, championship_id)
        REFERENCES play_neml.teams(id, championship_id)
);

INSERT INTO play_neml.competition_configurations (
    id, championship_id, sport_code, round_robin_mode, points_per_win, points_per_loss,
    tie_breaker_priority, qualifying_team_count, venues, fixture_spacing_days,
    manual_date_editing, result_schema_version, created_at, updated_at
)
SELECT gen_random_uuid(), c.id,
       upper(regexp_replace(trim(c.sport_type), '[^A-Za-z0-9]+', '_', 'g')),
       'SINGLE', COALESCE(s.points_per_win, 3), 0,
       'HEAD_TO_HEAD,FORMAT_WINS,FORMAT_DIFF,MATCHES_WON,NAME',
       CASE WHEN (SELECT count(*) FROM play_neml.teams t WHERE t.championship_id=c.id) >= 4 THEN 4 ELSE 2 END,
       'Center Court', 1, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM play_neml.championships c
LEFT JOIN play_neml.tournament_settings s ON s.championship_id=c.id
WHERE NOT EXISTS (
    SELECT 1 FROM play_neml.competition_configurations cc WHERE cc.championship_id=c.id
);

CREATE TABLE IF NOT EXISTS play_neml.competition_stages (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    championship_id uuid NOT NULL,
    stage_type varchar(24) NOT NULL,
    stage_order integer NOT NULL,
    status varchar(24) NOT NULL DEFAULT 'NOT_STARTED',
    started_at timestamptz,
    completed_at timestamptz,
    frozen_at timestamptz,
    standings_snapshot text,
    stage_metadata text NOT NULL DEFAULT '{}',
    CONSTRAINT uq_competition_stage_id_tenant UNIQUE (id, championship_id),
    CONSTRAINT uq_competition_stage_type UNIQUE (championship_id, stage_type),
    CONSTRAINT ck_competition_stage_type CHECK (stage_type IN ('LEAGUE','SEMI_FINAL','THIRD_PLACE','FINAL')),
    CONSTRAINT ck_competition_stage_status CHECK (status IN ('NOT_STARTED','ACTIVE','COMPLETED','FROZEN')),
    CONSTRAINT fk_competition_stage_championship FOREIGN KEY (championship_id)
        REFERENCES play_neml.championships(id)
);
CREATE INDEX IF NOT EXISTS idx_competition_stage_championship
    ON play_neml.competition_stages (championship_id, stage_order);

ALTER TABLE play_neml.matches
    ADD COLUMN IF NOT EXISTS competition_stage_id uuid,
    ADD COLUMN IF NOT EXISTS stage_championship_id uuid,
    ADD COLUMN IF NOT EXISTS stage_type varchar(24) DEFAULT 'LEAGUE',
    ADD COLUMN IF NOT EXISTS round_number integer DEFAULT 1,
    ADD COLUMN IF NOT EXISTS leg_number integer DEFAULT 1,
    ADD COLUMN IF NOT EXISTS fixture_key varchar(180),
    ADD COLUMN IF NOT EXISTS seed_a integer,
    ADD COLUMN IF NOT EXISTS seed_b integer,
    ADD COLUMN IF NOT EXISTS team_a_points integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS team_b_points integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS result_revision integer DEFAULT 0;

INSERT INTO play_neml.competition_stages (
    id, championship_id, stage_type, stage_order, status, started_at, completed_at, stage_metadata
)
SELECT gen_random_uuid(), m.championship_id, 'LEAGUE', 1,
       CASE WHEN bool_and(m.status='COMPLETED') THEN 'COMPLETED' ELSE 'ACTIVE' END,
       min(m.scheduled_at),
       CASE WHEN bool_and(m.status='COMPLETED') THEN CURRENT_TIMESTAMP ELSE NULL END,
       '{"source":"legacy-matches"}'
FROM play_neml.matches m
GROUP BY m.championship_id
ON CONFLICT (championship_id, stage_type) DO NOTHING;

UPDATE play_neml.matches m
SET stage_type=COALESCE(m.stage_type,'LEAGUE'),
    round_number=COALESCE(m.round_number,1),
    leg_number=COALESCE(m.leg_number,1),
    fixture_key=COALESCE(m.fixture_key,'LEGACY-'||m.id::text),
    team_a_points=CASE WHEN m.status='COMPLETED' AND m.winner_team_id=m.team_a_id
             THEN COALESCE(s.points_per_win,3) ELSE 0 END,
    team_b_points=CASE WHEN m.status='COMPLETED' AND m.winner_team_id=m.team_b_id
             THEN COALESCE(s.points_per_win,3) ELSE 0 END,
    result_revision=CASE WHEN m.status='COMPLETED' THEN GREATEST(COALESCE(m.result_revision,0),1)
                         ELSE COALESCE(m.result_revision,0) END,
    competition_stage_id=COALESCE(m.competition_stage_id,st.id),
    stage_championship_id=COALESCE(m.stage_championship_id,m.championship_id)
FROM play_neml.competition_stages st
LEFT JOIN play_neml.tournament_settings s ON s.championship_id=st.championship_id
WHERE st.championship_id=m.championship_id AND st.stage_type='LEAGUE';

ALTER TABLE play_neml.matches
    ALTER COLUMN stage_type SET NOT NULL,
    ALTER COLUMN round_number SET NOT NULL,
    ALTER COLUMN leg_number SET NOT NULL,
    ALTER COLUMN fixture_key SET NOT NULL,
    ALTER COLUMN team_a_points SET NOT NULL,
    ALTER COLUMN team_b_points SET NOT NULL,
    ALTER COLUMN result_revision SET NOT NULL;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint
                   WHERE conrelid='play_neml.matches'::regclass AND conname='uq_matches_id_championship') THEN
        ALTER TABLE play_neml.matches ADD CONSTRAINT uq_matches_id_championship UNIQUE (id, championship_id);
    END IF;
END $$;
ALTER TABLE play_neml.matches DROP CONSTRAINT IF EXISTS uq_match_fixture_key;
ALTER TABLE play_neml.matches ADD CONSTRAINT uq_match_fixture_key UNIQUE (championship_id, fixture_key);
ALTER TABLE play_neml.matches DROP CONSTRAINT IF EXISTS fk_matches_competition_stage_tenant;
ALTER TABLE play_neml.matches ADD CONSTRAINT fk_matches_competition_stage_tenant
    FOREIGN KEY (competition_stage_id, stage_championship_id)
    REFERENCES play_neml.competition_stages(id, championship_id);
CREATE INDEX IF NOT EXISTS idx_match_stage
    ON play_neml.matches (championship_id, stage_type, status);

ALTER TABLE play_neml.match_formats ADD COLUMN IF NOT EXISTS championship_id uuid;
UPDATE play_neml.match_formats f
SET championship_id=m.championship_id
FROM play_neml.matches m
WHERE m.id=f.match_id AND f.championship_id IS NULL;
ALTER TABLE play_neml.match_formats ALTER COLUMN championship_id SET NOT NULL;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint
                   WHERE conrelid='play_neml.match_formats'::regclass AND conname='uq_match_formats_id_championship') THEN
        ALTER TABLE play_neml.match_formats ADD CONSTRAINT uq_match_formats_id_championship UNIQUE (id, championship_id);
    END IF;
END $$;
ALTER TABLE play_neml.match_formats DROP CONSTRAINT IF EXISTS fk_match_formats_match_tenant;
ALTER TABLE play_neml.match_formats ADD CONSTRAINT fk_match_formats_match_tenant
    FOREIGN KEY (match_id, championship_id) REFERENCES play_neml.matches(id, championship_id);
ALTER TABLE play_neml.match_formats DROP CONSTRAINT IF EXISTS fk_match_formats_winner_tenant;
ALTER TABLE play_neml.match_formats ADD CONSTRAINT fk_match_formats_winner_tenant
    FOREIGN KEY (winner_team_id, championship_id) REFERENCES play_neml.teams(id, championship_id);

ALTER TABLE play_neml.format_side_a_players
    ADD COLUMN IF NOT EXISTS championship_id uuid,
    ADD COLUMN IF NOT EXISTS player_championship_id uuid;
ALTER TABLE play_neml.format_side_b_players
    ADD COLUMN IF NOT EXISTS championship_id uuid,
    ADD COLUMN IF NOT EXISTS player_championship_id uuid;
UPDATE play_neml.format_side_a_players side
SET championship_id=f.championship_id, player_championship_id=p.championship_id
FROM play_neml.match_formats f, play_neml.players p
WHERE f.id=side.format_id AND p.id=side.player_id
  AND (side.championship_id IS NULL OR side.player_championship_id IS NULL);
UPDATE play_neml.format_side_b_players side
SET championship_id=f.championship_id, player_championship_id=p.championship_id
FROM play_neml.match_formats f, play_neml.players p
WHERE f.id=side.format_id AND p.id=side.player_id
  AND (side.championship_id IS NULL OR side.player_championship_id IS NULL);

-- Hibernate writes the two legacy join keys. This schema-local trigger derives
-- both tenant keys and rejects any attempted cross-championship lineup.
CREATE OR REPLACE FUNCTION play_neml.enforce_format_player_tenant()
RETURNS trigger
LANGUAGE plpgsql
SET search_path TO play_neml, pg_catalog
AS $$
DECLARE
    format_tenant uuid;
    player_tenant uuid;
BEGIN
    SELECT championship_id INTO STRICT format_tenant
    FROM play_neml.match_formats WHERE id=NEW.format_id;
    SELECT championship_id INTO STRICT player_tenant
    FROM play_neml.players WHERE id=NEW.player_id;
    IF format_tenant<>player_tenant THEN
        RAISE EXCEPTION 'Cross-championship lineup assignment is forbidden';
    END IF;
    NEW.championship_id:=format_tenant;
    NEW.player_championship_id:=player_tenant;
    RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_format_side_a_tenant ON play_neml.format_side_a_players;
CREATE TRIGGER trg_format_side_a_tenant
BEFORE INSERT OR UPDATE OF format_id,player_id ON play_neml.format_side_a_players
FOR EACH ROW EXECUTE FUNCTION play_neml.enforce_format_player_tenant();
DROP TRIGGER IF EXISTS trg_format_side_b_tenant ON play_neml.format_side_b_players;
CREATE TRIGGER trg_format_side_b_tenant
BEFORE INSERT OR UPDATE OF format_id,player_id ON play_neml.format_side_b_players
FOR EACH ROW EXECUTE FUNCTION play_neml.enforce_format_player_tenant();

ALTER TABLE play_neml.format_side_a_players
    ALTER COLUMN championship_id SET NOT NULL,
    ALTER COLUMN player_championship_id SET NOT NULL;
ALTER TABLE play_neml.format_side_b_players
    ALTER COLUMN championship_id SET NOT NULL,
    ALTER COLUMN player_championship_id SET NOT NULL;

ALTER TABLE play_neml.format_side_a_players DROP CONSTRAINT IF EXISTS fk_format_side_a_format_tenant;
ALTER TABLE play_neml.format_side_a_players ADD CONSTRAINT fk_format_side_a_format_tenant
    FOREIGN KEY (format_id, championship_id) REFERENCES play_neml.match_formats(id, championship_id) ON DELETE CASCADE;
ALTER TABLE play_neml.format_side_a_players DROP CONSTRAINT IF EXISTS fk_format_side_a_player_tenant;
ALTER TABLE play_neml.format_side_a_players ADD CONSTRAINT fk_format_side_a_player_tenant
    FOREIGN KEY (player_id, player_championship_id) REFERENCES play_neml.players(id, championship_id);
ALTER TABLE play_neml.format_side_a_players DROP CONSTRAINT IF EXISTS ck_format_side_a_same_tenant;
ALTER TABLE play_neml.format_side_a_players ADD CONSTRAINT ck_format_side_a_same_tenant
    CHECK (championship_id=player_championship_id);
ALTER TABLE play_neml.format_side_b_players DROP CONSTRAINT IF EXISTS fk_format_side_b_format_tenant;
ALTER TABLE play_neml.format_side_b_players ADD CONSTRAINT fk_format_side_b_format_tenant
    FOREIGN KEY (format_id, championship_id) REFERENCES play_neml.match_formats(id, championship_id) ON DELETE CASCADE;
ALTER TABLE play_neml.format_side_b_players DROP CONSTRAINT IF EXISTS fk_format_side_b_player_tenant;
ALTER TABLE play_neml.format_side_b_players ADD CONSTRAINT fk_format_side_b_player_tenant
    FOREIGN KEY (player_id, player_championship_id) REFERENCES play_neml.players(id, championship_id);
ALTER TABLE play_neml.format_side_b_players DROP CONSTRAINT IF EXISTS ck_format_side_b_same_tenant;
ALTER TABLE play_neml.format_side_b_players ADD CONSTRAINT ck_format_side_b_same_tenant
    CHECK (championship_id=player_championship_id);

CREATE TABLE IF NOT EXISTS play_neml.competition_qualifications (
    id uuid PRIMARY KEY,
    championship_id uuid NOT NULL,
    source_stage varchar(24) NOT NULL,
    team_id uuid NOT NULL,
    seed integer NOT NULL,
    qualified_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_competition_qualification_seed UNIQUE (championship_id, source_stage, seed),
    CONSTRAINT uq_competition_qualification_team UNIQUE (championship_id, source_stage, team_id),
    CONSTRAINT fk_competition_qualification_championship FOREIGN KEY (championship_id)
        REFERENCES play_neml.championships(id),
    CONSTRAINT fk_competition_qualification_team FOREIGN KEY (team_id, championship_id)
        REFERENCES play_neml.teams(id, championship_id),
    CONSTRAINT ck_competition_qualification_stage CHECK (source_stage IN ('LEAGUE','SEMI_FINAL','THIRD_PLACE','FINAL')),
    CONSTRAINT ck_competition_qualification_seed CHECK (seed > 0)
);

CREATE TABLE IF NOT EXISTS play_neml.competition_audit_events (
    id uuid PRIMARY KEY,
    championship_id uuid NOT NULL,
    fixture_id uuid,
    fixture_championship_id uuid,
    actor_user_id uuid,
    event_type varchar(64) NOT NULL,
    revision integer NOT NULL DEFAULT 1,
    payload text NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_competition_audit_championship FOREIGN KEY (championship_id)
        REFERENCES play_neml.championships(id),
    CONSTRAINT fk_competition_audit_fixture FOREIGN KEY (fixture_id, fixture_championship_id)
        REFERENCES play_neml.matches(id, championship_id) ON DELETE SET NULL,
    CONSTRAINT fk_competition_audit_actor FOREIGN KEY (actor_user_id)
        REFERENCES play_neml.users(id)
);
CREATE INDEX IF NOT EXISTS idx_competition_audit_championship
    ON play_neml.competition_audit_events (championship_id, occurred_at DESC);

INSERT INTO play_neml.app_screens (id,code,label,path,icon,section_name,display_order,active)
VALUES (gen_random_uuid(),'COMPETITION_MANAGEMENT','League & Playoffs','/competition','Brackets','Competition',65,true)
ON CONFLICT (code) DO UPDATE SET label=EXCLUDED.label,path=EXCLUDED.path,icon=EXCLUDED.icon,
    section_name=EXCLUDED.section_name,display_order=EXCLUDED.display_order,active=true;
INSERT INTO play_neml.role_screen_mappings (id,role_key,screen_id,visible)
SELECT gen_random_uuid(),roles.role_key,screen.id,true
FROM (VALUES ('CHAMPIONSHIP_ADMIN'),('TEAM_CAPTAIN'),('SPECTATOR')) roles(role_key)
JOIN play_neml.app_screens screen ON screen.code='COMPETITION_MANAGEMENT'
ON CONFLICT (role_key,screen_id) DO UPDATE SET visible=true;
UPDATE play_neml.role_screen_mappings mapping SET visible=false
FROM play_neml.app_screens screen
WHERE mapping.screen_id=screen.id AND mapping.role_key='SUPER_ADMIN'
  AND screen.code='COMPETITION_MANAGEMENT';

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM play_neml.matches m
        JOIN play_neml.competition_stages s ON s.id=m.competition_stage_id
        WHERE m.championship_id<>s.championship_id
    ) OR EXISTS (
        SELECT 1 FROM play_neml.match_formats f JOIN play_neml.matches m ON m.id=f.match_id
        WHERE f.championship_id<>m.championship_id
    ) OR EXISTS (
        SELECT 1 FROM play_neml.format_side_a_players WHERE championship_id<>player_championship_id
    ) OR EXISTS (
        SELECT 1 FROM play_neml.format_side_b_players WHERE championship_id<>player_championship_id
    ) THEN
        RAISE EXCEPTION 'Competition migration detected a championship ownership mismatch';
    END IF;
END $$;

COMMIT;

-- Read-only verification. Every violations value must be zero.
SELECT 'fixture_stage_mismatch' check_name,count(*) violations
FROM play_neml.matches m JOIN play_neml.competition_stages s ON s.id=m.competition_stage_id
WHERE m.championship_id<>s.championship_id
UNION ALL
SELECT 'format_fixture_mismatch',count(*)
FROM play_neml.match_formats f JOIN play_neml.matches m ON m.id=f.match_id
WHERE f.championship_id<>m.championship_id
UNION ALL
SELECT 'side_a_player_mismatch',count(*)
FROM play_neml.format_side_a_players WHERE championship_id<>player_championship_id
UNION ALL
SELECT 'side_b_player_mismatch',count(*)
FROM play_neml.format_side_b_players WHERE championship_id<>player_championship_id
UNION ALL
SELECT 'qualification_team_mismatch',count(*)
FROM play_neml.competition_qualifications q JOIN play_neml.teams t ON t.id=q.team_id
WHERE q.championship_id<>t.championship_id;
