-- VERSIONED CHAMPIONSHIP AND LIMITED-OVERS CRICKET CONFIGURATION
-- Manual DBA migration. Do not run against a shared database from the application.
BEGIN;
SET LOCAL search_path TO play_neml, pg_catalog;
SET LOCAL lock_timeout TO '10s';
SET LOCAL statement_timeout TO '5min';

ALTER TABLE play_neml.competition_audit_events ADD COLUMN IF NOT EXISTS configuration_version integer;
ALTER TABLE play_neml.competition_audit_events ADD COLUMN IF NOT EXISTS entity_type varchar(32);
ALTER TABLE play_neml.competition_audit_events ADD COLUMN IF NOT EXISTS entity_id uuid;
ALTER TABLE play_neml.competition_audit_events ADD COLUMN IF NOT EXISTS reason varchar(500);
ALTER TABLE play_neml.competition_audit_events ADD COLUMN IF NOT EXISTS correlation_id uuid;
UPDATE play_neml.competition_audit_events SET entity_type=CASE WHEN fixture_id IS NULL THEN 'CHAMPIONSHIP' ELSE 'MATCH' END,entity_id=COALESCE(fixture_id,championship_id),correlation_id=gen_random_uuid() WHERE entity_type IS NULL OR entity_id IS NULL OR correlation_id IS NULL;
ALTER TABLE play_neml.competition_audit_events ALTER COLUMN entity_type SET NOT NULL,ALTER COLUMN entity_id SET NOT NULL,ALTER COLUMN correlation_id SET NOT NULL;

CREATE TABLE IF NOT EXISTS play_neml.configuration_section_states(
 id uuid PRIMARY KEY,row_version bigint NOT NULL DEFAULT 0,championship_id uuid NOT NULL,section_code varchar(40) NOT NULL,
 configuration_version integer NOT NULL DEFAULT 1,status varchar(16) NOT NULL DEFAULT 'DRAFT',locked_at timestamptz,
 last_changed_by uuid,validation_messages text NOT NULL DEFAULT '[]',updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_configuration_section UNIQUE(championship_id,section_code),
 CONSTRAINT fk_configuration_section_championship FOREIGN KEY(championship_id) REFERENCES play_neml.championships(id),
 CONSTRAINT fk_configuration_section_actor FOREIGN KEY(last_changed_by) REFERENCES play_neml.users(id)
);
INSERT INTO play_neml.configuration_section_states(id,championship_id,section_code)
SELECT gen_random_uuid(),c.id,v.code FROM play_neml.championships c CROSS JOIN(VALUES('BASICS'),('BRANDING'),('TEAMS_AND_ROSTERS'),('AUCTION'),('SPORT_RULES'),('COMPETITION_FORMAT'),('SCHEDULING'),('QUALIFICATION_AND_PLAYOFFS'),('PERMISSIONS_AND_VISIBILITY'),('REVIEW_AND_LAUNCH'))v(code)
WHERE NOT EXISTS(SELECT 1 FROM play_neml.configuration_section_states s WHERE s.championship_id=c.id AND s.section_code=v.code);
CREATE INDEX IF NOT EXISTS idx_configuration_section_status ON play_neml.configuration_section_states(championship_id,status);

CREATE TABLE IF NOT EXISTS play_neml.championship_branding_configurations(
 id uuid PRIMARY KEY,row_version bigint NOT NULL DEFAULT 0,championship_id uuid NOT NULL,configuration_version integer NOT NULL,status varchar(16) NOT NULL DEFAULT 'DRAFT',
 display_name varchar(150) NOT NULL,short_name varchar(32) NOT NULL,logo_reference varchar(500),primary_color varchar(16) NOT NULL DEFAULT '#7CFF6B',secondary_color varchar(16) NOT NULL DEFAULT '#111827',
 sponsor_name varchar(150),sponsor_logo_reference varchar(500),public_description text,contact_information varchar(500),public_visibility boolean NOT NULL DEFAULT false,created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_branding_config_version UNIQUE(championship_id,configuration_version),CONSTRAINT fk_branding_config_championship FOREIGN KEY(championship_id) REFERENCES play_neml.championships(id),
 CONSTRAINT ck_branding_colors CHECK(primary_color ~ '^#[0-9A-Fa-f]{6}$' AND secondary_color ~ '^#[0-9A-Fa-f]{6}$')
);
INSERT INTO play_neml.championship_branding_configurations(id,championship_id,configuration_version,display_name,short_name,public_visibility)
SELECT gen_random_uuid(),c.id,1,c.name,left(c.name,32),c.is_public FROM play_neml.championships c WHERE NOT EXISTS(SELECT 1 FROM play_neml.championship_branding_configurations b WHERE b.championship_id=c.id);
CREATE INDEX IF NOT EXISTS idx_branding_config_latest ON play_neml.championship_branding_configurations(championship_id,configuration_version DESC);

CREATE TABLE IF NOT EXISTS play_neml.auction_configurations(
 id uuid PRIMARY KEY,row_version bigint NOT NULL DEFAULT 0,championship_id uuid NOT NULL,configuration_version integer NOT NULL,status varchar(16) NOT NULL DEFAULT 'DRAFT',enabled boolean NOT NULL DEFAULT true,auction_type varchar(32) NOT NULL DEFAULT 'OPEN_ASCENDING',
 team_budget numeric(19,2) NOT NULL,minimum_bid numeric(19,2) NOT NULL,bid_increment numeric(19,2) NOT NULL,minimum_roster_size integer NOT NULL,maximum_roster_size integer NOT NULL,
 retained_players_allowed boolean NOT NULL DEFAULT false,maximum_retained_players integer NOT NULL DEFAULT 0,unsold_player_reauction_allowed boolean NOT NULL DEFAULT true,purse_carry_forward_policy varchar(32) NOT NULL DEFAULT 'NONE',auction_order_policy varchar(32) NOT NULL DEFAULT 'REGISTRATION_ORDER',timer_seconds integer NOT NULL DEFAULT 30,created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_auction_config_version UNIQUE(championship_id,configuration_version),CONSTRAINT fk_auction_config_championship FOREIGN KEY(championship_id) REFERENCES play_neml.championships(id),
 CONSTRAINT ck_auction_config_money CHECK(team_budget>0 AND minimum_bid>0 AND bid_increment>0),CONSTRAINT ck_auction_config_roster CHECK(minimum_roster_size>0 AND minimum_roster_size<=maximum_roster_size AND maximum_retained_players BETWEEN 0 AND maximum_roster_size),CONSTRAINT ck_auction_config_timer CHECK(timer_seconds BETWEEN 5 AND 600)
);
INSERT INTO play_neml.auction_configurations(id,championship_id,configuration_version,team_budget,minimum_bid,bid_increment,minimum_roster_size,maximum_roster_size,timer_seconds)
SELECT gen_random_uuid(),c.id,1,s.purse_limit,s.player_base_price,s.bid_increment,LEAST(s.max_squad_size,CASE WHEN upper(c.sport_type)='CRICKET' THEN 14 ELSE 8 END),s.max_squad_size,s.timer_seconds FROM play_neml.championships c JOIN play_neml.tournament_settings s ON s.championship_id=c.id WHERE NOT EXISTS(SELECT 1 FROM play_neml.auction_configurations a WHERE a.championship_id=c.id);
CREATE INDEX IF NOT EXISTS idx_auction_config_latest ON play_neml.auction_configurations(championship_id,configuration_version DESC);

ALTER TABLE play_neml.competition_configurations
  ADD COLUMN IF NOT EXISTS configuration_version integer NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS configuration_status varchar(16) NOT NULL DEFAULT 'DRAFT',
  ADD COLUMN IF NOT EXISTS configured_team_count integer NOT NULL DEFAULT 4,
  ADD COLUMN IF NOT EXISTS minimum_team_count integer NOT NULL DEFAULT 2,
  ADD COLUMN IF NOT EXISTS maximum_team_count integer NOT NULL DEFAULT 4,
  ADD COLUMN IF NOT EXISTS minimum_roster_size integer NOT NULL DEFAULT 8,
  ADD COLUMN IF NOT EXISTS maximum_roster_size integer NOT NULL DEFAULT 12,
  ADD COLUMN IF NOT EXISTS playing_side_size integer NOT NULL DEFAULT 7,
  ADD COLUMN IF NOT EXISTS maximum_substitutes integer NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS number_of_legs integer NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS playoff_format varchar(48) NOT NULL DEFAULT 'SEMI_FINALS_AND_FINAL',
  ADD COLUMN IF NOT EXISTS points_per_tie integer NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS points_per_no_result integer NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS timezone varchar(64) NOT NULL DEFAULT 'UTC',
  ADD COLUMN IF NOT EXISTS preset_code varchar(64),
  ADD COLUMN IF NOT EXISTS locked_at timestamptz,
  ADD COLUMN IF NOT EXISTS allowed_weekdays varchar(100) NOT NULL DEFAULT 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY',
  ADD COLUMN IF NOT EXISTS daily_start_time time NOT NULL DEFAULT '09:00',
  ADD COLUMN IF NOT EXISTS daily_end_time time NOT NULL DEFAULT '22:00',
  ADD COLUMN IF NOT EXISTS fixture_slot_minutes integer NOT NULL DEFAULT 120,
  ADD COLUMN IF NOT EXISTS maximum_fixtures_per_day integer NOT NULL DEFAULT 20,
  ADD COLUMN IF NOT EXISTS blackout_dates text NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS minimum_rest_hours integer NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS simultaneous_fixtures_allowed boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS team_may_play_twice_per_day boolean NOT NULL DEFAULT false;

UPDATE play_neml.competition_configurations c SET
  configured_team_count=GREATEST(2,(SELECT count(*) FROM play_neml.teams t WHERE t.championship_id=c.championship_id)),
  maximum_team_count=GREATEST(4,(SELECT count(*) FROM play_neml.teams t WHERE t.championship_id=c.championship_id)),
  maximum_roster_size=GREATEST(12,COALESCE((SELECT max(n) FROM (SELECT count(*) n FROM play_neml.players p WHERE p.championship_id=c.championship_id AND p.team_id IS NOT NULL GROUP BY p.team_id) x),0)),
  preset_code='CUSTOM_CHAMPIONSHIP'
WHERE configuration_version=1;

ALTER TABLE play_neml.competition_configurations DROP CONSTRAINT IF EXISTS ck_competition_round_robin;
ALTER TABLE play_neml.competition_configurations DROP CONSTRAINT IF EXISTS ck_competition_qualifiers;
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='play_neml.competition_configurations'::regclass AND conname='ck_competition_team_limits') THEN
  ALTER TABLE play_neml.competition_configurations ADD CONSTRAINT ck_competition_team_limits CHECK(minimum_team_count>=2 AND minimum_team_count<=configured_team_count AND configured_team_count<=maximum_team_count);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='play_neml.competition_configurations'::regclass AND conname='ck_competition_roster_limits') THEN
  ALTER TABLE play_neml.competition_configurations ADD CONSTRAINT ck_competition_roster_limits CHECK(minimum_roster_size>=1 AND minimum_roster_size<=maximum_roster_size AND playing_side_size BETWEEN 1 AND maximum_roster_size);
 END IF;
END $$;

CREATE TABLE IF NOT EXISTS play_neml.cricket_configurations(
 id uuid PRIMARY KEY,row_version bigint NOT NULL DEFAULT 0, championship_id uuid NOT NULL, configuration_version integer NOT NULL,
 status varchar(16) NOT NULL DEFAULT 'DRAFT', preset_code varchar(16) NOT NULL DEFAULT 'T20',
 players_per_side integer NOT NULL DEFAULT 11, overs_per_innings integer NOT NULL DEFAULT 20,
 maximum_overs_per_bowler integer NOT NULL DEFAULT 4, required_wicketkeepers integer NOT NULL DEFAULT 1,
 tie_policy varchar(32) NOT NULL DEFAULT 'SUPER_OVER', maximum_super_overs integer NOT NULL DEFAULT 1,
 powerplay_ranges varchar(100) NOT NULL DEFAULT '1-6',wide_ball_runs integer NOT NULL DEFAULT 1,no_ball_runs integer NOT NULL DEFAULT 1,free_hit_enabled boolean NOT NULL DEFAULT true,toss_required boolean NOT NULL DEFAULT true,
 abandonment_policy varchar(40) NOT NULL DEFAULT 'ADMINISTRATIVE_NO_RESULT', administrative_target_override_allowed boolean NOT NULL DEFAULT true,
 administrative_result_override_allowed boolean NOT NULL DEFAULT true, playing_xi_lock_policy varchar(40) NOT NULL DEFAULT 'LOCK_ON_SUBMISSION', net_run_rate_policy varchar(40) NOT NULL DEFAULT 'STANDARD',
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_cricket_config_championship_version UNIQUE(championship_id,configuration_version),
 CONSTRAINT fk_cricket_config_championship FOREIGN KEY(championship_id) REFERENCES play_neml.championships(id),
 CONSTRAINT ck_cricket_overs CHECK(overs_per_innings>0 AND maximum_overs_per_bowler>0 AND maximum_overs_per_bowler<=overs_per_innings),
 CONSTRAINT ck_cricket_side CHECK(players_per_side BETWEEN 2 AND 11)
);
ALTER TABLE play_neml.cricket_configurations ADD COLUMN IF NOT EXISTS abandonment_policy varchar(40) NOT NULL DEFAULT 'ADMINISTRATIVE_NO_RESULT';
ALTER TABLE play_neml.cricket_configurations ADD COLUMN IF NOT EXISTS administrative_target_override_allowed boolean NOT NULL DEFAULT true;
ALTER TABLE play_neml.cricket_configurations ADD COLUMN IF NOT EXISTS administrative_result_override_allowed boolean NOT NULL DEFAULT true;
ALTER TABLE play_neml.cricket_configurations ADD COLUMN IF NOT EXISTS playing_xi_lock_policy varchar(40) NOT NULL DEFAULT 'LOCK_ON_SUBMISSION';
ALTER TABLE play_neml.cricket_configurations ADD COLUMN IF NOT EXISTS net_run_rate_policy varchar(40) NOT NULL DEFAULT 'STANDARD';
INSERT INTO play_neml.cricket_configurations(id,championship_id,configuration_version,status)
SELECT gen_random_uuid(),c.id,cc.configuration_version,'DRAFT' FROM play_neml.championships c
JOIN play_neml.competition_configurations cc ON cc.championship_id=c.id
WHERE upper(c.sport_type)='CRICKET' AND NOT EXISTS(SELECT 1 FROM play_neml.cricket_configurations x WHERE x.championship_id=c.id);

CREATE INDEX IF NOT EXISTS idx_competition_configuration_status ON play_neml.competition_configurations(championship_id,configuration_status);
CREATE INDEX IF NOT EXISTS idx_cricket_configuration_tenant ON play_neml.cricket_configurations(championship_id,configuration_version DESC);

CREATE TABLE IF NOT EXISTS play_neml.badminton_configurations(
 id uuid PRIMARY KEY,row_version bigint NOT NULL DEFAULT 0, championship_id uuid NOT NULL, configuration_version integer NOT NULL,
 status varchar(16) NOT NULL DEFAULT 'DRAFT', preset_code varchar(64) NOT NULL DEFAULT 'BADMINTON_STANDARD_FIVE',
 maximum_appearances_per_player integer NOT NULL DEFAULT 2,multiple_rubber_categories_allowed boolean NOT NULL DEFAULT false,lineup_changes_after_start_allowed boolean NOT NULL DEFAULT false,tie_handling varchar(32) NOT NULL DEFAULT 'LEAGUE_TIE',walkover_policy varchar(32) NOT NULL DEFAULT 'ADMINISTRATIVE',retirement_policy varchar(32) NOT NULL DEFAULT 'OPPONENT_WIN',lineup_visibility_policy varchar(32) NOT NULL DEFAULT 'AFTER_BOTH_SUBMIT', created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_badminton_config_version UNIQUE(championship_id,configuration_version),
 CONSTRAINT fk_badminton_config_championship FOREIGN KEY(championship_id) REFERENCES play_neml.championships(id),
 CONSTRAINT ck_badminton_appearances CHECK(maximum_appearances_per_player>0)
);
CREATE TABLE IF NOT EXISTS play_neml.badminton_rubber_rules(
 id uuid PRIMARY KEY, badminton_configuration_id uuid NOT NULL, rubber_code varchar(32) NOT NULL,
 display_label varchar(100) NOT NULL, short_label varchar(16) NOT NULL, display_order integer NOT NULL,
 participant_count_per_side integer NOT NULL, gender_rule varchar(32) NOT NULL, best_of_games integer NOT NULL DEFAULT 3,
 points_per_game integer NOT NULL DEFAULT 21, win_by integer NOT NULL DEFAULT 2, score_cap integer NOT NULL DEFAULT 30, enabled boolean NOT NULL DEFAULT true,
 CONSTRAINT uq_badminton_rubber_code UNIQUE(badminton_configuration_id,rubber_code),
 CONSTRAINT fk_badminton_rubber_config FOREIGN KEY(badminton_configuration_id) REFERENCES play_neml.badminton_configurations(id) ON DELETE CASCADE,
 CONSTRAINT ck_badminton_rubber_scoring CHECK(best_of_games>0 AND best_of_games%2=1 AND points_per_game>0 AND win_by>0 AND score_cap>=points_per_game)
);
INSERT INTO play_neml.badminton_configurations(id,championship_id,configuration_version,status)
SELECT gen_random_uuid(),c.id,cc.configuration_version,'DRAFT' FROM play_neml.championships c JOIN play_neml.competition_configurations cc ON cc.championship_id=c.id
WHERE upper(c.sport_type)='BADMINTON' AND NOT EXISTS(SELECT 1 FROM play_neml.badminton_configurations b WHERE b.championship_id=c.id);
INSERT INTO play_neml.badminton_rubber_rules(id,badminton_configuration_id,rubber_code,display_label,short_label,display_order,participant_count_per_side,gender_rule)
SELECT gen_random_uuid(),b.id,v.code,v.label,v.short_label,v.ord,v.players,v.gender_rule FROM play_neml.badminton_configurations b
CROSS JOIN (VALUES ('MENS_SINGLES','Men''s Singles','MS',1,1,'MALE'),('WOMENS_SINGLES','Women''s Singles','WS',2,1,'FEMALE'),('MENS_DOUBLES','Men''s Doubles','MD',3,2,'MALE'),('MIXED_DOUBLES','Mixed Doubles','XD',4,2,'MIXED'),('MENS_DOUBLES_TWO','Men''s Doubles II','MD2',5,2,'MALE')) v(code,label,short_label,ord,players,gender_rule)
WHERE NOT EXISTS(SELECT 1 FROM play_neml.badminton_rubber_rules r WHERE r.badminton_configuration_id=b.id AND r.rubber_code=v.code);
CREATE INDEX IF NOT EXISTS idx_badminton_config_tenant ON play_neml.badminton_configurations(championship_id,configuration_version DESC);
ALTER TABLE play_neml.match_formats ADD COLUMN IF NOT EXISTS display_label varchar(100),ADD COLUMN IF NOT EXISTS short_label varchar(16),ADD COLUMN IF NOT EXISTS players_per_side integer,ADD COLUMN IF NOT EXISTS gender_rule varchar(32);
CREATE TABLE IF NOT EXISTS play_neml.roster_quota_rules(
 id uuid PRIMARY KEY,row_version bigint NOT NULL DEFAULT 0,championship_id uuid NOT NULL,configuration_version integer NOT NULL DEFAULT 1,status varchar(16) NOT NULL DEFAULT 'VALID',sport_code varchar(32) NOT NULL,rule_code varchar(64) NOT NULL,
 category_type varchar(32) NOT NULL,category_value varchar(64) NOT NULL,minimum_count integer,maximum_count integer,
 applies_to varchar(32) NOT NULL,enabled boolean NOT NULL DEFAULT true,validation_message varchar(255),
 CONSTRAINT uq_roster_quota_rule UNIQUE(championship_id,configuration_version,rule_code),
 CONSTRAINT fk_roster_quota_championship FOREIGN KEY(championship_id) REFERENCES play_neml.championships(id),
 CONSTRAINT ck_roster_quota_counts CHECK((minimum_count IS NOT NULL OR maximum_count IS NOT NULL) AND COALESCE(minimum_count,0)>=0 AND COALESCE(maximum_count,minimum_count)>=COALESCE(minimum_count,0)),
 CONSTRAINT ck_roster_quota_target CHECK(applies_to IN('SQUAD','PLAYING_SIDE'))
);
ALTER TABLE play_neml.roster_quota_rules ADD COLUMN IF NOT EXISTS row_version bigint NOT NULL DEFAULT 0;
ALTER TABLE play_neml.roster_quota_rules ADD COLUMN IF NOT EXISTS configuration_version integer NOT NULL DEFAULT 1;
ALTER TABLE play_neml.roster_quota_rules ADD COLUMN IF NOT EXISTS status varchar(16) NOT NULL DEFAULT 'VALID';
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='play_neml.roster_quota_rules'::regclass AND conname='uq_roster_quota_rule') THEN ALTER TABLE play_neml.roster_quota_rules DROP CONSTRAINT uq_roster_quota_rule; END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='play_neml.roster_quota_rules'::regclass AND conname='uq_roster_quota_rule') THEN ALTER TABLE play_neml.roster_quota_rules ADD CONSTRAINT uq_roster_quota_rule UNIQUE(championship_id,configuration_version,rule_code); END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_roster_quota_version ON play_neml.roster_quota_rules(championship_id,configuration_version DESC,enabled);

CREATE TABLE IF NOT EXISTS play_neml.cricket_match_states(
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, championship_id uuid NOT NULL, match_id uuid NOT NULL,
 toss_winner_team_id uuid, toss_decision varchar(8), target_runs integer, result_text varchar(255),super_over_count integer NOT NULL DEFAULT 0, updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_cricket_state_match_tenant UNIQUE(match_id,championship_id),
 CONSTRAINT fk_cricket_state_match FOREIGN KEY(match_id,championship_id) REFERENCES play_neml.matches(id,championship_id),
 CONSTRAINT fk_cricket_state_toss_team FOREIGN KEY(toss_winner_team_id,championship_id) REFERENCES play_neml.teams(id,championship_id),
 CONSTRAINT ck_cricket_toss_decision CHECK(toss_decision IS NULL OR toss_decision IN('BAT','BOWL'))
);
CREATE TABLE IF NOT EXISTS play_neml.cricket_playing_members(
 id uuid PRIMARY KEY, championship_id uuid NOT NULL, match_id uuid NOT NULL, team_id uuid NOT NULL, player_id uuid NOT NULL,
 captain boolean NOT NULL, wicketkeeper boolean NOT NULL, locked_at timestamptz NOT NULL,
 CONSTRAINT uq_cricket_xi_player UNIQUE(championship_id,match_id,team_id,player_id),
 CONSTRAINT fk_cricket_xi_match FOREIGN KEY(match_id,championship_id) REFERENCES play_neml.matches(id,championship_id),
 CONSTRAINT fk_cricket_xi_team FOREIGN KEY(team_id,championship_id) REFERENCES play_neml.teams(id,championship_id),
 CONSTRAINT fk_cricket_xi_player FOREIGN KEY(player_id,championship_id) REFERENCES play_neml.players(id,championship_id)
);
CREATE TABLE IF NOT EXISTS play_neml.cricket_innings(
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, championship_id uuid NOT NULL, match_id uuid NOT NULL, batting_team_id uuid NOT NULL,
 innings_number integer NOT NULL, runs integer NOT NULL DEFAULT 0, wickets integer NOT NULL DEFAULT 0, legal_balls integer NOT NULL DEFAULT 0,
 status varchar(16) NOT NULL DEFAULT 'LIVE', started_at timestamptz NOT NULL, completed_at timestamptz,
 CONSTRAINT uq_cricket_innings_number UNIQUE(championship_id,match_id,innings_number),
 CONSTRAINT uq_cricket_innings_id_tenant UNIQUE(id,championship_id),
 CONSTRAINT fk_cricket_innings_match FOREIGN KEY(match_id,championship_id) REFERENCES play_neml.matches(id,championship_id),
 CONSTRAINT fk_cricket_innings_team FOREIGN KEY(batting_team_id,championship_id) REFERENCES play_neml.teams(id,championship_id),
 CONSTRAINT ck_cricket_innings_totals CHECK(runs>=0 AND wickets BETWEEN 0 AND 10 AND legal_balls>=0)
);
CREATE TABLE IF NOT EXISTS play_neml.cricket_deliveries(
 id uuid PRIMARY KEY, championship_id uuid NOT NULL, innings_id uuid NOT NULL, delivery_sequence integer NOT NULL,
 striker_player_id uuid NOT NULL,non_striker_player_id uuid NOT NULL,bowler_player_id uuid NOT NULL,wicket_player_id uuid,
 idempotency_key varchar(100) NOT NULL, batter_runs integer NOT NULL, wides integer NOT NULL, no_balls integer NOT NULL,
 byes integer NOT NULL, leg_byes integer NOT NULL, penalty_runs integer NOT NULL, wicket boolean NOT NULL,
 dismissal_type varchar(32), legal_ball boolean NOT NULL, total_runs integer NOT NULL, occurred_at timestamptz NOT NULL,
 CONSTRAINT uq_cricket_delivery_idempotency UNIQUE(championship_id,idempotency_key),
 CONSTRAINT uq_cricket_delivery_sequence UNIQUE(innings_id,delivery_sequence),
 CONSTRAINT fk_cricket_delivery_innings FOREIGN KEY(innings_id,championship_id) REFERENCES play_neml.cricket_innings(id,championship_id),
 CONSTRAINT fk_cricket_delivery_striker FOREIGN KEY(striker_player_id,championship_id) REFERENCES play_neml.players(id,championship_id),
 CONSTRAINT fk_cricket_delivery_non_striker FOREIGN KEY(non_striker_player_id,championship_id) REFERENCES play_neml.players(id,championship_id),
 CONSTRAINT fk_cricket_delivery_bowler FOREIGN KEY(bowler_player_id,championship_id) REFERENCES play_neml.players(id,championship_id),
 CONSTRAINT fk_cricket_delivery_wicket_player FOREIGN KEY(wicket_player_id,championship_id) REFERENCES play_neml.players(id,championship_id),
 CONSTRAINT ck_cricket_delivery_values CHECK(batter_runs BETWEEN 0 AND 6 AND wides>=0 AND no_balls>=0 AND byes>=0 AND leg_byes>=0 AND penalty_runs>=0 AND total_runs>=0)
);
CREATE INDEX IF NOT EXISTS idx_cricket_xi_match ON play_neml.cricket_playing_members(championship_id,match_id,team_id);
CREATE INDEX IF NOT EXISTS idx_cricket_innings_match ON play_neml.cricket_innings(championship_id,match_id,innings_number);
CREATE INDEX IF NOT EXISTS idx_cricket_delivery_ledger ON play_neml.cricket_deliveries(championship_id,innings_id,delivery_sequence);
ALTER TABLE play_neml.cricket_deliveries ADD COLUMN IF NOT EXISTS voided boolean NOT NULL DEFAULT false;
ALTER TABLE play_neml.cricket_deliveries ADD COLUMN IF NOT EXISTS correction_reason varchar(500);
ALTER TABLE play_neml.cricket_deliveries ADD COLUMN IF NOT EXISTS corrected_at timestamptz;
ALTER TABLE play_neml.cricket_deliveries ADD COLUMN IF NOT EXISTS corrected_by_user_id uuid;
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='play_neml.cricket_deliveries'::regclass AND conname='fk_cricket_delivery_corrected_by') THEN
  ALTER TABLE play_neml.cricket_deliveries ADD CONSTRAINT fk_cricket_delivery_corrected_by FOREIGN KEY(corrected_by_user_id) REFERENCES play_neml.users(id);
 END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_cricket_delivery_corrections ON play_neml.cricket_deliveries(championship_id,corrected_at) WHERE voided;
COMMIT;

-- Read-only verification: every value must be zero.
SELECT count(*) AS invalid_team_limits FROM play_neml.competition_configurations WHERE NOT(minimum_team_count>=2 AND minimum_team_count<=configured_team_count AND configured_team_count<=maximum_team_count);
SELECT count(*) AS invalid_roster_limits FROM play_neml.competition_configurations WHERE NOT(minimum_roster_size>=1 AND minimum_roster_size<=maximum_roster_size AND playing_side_size BETWEEN 1 AND maximum_roster_size);
SELECT count(*) AS cricket_tenant_mismatches FROM play_neml.cricket_configurations x JOIN play_neml.championships c ON c.id=x.championship_id WHERE upper(c.sport_type)<>'CRICKET';
SELECT count(*) AS badminton_tenant_mismatches FROM play_neml.badminton_configurations x JOIN play_neml.championships c ON c.id=x.championship_id WHERE upper(c.sport_type)<>'BADMINTON';
SELECT count(*) AS match_format_tenant_mismatches FROM play_neml.match_formats f JOIN play_neml.matches m ON m.id=f.match_id WHERE m.championship_id<>f.championship_id;
SELECT count(*) AS qualification_team_tenant_mismatches FROM play_neml.competition_qualifications q JOIN play_neml.teams t ON t.id=q.team_id WHERE t.championship_id<>q.championship_id;
SELECT count(*) AS audit_fixture_tenant_mismatches FROM play_neml.competition_audit_events a JOIN play_neml.matches m ON m.id=a.fixture_id WHERE a.fixture_id IS NOT NULL AND m.championship_id<>a.championship_id;
SELECT count(*) AS cricket_match_tenant_mismatches FROM play_neml.cricket_match_states s JOIN play_neml.matches m ON m.id=s.match_id WHERE m.championship_id<>s.championship_id;
SELECT count(*) AS cricket_xi_tenant_mismatches FROM play_neml.cricket_playing_members x JOIN play_neml.matches m ON m.id=x.match_id JOIN play_neml.teams t ON t.id=x.team_id JOIN play_neml.players p ON p.id=x.player_id WHERE m.championship_id<>x.championship_id OR t.championship_id<>x.championship_id OR p.championship_id<>x.championship_id;
SELECT count(*) AS cricket_innings_tenant_mismatches FROM play_neml.cricket_innings i JOIN play_neml.matches m ON m.id=i.match_id JOIN play_neml.teams t ON t.id=i.batting_team_id WHERE m.championship_id<>i.championship_id OR t.championship_id<>i.championship_id;
SELECT count(*) AS cricket_delivery_tenant_mismatches FROM play_neml.cricket_deliveries d JOIN play_neml.cricket_innings i ON i.id=d.innings_id WHERE i.championship_id<>d.championship_id;
SELECT count(*) AS cricket_delivery_player_tenant_mismatches FROM play_neml.cricket_deliveries d JOIN play_neml.players s ON s.id=d.striker_player_id JOIN play_neml.players n ON n.id=d.non_striker_player_id JOIN play_neml.players b ON b.id=d.bowler_player_id LEFT JOIN play_neml.players w ON w.id=d.wicket_player_id WHERE s.championship_id<>d.championship_id OR n.championship_id<>d.championship_id OR b.championship_id<>d.championship_id OR (w.id IS NOT NULL AND w.championship_id<>d.championship_id);
