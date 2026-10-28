CREATE TABLE play_neml.certificate_batches (
 id uuid PRIMARY KEY, championship_id uuid NOT NULL REFERENCES play_neml.championships(id),
 request_id uuid NOT NULL, issued_by uuid NOT NULL REFERENCES play_neml.users(id), request_payload text NOT NULL,
 certificate_ids text NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(championship_id,request_id)
);
-- New fixtures after approval also invalidate the previously approved scope.
CREATE FUNCTION play_neml.invalidate_certificate_scope() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 INSERT INTO play_neml.certificate_audit(id,championship_id,certificate_id,approval_id,action,reason)
 SELECT gen_random_uuid(),championship_id,id,approval_id,'SOURCE_INVALIDATED','Competition scope changed; approve results and reissue certificates'
 FROM play_neml.certificates WHERE championship_id=NEW.championship_id AND status='ISSUED';
 UPDATE play_neml.certificates SET status='REVOKED',revoked_at=now(),revocation_reason='Competition scope changed; approve results and reissue certificates'
 WHERE championship_id=NEW.championship_id AND status='ISSUED';
 UPDATE play_neml.certificate_approvals SET invalidated_at=now() WHERE championship_id=NEW.championship_id AND invalidated_at IS NULL;
 RETURN NEW;
END $$;
CREATE TRIGGER certificate_scope_change AFTER INSERT ON play_neml.matches FOR EACH ROW EXECUTE FUNCTION play_neml.invalidate_certificate_scope();
CREATE FUNCTION play_neml.protect_certificate_audit() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Certificate audit history is append only'; END $$;
CREATE TRIGGER certificate_audit_immutable BEFORE UPDATE OR DELETE ON play_neml.certificate_audit FOR EACH ROW EXECUTE FUNCTION play_neml.protect_certificate_audit();
CREATE FUNCTION play_neml.protect_certificate_approval() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Certificate approval history cannot be deleted'; END IF;
 IF (to_jsonb(NEW)-'invalidated_at') IS DISTINCT FROM (to_jsonb(OLD)-'invalidated_at') OR OLD.invalidated_at IS NOT NULL THEN
 RAISE EXCEPTION 'Certificate approval snapshots are immutable'; END IF; RETURN NEW;
END $$;
CREATE TRIGGER certificate_approval_immutable BEFORE UPDATE OR DELETE ON play_neml.certificate_approvals FOR EACH ROW EXECUTE FUNCTION play_neml.protect_certificate_approval();
