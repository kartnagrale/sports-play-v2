param([string]$Psql='psql',[switch]$LegacyCredentials,[ValidateSet('2026-10-28-phase4-player-import','2026-10-28-phase4-player-photo','2026-10-28-phase5-certificates','2026-10-28-phase5-certificate-navigation','2026-10-28-phase5-certificate-batches','2026-10-29-npl-auction-rules')][string]$Migration)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
$version=if($LegacyCredentials){'2026-10-27-phase1-legacy-credentials'}else{'2026-10-27-phase1-account-security'}
if($Migration){$version=$Migration}
$fileName=$version+'.sql'
$migrationPath=Join-Path $repoRoot ('docs/sql/'+$fileName)
$checksum=(Get-FileHash -Algorithm SHA256 -LiteralPath $migrationPath).Hash.ToLowerInvariant()
$driver=Join-Path $repoRoot 'docs/sql/phase1-apply.psql'
# Standard PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD or .pgpass supply the connection.
& $Psql -X -v ON_ERROR_STOP=1 -v "checksum=$checksum" -v "version=$version" -v "migration_file=$fileName" -f $driver
if($LASTEXITCODE -ne 0){throw 'Migration failed; transaction was not committed'}
