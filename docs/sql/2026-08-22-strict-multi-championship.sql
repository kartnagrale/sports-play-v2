-- STRICT MULTI-CHAMPIONSHIP MIGRATION
-- Run manually as a user that can alter objects in play_neml.
-- This script never changes another schema and never drops application data.
-- It assumes the application tables contain no production data, as confirmed for this rollout.

BEGIN;
SET LOCAL search_path TO play_neml, pg_catalog;
SET LOCAL lock_timeout TO '10s';
SET LOCAL statement_timeout TO '5min';

CREATE TABLE IF NOT EXISTS play_neml.message_tracker (
    msg_id serial4 PRIMARY KEY,
    msg_transcode varchar(10), msg_message_type varchar(1), msg_parameters varchar(5000),
    msg_mobile_nos varchar(255), msg_recipients_to varchar(500), msg_recipients_cc varchar(500),
    msg_recipients_bcc varchar(255), msg_subject varchar(255), msg_status varchar(20),
    msg_created_on timestamp NOT NULL, msg_has_attachment float8 DEFAULT 0,
    msg_attachment_location varchar(255), msg_attachment_filename varchar(255), msg_owner varchar(15),
    msg_modified_on timestamp, msg_remarks varchar(500), msg_request_id varchar(100),
    msg_app_id varchar(150), msg_is_attachment varchar(1), msg_incorrect_num varchar(255),
    msg_http_response_status varchar(20), msg_error_response_message varchar(200),
    msg_error_response_code varchar(100), msg_error_response_layer varchar(50),
    msg_vendor_status varchar(4), msg_vendor_remarks varchar(200),
    msg_incorrect_mobile_nos varchar(255), msg_message_id varchar(100), temp_id varchar(10),
    msg_batch_id numeric, app_id varchar(25), db_type varchar(50), msg_api_id varchar(200),
    msg_delivered_on timestamp
);

ALTER TABLE play_neml.auction_state
    ADD COLUMN IF NOT EXISTS championship_id uuid,
    ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE play_neml.teams
    ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE play_neml.matches
    ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE play_neml.match_formats
    ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE play_neml.bids
    ADD COLUMN IF NOT EXISTS championship_id uuid,
    ADD COLUMN IF NOT EXISTS sequence_no bigint;
ALTER TABLE play_neml.announcements ADD COLUMN IF NOT EXISTS championship_id uuid;
ALTER TABLE play_neml.seasons ADD COLUMN IF NOT EXISTS championship_id uuid;

-- Safe derivations make this migration usable in empty environments and local demo databases.
UPDATE play_neml.auction_state s
SET championship_id = a.championship_id
FROM play_neml.auctions a
WHERE s.auction_id = a.id AND s.championship_id IS NULL;

UPDATE play_neml.bids b
SET championship_id = a.championship_id
FROM play_neml.auctions a
WHERE b.auction_id = a.id AND b.championship_id IS NULL;

WITH ordered AS (
    SELECT id, row_number() OVER (PARTITION BY auction_id ORDER BY created_at, id) AS seq
    FROM play_neml.bids
    WHERE sequence_no IS NULL
)
UPDATE play_neml.bids b SET sequence_no = ordered.seq
FROM ordered WHERE b.id = ordered.id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM play_neml.announcements WHERE championship_id IS NULL)
       OR EXISTS (SELECT 1 FROM play_neml.seasons WHERE championship_id IS NULL) THEN
        RAISE EXCEPTION 'Existing announcements/seasons need an explicit championship_id before strict isolation can be enabled';
    END IF;
END $$;

ALTER TABLE play_neml.auction_state ALTER COLUMN championship_id SET NOT NULL;
ALTER TABLE play_neml.bids ALTER COLUMN championship_id SET NOT NULL;
ALTER TABLE play_neml.bids ALTER COLUMN sequence_no SET NOT NULL;
ALTER TABLE play_neml.announcements ALTER COLUMN championship_id SET NOT NULL;
ALTER TABLE play_neml.seasons ALTER COLUMN championship_id SET NOT NULL;

-- Parent composite keys are required so every child FK proves tenant ownership.
ALTER TABLE play_neml.auctions DROP CONSTRAINT IF EXISTS uq_auctions_id_championship;
ALTER TABLE play_neml.auctions ADD CONSTRAINT uq_auctions_id_championship UNIQUE (id, championship_id);
ALTER TABLE play_neml.teams DROP CONSTRAINT IF EXISTS uq_teams_id_championship;
ALTER TABLE play_neml.teams ADD CONSTRAINT uq_teams_id_championship UNIQUE (id, championship_id);
ALTER TABLE play_neml.players DROP CONSTRAINT IF EXISTS uq_players_id_championship;
ALTER TABLE play_neml.players ADD CONSTRAINT uq_players_id_championship UNIQUE (id, championship_id);

ALTER TABLE play_neml.auction_state DROP CONSTRAINT IF EXISTS fk_auction_state_championship;
ALTER TABLE play_neml.auction_state ADD CONSTRAINT fk_auction_state_championship
    FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);
ALTER TABLE play_neml.auction_state DROP CONSTRAINT IF EXISTS fk_auction_state_auction_tenant;
ALTER TABLE play_neml.auction_state ADD CONSTRAINT fk_auction_state_auction_tenant
    FOREIGN KEY (auction_id, championship_id) REFERENCES play_neml.auctions(id, championship_id);
ALTER TABLE play_neml.auction_state DROP CONSTRAINT IF EXISTS fk_auction_state_player_tenant;
ALTER TABLE play_neml.auction_state ADD CONSTRAINT fk_auction_state_player_tenant
    FOREIGN KEY (current_player_id, championship_id) REFERENCES play_neml.players(id, championship_id);

ALTER TABLE play_neml.players DROP CONSTRAINT IF EXISTS fk_players_auction_tenant;
ALTER TABLE play_neml.players ADD CONSTRAINT fk_players_auction_tenant
    FOREIGN KEY (auction_id, championship_id) REFERENCES play_neml.auctions(id, championship_id);
ALTER TABLE play_neml.players DROP CONSTRAINT IF EXISTS fk_players_team_tenant;
ALTER TABLE play_neml.players ADD CONSTRAINT fk_players_team_tenant
    FOREIGN KEY (team_id, championship_id) REFERENCES play_neml.teams(id, championship_id);

ALTER TABLE play_neml.bids DROP CONSTRAINT IF EXISTS fk_bids_championship;
ALTER TABLE play_neml.bids ADD CONSTRAINT fk_bids_championship
    FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);
ALTER TABLE play_neml.bids DROP CONSTRAINT IF EXISTS fk_bids_auction_tenant;
ALTER TABLE play_neml.bids ADD CONSTRAINT fk_bids_auction_tenant
    FOREIGN KEY (auction_id, championship_id) REFERENCES play_neml.auctions(id, championship_id);
ALTER TABLE play_neml.bids DROP CONSTRAINT IF EXISTS fk_bids_player_tenant;
ALTER TABLE play_neml.bids ADD CONSTRAINT fk_bids_player_tenant
    FOREIGN KEY (player_id, championship_id) REFERENCES play_neml.players(id, championship_id);
ALTER TABLE play_neml.bids DROP CONSTRAINT IF EXISTS fk_bids_team_tenant;
ALTER TABLE play_neml.bids ADD CONSTRAINT fk_bids_team_tenant
    FOREIGN KEY (team_id, championship_id) REFERENCES play_neml.teams(id, championship_id);

ALTER TABLE play_neml.matches DROP CONSTRAINT IF EXISTS fk_matches_team_a_tenant;
ALTER TABLE play_neml.matches ADD CONSTRAINT fk_matches_team_a_tenant
    FOREIGN KEY (team_a_id, championship_id) REFERENCES play_neml.teams(id, championship_id);
ALTER TABLE play_neml.matches DROP CONSTRAINT IF EXISTS fk_matches_team_b_tenant;
ALTER TABLE play_neml.matches ADD CONSTRAINT fk_matches_team_b_tenant
    FOREIGN KEY (team_b_id, championship_id) REFERENCES play_neml.teams(id, championship_id);
ALTER TABLE play_neml.matches DROP CONSTRAINT IF EXISTS fk_matches_winner_tenant;
ALTER TABLE play_neml.matches ADD CONSTRAINT fk_matches_winner_tenant
    FOREIGN KEY (winner_team_id, championship_id) REFERENCES play_neml.teams(id, championship_id);

ALTER TABLE play_neml.championship_roles DROP CONSTRAINT IF EXISTS fk_championship_roles_team_tenant;
ALTER TABLE play_neml.championship_roles ADD CONSTRAINT fk_championship_roles_team_tenant
    FOREIGN KEY (team_id, championship_id) REFERENCES play_neml.teams(id, championship_id);
ALTER TABLE play_neml.announcements DROP CONSTRAINT IF EXISTS fk_announcements_championship;
ALTER TABLE play_neml.announcements ADD CONSTRAINT fk_announcements_championship
    FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);
ALTER TABLE play_neml.seasons DROP CONSTRAINT IF EXISTS fk_seasons_championship;
ALTER TABLE play_neml.seasons ADD CONSTRAINT fk_seasons_championship
    FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);

-- Race-proof allocation and championship-local business uniqueness.
DO $$
DECLARE constraint_name text;
BEGIN
    SELECT c.conname INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON t.oid=c.conrelid
    JOIN pg_namespace n ON n.oid=t.relnamespace
    WHERE n.nspname='play_neml' AND t.relname='seasons' AND c.contype='u'
      AND pg_get_constraintdef(c.oid) = 'UNIQUE (name)'
    LIMIT 1;
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE play_neml.seasons DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_bids_auction_sequence
    ON play_neml.bids (auction_id, sequence_no);
CREATE UNIQUE INDEX IF NOT EXISTS uq_matches_championship_number
    ON play_neml.matches (championship_id, match_number);
CREATE UNIQUE INDEX IF NOT EXISTS uq_teams_championship_short_code
    ON play_neml.teams (championship_id, short_code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_players_auction_order
    ON play_neml.players (auction_id, auction_order)
    WHERE auction_id IS NOT NULL AND auction_order IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_seasons_championship_name
    ON play_neml.seasons (championship_id, name);
CREATE UNIQUE INDEX IF NOT EXISTS uq_seasons_one_active_per_championship
    ON play_neml.seasons (championship_id) WHERE active IS TRUE;

CREATE INDEX IF NOT EXISTS idx_auction_state_timer_sweep
    ON play_neml.auction_state (bid_deadline)
    WHERE status = 'RUNNING';
CREATE INDEX IF NOT EXISTS idx_bids_championship_auction
    ON play_neml.bids (championship_id, auction_id, sequence_no DESC);

-- Super admin owns championship provisioning only; championship admins own operations.
UPDATE play_neml.role_screen_mappings
SET visible = false
WHERE role_key = 'SUPER_ADMIN';
UPDATE play_neml.role_screen_mappings mapping
SET visible = true
FROM play_neml.app_screens screen
WHERE mapping.screen_id = screen.id
  AND mapping.role_key = 'SUPER_ADMIN'
  AND screen.code IN ('CHAMPIONSHIP_LAUNCHPAD', 'CHAMPIONSHIPS');

COMMIT;

-- Read-only verification. Every mismatch query must return zero rows.
SELECT 'player_auction_mismatch' AS check_name, count(*) AS violations
FROM play_neml.players p JOIN play_neml.auctions a ON a.id=p.auction_id
WHERE p.championship_id<>a.championship_id
UNION ALL
SELECT 'player_team_mismatch', count(*)
FROM play_neml.players p JOIN play_neml.teams t ON t.id=p.team_id
WHERE p.championship_id<>t.championship_id
UNION ALL
SELECT 'bid_tenant_mismatch', count(*)
FROM play_neml.bids b JOIN play_neml.auctions a ON a.id=b.auction_id
WHERE b.championship_id<>a.championship_id
UNION ALL
SELECT 'match_team_a_mismatch', count(*)
FROM play_neml.matches m JOIN play_neml.teams t ON t.id=m.team_a_id
WHERE m.championship_id<>t.championship_id
UNION ALL
SELECT 'match_team_b_mismatch', count(*)
FROM play_neml.matches m JOIN play_neml.teams t ON t.id=m.team_b_id
WHERE m.championship_id<>t.championship_id;
