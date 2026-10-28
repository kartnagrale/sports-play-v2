param([Parameter(Mandatory)][string]$Container)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
$purpose=docker inspect --format '{{ index .Config.Labels "purpose" }}' $Container
if($LASTEXITCODE -ne 0 -or $purpose -ne 'play-neml-phase5'){throw 'Only a disposable Phase 5 container is permitted'}
$database='phase5_upgrade_'+[guid]::NewGuid().ToString('N').Substring(0,8)
$evidence=Join-Path $repoRoot 'docs/phase-5/evidence'
docker exec $Container createdb -U phase5 $database
docker exec $Container psql -U phase5 -d $database -v ON_ERROR_STOP=1 -f /opt/play-neml-migrations/sports-play-complete-schema.sql *> "$evidence/upgrade-bootstrap.log"
if($LASTEXITCODE -ne 0){throw 'Baseline schema failed'}
docker cp "$repoRoot/backend/src/test/resources/phase1-upgrade-fixture.sql" "${Container}:/tmp/phase5-upgrade-fixture.sql"
docker exec $Container psql -U phase5 -d $database -v ON_ERROR_STOP=1 -f /tmp/phase5-upgrade-fixture.sql *> "$evidence/upgrade-fixture.log"
if($LASTEXITCODE -ne 0){throw 'Populated fixture failed'}
$versions=@('2026-10-27-phase1-account-security','2026-10-28-phase4-player-import')
if(Test-Path (Join-Path $repoRoot 'docs/sql/2026-10-28-phase4-player-photo.sql')){$versions+='2026-10-28-phase4-player-photo'}
$versions+=@('2026-10-28-phase5-certificates','2026-10-28-phase5-certificate-navigation','2026-10-28-phase5-certificate-batches')
foreach($pass in 1,2){foreach($version in $versions){
 $checksum=(Get-FileHash -Algorithm SHA256 -LiteralPath "$repoRoot/docs/sql/$version.sql").Hash.ToLowerInvariant()
 docker exec $Container psql -U phase5 -d $database -v ON_ERROR_STOP=1 -v "version=$version" -v "checksum=$checksum" -v "migration_file=$version.sql" -f /opt/play-neml-migrations/phase1-apply.psql >> "$evidence/migration-pass-$pass.log"
 if($LASTEXITCODE -ne 0){throw 'Tracked migration failed'}
}}
docker exec $Container psql -U phase5 -d $database -v ON_ERROR_STOP=1 -v version=2026-10-28-phase5-certificate-batches -v checksum=incorrect -v migration_file=2026-10-28-phase5-certificate-batches.sql -f /opt/play-neml-migrations/phase1-apply.psql *> "$evidence/migration-checksum-rejection.log"
if($LASTEXITCODE -eq 0){throw 'Checksum mismatch must fail'}
$assertions=@'
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM play_neml.players WHERE full_name='Preserved Player' AND status='SOLD' AND sold_price=100 AND team_id='44444444-4444-4444-8444-444444444444' AND player_reference IS NULL) THEN RAISE EXCEPTION 'Existing player changed'; END IF;
 IF NOT EXISTS(SELECT 1 FROM play_neml.teams WHERE purse_remaining=900 AND version=7) THEN RAISE EXCEPTION 'Existing purse changed'; END IF;
 IF (SELECT count(*) FROM play_neml.players) <> 1 THEN RAISE EXCEPTION 'Player count changed'; END IF;
 IF (SELECT count(*) FROM play_neml.player_imports) <> 0 THEN RAISE EXCEPTION 'Unexpected import'; END IF;
END $$;
SELECT 'populated upgrade, repeat, checksum rejection and preserved sold player/team/purse passed';
'@
$assertions | docker exec -i $Container psql -U phase5 -d $database -v ON_ERROR_STOP=1 *> "$evidence/migration-validation.log"
if($LASTEXITCODE -ne 0){throw 'Migration assertions failed'}
Write-Output "Migration verification passed in disposable database $database"
