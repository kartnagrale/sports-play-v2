# Requires the completed isolated Docker verification fixture; never targets company storage.
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Set-Location $root
$project=(Get-Content tmp/docker-project.txt -Raw).Trim()
if($project -notmatch '^playneml65-[a-f0-9]{8}$'){throw 'Unexpected disposable project'}
$settings=@{}
foreach($line in Get-Content tmp/docker-test.env){if($line -match '^([^#=]+)=(.*)$'){$settings[$Matches[1]]=$Matches[2]}}
if($settings.COMPOSE_PROJECT_NAME -ne $project){throw 'Project mismatch'}
function dc {docker compose --env-file tmp/docker-test.env -p $project @args;if($LASTEXITCODE -ne 0){throw 'Recovery Compose command failed'}}
$backup=Get-ChildItem tmp -Directory -Filter 'docker-backup-*'|Sort-Object LastWriteTime -Descending|Select-Object -First 1
if(!$backup -or !(Test-Path (Join-Path $backup.FullName 'database.dump'))){throw 'Matched backup missing'}
$suffix=[guid]::NewGuid().ToString('N').Substring(0,8)
$recoveryEnv=Join-Path $root "tmp/recovery-$suffix.env"
[IO.File]::WriteAllLines($recoveryEnv,@("DB_USER=$($settings.DB_USER)","DB_PASSWORD=$($settings.DB_PASSWORD)","JWT_SECRET=$($settings.JWT_SECRET)","CORS_ORIGINS=$($settings.PUBLIC_ORIGIN)"),[Text.UTF8Encoding]::new($false))
$database='recovery_'+$suffix;$volume=$project+'_recovered_photos_'+$suffix;$container=$project+'-recovery-'+$suffix
if((docker volume ls -q --filter "name=^$volume$") -or (docker ps -aq --filter "name=^/$container$")){throw 'Recovery resource already exists; refusing to adopt storage'}
try{
 dc up -d --wait db
 $db=dc ps -q db
 if((docker inspect --format '{{ index .Config.Labels "purpose" }}' $db) -ne 'play-neml-phase6.5'){throw 'Unexpected database label'}
 docker cp (Join-Path $backup.FullName 'database.dump') "${db}:/tmp/recovery-$suffix.dump"
 if($LASTEXITCODE -ne 0){throw 'Backup copy failed'}
 dc exec -T db createdb -U $settings.POSTGRES_USER $database
 dc exec -T db pg_restore --exit-on-error --no-owner --no-privileges -U $settings.POSTGRES_USER -d $database "/tmp/recovery-$suffix.dump"
 node tools/check-release-recovery.mjs database $project $database
 if($LASTEXITCODE -ne 0){throw 'Recovered table comparison failed'}
 dc exec -T db psql -X -U $settings.POSTGRES_USER -d $database -v ON_ERROR_STOP=1 -v "app_user=$($settings.DB_USER)" -f /opt/play-neml/grants.psql
 docker volume create --label purpose=play-neml-phase7 $volume | Out-Null
 docker run --rm --user 0:0 -v "${volume}:/restore" -v "$($backup.FullName)/player-photos:/backup:ro" "$project-db:phase6.5" sh -c 'cp -a /backup/. /restore/ && chown -R 10001:10001 /restore'
 if($LASTEXITCODE -ne 0){throw 'Photo restore failed'}
 docker run -d --name $container --label purpose=play-neml-phase7 --network "${project}_database" --env-file $recoveryEnv -e "DB_URL=jdbc:postgresql://db:5432/${database}?currentSchema=play_neml" -e APP_BOOTSTRAP_ENABLED=false -e AUTH_COOKIE_SECURE=true -e CERTIFICATES_PUBLIC_ENABLED=false -e DATA_SEED_ENABLED=false -e PLAYER_PHOTO_DIR=/app/var/player-photos -e LOG_PATH=/app/logs --read-only --cap-drop ALL --security-opt no-new-privileges --memory 768m --tmpfs /tmp:rw,nosuid,size=128m --tmpfs /app/logs:rw,nosuid,uid=10001,gid=10001,size=64m -v "${volume}:/app/var/player-photos" "$project-backend:phase6.5" | Out-Null
 if($LASTEXITCODE -ne 0){throw 'Recovered backend creation failed'}
 $ready=$false
 for($i=0;$i -lt 90;$i++){docker exec $container curl --fail --silent --max-time 2 http://127.0.0.1:8001/api/health *> $null;if($LASTEXITCODE -eq 0){$ready=$true;break};Start-Sleep -Seconds 1}
 if(!$ready){throw 'Recovered backend startup failed'}
 docker run --rm --network "${project}_database" --entrypoint node -v "${root}/tmp:/evidence:ro" -v "${root}/tools:/checks:ro" "$project-frontend:phase6.5" /checks/check-release-recovery.mjs api "http://${container}:8001"
 if($LASTEXITCODE -ne 0){throw 'Recovered authorized API verification failed'}
 $expected=Get-Content tmp/docker-persistence-before.json -Raw|ConvertFrom-Json
 $actual=docker exec $container sh -c "find /app/var/player-photos -type f -name '*.jpg' -exec sha256sum {} +"
 foreach($property in $expected.volumePhotos.PSObject.Properties){if(!($actual|Where-Object {$_ -like "$($property.Value)*$($property.Name)"})){throw 'Recovered photo differs'}}
 @{recoveredDatabaseCreatedSeparately=$true;restoredPhotosInNewVolume=$true;all43TableHashesMatch=$true;photoHashesMatch=$true;ownership='10001:10001';recoveredBackendHealthy=$true;authorizedRequestsPassed=$true;pdfDownloadPassed=$true;validAndRevokedStatesPreserved=$true;publicVerificationDisabled=$true;existingStorageUntouched=$true}|ConvertTo-Json|Set-Content tmp/release-recovery.json
}finally{
 if(docker ps -aq --filter "name=^/$container$"){docker stop $container|Out-Null}
 dc stop | Out-Null
}
