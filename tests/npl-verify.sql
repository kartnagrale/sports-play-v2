\set ON_ERROR_STOP on
BEGIN READ ONLY;
SELECT set_config('npl.mock_cid', :'cid', true);
DO $$
DECLARE c uuid := current_setting('npl.mock_cid')::uuid;
BEGIN
 IF (SELECT count(*) FROM play_neml.schema_migrations)<>7 THEN RAISE EXCEPTION 'Expected seven tracked migrations'; END IF;
 IF NOT EXISTS(SELECT 1 FROM play_neml.championships WHERE id=c AND name='NPL MOCK 2026' AND sport_type='CRICKET' AND status='COMPLETED' AND lifecycle_phase='COMPLETED') THEN RAISE EXCEPTION 'Main championship is not complete'; END IF;
 IF (SELECT count(*) FROM play_neml.teams WHERE championship_id=c)<>4 THEN RAISE EXCEPTION 'Expected four teams'; END IF;
 IF (SELECT count(DISTINCT primary_color) FROM play_neml.teams WHERE championship_id=c)<>4 THEN RAISE EXCEPTION 'Expected four distinctive team colours'; END IF;
 IF (SELECT count(*) FROM play_neml.championship_roles r JOIN play_neml.users u ON u.id=r.user_id WHERE r.championship_id=c AND r.role='TEAM_CAPTAIN' AND u.account_enabled)<>4 THEN RAISE EXCEPTION 'Expected four activated captains'; END IF;
 IF EXISTS(SELECT team_id FROM play_neml.championship_roles WHERE championship_id=c AND role='TEAM_CAPTAIN' GROUP BY team_id HAVING count(*)<>1) THEN RAISE EXCEPTION 'Captain team membership mismatch'; END IF;
 IF (SELECT count(*) FROM play_neml.players WHERE championship_id=c)<>44 THEN RAISE EXCEPTION 'Expected 44 identities'; END IF;
 IF (SELECT count(DISTINCT player_reference) FROM play_neml.players WHERE championship_id=c)<>44 THEN RAISE EXCEPTION 'Duplicate player references'; END IF;
 IF (SELECT count(*) FROM play_neml.players WHERE championship_id=c AND status='PREASSIGNED' AND gender='MALE' AND sold_price IS NULL AND auction_id IS NULL)<>4 THEN RAISE EXCEPTION 'Captain preassignment mismatch'; END IF;
 IF (SELECT count(*) FROM play_neml.players WHERE championship_id=c AND status='SOLD')<>40 THEN RAISE EXCEPTION 'Expected 40 purchases'; END IF;
 IF (SELECT count(*) FROM play_neml.players WHERE championship_id=c AND status='SOLD' AND sold_price=15000)<>16 THEN RAISE EXCEPTION 'Expected 16 premium purchases'; END IF;
 IF (SELECT count(*) FROM play_neml.players WHERE championship_id=c AND status='SOLD' AND sold_price=5000)<>24 THEN RAISE EXCEPTION 'Expected 24 regular purchases'; END IF;
 IF EXISTS(SELECT 1 FROM play_neml.teams t WHERE t.championship_id=c AND (t.purse_total<>100000 OR t.purse_remaining<>10000 OR t.male_count<>8 OR t.female_count<>3 OR t.purse_total-t.purse_remaining<>(SELECT coalesce(sum(p.sold_price),0) FROM play_neml.players p WHERE p.team_id=t.id AND p.championship_id=c))) THEN RAISE EXCEPTION 'Financial or gender totals disagree'; END IF;
 IF EXISTS(SELECT team_id FROM play_neml.players WHERE championship_id=c GROUP BY team_id HAVING count(*)<>11 OR count(*) FILTER(WHERE status='PREASSIGNED')<>1 OR count(*) FILTER(WHERE status='SOLD')<>10 OR count(*) FILTER(WHERE gender='FEMALE')<>3 OR count(*) FILTER(WHERE sold_price=15000)<>4) THEN RAISE EXCEPTION 'Invalid squad'; END IF;
 IF EXISTS(SELECT 1 FROM play_neml.players p WHERE p.championship_id=c AND p.status='SOLD' AND NOT EXISTS(SELECT 1 FROM play_neml.bids b WHERE b.id=(SELECT b2.id FROM play_neml.bids b2 WHERE b2.player_id=p.id AND b2.championship_id=c AND b2.active ORDER BY b2.sequence_no DESC LIMIT 1) AND b.team_id=p.team_id AND b.amount=p.sold_price)) THEN RAISE EXCEPTION 'Sale does not match final active bid'; END IF;
 IF (SELECT count(*) FROM play_neml.auction_state WHERE championship_id=c AND status='COMPLETED')<>1 THEN RAISE EXCEPTION 'Auction incomplete'; END IF;
 IF (SELECT count(*) FROM play_neml.squad_confirmations WHERE championship_id=c AND status='LOCKED')<>4 THEN RAISE EXCEPTION 'Squads not locked'; END IF;
 IF (SELECT count(*) FROM play_neml.matches WHERE championship_id=c)<>7 OR (SELECT count(*) FROM play_neml.matches WHERE championship_id=c AND stage_type='LEAGUE' AND status='COMPLETED')<>6 OR (SELECT count(*) FROM play_neml.matches WHERE championship_id=c AND stage_type='FINAL' AND status='COMPLETED')<>1 THEN RAISE EXCEPTION 'Expected six completed league fixtures and one completed final'; END IF;
 IF (SELECT count(DISTINCT least(team_a_id,team_b_id)::text||greatest(team_a_id,team_b_id)::text) FROM play_neml.matches WHERE championship_id=c AND stage_type='LEAGUE')<>6 THEN RAISE EXCEPTION 'Duplicate pairing'; END IF;
 IF EXISTS(SELECT 1 FROM play_neml.teams t WHERE t.championship_id=c AND (SELECT count(*) FROM play_neml.matches m WHERE m.championship_id=c AND m.stage_type='LEAGUE' AND t.id IN(m.team_a_id,m.team_b_id))<>3) THEN RAISE EXCEPTION 'League appearances mismatch'; END IF;
 IF (SELECT count(*) FROM play_neml.cricket_innings WHERE championship_id=c AND status='COMPLETED')<>14 THEN RAISE EXCEPTION 'Expected 14 completed innings'; END IF;
 IF EXISTS(SELECT 1 FROM play_neml.cricket_innings i WHERE i.championship_id=c AND (i.runs<>(SELECT coalesce(sum(d.total_runs),0) FROM play_neml.cricket_deliveries d WHERE d.innings_id=i.id AND NOT d.voided) OR i.legal_balls<>(SELECT count(*) FROM play_neml.cricket_deliveries d WHERE d.innings_id=i.id AND NOT d.voided AND d.legal_ball))) THEN RAISE EXCEPTION 'Innings disagree with delivery ledger'; END IF;
 IF (SELECT count(*) FROM play_neml.cricket_playing_members WHERE championship_id=c)<>154 THEN RAISE EXCEPTION 'Expected 22 playing members in each of seven fixtures'; END IF;
 IF (SELECT count(*) FROM play_neml.cricket_playing_members cm JOIN play_neml.players p ON p.id=cm.player_id WHERE cm.championship_id=c AND cm.captain AND p.status='PREASSIGNED')<>14 THEN RAISE EXCEPTION 'Captains missing from playing sides'; END IF;
 IF (SELECT count(*) FROM play_neml.cricket_playing_members WHERE championship_id=c AND wicketkeeper)<>14 THEN RAISE EXCEPTION 'Wicketkeepers missing'; END IF;
 IF NOT EXISTS(SELECT 1 FROM play_neml.competition_stages WHERE championship_id=c AND stage_type='LEAGUE' AND status='FROZEN' AND standings_snapshot IS NOT NULL) THEN RAISE EXCEPTION 'League qualification is not frozen'; END IF;
 IF NOT EXISTS(SELECT 1 FROM play_neml.competition_configurations cc JOIN play_neml.teams winner ON winner.id=cc.champion_team_id JOIN play_neml.teams runner ON runner.id=cc.runner_up_team_id WHERE cc.championship_id=c AND winner.name='NPL Thunder' AND runner.name='NPL Warriors') THEN RAISE EXCEPTION 'Incorrect champion or runner-up'; END IF;
 IF (SELECT count(*) FROM play_neml.certificates WHERE championship_id=c AND status='ISSUED')<>3 THEN RAISE EXCEPTION 'Expected three active reissued certificates'; END IF;
END $$;
SELECT t.name,t.purse_total,t.purse_remaining,count(p.id) players,count(p.id) FILTER(WHERE p.status='PREASSIGNED') captains,count(p.id) FILTER(WHERE p.gender='FEMALE') female,count(p.id) FILTER(WHERE p.sold_price=15000) premium,sum(p.sold_price) spent FROM play_neml.teams t JOIN play_neml.players p ON p.team_id=t.id WHERE t.championship_id=:'cid'::uuid GROUP BY t.id ORDER BY t.name;
SELECT m.match_number,m.stage_type,a.name team_a,b.name team_b,m.status,w.name winner FROM play_neml.matches m JOIN play_neml.teams a ON a.id=m.team_a_id JOIN play_neml.teams b ON b.id=m.team_b_id LEFT JOIN play_neml.teams w ON w.id=m.winner_team_id WHERE m.championship_id=:'cid'::uuid ORDER BY m.match_number;
SELECT i.innings_number,t.name,i.runs,i.wickets,i.legal_balls,i.status FROM play_neml.cricket_innings i JOIN play_neml.matches m ON m.id=i.match_id JOIN play_neml.teams t ON t.id=i.batting_team_id WHERE i.championship_id=:'cid'::uuid AND m.stage_type='FINAL' ORDER BY i.innings_number;
COMMIT;
