-- Run this manually against the company database.
-- Every object is explicitly constrained to the play_neml schema.
BEGIN;

ALTER TABLE play_neml.tournament_settings
    ADD COLUMN IF NOT EXISTS min_male integer,
    ADD COLUMN IF NOT EXISTS min_female integer,
    ADD COLUMN IF NOT EXISTS player_base_price numeric(19,2),
    ADD COLUMN IF NOT EXISTS bid_increment numeric(19,2),
    ADD COLUMN IF NOT EXISTS timer_seconds integer;

UPDATE play_neml.tournament_settings
SET min_male = COALESCE(min_male, 9),
    min_female = COALESCE(min_female, 3),
    player_base_price = COALESCE(player_base_price, 2000000.00),
    bid_increment = COALESCE(bid_increment, 500000.00),
    timer_seconds = COALESCE(timer_seconds, 30);

ALTER TABLE play_neml.tournament_settings
    ALTER COLUMN min_male SET DEFAULT 9,
    ALTER COLUMN min_male SET NOT NULL,
    ALTER COLUMN min_female SET DEFAULT 3,
    ALTER COLUMN min_female SET NOT NULL,
    ALTER COLUMN player_base_price SET DEFAULT 2000000.00,
    ALTER COLUMN player_base_price SET NOT NULL,
    ALTER COLUMN bid_increment SET DEFAULT 500000.00,
    ALTER COLUMN bid_increment SET NOT NULL,
    ALTER COLUMN timer_seconds SET DEFAULT 30,
    ALTER COLUMN timer_seconds SET NOT NULL;

INSERT INTO play_neml.app_screens
    (id, code, label, path, icon, section_name, display_order, active)
VALUES
    ('a3d4729a-95d8-4e26-9f42-5f4e1e4ac045', 'CHAMPIONSHIP_SETTINGS', 'Championship Setup', '/admin/championship-settings', 'Settings2', 'Management', 45, true),
    ('f7149b85-c70d-4f4a-93dc-12b788221955', 'TEAM_MANAGEMENT', 'Teams & Captains', '/admin/team-management', 'UserCog', 'Management', 55, true)
ON CONFLICT (code) DO UPDATE SET
    label = EXCLUDED.label,
    path = EXCLUDED.path,
    icon = EXCLUDED.icon,
    section_name = EXCLUDED.section_name,
    display_order = EXCLUDED.display_order,
    active = EXCLUDED.active;

INSERT INTO play_neml.role_screen_mappings
    (id, role_key, screen_id, visible)
SELECT CASE screen.code
           WHEN 'CHAMPIONSHIP_SETTINGS' THEN '2f204e39-84f6-42e9-b48c-b70f1f562145'::uuid
           ELSE 'cf5a1f77-28d4-44c4-a29e-01cf1591dd55'::uuid
       END,
       'CHAMPIONSHIP_ADMIN', screen.id, true
FROM play_neml.app_screens screen
WHERE screen.code IN ('CHAMPIONSHIP_SETTINGS', 'TEAM_MANAGEMENT')
ON CONFLICT (role_key, screen_id) DO UPDATE SET visible = true;

-- These configuration screens are intentionally owned by championship admins.
UPDATE play_neml.role_screen_mappings mapping
SET visible = false
FROM play_neml.app_screens screen
WHERE mapping.screen_id = screen.id
  AND mapping.role_key = 'SUPER_ADMIN'
  AND screen.code IN ('CHAMPIONSHIP_SETTINGS', 'TEAM_MANAGEMENT');

COMMIT;

-- Verification (read-only):
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'play_neml'
  AND table_name = 'tournament_settings'
  AND column_name IN ('min_male', 'min_female', 'player_base_price', 'bid_increment', 'timer_seconds')
ORDER BY column_name;

SELECT mapping.role_key, screen.code, screen.path, mapping.visible
FROM play_neml.role_screen_mappings mapping
JOIN play_neml.app_screens screen ON screen.id = mapping.screen_id
WHERE screen.code IN ('CHAMPIONSHIP_SETTINGS', 'TEAM_MANAGEMENT')
ORDER BY mapping.role_key, screen.code;
