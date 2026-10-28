param([string]$Container='play-neml-phase1-20261027',[string]$Psql='psql')
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $repoRoot
$purpose=docker inspect --format '{{ index .Config.Labels "purpose" }}' $Container
if($LASTEXITCODE -ne 0 -or $purpose -ne 'play-neml-phase1'){throw 'Only the dedicated test container is permitted'}
$database='phase1_upgrade_'+[guid]::NewGuid().ToString('N').Substring(0,8)
docker exec $Container createdb -U phase1 $database
if($LASTEXITCODE -ne 0){throw 'Could not create a new upgrade fixture database'}
docker cp docs/sql "${Container}:/tmp/phase1-upgrade-sql"
docker cp backend/src/test/resources/phase1-upgrade-fixture.sql "${Container}:/tmp/phase1-upgrade-fixture.sql"
$evidence=Join-Path $repoRoot 'docs/phase-1/evidence'
docker exec $Container psql -U phase1 -d $database -v ON_ERROR_STOP=1 -f /tmp/phase1-upgrade-sql/sports-play-complete-schema.sql *> "$evidence/upgrade-bootstrap.log"
if($LASTEXITCODE -ne 0){throw 'Baseline schema failed'}
docker exec $Container psql -U phase1 -d $database -v ON_ERROR_STOP=1 -f /tmp/phase1-upgrade-fixture.sql *> "$evidence/upgrade-fixture.log"
if($LASTEXITCODE -ne 0){throw 'Synthetic fixture failed'}
$env:PGHOST='127.0.0.1';$env:PGPORT='55441';$env:PGUSER='phase1';$env:PGDATABASE=$database
$env:PGPASSWORD=$env:PHASE1_DB_PASSWORD
& ./tools/apply-migrations.ps1 -Psql $Psql *> "$evidence/migration-populated.log"
& ./tools/apply-migrations.ps1 -Psql $Psql *> "$evidence/migration-populated-repeat.log"
& ./tools/apply-migrations.ps1 -Psql $Psql -LegacyCredentials *> "$evidence/migration-legacy.log"
& ./tools/apply-migrations.ps1 -Psql $Psql -LegacyCredentials *> "$evidence/migration-legacy-repeat.log"
& $Psql -X -v ON_ERROR_STOP=1 -v checksum=incorrect -v version=2026-10-27-phase1-account-security -v migration_file=2026-10-27-phase1-account-security.sql -f docs/sql/phase1-apply.psql *> "$evidence/migration-checksum-rejection.log"
if($LASTEXITCODE -ne 3){throw 'Expected a checksum mismatch rejection'}
$assertions=@'
DO $$ BEGIN
 IF (SELECT count(*) FROM play_neml.users) <> 2 THEN RAISE EXCEPTION 'User count changed'; END IF;
 IF NOT EXISTS(SELECT 1 FROM play_neml.players WHERE full_name='Preserved Player' AND status='SOLD' AND sold_price=100) THEN RAISE EXCEPTION 'Roster changed'; END IF;
 IF NOT EXISTS(SELECT 1 FROM play_neml.teams WHERE purse_remaining=900 AND version=7) THEN RAISE EXCEPTION 'Purse changed'; END IF;
 IF NOT EXISTS(SELECT 1 FROM play_neml.users WHERE email='legacy@example.invalid' AND NOT account_enabled AND credential_version=1) THEN RAISE EXCEPTION 'Legacy account was not quarantined exactly once'; END IF;
 IF NOT EXISTS(SELECT 1 FROM play_neml.users WHERE email='unrelated@example.invalid' AND account_enabled AND credential_version=0 AND password_hash='unchanged-fixture-hash') THEN RAISE EXCEPTION 'Unrelated account changed'; END IF;
 IF EXISTS(SELECT 1 FROM play_neml.message_tracker WHERE msg_owner='PLAY_NEML' AND msg_parameters::jsonb ? 'temporaryPassword') THEN RAISE EXCEPTION 'Legacy secret remains'; END IF;
 IF NOT EXISTS(SELECT 1 FROM play_neml.message_tracker WHERE msg_owner='OTHER_APP' AND msg_parameters::jsonb ? 'temporaryPassword' AND msg_status='PENDING') THEN RAISE EXCEPTION 'Other application data changed'; END IF;
 IF (SELECT count(*) FROM play_neml.schema_migrations) <> 2 THEN RAISE EXCEPTION 'Migration ledger mismatch'; END IF;
END $$;
SELECT 'populated upgrade, repeat, checksum rejection, quarantine and preserved tenant data passed' AS verification;
'@
$assertions | & $Psql -X -v ON_ERROR_STOP=1 *> "$evidence/migration-validation.log"
if($LASTEXITCODE -ne 0){throw 'Migration assertions failed'}
Write-Output "Migration verification passed in new disposable database $database"
