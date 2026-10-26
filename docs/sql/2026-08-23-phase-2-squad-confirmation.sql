-- PHASE 2: SQUAD CONFIRMATION
-- Run manually as a user allowed to create/alter objects in play_neml.
-- This script is idempotent and does not read or write any other schema.

BEGIN;
SET LOCAL search_path TO play_neml, pg_catalog;
SET LOCAL lock_timeout TO '10s';
SET LOCAL statement_timeout TO '5min';

CREATE TABLE IF NOT EXISTS play_neml.squad_confirmations (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    championship_id uuid NOT NULL,
    team_id uuid NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'DRAFT',
    confirmed_by uuid,
    confirmed_at timestamptz,
    locked_by uuid,
    locked_at timestamptz,
    reopened_by uuid,
    reopened_at timestamptz,
    notes varchar(500),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_squad_confirmation_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'LOCKED')),
    CONSTRAINT uq_squad_confirmation_team UNIQUE (championship_id, team_id),
    CONSTRAINT uq_squad_confirmation_id_tenant UNIQUE (id, championship_id, team_id),
    CONSTRAINT fk_squad_confirmation_championship FOREIGN KEY (championship_id)
        REFERENCES play_neml.championships(id),
    CONSTRAINT fk_squad_confirmation_team_tenant FOREIGN KEY (team_id, championship_id)
        REFERENCES play_neml.teams(id, championship_id),
    CONSTRAINT fk_squad_confirmation_confirmed_by FOREIGN KEY (confirmed_by)
        REFERENCES play_neml.users(id),
    CONSTRAINT fk_squad_confirmation_locked_by FOREIGN KEY (locked_by)
        REFERENCES play_neml.users(id),
    CONSTRAINT fk_squad_confirmation_reopened_by FOREIGN KEY (reopened_by)
        REFERENCES play_neml.users(id)
);

CREATE INDEX IF NOT EXISTS idx_squad_confirmation_championship
    ON play_neml.squad_confirmations (championship_id);
CREATE INDEX IF NOT EXISTS idx_squad_confirmation_status
    ON play_neml.squad_confirmations (championship_id, status);

CREATE TABLE IF NOT EXISTS play_neml.squad_confirmation_events (
    id uuid PRIMARY KEY,
    championship_id uuid NOT NULL,
    team_id uuid NOT NULL,
    confirmation_id uuid NOT NULL,
    action varchar(20) NOT NULL,
    actor_user_id uuid NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes varchar(500),
    roster_snapshot text NOT NULL,
    CONSTRAINT ck_squad_confirmation_action CHECK (action IN ('CONFIRMED', 'LOCKED', 'REOPENED')),
    CONSTRAINT fk_squad_event_confirmation_tenant FOREIGN KEY (confirmation_id, championship_id, team_id)
        REFERENCES play_neml.squad_confirmations(id, championship_id, team_id),
    CONSTRAINT fk_squad_event_actor FOREIGN KEY (actor_user_id)
        REFERENCES play_neml.users(id)
);

CREATE INDEX IF NOT EXISTS idx_squad_event_championship
    ON play_neml.squad_confirmation_events (championship_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_squad_event_team
    ON play_neml.squad_confirmation_events (team_id, occurred_at DESC);

-- Existing teams start open. Captains must actively confirm them; the migration
-- never pretends a squad was reviewed by a user.
INSERT INTO play_neml.squad_confirmations (
    id, championship_id, team_id, status, created_at, updated_at
)
SELECT gen_random_uuid(), team.championship_id, team.id, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM play_neml.teams team
WHERE NOT EXISTS (
    SELECT 1 FROM play_neml.squad_confirmations confirmation
    WHERE confirmation.championship_id = team.championship_id
      AND confirmation.team_id = team.id
);

-- Database-driven navigation: visible to championship admins and team captains.
INSERT INTO play_neml.app_screens (
    id, code, label, path, icon, section_name, display_order, active
)
VALUES (
    gen_random_uuid(), 'SQUAD_CONFIRMATION', 'Squad Confirmation',
    '/squad-confirmation', 'ClipboardCheck', 'Competition', 35, true
)
ON CONFLICT (code) DO UPDATE SET
    label = EXCLUDED.label,
    path = EXCLUDED.path,
    icon = EXCLUDED.icon,
    section_name = EXCLUDED.section_name,
    display_order = EXCLUDED.display_order,
    active = true;

INSERT INTO play_neml.role_screen_mappings (id, role_key, screen_id, visible)
SELECT gen_random_uuid(), role_key, screen.id, true
FROM (VALUES ('CHAMPIONSHIP_ADMIN'), ('TEAM_CAPTAIN')) AS roles(role_key)
JOIN play_neml.app_screens screen ON screen.code = 'SQUAD_CONFIRMATION'
ON CONFLICT (role_key, screen_id) DO UPDATE SET visible = true;

-- Safety check: every confirmation must agree with its team's tenant.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM play_neml.squad_confirmations confirmation
        JOIN play_neml.teams team ON team.id = confirmation.team_id
        WHERE confirmation.championship_id <> team.championship_id
    ) THEN
        RAISE EXCEPTION 'Squad confirmation tenant mismatch detected';
    END IF;
END $$;

COMMIT;

-- Verification (all mismatch values must be 0):
SELECT
    (SELECT count(*) FROM play_neml.squad_confirmations) AS confirmation_rows,
    (SELECT count(*) FROM play_neml.squad_confirmation_events) AS event_rows,
    (SELECT count(*)
       FROM play_neml.squad_confirmations confirmation
       JOIN play_neml.teams team ON team.id = confirmation.team_id
      WHERE confirmation.championship_id <> team.championship_id) AS tenant_mismatches;
