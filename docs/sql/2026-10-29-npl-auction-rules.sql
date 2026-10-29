-- Additive upgrade. Historical purchases and configuration rows are preserved.
ALTER TABLE play_neml.auction_configurations
 ADD COLUMN captain_preassignment_required boolean NOT NULL DEFAULT false,
 ADD COLUMN maximum_bid numeric(19,2),
 ADD COLUMN maximum_premium_purchases integer,
 ADD CONSTRAINT auction_bid_ceiling_valid CHECK (maximum_bid IS NULL OR maximum_bid >= minimum_bid),
 ADD CONSTRAINT auction_premium_limit_valid CHECK (maximum_premium_purchases IS NULL OR (maximum_premium_purchases >= 0 AND maximum_bid IS NOT NULL));

ALTER TABLE play_neml.players ADD COLUMN captain_user_id uuid;
ALTER TABLE play_neml.players DROP CONSTRAINT players_status_check;
ALTER TABLE play_neml.players ADD CONSTRAINT players_status_check
 CHECK (status IN ('AVAILABLE','ON_BLOCK','SOLD','UNSOLD','PREASSIGNED'));
ALTER TABLE play_neml.players ADD CONSTRAINT preassigned_captain_integrity CHECK (
 (captain_user_id IS NULL AND status <> 'PREASSIGNED') OR
 (captain_user_id IS NOT NULL AND status = 'PREASSIGNED' AND team_id IS NOT NULL AND auction_id IS NULL AND sold_price IS NULL));
CREATE UNIQUE INDEX uq_captain_player_identity ON play_neml.players(championship_id,captain_user_id) WHERE captain_user_id IS NOT NULL;
CREATE UNIQUE INDEX uq_team_preassigned_captain ON play_neml.players(championship_id,team_id) WHERE status='PREASSIGNED';
ALTER TABLE play_neml.championship_roles ADD CONSTRAINT uq_captain_membership_team UNIQUE(user_id,championship_id,team_id);
ALTER TABLE play_neml.players ADD CONSTRAINT fk_captain_membership
 FOREIGN KEY(captain_user_id,championship_id,team_id) REFERENCES play_neml.championship_roles(user_id,championship_id,team_id);
