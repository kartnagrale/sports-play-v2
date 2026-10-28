-- Additive migration; run through tools/apply-migrations.ps1 as the database owner.
-- Existing accounts remain enabled. A changed JWT contract requires sign-in again.
ALTER TABLE play_neml.users ADD COLUMN IF NOT EXISTS account_enabled boolean NOT NULL DEFAULT true;
ALTER TABLE play_neml.users ADD COLUMN IF NOT EXISTS credential_version integer NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS play_neml.account_activations (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES play_neml.users(id),
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    credential_version integer NOT NULL,
    used_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_activation_user ON play_neml.account_activations(user_id);
