--
-- PostgreSQL database dump
--

-- Sports-Play complete schema bootstrap for PostgreSQL 15+.
-- Run this once against an empty database. It creates only the play_neml schema.
-- The application remains configured with spring.jpa.hibernate.ddl-auto=validate.

-- Dumped from database version 17.6
-- Dumped by pg_dump version 17.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: play_neml; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA play_neml;


--
-- Name: enforce_format_player_tenant(); Type: FUNCTION; Schema: play_neml; Owner: -
--

CREATE FUNCTION play_neml.enforce_format_player_tenant() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'play_neml', 'pg_catalog'
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


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: announcements; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.announcements (
    created_at timestamp(6) with time zone NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    severity character varying(32) NOT NULL,
    title character varying(200) NOT NULL,
    body text NOT NULL,
    created_by_name character varying(255)
);


--
-- Name: app_screens; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.app_screens (
    active boolean NOT NULL,
    display_order integer NOT NULL,
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    icon character varying(64) NOT NULL,
    section_name character varying(64) NOT NULL,
    label character varying(80) NOT NULL,
    path character varying(160) NOT NULL
);


--
-- Name: auction_configurations; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.auction_configurations (
    bid_increment numeric(19,2) NOT NULL,
    configuration_version integer NOT NULL,
    enabled boolean NOT NULL,
    maximum_retained_players integer NOT NULL,
    maximum_roster_size integer NOT NULL,
    minimum_bid numeric(19,2) NOT NULL,
    minimum_roster_size integer NOT NULL,
    retained_players_allowed boolean NOT NULL,
    team_budget numeric(19,2) NOT NULL,
    timer_seconds integer NOT NULL,
    unsold_player_reauction_allowed boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    row_version bigint NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    status character varying(16) NOT NULL,
    auction_order_policy character varying(32) NOT NULL,
    auction_type character varying(32) NOT NULL,
    purse_carry_forward_policy character varying(32) NOT NULL,
    CONSTRAINT auction_configurations_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALID'::character varying, 'LOCKED'::character varying, 'SUPERSEDED'::character varying])::text[])))
);


--
-- Name: auction_state; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.auction_state (
    timer_seconds integer,
    bid_deadline timestamp(6) with time zone,
    version bigint,
    auction_id uuid NOT NULL,
    championship_id uuid NOT NULL,
    current_player_id uuid,
    id uuid NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT auction_state_status_check CHECK (((status)::text = ANY ((ARRAY['NOT_STARTED'::character varying, 'RUNNING'::character varying, 'PAUSED'::character varying, 'COMPLETED'::character varying])::text[])))
);


--
-- Name: auctions; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.auctions (
    created_at timestamp(6) with time zone NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    name character varying(150) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT auctions_status_check CHECK (((status)::text = ANY ((ARRAY['NOT_STARTED'::character varying, 'RUNNING'::character varying, 'PAUSED'::character varying, 'COMPLETED'::character varying])::text[])))
);


--
-- Name: badminton_configurations; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.badminton_configurations (
    configuration_version integer NOT NULL,
    lineup_changes_after_start_allowed boolean NOT NULL,
    maximum_appearances_per_player integer NOT NULL,
    multiple_rubber_categories_allowed boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    row_version bigint NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    status character varying(16) NOT NULL,
    lineup_visibility_policy character varying(32) NOT NULL,
    retirement_policy character varying(32) NOT NULL,
    tie_handling character varying(32) NOT NULL,
    walkover_policy character varying(32) NOT NULL,
    preset_code character varying(64) NOT NULL,
    CONSTRAINT badminton_configurations_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALID'::character varying, 'LOCKED'::character varying, 'SUPERSEDED'::character varying])::text[])))
);


--
-- Name: badminton_rubber_rules; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.badminton_rubber_rules (
    best_of_games integer NOT NULL,
    display_order integer NOT NULL,
    enabled boolean NOT NULL,
    participant_count_per_side integer NOT NULL,
    points_per_game integer NOT NULL,
    score_cap integer NOT NULL,
    win_by integer NOT NULL,
    badminton_configuration_id uuid NOT NULL,
    id uuid NOT NULL,
    short_label character varying(16) NOT NULL,
    gender_rule character varying(32) NOT NULL,
    rubber_code character varying(32) NOT NULL,
    display_label character varying(100) NOT NULL
);


--
-- Name: bids; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.bids (
    active boolean NOT NULL,
    amount numeric(19,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    sequence_no bigint NOT NULL,
    auction_id uuid NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    player_id uuid NOT NULL,
    team_id uuid NOT NULL
);


--
-- Name: championship_branding_configurations; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.championship_branding_configurations (
    configuration_version integer NOT NULL,
    public_visibility boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    row_version bigint NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    primary_color character varying(16) NOT NULL,
    secondary_color character varying(16) NOT NULL,
    status character varying(16) NOT NULL,
    short_name character varying(32) NOT NULL,
    display_name character varying(150) NOT NULL,
    sponsor_name character varying(150),
    contact_information character varying(500),
    logo_reference character varying(500),
    sponsor_logo_reference character varying(500),
    public_description text,
    CONSTRAINT championship_branding_configurations_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALID'::character varying, 'LOCKED'::character varying, 'SUPERSEDED'::character varying])::text[])))
);


--
-- Name: championship_roles; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.championship_roles (
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    team_id uuid,
    user_id uuid NOT NULL,
    role character varying(32) NOT NULL,
    CONSTRAINT championship_roles_role_check CHECK (((role)::text = ANY ((ARRAY['CHAMPIONSHIP_ADMIN'::character varying, 'TEAM_CAPTAIN'::character varying, 'SPECTATOR'::character varying])::text[])))
);


--
-- Name: championships; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.championships (
    is_public boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid NOT NULL,
    status character varying(24) NOT NULL,
    lifecycle_phase character varying(32) DEFAULT 'CREATION'::character varying NOT NULL,
    room_code character varying(32) NOT NULL,
    sport_type character varying(64) NOT NULL,
    name character varying(150) NOT NULL,
    passcode_hash character varying(255),
    CONSTRAINT championships_lifecycle_phase_check CHECK (((lifecycle_phase)::text = ANY ((ARRAY['CREATION'::character varying, 'CONFIGURATION'::character varying, 'TEAM_PROVISIONING'::character varying, 'PLAYER_REGISTRATION'::character varying, 'AUCTION'::character varying, 'SQUAD_CONFIRMATION'::character varying, 'LEAGUE_STAGE'::character varying, 'PLAYOFFS'::character varying, 'FINAL'::character varying, 'COMPLETED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT championships_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'COMPLETED'::character varying, 'ARCHIVED'::character varying])::text[])))
);


--
-- Name: competition_audit_events; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.competition_audit_events (
    configuration_version integer,
    revision integer NOT NULL,
    occurred_at timestamp(6) with time zone NOT NULL,
    actor_user_id uuid,
    championship_id uuid NOT NULL,
    correlation_id uuid NOT NULL,
    entity_id uuid NOT NULL,
    fixture_championship_id uuid,
    fixture_id uuid,
    id uuid NOT NULL,
    entity_type character varying(32) NOT NULL,
    event_type character varying(64) NOT NULL,
    reason character varying(500),
    payload text NOT NULL
);


--
-- Name: competition_configurations; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.competition_configurations (
    configuration_version integer NOT NULL,
    configured_team_count integer NOT NULL,
    daily_end_time time(6) without time zone NOT NULL,
    daily_start_time time(6) without time zone NOT NULL,
    fixture_slot_minutes integer NOT NULL,
    fixture_spacing_days integer NOT NULL,
    manual_date_editing boolean NOT NULL,
    maximum_fixtures_per_day integer NOT NULL,
    maximum_roster_size integer NOT NULL,
    maximum_substitutes integer NOT NULL,
    maximum_team_count integer NOT NULL,
    minimum_rest_hours integer NOT NULL,
    minimum_roster_size integer NOT NULL,
    minimum_team_count integer NOT NULL,
    number_of_legs integer NOT NULL,
    playing_side_size integer NOT NULL,
    points_per_loss integer NOT NULL,
    points_per_no_result integer NOT NULL,
    points_per_tie integer NOT NULL,
    points_per_win integer NOT NULL,
    qualifying_team_count integer NOT NULL,
    result_schema_version integer NOT NULL,
    simultaneous_fixtures_allowed boolean NOT NULL,
    team_may_play_twice_per_day boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    league_start_at timestamp(6) with time zone,
    locked_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint,
    champion_team_id uuid,
    championship_id uuid NOT NULL,
    configuration_status character varying(16) NOT NULL,
    id uuid NOT NULL,
    round_robin_mode character varying(16) NOT NULL,
    runner_up_team_id uuid,
    playoff_format character varying(48) NOT NULL,
    preset_code character varying(64),
    sport_code character varying(64) NOT NULL,
    timezone character varying(64) NOT NULL,
    allowed_weekdays character varying(100) NOT NULL,
    tie_breaker_priority character varying(500) NOT NULL,
    blackout_dates text NOT NULL,
    venues text NOT NULL,
    CONSTRAINT ck_competition_roster_limits CHECK (((minimum_roster_size >= 1) AND (minimum_roster_size <= maximum_roster_size) AND ((playing_side_size >= 1) AND (playing_side_size <= maximum_roster_size)))),
    CONSTRAINT ck_competition_team_limits CHECK (((minimum_team_count >= 2) AND (minimum_team_count <= configured_team_count) AND (configured_team_count <= maximum_team_count))),
    CONSTRAINT competition_configurations_configuration_status_check CHECK (((configuration_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALID'::character varying, 'LOCKED'::character varying, 'SUPERSEDED'::character varying])::text[]))),
    CONSTRAINT competition_configurations_playoff_format_check CHECK (((playoff_format)::text = ANY ((ARRAY['NONE'::character varying, 'TOP_TWO_FINAL'::character varying, 'SEMI_FINALS_AND_FINAL'::character varying, 'SEMI_FINALS_THIRD_PLACE_AND_FINAL'::character varying])::text[]))),
    CONSTRAINT competition_configurations_round_robin_mode_check CHECK (((round_robin_mode)::text = ANY ((ARRAY['SINGLE'::character varying, 'DOUBLE'::character varying, 'CUSTOM'::character varying])::text[])))
);


--
-- Name: competition_qualifications; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.competition_qualifications (
    seed integer NOT NULL,
    qualified_at timestamp(6) with time zone NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    team_id uuid NOT NULL,
    source_stage character varying(24) NOT NULL,
    CONSTRAINT competition_qualifications_source_stage_check CHECK (((source_stage)::text = ANY ((ARRAY['LEAGUE'::character varying, 'SEMI_FINAL'::character varying, 'THIRD_PLACE'::character varying, 'FINAL'::character varying])::text[])))
);


--
-- Name: competition_stages; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.competition_stages (
    stage_order integer NOT NULL,
    completed_at timestamp(6) with time zone,
    frozen_at timestamp(6) with time zone,
    started_at timestamp(6) with time zone,
    version bigint,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    stage_type character varying(24) NOT NULL,
    status character varying(24) NOT NULL,
    stage_metadata text NOT NULL,
    standings_snapshot text,
    CONSTRAINT competition_stages_stage_type_check CHECK (((stage_type)::text = ANY ((ARRAY['LEAGUE'::character varying, 'SEMI_FINAL'::character varying, 'THIRD_PLACE'::character varying, 'FINAL'::character varying])::text[]))),
    CONSTRAINT competition_stages_status_check CHECK (((status)::text = ANY ((ARRAY['NOT_STARTED'::character varying, 'ACTIVE'::character varying, 'COMPLETED'::character varying, 'FROZEN'::character varying])::text[])))
);


--
-- Name: configuration_section_states; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.configuration_section_states (
    configuration_version integer NOT NULL,
    locked_at timestamp(6) with time zone,
    row_version bigint,
    updated_at timestamp(6) with time zone NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    last_changed_by uuid,
    status character varying(16) NOT NULL,
    section_code character varying(40) NOT NULL,
    validation_messages text NOT NULL,
    CONSTRAINT configuration_section_states_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALID'::character varying, 'LOCKED'::character varying, 'SUPERSEDED'::character varying])::text[])))
);


--
-- Name: cricket_configurations; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.cricket_configurations (
    administrative_result_override_allowed boolean NOT NULL,
    administrative_target_override_allowed boolean NOT NULL,
    configuration_version integer NOT NULL,
    free_hit_enabled boolean NOT NULL,
    maximum_overs_per_bowler integer NOT NULL,
    maximum_super_overs integer NOT NULL,
    no_ball_runs integer NOT NULL,
    overs_per_innings integer NOT NULL,
    players_per_side integer NOT NULL,
    required_wicketkeepers integer NOT NULL,
    toss_required boolean NOT NULL,
    wide_ball_runs integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    row_version bigint NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    preset_code character varying(16) NOT NULL,
    status character varying(16) NOT NULL,
    tie_policy character varying(32) NOT NULL,
    abandonment_policy character varying(40) NOT NULL,
    net_run_rate_policy character varying(40) NOT NULL,
    playing_xi_lock_policy character varying(40) NOT NULL,
    powerplay_ranges character varying(100) NOT NULL,
    CONSTRAINT cricket_configurations_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALID'::character varying, 'LOCKED'::character varying, 'SUPERSEDED'::character varying])::text[])))
);


--
-- Name: cricket_deliveries; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.cricket_deliveries (
    batter_runs integer NOT NULL,
    byes integer NOT NULL,
    delivery_sequence integer NOT NULL,
    leg_byes integer NOT NULL,
    legal_ball boolean NOT NULL,
    no_balls integer NOT NULL,
    penalty_runs integer NOT NULL,
    total_runs integer NOT NULL,
    voided boolean NOT NULL,
    wicket boolean NOT NULL,
    wides integer NOT NULL,
    corrected_at timestamp(6) with time zone,
    occurred_at timestamp(6) with time zone NOT NULL,
    bowler_player_id uuid NOT NULL,
    championship_id uuid NOT NULL,
    corrected_by_user_id uuid,
    id uuid NOT NULL,
    innings_id uuid NOT NULL,
    non_striker_player_id uuid NOT NULL,
    striker_player_id uuid NOT NULL,
    wicket_player_id uuid,
    dismissal_type character varying(32),
    idempotency_key character varying(100) NOT NULL,
    correction_reason character varying(500)
);


--
-- Name: cricket_innings; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.cricket_innings (
    innings_number integer NOT NULL,
    legal_balls integer NOT NULL,
    runs integer NOT NULL,
    wickets integer NOT NULL,
    completed_at timestamp(6) with time zone,
    started_at timestamp(6) with time zone NOT NULL,
    version bigint,
    batting_team_id uuid NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    match_id uuid NOT NULL,
    status character varying(16) NOT NULL,
    CONSTRAINT cricket_innings_status_check CHECK (((status)::text = ANY ((ARRAY['NOT_STARTED'::character varying, 'LIVE'::character varying, 'COMPLETED'::character varying])::text[])))
);


--
-- Name: cricket_match_states; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.cricket_match_states (
    super_over_count integer NOT NULL,
    target_runs integer,
    toss_decision character varying(8),
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    match_id uuid NOT NULL,
    toss_winner_team_id uuid,
    result_text character varying(255)
);


--
-- Name: cricket_playing_members; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.cricket_playing_members (
    captain boolean NOT NULL,
    wicketkeeper boolean NOT NULL,
    locked_at timestamp(6) with time zone NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    match_id uuid NOT NULL,
    player_id uuid NOT NULL,
    team_id uuid NOT NULL
);


--
-- Name: format_side_a_players; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.format_side_a_players (
    format_id uuid NOT NULL,
    player_id uuid NOT NULL,
    championship_id uuid NOT NULL,
    player_championship_id uuid NOT NULL,
    CONSTRAINT ck_format_side_a_same_tenant CHECK ((championship_id = player_championship_id))
);


--
-- Name: format_side_b_players; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.format_side_b_players (
    format_id uuid NOT NULL,
    player_id uuid NOT NULL,
    championship_id uuid NOT NULL,
    player_championship_id uuid NOT NULL,
    CONSTRAINT ck_format_side_b_same_tenant CHECK ((championship_id = player_championship_id))
);


--
-- Name: match_formats; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.match_formats (
    completed boolean NOT NULL,
    format_order integer NOT NULL,
    players_per_side integer,
    score_a integer NOT NULL,
    score_b integer NOT NULL,
    version bigint,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    match_id uuid NOT NULL,
    short_label character varying(16),
    winner_team_id uuid,
    gender_rule character varying(32),
    display_label character varying(100),
    format_type character varying(255) NOT NULL,
    CONSTRAINT match_formats_format_type_check CHECK (((format_type)::text = ANY ((ARRAY['MENS_SINGLES'::character varying, 'WOMENS_SINGLES'::character varying, 'MENS_DOUBLES'::character varying, 'MIXED_DOUBLES'::character varying, 'MENS_DOUBLES_TWO'::character varying])::text[])))
);


--
-- Name: matches; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.matches (
    leg_number integer NOT NULL,
    match_number integer NOT NULL,
    result_revision integer NOT NULL,
    round_number integer NOT NULL,
    seed_a integer,
    seed_b integer,
    team_a_format_wins integer NOT NULL,
    team_a_points integer NOT NULL,
    team_b_format_wins integer NOT NULL,
    team_b_points integer NOT NULL,
    scheduled_at timestamp(6) with time zone NOT NULL,
    version bigint,
    championship_id uuid NOT NULL,
    competition_stage_id uuid,
    id uuid NOT NULL,
    stage_championship_id uuid,
    team_a_id uuid NOT NULL,
    team_b_id uuid NOT NULL,
    winner_team_id uuid,
    stage_type character varying(24) NOT NULL,
    fixture_key character varying(180) NOT NULL,
    score_data text,
    status character varying(255) NOT NULL,
    venue character varying(255),
    CONSTRAINT matches_stage_type_check CHECK (((stage_type)::text = ANY ((ARRAY['LEAGUE'::character varying, 'SEMI_FINAL'::character varying, 'THIRD_PLACE'::character varying, 'FINAL'::character varying])::text[]))),
    CONSTRAINT matches_status_check CHECK (((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'LIVE'::character varying, 'COMPLETED'::character varying])::text[])))
);


--
-- Name: message_tracker; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.message_tracker (
    msg_id integer NOT NULL,
    msg_transcode character varying(10),
    msg_message_type character varying(1),
    msg_parameters character varying(5000),
    msg_mobile_nos character varying(255),
    msg_recipients_to character varying(500),
    msg_recipients_cc character varying(500),
    msg_recipients_bcc character varying(255),
    msg_subject character varying(255),
    msg_status character varying(20),
    msg_created_on timestamp without time zone NOT NULL,
    msg_has_attachment double precision DEFAULT 0,
    msg_attachment_location character varying(255),
    msg_attachment_filename character varying(255),
    msg_owner character varying(15),
    msg_modified_on timestamp without time zone,
    msg_remarks character varying(500),
    msg_request_id character varying(100),
    msg_app_id character varying(150),
    msg_is_attachment character varying(1),
    msg_incorrect_num character varying(255),
    msg_http_response_status character varying(20),
    msg_error_response_message character varying(200),
    msg_error_response_code character varying(100),
    msg_error_response_layer character varying(50),
    msg_vendor_status character varying(4),
    msg_vendor_remarks character varying(200),
    msg_incorrect_mobile_nos character varying(255),
    msg_message_id character varying(100),
    temp_id character varying(10),
    msg_batch_id numeric,
    app_id character varying(25),
    db_type character varying(50),
    msg_api_id character varying(200),
    msg_delivered_on timestamp without time zone
);


--
-- Name: message_tracker_msg_id_seq; Type: SEQUENCE; Schema: play_neml; Owner: -
--

CREATE SEQUENCE play_neml.message_tracker_msg_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: message_tracker_msg_id_seq; Type: SEQUENCE OWNED BY; Schema: play_neml; Owner: -
--

ALTER SEQUENCE play_neml.message_tracker_msg_id_seq OWNED BY play_neml.message_tracker.msg_id;


--
-- Name: players; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.players (
    auction_order integer,
    base_price numeric(19,2) NOT NULL,
    sold_price numeric(19,2),
    auction_id uuid,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    team_id uuid,
    full_name character varying(255) NOT NULL,
    gender character varying(255) NOT NULL,
    skill_level character varying(255),
    status character varying(255) NOT NULL,
    CONSTRAINT players_gender_check CHECK (((gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying])::text[]))),
    CONSTRAINT players_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'ON_BLOCK'::character varying, 'SOLD'::character varying, 'UNSOLD'::character varying])::text[])))
);


--
-- Name: role_screen_mappings; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.role_screen_mappings (
    visible boolean NOT NULL,
    id uuid NOT NULL,
    screen_id uuid NOT NULL,
    role_key character varying(40) NOT NULL,
    CONSTRAINT role_screen_mappings_role_key_check CHECK (((role_key)::text = ANY ((ARRAY['SUPER_ADMIN'::character varying, 'CHAMPIONSHIP_ADMIN'::character varying, 'TEAM_CAPTAIN'::character varying, 'SPECTATOR'::character varying])::text[])))
);


--
-- Name: roster_quota_rules; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.roster_quota_rules (
    configuration_version integer NOT NULL,
    enabled boolean NOT NULL,
    maximum_count integer,
    minimum_count integer,
    row_version bigint NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    status character varying(16) NOT NULL,
    applies_to character varying(32) NOT NULL,
    category_type character varying(32) NOT NULL,
    sport_code character varying(32) NOT NULL,
    category_value character varying(64) NOT NULL,
    rule_code character varying(64) NOT NULL,
    validation_message character varying(255),
    CONSTRAINT roster_quota_rules_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALID'::character varying, 'LOCKED'::character varying, 'SUPERSEDED'::character varying])::text[])))
);


--
-- Name: seasons; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.seasons (
    active boolean NOT NULL,
    start_date timestamp(6) with time zone NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    description character varying(255),
    name character varying(255) NOT NULL
);


--
-- Name: squad_confirmation_events; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.squad_confirmation_events (
    occurred_at timestamp(6) with time zone NOT NULL,
    actor_user_id uuid NOT NULL,
    championship_id uuid NOT NULL,
    confirmation_id uuid NOT NULL,
    id uuid NOT NULL,
    team_id uuid NOT NULL,
    action character varying(20) NOT NULL,
    notes character varying(500),
    roster_snapshot text NOT NULL,
    CONSTRAINT squad_confirmation_events_action_check CHECK (((action)::text = ANY ((ARRAY['CONFIRMED'::character varying, 'LOCKED'::character varying, 'REOPENED'::character varying])::text[])))
);


--
-- Name: squad_confirmations; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.squad_confirmations (
    confirmed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    locked_at timestamp(6) with time zone,
    reopened_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint,
    championship_id uuid NOT NULL,
    confirmed_by uuid,
    id uuid NOT NULL,
    locked_by uuid,
    reopened_by uuid,
    team_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    notes character varying(500),
    CONSTRAINT squad_confirmations_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'CONFIRMED'::character varying, 'LOCKED'::character varying])::text[])))
);


--
-- Name: teams; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.teams (
    female_count integer NOT NULL,
    male_count integer NOT NULL,
    match_points integer DEFAULT 0 NOT NULL,
    purse_remaining numeric(19,2) NOT NULL,
    purse_total numeric(19,2) NOT NULL,
    version bigint,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    logo_url character varying(255),
    name character varying(255) NOT NULL,
    primary_color character varying(255),
    short_code character varying(255) NOT NULL
);


--
-- Name: tie_breaker_order; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.tie_breaker_order (
    "position" integer NOT NULL,
    settings_id uuid NOT NULL,
    tie_breaker character varying(32),
    CONSTRAINT tie_breaker_order_tie_breaker_check CHECK (((tie_breaker)::text = ANY ((ARRAY['HEAD_TO_HEAD'::character varying, 'FORMAT_WINS'::character varying, 'FORMAT_DIFF'::character varying, 'MATCHES_WON'::character varying, 'NAME'::character varying])::text[])))
);


--
-- Name: tournament_settings; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.tournament_settings (
    bid_increment numeric(19,2) DEFAULT 500000.00 NOT NULL,
    max_squad_size integer NOT NULL,
    min_female integer DEFAULT 3 NOT NULL,
    min_male integer DEFAULT 9 NOT NULL,
    penalty_per_unplayed integer NOT NULL,
    player_base_price numeric(19,2) DEFAULT 2000000.00 NOT NULL,
    points_per_win integer NOT NULL,
    purse_limit numeric(19,2) NOT NULL,
    timer_seconds integer DEFAULT 30 NOT NULL,
    championship_id uuid NOT NULL,
    id uuid NOT NULL,
    custom_rules text
);


--
-- Name: users; Type: TABLE; Schema: play_neml; Owner: -
--

CREATE TABLE play_neml.users (
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    global_role character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    CONSTRAINT users_global_role_check CHECK (((global_role)::text = ANY ((ARRAY['SUPER_ADMIN'::character varying, 'USER'::character varying, 'ADMIN'::character varying, 'TEAM_OWNER'::character varying, 'VIEWER'::character varying])::text[])))
);


--
-- Name: message_tracker msg_id; Type: DEFAULT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.message_tracker ALTER COLUMN msg_id SET DEFAULT nextval('play_neml.message_tracker_msg_id_seq'::regclass);


--
-- Name: announcements announcements_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.announcements
    ADD CONSTRAINT announcements_pkey PRIMARY KEY (id);


--
-- Name: app_screens app_screens_code_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.app_screens
    ADD CONSTRAINT app_screens_code_key UNIQUE (code);


--
-- Name: app_screens app_screens_path_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.app_screens
    ADD CONSTRAINT app_screens_path_key UNIQUE (path);


--
-- Name: app_screens app_screens_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.app_screens
    ADD CONSTRAINT app_screens_pkey PRIMARY KEY (id);


--
-- Name: auction_configurations auction_configurations_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_configurations
    ADD CONSTRAINT auction_configurations_pkey PRIMARY KEY (id);


--
-- Name: auction_state auction_state_auction_id_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_state
    ADD CONSTRAINT auction_state_auction_id_key UNIQUE (auction_id);


--
-- Name: auction_state auction_state_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_state
    ADD CONSTRAINT auction_state_pkey PRIMARY KEY (id);


--
-- Name: auctions auctions_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auctions
    ADD CONSTRAINT auctions_pkey PRIMARY KEY (id);


--
-- Name: badminton_configurations badminton_configurations_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.badminton_configurations
    ADD CONSTRAINT badminton_configurations_pkey PRIMARY KEY (id);


--
-- Name: badminton_rubber_rules badminton_rubber_rules_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.badminton_rubber_rules
    ADD CONSTRAINT badminton_rubber_rules_pkey PRIMARY KEY (id);


--
-- Name: bids bids_auction_id_sequence_no_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.bids
    ADD CONSTRAINT bids_auction_id_sequence_no_key UNIQUE (auction_id, sequence_no);


--
-- Name: bids bids_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.bids
    ADD CONSTRAINT bids_pkey PRIMARY KEY (id);


--
-- Name: championship_branding_configurations championship_branding_configurations_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championship_branding_configurations
    ADD CONSTRAINT championship_branding_configurations_pkey PRIMARY KEY (id);


--
-- Name: championship_roles championship_roles_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championship_roles
    ADD CONSTRAINT championship_roles_pkey PRIMARY KEY (id);


--
-- Name: championship_roles championship_roles_user_id_championship_id_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championship_roles
    ADD CONSTRAINT championship_roles_user_id_championship_id_key UNIQUE (user_id, championship_id);


--
-- Name: championships championships_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championships
    ADD CONSTRAINT championships_pkey PRIMARY KEY (id);


--
-- Name: championships championships_room_code_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championships
    ADD CONSTRAINT championships_room_code_key UNIQUE (room_code);


--
-- Name: competition_audit_events competition_audit_events_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_audit_events
    ADD CONSTRAINT competition_audit_events_pkey PRIMARY KEY (id);


--
-- Name: competition_configurations competition_configurations_championship_id_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_configurations
    ADD CONSTRAINT competition_configurations_championship_id_key UNIQUE (championship_id);


--
-- Name: competition_configurations competition_configurations_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_configurations
    ADD CONSTRAINT competition_configurations_pkey PRIMARY KEY (id);


--
-- Name: competition_qualifications competition_qualifications_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_qualifications
    ADD CONSTRAINT competition_qualifications_pkey PRIMARY KEY (id);


--
-- Name: competition_stages competition_stages_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_stages
    ADD CONSTRAINT competition_stages_pkey PRIMARY KEY (id);


--
-- Name: configuration_section_states configuration_section_states_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.configuration_section_states
    ADD CONSTRAINT configuration_section_states_pkey PRIMARY KEY (id);


--
-- Name: cricket_configurations cricket_configurations_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_configurations
    ADD CONSTRAINT cricket_configurations_pkey PRIMARY KEY (id);


--
-- Name: cricket_deliveries cricket_deliveries_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_deliveries
    ADD CONSTRAINT cricket_deliveries_pkey PRIMARY KEY (id);


--
-- Name: cricket_innings cricket_innings_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_innings
    ADD CONSTRAINT cricket_innings_pkey PRIMARY KEY (id);


--
-- Name: cricket_match_states cricket_match_states_match_id_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_match_states
    ADD CONSTRAINT cricket_match_states_match_id_key UNIQUE (match_id);


--
-- Name: cricket_match_states cricket_match_states_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_match_states
    ADD CONSTRAINT cricket_match_states_pkey PRIMARY KEY (id);


--
-- Name: cricket_playing_members cricket_playing_members_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_playing_members
    ADD CONSTRAINT cricket_playing_members_pkey PRIMARY KEY (id);


--
-- Name: match_formats match_formats_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.match_formats
    ADD CONSTRAINT match_formats_pkey PRIMARY KEY (id);


--
-- Name: matches matches_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT matches_pkey PRIMARY KEY (id);


--
-- Name: message_tracker message_tracker_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.message_tracker
    ADD CONSTRAINT message_tracker_pkey PRIMARY KEY (msg_id);


--
-- Name: players players_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.players
    ADD CONSTRAINT players_pkey PRIMARY KEY (id);


--
-- Name: role_screen_mappings role_screen_mappings_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.role_screen_mappings
    ADD CONSTRAINT role_screen_mappings_pkey PRIMARY KEY (id);


--
-- Name: roster_quota_rules roster_quota_rules_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.roster_quota_rules
    ADD CONSTRAINT roster_quota_rules_pkey PRIMARY KEY (id);


--
-- Name: seasons seasons_championship_id_name_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.seasons
    ADD CONSTRAINT seasons_championship_id_name_key UNIQUE (championship_id, name);


--
-- Name: seasons seasons_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.seasons
    ADD CONSTRAINT seasons_pkey PRIMARY KEY (id);


--
-- Name: squad_confirmation_events squad_confirmation_events_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmation_events
    ADD CONSTRAINT squad_confirmation_events_pkey PRIMARY KEY (id);


--
-- Name: squad_confirmations squad_confirmations_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmations
    ADD CONSTRAINT squad_confirmations_pkey PRIMARY KEY (id);


--
-- Name: squad_confirmations squad_confirmations_team_id_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmations
    ADD CONSTRAINT squad_confirmations_team_id_key UNIQUE (team_id);


--
-- Name: teams teams_championship_id_name_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.teams
    ADD CONSTRAINT teams_championship_id_name_key UNIQUE (championship_id, name);


--
-- Name: teams teams_championship_id_short_code_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.teams
    ADD CONSTRAINT teams_championship_id_short_code_key UNIQUE (championship_id, short_code);


--
-- Name: teams teams_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.teams
    ADD CONSTRAINT teams_pkey PRIMARY KEY (id);


--
-- Name: tie_breaker_order tie_breaker_order_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.tie_breaker_order
    ADD CONSTRAINT tie_breaker_order_pkey PRIMARY KEY ("position", settings_id);


--
-- Name: tournament_settings tournament_settings_championship_id_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.tournament_settings
    ADD CONSTRAINT tournament_settings_championship_id_key UNIQUE (championship_id);


--
-- Name: tournament_settings tournament_settings_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.tournament_settings
    ADD CONSTRAINT tournament_settings_pkey PRIMARY KEY (id);


--
-- Name: role_screen_mappings uk_role_screen; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.role_screen_mappings
    ADD CONSTRAINT uk_role_screen UNIQUE (role_key, screen_id);


--
-- Name: auction_configurations uq_auction_config_version; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_configurations
    ADD CONSTRAINT uq_auction_config_version UNIQUE (championship_id, configuration_version);


--
-- Name: auctions uq_auctions_id_championship; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auctions
    ADD CONSTRAINT uq_auctions_id_championship UNIQUE (id, championship_id);


--
-- Name: badminton_configurations uq_badminton_config_version; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.badminton_configurations
    ADD CONSTRAINT uq_badminton_config_version UNIQUE (championship_id, configuration_version);


--
-- Name: badminton_rubber_rules uq_badminton_rubber_code; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.badminton_rubber_rules
    ADD CONSTRAINT uq_badminton_rubber_code UNIQUE (badminton_configuration_id, rubber_code);


--
-- Name: championship_branding_configurations uq_branding_config_version; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championship_branding_configurations
    ADD CONSTRAINT uq_branding_config_version UNIQUE (championship_id, configuration_version);


--
-- Name: competition_qualifications uq_competition_qualification_seed; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_qualifications
    ADD CONSTRAINT uq_competition_qualification_seed UNIQUE (championship_id, source_stage, seed);


--
-- Name: competition_qualifications uq_competition_qualification_team; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_qualifications
    ADD CONSTRAINT uq_competition_qualification_team UNIQUE (championship_id, source_stage, team_id);


--
-- Name: competition_stages uq_competition_stage_id_tenant; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_stages
    ADD CONSTRAINT uq_competition_stage_id_tenant UNIQUE (id, championship_id);


--
-- Name: competition_stages uq_competition_stage_type; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_stages
    ADD CONSTRAINT uq_competition_stage_type UNIQUE (championship_id, stage_type);


--
-- Name: configuration_section_states uq_configuration_section; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.configuration_section_states
    ADD CONSTRAINT uq_configuration_section UNIQUE (championship_id, section_code);


--
-- Name: cricket_configurations uq_cricket_config_championship_version; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_configurations
    ADD CONSTRAINT uq_cricket_config_championship_version UNIQUE (championship_id, configuration_version);


--
-- Name: cricket_deliveries uq_cricket_delivery_idempotency; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_deliveries
    ADD CONSTRAINT uq_cricket_delivery_idempotency UNIQUE (championship_id, idempotency_key);


--
-- Name: cricket_deliveries uq_cricket_delivery_sequence; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_deliveries
    ADD CONSTRAINT uq_cricket_delivery_sequence UNIQUE (innings_id, delivery_sequence);


--
-- Name: cricket_innings uq_cricket_innings_number; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_innings
    ADD CONSTRAINT uq_cricket_innings_number UNIQUE (championship_id, match_id, innings_number);


--
-- Name: cricket_match_states uq_cricket_state_match_tenant; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_match_states
    ADD CONSTRAINT uq_cricket_state_match_tenant UNIQUE (match_id, championship_id);


--
-- Name: cricket_playing_members uq_cricket_xi_player; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_playing_members
    ADD CONSTRAINT uq_cricket_xi_player UNIQUE (championship_id, match_id, team_id, player_id);


--
-- Name: matches uq_match_fixture_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT uq_match_fixture_key UNIQUE (championship_id, fixture_key);


--
-- Name: match_formats uq_match_formats_id_championship; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.match_formats
    ADD CONSTRAINT uq_match_formats_id_championship UNIQUE (id, championship_id);


--
-- Name: matches uq_match_number; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT uq_match_number UNIQUE (championship_id, match_number);


--
-- Name: matches uq_matches_id_championship; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT uq_matches_id_championship UNIQUE (id, championship_id);


--
-- Name: players uq_players_id_championship; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.players
    ADD CONSTRAINT uq_players_id_championship UNIQUE (id, championship_id);


--
-- Name: roster_quota_rules uq_roster_quota_rule; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.roster_quota_rules
    ADD CONSTRAINT uq_roster_quota_rule UNIQUE (championship_id, configuration_version, rule_code);


--
-- Name: squad_confirmations uq_squad_confirmation_team; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmations
    ADD CONSTRAINT uq_squad_confirmation_team UNIQUE (championship_id, team_id);


--
-- Name: teams uq_teams_id_championship; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.teams
    ADD CONSTRAINT uq_teams_id_championship UNIQUE (id, championship_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_app_screen_order; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_app_screen_order ON play_neml.app_screens USING btree (section_name, display_order);


--
-- Name: idx_auction_championship; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_auction_championship ON play_neml.auctions USING btree (championship_id);


--
-- Name: idx_auction_config_latest; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_auction_config_latest ON play_neml.auction_configurations USING btree (championship_id, configuration_version DESC);


--
-- Name: idx_auction_state_timer_sweep; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_auction_state_timer_sweep ON play_neml.auction_state USING btree (bid_deadline) WHERE ((status)::text = 'RUNNING'::text);


--
-- Name: idx_badminton_config_tenant; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_badminton_config_tenant ON play_neml.badminton_configurations USING btree (championship_id, configuration_version DESC);


--
-- Name: idx_bid_auction; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_bid_auction ON play_neml.bids USING btree (auction_id);


--
-- Name: idx_bids_championship_auction; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_bids_championship_auction ON play_neml.bids USING btree (championship_id, auction_id, sequence_no DESC);


--
-- Name: idx_branding_config_latest; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_branding_config_latest ON play_neml.championship_branding_configurations USING btree (championship_id, configuration_version DESC);


--
-- Name: idx_champ_role_championship; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_champ_role_championship ON play_neml.championship_roles USING btree (championship_id);


--
-- Name: idx_champ_role_user; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_champ_role_user ON play_neml.championship_roles USING btree (user_id);


--
-- Name: idx_championship_status; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_championship_status ON play_neml.championships USING btree (status);


--
-- Name: idx_competition_audit_championship; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_competition_audit_championship ON play_neml.competition_audit_events USING btree (championship_id, occurred_at);


--
-- Name: idx_competition_configuration_status; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_competition_configuration_status ON play_neml.competition_configurations USING btree (championship_id, configuration_status);


--
-- Name: idx_competition_stage_championship; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_competition_stage_championship ON play_neml.competition_stages USING btree (championship_id, stage_order);


--
-- Name: idx_configuration_section_status; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_configuration_section_status ON play_neml.configuration_section_states USING btree (championship_id, status);


--
-- Name: idx_cricket_configuration_tenant; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_cricket_configuration_tenant ON play_neml.cricket_configurations USING btree (championship_id, configuration_version DESC);


--
-- Name: idx_cricket_delivery_corrections; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_cricket_delivery_corrections ON play_neml.cricket_deliveries USING btree (championship_id, corrected_at) WHERE voided;


--
-- Name: idx_cricket_delivery_ledger; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_cricket_delivery_ledger ON play_neml.cricket_deliveries USING btree (championship_id, innings_id, delivery_sequence);


--
-- Name: idx_cricket_innings_match; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_cricket_innings_match ON play_neml.cricket_innings USING btree (championship_id, match_id, innings_number);


--
-- Name: idx_cricket_xi_match; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_cricket_xi_match ON play_neml.cricket_playing_members USING btree (championship_id, match_id, team_id);


--
-- Name: idx_match_championship; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_match_championship ON play_neml.matches USING btree (championship_id);


--
-- Name: idx_match_stage; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_match_stage ON play_neml.matches USING btree (championship_id, stage_type, status);


--
-- Name: idx_player_auction; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_player_auction ON play_neml.players USING btree (auction_id);


--
-- Name: idx_player_championship; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_player_championship ON play_neml.players USING btree (championship_id);


--
-- Name: idx_role_screen_role; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_role_screen_role ON play_neml.role_screen_mappings USING btree (role_key);


--
-- Name: idx_roster_quota_version; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_roster_quota_version ON play_neml.roster_quota_rules USING btree (championship_id, configuration_version DESC, enabled);


--
-- Name: idx_squad_confirmation_championship; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_squad_confirmation_championship ON play_neml.squad_confirmations USING btree (championship_id);


--
-- Name: idx_squad_confirmation_status; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_squad_confirmation_status ON play_neml.squad_confirmations USING btree (championship_id, status);


--
-- Name: idx_squad_event_championship; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_squad_event_championship ON play_neml.squad_confirmation_events USING btree (championship_id, occurred_at);


--
-- Name: idx_squad_event_team; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_squad_event_team ON play_neml.squad_confirmation_events USING btree (team_id, occurred_at);


--
-- Name: idx_team_championship; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE INDEX idx_team_championship ON play_neml.teams USING btree (championship_id);


--
-- Name: uq_bids_auction_sequence; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE UNIQUE INDEX uq_bids_auction_sequence ON play_neml.bids USING btree (auction_id, sequence_no);


--
-- Name: uq_matches_championship_number; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE UNIQUE INDEX uq_matches_championship_number ON play_neml.matches USING btree (championship_id, match_number);


--
-- Name: uq_players_auction_order; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE UNIQUE INDEX uq_players_auction_order ON play_neml.players USING btree (auction_id, auction_order) WHERE ((auction_id IS NOT NULL) AND (auction_order IS NOT NULL));


--
-- Name: uq_seasons_championship_name; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE UNIQUE INDEX uq_seasons_championship_name ON play_neml.seasons USING btree (championship_id, name);


--
-- Name: uq_seasons_one_active_per_championship; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE UNIQUE INDEX uq_seasons_one_active_per_championship ON play_neml.seasons USING btree (championship_id) WHERE (active IS TRUE);


--
-- Name: uq_teams_championship_short_code; Type: INDEX; Schema: play_neml; Owner: -
--

CREATE UNIQUE INDEX uq_teams_championship_short_code ON play_neml.teams USING btree (championship_id, short_code);


--
-- Name: format_side_a_players trg_format_side_a_tenant; Type: TRIGGER; Schema: play_neml; Owner: -
--

CREATE TRIGGER trg_format_side_a_tenant BEFORE INSERT OR UPDATE OF format_id, player_id ON play_neml.format_side_a_players FOR EACH ROW EXECUTE FUNCTION play_neml.enforce_format_player_tenant();


--
-- Name: format_side_b_players trg_format_side_b_tenant; Type: TRIGGER; Schema: play_neml; Owner: -
--

CREATE TRIGGER trg_format_side_b_tenant BEFORE INSERT OR UPDATE OF format_id, player_id ON play_neml.format_side_b_players FOR EACH ROW EXECUTE FUNCTION play_neml.enforce_format_player_tenant();


--
-- Name: cricket_playing_members fk122v3leidy1q2b60v4ksl5m67; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_playing_members
    ADD CONSTRAINT fk122v3leidy1q2b60v4ksl5m67 FOREIGN KEY (match_id) REFERENCES play_neml.matches(id);


--
-- Name: badminton_configurations fk1hxheg5yd2nuv1j3mjbf2dq3n; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.badminton_configurations
    ADD CONSTRAINT fk1hxheg5yd2nuv1j3mjbf2dq3n FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: cricket_match_states fk1uxw2q5ew9lucjgmmd1w2tv2f; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_match_states
    ADD CONSTRAINT fk1uxw2q5ew9lucjgmmd1w2tv2f FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: competition_audit_events fk285pjpf09sd7ugoyah6wv836g; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_audit_events
    ADD CONSTRAINT fk285pjpf09sd7ugoyah6wv836g FOREIGN KEY (fixture_id) REFERENCES play_neml.matches(id);


--
-- Name: squad_confirmation_events fk29bw26nth39l22oqnuwvmucf3; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmation_events
    ADD CONSTRAINT fk29bw26nth39l22oqnuwvmucf3 FOREIGN KEY (team_id) REFERENCES play_neml.teams(id);


--
-- Name: championship_roles fk2y52e2mkrrsxcdutvnsl6ijr0; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championship_roles
    ADD CONSTRAINT fk2y52e2mkrrsxcdutvnsl6ijr0 FOREIGN KEY (team_id) REFERENCES play_neml.teams(id);


--
-- Name: squad_confirmation_events fk3jd10y69e4ckju38fa4a58sx7; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmation_events
    ADD CONSTRAINT fk3jd10y69e4ckju38fa4a58sx7 FOREIGN KEY (confirmation_id) REFERENCES play_neml.squad_confirmations(id);


--
-- Name: cricket_match_states fk4sn2rdaecr90senmiydj9wc74; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_match_states
    ADD CONSTRAINT fk4sn2rdaecr90senmiydj9wc74 FOREIGN KEY (match_id) REFERENCES play_neml.matches(id);


--
-- Name: badminton_rubber_rules fk57hry0n1buq9f70vdqycvduy3; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.badminton_rubber_rules
    ADD CONSTRAINT fk57hry0n1buq9f70vdqycvduy3 FOREIGN KEY (badminton_configuration_id) REFERENCES play_neml.badminton_configurations(id);


--
-- Name: players fk5nglidr00c4dyybl171v6kask; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.players
    ADD CONSTRAINT fk5nglidr00c4dyybl171v6kask FOREIGN KEY (team_id) REFERENCES play_neml.teams(id);


--
-- Name: cricket_playing_members fk5utwt28qq1tkga6keuei00yqy; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_playing_members
    ADD CONSTRAINT fk5utwt28qq1tkga6keuei00yqy FOREIGN KEY (player_id) REFERENCES play_neml.players(id);


--
-- Name: format_side_a_players fk5vy73sijo2jshjlo8by6ph3d7; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.format_side_a_players
    ADD CONSTRAINT fk5vy73sijo2jshjlo8by6ph3d7 FOREIGN KEY (format_id) REFERENCES play_neml.match_formats(id);


--
-- Name: cricket_playing_members fk77mkpb05n1ay3kvpicif4qedg; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_playing_members
    ADD CONSTRAINT fk77mkpb05n1ay3kvpicif4qedg FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: format_side_a_players fk85nbnoikb145eg9ix6firygst; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.format_side_a_players
    ADD CONSTRAINT fk85nbnoikb145eg9ix6firygst FOREIGN KEY (player_id) REFERENCES play_neml.players(id);


--
-- Name: competition_configurations fk8qr5mhectmy00wdhn8fmwxbw0; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_configurations
    ADD CONSTRAINT fk8qr5mhectmy00wdhn8fmwxbw0 FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: cricket_innings fk923rbw8ri6x81o0ihctxf0u8t; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_innings
    ADD CONSTRAINT fk923rbw8ri6x81o0ihctxf0u8t FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: configuration_section_states fk9u36gmyqerjtx92sncouxrmy; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.configuration_section_states
    ADD CONSTRAINT fk9u36gmyqerjtx92sncouxrmy FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: announcements fk_announcements_championship; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.announcements
    ADD CONSTRAINT fk_announcements_championship FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: auction_state fk_auction_state_auction_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_state
    ADD CONSTRAINT fk_auction_state_auction_tenant FOREIGN KEY (auction_id, championship_id) REFERENCES play_neml.auctions(id, championship_id);


--
-- Name: auction_state fk_auction_state_championship; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_state
    ADD CONSTRAINT fk_auction_state_championship FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: auction_state fk_auction_state_player_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_state
    ADD CONSTRAINT fk_auction_state_player_tenant FOREIGN KEY (current_player_id, championship_id) REFERENCES play_neml.players(id, championship_id);


--
-- Name: bids fk_bids_auction_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.bids
    ADD CONSTRAINT fk_bids_auction_tenant FOREIGN KEY (auction_id, championship_id) REFERENCES play_neml.auctions(id, championship_id);


--
-- Name: bids fk_bids_championship; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.bids
    ADD CONSTRAINT fk_bids_championship FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: bids fk_bids_player_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.bids
    ADD CONSTRAINT fk_bids_player_tenant FOREIGN KEY (player_id, championship_id) REFERENCES play_neml.players(id, championship_id);


--
-- Name: bids fk_bids_team_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.bids
    ADD CONSTRAINT fk_bids_team_tenant FOREIGN KEY (team_id, championship_id) REFERENCES play_neml.teams(id, championship_id);


--
-- Name: championship_roles fk_championship_roles_team_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championship_roles
    ADD CONSTRAINT fk_championship_roles_team_tenant FOREIGN KEY (team_id, championship_id) REFERENCES play_neml.teams(id, championship_id);


--
-- Name: cricket_deliveries fk_cricket_delivery_corrected_by; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_deliveries
    ADD CONSTRAINT fk_cricket_delivery_corrected_by FOREIGN KEY (corrected_by_user_id) REFERENCES play_neml.users(id);


--
-- Name: format_side_a_players fk_format_side_a_format_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.format_side_a_players
    ADD CONSTRAINT fk_format_side_a_format_tenant FOREIGN KEY (format_id, championship_id) REFERENCES play_neml.match_formats(id, championship_id) ON DELETE CASCADE;


--
-- Name: format_side_a_players fk_format_side_a_player_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.format_side_a_players
    ADD CONSTRAINT fk_format_side_a_player_tenant FOREIGN KEY (player_id, player_championship_id) REFERENCES play_neml.players(id, championship_id);


--
-- Name: format_side_b_players fk_format_side_b_format_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.format_side_b_players
    ADD CONSTRAINT fk_format_side_b_format_tenant FOREIGN KEY (format_id, championship_id) REFERENCES play_neml.match_formats(id, championship_id) ON DELETE CASCADE;


--
-- Name: format_side_b_players fk_format_side_b_player_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.format_side_b_players
    ADD CONSTRAINT fk_format_side_b_player_tenant FOREIGN KEY (player_id, player_championship_id) REFERENCES play_neml.players(id, championship_id);


--
-- Name: match_formats fk_match_formats_match_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.match_formats
    ADD CONSTRAINT fk_match_formats_match_tenant FOREIGN KEY (match_id, championship_id) REFERENCES play_neml.matches(id, championship_id);


--
-- Name: match_formats fk_match_formats_winner_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.match_formats
    ADD CONSTRAINT fk_match_formats_winner_tenant FOREIGN KEY (winner_team_id, championship_id) REFERENCES play_neml.teams(id, championship_id);


--
-- Name: matches fk_matches_competition_stage_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT fk_matches_competition_stage_tenant FOREIGN KEY (competition_stage_id, stage_championship_id) REFERENCES play_neml.competition_stages(id, championship_id);


--
-- Name: matches fk_matches_team_a_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT fk_matches_team_a_tenant FOREIGN KEY (team_a_id, championship_id) REFERENCES play_neml.teams(id, championship_id);


--
-- Name: matches fk_matches_team_b_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT fk_matches_team_b_tenant FOREIGN KEY (team_b_id, championship_id) REFERENCES play_neml.teams(id, championship_id);


--
-- Name: matches fk_matches_winner_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT fk_matches_winner_tenant FOREIGN KEY (winner_team_id, championship_id) REFERENCES play_neml.teams(id, championship_id);


--
-- Name: players fk_players_auction_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.players
    ADD CONSTRAINT fk_players_auction_tenant FOREIGN KEY (auction_id, championship_id) REFERENCES play_neml.auctions(id, championship_id);


--
-- Name: players fk_players_team_tenant; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.players
    ADD CONSTRAINT fk_players_team_tenant FOREIGN KEY (team_id, championship_id) REFERENCES play_neml.teams(id, championship_id);


--
-- Name: seasons fk_seasons_championship; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.seasons
    ADD CONSTRAINT fk_seasons_championship FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: championship_roles fka98v9w9nbyhv5xubhqmar5the; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championship_roles
    ADD CONSTRAINT fka98v9w9nbyhv5xubhqmar5the FOREIGN KEY (user_id) REFERENCES play_neml.users(id);


--
-- Name: format_side_b_players fkabatxthoaixqj36nf28wmnuft; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.format_side_b_players
    ADD CONSTRAINT fkabatxthoaixqj36nf28wmnuft FOREIGN KEY (player_id) REFERENCES play_neml.players(id);


--
-- Name: auctions fkbd56msyyen2vdri09ci2a56r7; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auctions
    ADD CONSTRAINT fkbd56msyyen2vdri09ci2a56r7 FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: cricket_innings fkbhhi4r3bop9w17r9rpe0kfc7q; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_innings
    ADD CONSTRAINT fkbhhi4r3bop9w17r9rpe0kfc7q FOREIGN KEY (match_id) REFERENCES play_neml.matches(id);


--
-- Name: auction_state fkbjuxwafp07jttmgianrievn0g; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_state
    ADD CONSTRAINT fkbjuxwafp07jttmgianrievn0g FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: bids fkbm89m2gow82dotpnlcp7t3p5f; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.bids
    ADD CONSTRAINT fkbm89m2gow82dotpnlcp7t3p5f FOREIGN KEY (auction_id) REFERENCES play_neml.auctions(id);


--
-- Name: competition_configurations fkbvyd1urjllw6pbvcejb0lgejc; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_configurations
    ADD CONSTRAINT fkbvyd1urjllw6pbvcejb0lgejc FOREIGN KEY (runner_up_team_id) REFERENCES play_neml.teams(id);


--
-- Name: bids fkc0f7ifexbhw97xqo6pmub1nxj; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.bids
    ADD CONSTRAINT fkc0f7ifexbhw97xqo6pmub1nxj FOREIGN KEY (team_id) REFERENCES play_neml.teams(id);


--
-- Name: squad_confirmation_events fkd99x2inrji6pahiqbbeqssiec; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmation_events
    ADD CONSTRAINT fkd99x2inrji6pahiqbbeqssiec FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: championships fkdgfe7npo4itvj2goeopdxj4w9; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championships
    ADD CONSTRAINT fkdgfe7npo4itvj2goeopdxj4w9 FOREIGN KEY (created_by) REFERENCES play_neml.users(id);


--
-- Name: auction_state fkdp69go4htg46n2fjcgbq9n0gr; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_state
    ADD CONSTRAINT fkdp69go4htg46n2fjcgbq9n0gr FOREIGN KEY (current_player_id) REFERENCES play_neml.players(id);


--
-- Name: championship_roles fket2uycaeicjltq02xsiu24awu; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championship_roles
    ADD CONSTRAINT fket2uycaeicjltq02xsiu24awu FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: squad_confirmations fkex20y3v4ib4gwwt3apt47wnbt; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmations
    ADD CONSTRAINT fkex20y3v4ib4gwwt3apt47wnbt FOREIGN KEY (confirmed_by) REFERENCES play_neml.users(id);


--
-- Name: competition_configurations fkfcksv4b75ogr75xmwqx52d4y4; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_configurations
    ADD CONSTRAINT fkfcksv4b75ogr75xmwqx52d4y4 FOREIGN KEY (champion_team_id) REFERENCES play_neml.teams(id);


--
-- Name: competition_qualifications fkffg3ec5rrual61b59mvmngvqd; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_qualifications
    ADD CONSTRAINT fkffg3ec5rrual61b59mvmngvqd FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: competition_audit_events fkfmc7lu4j7v1vdfgj4n1puqjhq; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_audit_events
    ADD CONSTRAINT fkfmc7lu4j7v1vdfgj4n1puqjhq FOREIGN KEY (actor_user_id) REFERENCES play_neml.users(id);


--
-- Name: squad_confirmations fkg209dr03utex9uxbc8gkllidd; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmations
    ADD CONSTRAINT fkg209dr03utex9uxbc8gkllidd FOREIGN KEY (locked_by) REFERENCES play_neml.users(id);


--
-- Name: seasons fkh14em75kgmxayoc7dsa52xngg; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.seasons
    ADD CONSTRAINT fkh14em75kgmxayoc7dsa52xngg FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: match_formats fkh18ehuf9in5irq1rl25vr9su1; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.match_formats
    ADD CONSTRAINT fkh18ehuf9in5irq1rl25vr9su1 FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: match_formats fkhibbjitai0qwd2ayu9u9yjjb6; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.match_formats
    ADD CONSTRAINT fkhibbjitai0qwd2ayu9u9yjjb6 FOREIGN KEY (winner_team_id) REFERENCES play_neml.teams(id);


--
-- Name: competition_qualifications fki4pm4p58l8qnfxgrhy5jjo6n4; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_qualifications
    ADD CONSTRAINT fki4pm4p58l8qnfxgrhy5jjo6n4 FOREIGN KEY (team_id) REFERENCES play_neml.teams(id);


--
-- Name: announcements fki5ejdxv9mo82h3ib4irv3vwie; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.announcements
    ADD CONSTRAINT fki5ejdxv9mo82h3ib4irv3vwie FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: cricket_deliveries fki61e2lfbofkf1ay0ohwop0o5j; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_deliveries
    ADD CONSTRAINT fki61e2lfbofkf1ay0ohwop0o5j FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: cricket_innings fkiks5qugchrusku34t3k9ampje; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_innings
    ADD CONSTRAINT fkiks5qugchrusku34t3k9ampje FOREIGN KEY (batting_team_id) REFERENCES play_neml.teams(id);


--
-- Name: auction_state fkjhvaw63nxswa8keb3ucddumjy; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_state
    ADD CONSTRAINT fkjhvaw63nxswa8keb3ucddumjy FOREIGN KEY (auction_id) REFERENCES play_neml.auctions(id);


--
-- Name: competition_audit_events fkjm3vpgtff3ncaco0e1ifk3j68; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_audit_events
    ADD CONSTRAINT fkjm3vpgtff3ncaco0e1ifk3j68 FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: championship_branding_configurations fkjom20hy0k1saeylwut5e8vd3k; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.championship_branding_configurations
    ADD CONSTRAINT fkjom20hy0k1saeylwut5e8vd3k FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: matches fkjrrimoay5909daif5ml6ek1nu; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT fkjrrimoay5909daif5ml6ek1nu FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: squad_confirmations fkkgma8aw35t1uk3eslpyt6s7vj; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmations
    ADD CONSTRAINT fkkgma8aw35t1uk3eslpyt6s7vj FOREIGN KEY (team_id) REFERENCES play_neml.teams(id);


--
-- Name: squad_confirmations fkkhemyl8swo5rqrs5kgddamvtk; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmations
    ADD CONSTRAINT fkkhemyl8swo5rqrs5kgddamvtk FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: format_side_b_players fkkvs1ma8wbfsbyx1ocp7s7wj30; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.format_side_b_players
    ADD CONSTRAINT fkkvs1ma8wbfsbyx1ocp7s7wj30 FOREIGN KEY (format_id) REFERENCES play_neml.match_formats(id);


--
-- Name: tie_breaker_order fkl1kr4w3k5eyjd608g4svarw38; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.tie_breaker_order
    ADD CONSTRAINT fkl1kr4w3k5eyjd608g4svarw38 FOREIGN KEY (settings_id) REFERENCES play_neml.tournament_settings(id);


--
-- Name: players fklu6rwnocqqab8dnopg6lx2k7e; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.players
    ADD CONSTRAINT fklu6rwnocqqab8dnopg6lx2k7e FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: cricket_configurations fkm1tlvlgyrqd2n2ym0aju2dn43; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_configurations
    ADD CONSTRAINT fkm1tlvlgyrqd2n2ym0aju2dn43 FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: roster_quota_rules fkmcxjrh7wb2w1cmjcmacyu83tb; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.roster_quota_rules
    ADD CONSTRAINT fkmcxjrh7wb2w1cmjcmacyu83tb FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: players fkmj6l687chi7ouqyo85vi1wj59; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.players
    ADD CONSTRAINT fkmj6l687chi7ouqyo85vi1wj59 FOREIGN KEY (auction_id) REFERENCES play_neml.auctions(id);


--
-- Name: matches fkmrgum5di41ywbw87fdecijmvs; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT fkmrgum5di41ywbw87fdecijmvs FOREIGN KEY (team_a_id) REFERENCES play_neml.teams(id);


--
-- Name: match_formats fkn30w72d9583tsftn966d19bg; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.match_formats
    ADD CONSTRAINT fkn30w72d9583tsftn966d19bg FOREIGN KEY (match_id) REFERENCES play_neml.matches(id);


--
-- Name: matches fkn66swy9if3qemrk5407f81hc5; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT fkn66swy9if3qemrk5407f81hc5 FOREIGN KEY (winner_team_id) REFERENCES play_neml.teams(id);


--
-- Name: teams fknfjgqfct46292yr0vjj5c5hvn; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.teams
    ADD CONSTRAINT fknfjgqfct46292yr0vjj5c5hvn FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: matches fkntr6m7io9k4xcxgn7prtyojko; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT fkntr6m7io9k4xcxgn7prtyojko FOREIGN KEY (team_b_id) REFERENCES play_neml.teams(id);


--
-- Name: bids fkpoine0tlihidy5g457xaoatrl; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.bids
    ADD CONSTRAINT fkpoine0tlihidy5g457xaoatrl FOREIGN KEY (player_id) REFERENCES play_neml.players(id);


--
-- Name: cricket_playing_members fkps4blxouqw0rxchb4ex9r4fys; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_playing_members
    ADD CONSTRAINT fkps4blxouqw0rxchb4ex9r4fys FOREIGN KEY (team_id) REFERENCES play_neml.teams(id);


--
-- Name: role_screen_mappings fkq6os469uix2iba8l88siall9c; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.role_screen_mappings
    ADD CONSTRAINT fkq6os469uix2iba8l88siall9c FOREIGN KEY (screen_id) REFERENCES play_neml.app_screens(id);


--
-- Name: competition_stages fkqsbj745g5myrgd2hwggp5u137; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.competition_stages
    ADD CONSTRAINT fkqsbj745g5myrgd2hwggp5u137 FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: bids fkr48u7qeiu8cmgdoxfmawipeea; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.bids
    ADD CONSTRAINT fkr48u7qeiu8cmgdoxfmawipeea FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: squad_confirmation_events fkr6oe51fd57yd9oe3wkw1o3g6s; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmation_events
    ADD CONSTRAINT fkr6oe51fd57yd9oe3wkw1o3g6s FOREIGN KEY (actor_user_id) REFERENCES play_neml.users(id);


--
-- Name: tournament_settings fkrqui0uym7jyey06syy2vge1sa; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.tournament_settings
    ADD CONSTRAINT fkrqui0uym7jyey06syy2vge1sa FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- Name: cricket_deliveries fkrwyvdkkdynt6an2o18lnul2ec; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_deliveries
    ADD CONSTRAINT fkrwyvdkkdynt6an2o18lnul2ec FOREIGN KEY (innings_id) REFERENCES play_neml.cricket_innings(id);


--
-- Name: squad_confirmations fkryy5gy7ge9uml1k57c9974hke; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.squad_confirmations
    ADD CONSTRAINT fkryy5gy7ge9uml1k57c9974hke FOREIGN KEY (reopened_by) REFERENCES play_neml.users(id);


--
-- Name: configuration_section_states fksbbwaoin9ni7jjvec6m1fq00h; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.configuration_section_states
    ADD CONSTRAINT fksbbwaoin9ni7jjvec6m1fq00h FOREIGN KEY (last_changed_by) REFERENCES play_neml.users(id);


--
-- Name: cricket_match_states fkt50ldq8hu7scwp7fmobsrexdh; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.cricket_match_states
    ADD CONSTRAINT fkt50ldq8hu7scwp7fmobsrexdh FOREIGN KEY (toss_winner_team_id) REFERENCES play_neml.teams(id);


--
-- Name: matches fktmuur5gvvu2bathq6ce8hhswx; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.matches
    ADD CONSTRAINT fktmuur5gvvu2bathq6ce8hhswx FOREIGN KEY (competition_stage_id) REFERENCES play_neml.competition_stages(id);


--
-- Name: auction_configurations fktr9lvq8j5w28owc1w4tk92sa6; Type: FK CONSTRAINT; Schema: play_neml; Owner: -
--

ALTER TABLE ONLY play_neml.auction_configurations
    ADD CONSTRAINT fktr9lvq8j5w28owc1w4tk92sa6 FOREIGN KEY (championship_id) REFERENCES play_neml.championships(id);


--
-- PostgreSQL database dump complete
--
