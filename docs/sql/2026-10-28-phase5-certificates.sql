CREATE TABLE play_neml.certificate_approvals (
 id uuid PRIMARY KEY, championship_id uuid NOT NULL REFERENCES play_neml.championships(id),
 snapshot text NOT NULL, approved_by uuid NOT NULL REFERENCES play_neml.users(id),
 approved_at timestamptz NOT NULL, invalidated_at timestamptz,
 UNIQUE(id, championship_id)
);
CREATE UNIQUE INDEX uq_certificate_approval_active ON play_neml.certificate_approvals(championship_id) WHERE invalidated_at IS NULL;
CREATE TABLE play_neml.certificates (
 id uuid PRIMARY KEY, public_reference varchar(48) NOT NULL UNIQUE,
 championship_id uuid NOT NULL REFERENCES play_neml.championships(id),
 approval_id uuid NOT NULL, category varchar(24) NOT NULL CHECK(category IN ('WINNER','RUNNER_UP','PARTICIPATION')),
 recipient_type varchar(8) NOT NULL CHECK(recipient_type IN ('TEAM','PLAYER')), recipient_id uuid NOT NULL,
 team_id uuid, player_id uuid, snapshot text NOT NULL,
 issued_by uuid NOT NULL REFERENCES play_neml.users(id), issued_at timestamptz NOT NULL,
 request_id uuid NOT NULL, request_payload text NOT NULL,
 status varchar(12) NOT NULL DEFAULT 'ISSUED' CHECK(status IN ('ISSUED','REVOKED')),
 revoked_at timestamptz, revoked_by uuid REFERENCES play_neml.users(id), revocation_reason varchar(1000),
 reissued_from uuid REFERENCES play_neml.certificates(id),
 UNIQUE(championship_id,request_id), UNIQUE(id,championship_id), UNIQUE(reissued_from),
 FOREIGN KEY(approval_id,championship_id) REFERENCES play_neml.certificate_approvals(id,championship_id),
 FOREIGN KEY(team_id,championship_id) REFERENCES play_neml.teams(id,championship_id),
 FOREIGN KEY(player_id,championship_id) REFERENCES play_neml.players(id,championship_id),
 CHECK ((recipient_type='TEAM' AND team_id=recipient_id AND player_id IS NULL AND team_id IS NOT NULL) OR
        (recipient_type='PLAYER' AND player_id=recipient_id AND team_id IS NULL AND player_id IS NOT NULL)),
 CHECK ((status='ISSUED' AND revoked_at IS NULL AND revocation_reason IS NULL) OR
        (status='REVOKED' AND revoked_at IS NOT NULL AND length(trim(revocation_reason))>=10))
);
CREATE UNIQUE INDEX uq_certificate_active_recipient ON play_neml.certificates(championship_id,category,recipient_type,recipient_id) WHERE status='ISSUED';
CREATE INDEX idx_certificate_history ON play_neml.certificates(championship_id,issued_at DESC);
CREATE TABLE play_neml.certificate_audit (
 id uuid PRIMARY KEY, championship_id uuid NOT NULL REFERENCES play_neml.championships(id),
 certificate_id uuid, approval_id uuid,
 actor_id uuid REFERENCES play_neml.users(id), action varchar(32) NOT NULL,
 reason varchar(1000) NOT NULL, occurred_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY(certificate_id,championship_id) REFERENCES play_neml.certificates(id,championship_id),
 FOREIGN KEY(approval_id,championship_id) REFERENCES play_neml.certificate_approvals(id,championship_id)
);
-- Result corrections/deletion revoke original awards in the same transaction as the correction.
-- Never alter an issuance snapshot, public reference or original source approval.
CREATE FUNCTION play_neml.invalidate_certificate_results() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE tenant uuid;
BEGIN
 IF TG_OP='UPDATE' AND NEW.result_revision IS NOT DISTINCT FROM OLD.result_revision
    AND NEW.status IS NOT DISTINCT FROM OLD.status AND NEW.winner_team_id IS NOT DISTINCT FROM OLD.winner_team_id
    AND NEW.team_a_id IS NOT DISTINCT FROM OLD.team_a_id AND NEW.team_b_id IS NOT DISTINCT FROM OLD.team_b_id THEN RETURN NEW; END IF;
 tenant=OLD.championship_id;
 INSERT INTO play_neml.certificate_audit(id,championship_id,certificate_id,approval_id,action,reason)
 SELECT gen_random_uuid(),championship_id,id,approval_id,'SOURCE_INVALIDATED','Competition result changed; obtain a new approval and reissue'
 FROM play_neml.certificates WHERE championship_id=tenant AND status='ISSUED';
 UPDATE play_neml.certificates SET status='REVOKED',revoked_at=now(),revocation_reason='Competition result changed; obtain a new approval and reissue'
 WHERE championship_id=tenant AND status='ISSUED';
 UPDATE play_neml.certificate_approvals SET invalidated_at=now() WHERE championship_id=tenant AND invalidated_at IS NULL;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF; RETURN NEW;
END $$;
CREATE TRIGGER certificate_result_change AFTER UPDATE OR DELETE ON play_neml.matches FOR EACH ROW EXECUTE FUNCTION play_neml.invalidate_certificate_results();
-- Keep historical issuances immutable even if application code accidentally attempts a rewrite.
CREATE FUNCTION play_neml.protect_certificate_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Certificate history cannot be deleted'; END IF;
 IF (to_jsonb(NEW)-ARRAY['status','revoked_at','revoked_by','revocation_reason']) IS DISTINCT FROM
    (to_jsonb(OLD)-ARRAY['status','revoked_at','revoked_by','revocation_reason']) OR
    OLD.status='REVOKED' THEN RAISE EXCEPTION 'Certificate history is immutable'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER certificate_immutable BEFORE UPDATE OR DELETE ON play_neml.certificates FOR EACH ROW EXECUTE FUNCTION play_neml.protect_certificate_snapshot();
