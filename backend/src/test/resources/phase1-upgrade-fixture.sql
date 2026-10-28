-- Synthetic legacy data only. The marker is not a usable password hash or credential.
INSERT INTO play_neml.users(id,email,full_name,global_role,password_hash,created_at) VALUES
 ('11111111-1111-4111-8111-111111111111','legacy@example.invalid','Legacy Fixture','USER','disabled-fixture-hash',now()),
 ('77777777-7777-4777-8777-777777777777','unrelated@example.invalid','Unrelated Fixture','USER','unchanged-fixture-hash',now());
INSERT INTO play_neml.championships(id,name,sport_type,room_code,is_public,status,lifecycle_phase,created_at)
 VALUES('22222222-2222-4222-8222-222222222222','Preserved championship','BADMINTON','UPGRADE-FIXTURE',false,'ACTIVE','AUCTION',now());
INSERT INTO play_neml.auctions(id,championship_id,name,status,created_at)
 VALUES('33333333-3333-4333-8333-333333333333','22222222-2222-4222-8222-222222222222','Preserved auction','RUNNING',now());
INSERT INTO play_neml.teams(id,championship_id,name,short_code,purse_total,purse_remaining,male_count,female_count,version)
 VALUES('44444444-4444-4444-8444-444444444444','22222222-2222-4222-8222-222222222222','Preserved Team','KEEP',1000,900,1,0,7);
INSERT INTO play_neml.players(id,championship_id,auction_id,team_id,full_name,gender,status,base_price,sold_price)
 VALUES('55555555-5555-4555-8555-555555555555','22222222-2222-4222-8222-222222222222',
 '33333333-3333-4333-8333-333333333333','44444444-4444-4444-8444-444444444444','Preserved Player','MALE','SOLD',100,100);
INSERT INTO play_neml.championship_roles(id,user_id,championship_id,team_id,role)
 VALUES('66666666-6666-4666-8666-666666666666','11111111-1111-4111-8111-111111111111',
 '22222222-2222-4222-8222-222222222222','44444444-4444-4444-8444-444444444444','TEAM_CAPTAIN');
INSERT INTO play_neml.message_tracker(msg_created_on,msg_owner,msg_app_id,msg_status,msg_parameters) VALUES
 (now(),'PLAY_NEML','SPORTS_PLAY','PENDING','{"userId":"11111111-1111-4111-8111-111111111111","temporaryPassword":"synthetic-legacy-marker","mustChangePassword":true}'),
 (now(),'OTHER_APP','OTHER_APP','PENDING','{"userId":"77777777-7777-4777-8777-777777777777","temporaryPassword":"unrelated-synthetic-marker"}');
