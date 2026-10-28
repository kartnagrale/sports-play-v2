-- OPT-IN ONLY: stop the notification consumer and review affected accounts first.
-- Run with tools/apply-migrations.ps1 -LegacyCredentials after the additive migration.
-- Removes only legacy application-owned password fields; no users, teams or players are deleted.
-- Known application JSON is parsed strictly: malformed historical payloads abort the transaction.
CREATE TABLE IF NOT EXISTS play_neml.legacy_credential_remediations (
    user_id uuid PRIMARY KEY REFERENCES play_neml.users(id),
    quarantined_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO play_neml.legacy_credential_remediations(user_id)
SELECT DISTINCT u.id FROM play_neml.message_tracker m
JOIN play_neml.users u ON u.id::text=(m.msg_parameters::jsonb ->> 'userId')
WHERE m.msg_owner='PLAY_NEML' AND m.msg_app_id='SPORTS_PLAY'
 AND m.msg_parameters::jsonb ? 'temporaryPassword' AND u.global_role <> 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
UPDATE play_neml.users u SET account_enabled=false,credential_version=credential_version+1
WHERE EXISTS(SELECT 1 FROM play_neml.legacy_credential_remediations r WHERE r.user_id=u.id);
UPDATE play_neml.message_tracker m
SET msg_parameters=((m.msg_parameters::jsonb - 'temporaryPassword' - 'mustChangePassword')
       || '{"activationRequired":true,"setupInstructions":"Contact the championship administrator for a new setup link"}'::jsonb)::text,
    msg_status=CASE WHEN m.msg_status='PENDING' THEN 'CANCELLED' ELSE m.msg_status END
WHERE m.msg_owner='PLAY_NEML' AND m.msg_app_id='SPORTS_PLAY'
 AND m.msg_parameters::jsonb ? 'temporaryPassword';
