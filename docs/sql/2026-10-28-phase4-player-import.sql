ALTER TABLE play_neml.players ADD COLUMN player_reference varchar(64);
ALTER TABLE play_neml.players ADD CONSTRAINT ck_player_reference CHECK (player_reference IS NULL OR player_reference ~ '^[A-Z0-9][A-Z0-9_-]{0,63}$');
CREATE UNIQUE INDEX uq_player_reference ON play_neml.players(championship_id,player_reference) WHERE player_reference IS NOT NULL;
CREATE TABLE play_neml.player_imports (
 id uuid PRIMARY KEY, championship_id uuid NOT NULL REFERENCES play_neml.championships(id),
 actor_id uuid NOT NULL REFERENCES play_neml.users(id), checksum varchar(64) NOT NULL,
 file_name varchar(150) NOT NULL, file_size bigint NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 expires_at timestamptz NOT NULL, confirmed_at timestamptz, status varchar(24) NOT NULL,
 payload text, imported_count integer NOT NULL DEFAULT 0, skipped_count integer NOT NULL DEFAULT 0,
 invalid_count integer NOT NULL DEFAULT 0, UNIQUE(championship_id,checksum)
);
