ALTER TABLE play_neml.players ADD COLUMN photo_key uuid,
 ADD COLUMN photo_size integer, ADD COLUMN photo_updated_at timestamptz,
 ADD COLUMN photo_updated_by uuid REFERENCES play_neml.users(id);
ALTER TABLE play_neml.players ADD CONSTRAINT ck_player_photo_metadata CHECK (
 (photo_key IS NULL AND photo_size IS NULL) OR
 (photo_key IS NOT NULL AND photo_size > 0 AND photo_updated_at IS NOT NULL AND photo_updated_by IS NOT NULL)
);
