param([ValidateSet('Init','Start','Stop')][string]$Action='Init')
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $repoRoot
$stateFile=Join-Path $repoRoot 'tmp/phase6-runtime.json'
$evidence=Join-Path $repoRoot 'docs/phase-6/evidence'
New-Item -ItemType Directory -Force $evidence,(Join-Path $repoRoot 'tmp')|Out-Null
if($Action -eq 'Init'){
 if(Test-Path $stateFile){throw 'An isolated Phase 6 state already exists. Use Start or Stop; never overwrite an existing environment.'}
 $container='play-neml-phase6-'+[guid]::NewGuid().ToString('N').Substring(0,8)
 $state=@{container=$container;password=[guid]::NewGuid().ToString('N');jwt=[guid]::NewGuid().ToString()+[guid]::NewGuid().ToString();bootstrapEmail='phase6-'+[guid]::NewGuid().ToString('N')+'@example.invalid';bootstrapPassword=[guid]::NewGuid().ToString();backendPid=0}
 $env:POSTGRES_PASSWORD=$state.password
 docker run -d --name $container --label purpose=play-neml-phase6 -p 127.0.0.1:55446:5432 -e POSTGRES_USER=phase6 -e POSTGRES_DB=phase6 -e POSTGRES_PASSWORD `
 --mount "type=bind,source=$repoRoot/docs/sql/sports-play-complete-schema.sql,target=/docker-entrypoint-initdb.d/01-schema.sql,readonly" `
 --mount "type=bind,source=$repoRoot/docs/sql/phase1-init.sh,target=/docker-entrypoint-initdb.d/02-security.sh,readonly" `
 --mount "type=bind,source=$repoRoot/docs/sql,target=/opt/play-neml-migrations,readonly" postgres:15-alpine|Out-Null
 if($LASTEXITCODE -ne 0){throw 'Disposable database creation failed'}
 $state|ConvertTo-Json|Set-Content $stateFile
 Write-Output "Created new disposable PostgreSQL: $container"
 exit
}
$state=Get-Content $stateFile -Raw|ConvertFrom-Json
if((docker inspect --format '{{ index .Config.Labels "purpose" }}' $state.container) -ne 'play-neml-phase6'){throw 'Unexpected database label'}
if($state.backendPid){$proc=Get-CimInstance Win32_Process -Filter "ProcessId=$($state.backendPid)";if($proc){if($proc.CommandLine -notlike '*phase6-runtime-backend.jar*'){throw 'Unexpected process; refusing to stop it'};Stop-Process -Id $state.backendPid}}
if($Action -eq 'Stop'){docker stop $state.container|Out-Null;Write-Output 'Disposable services stopped; database retained';exit}
docker start $state.container|Out-Null
$env:DB_URL='jdbc:postgresql://127.0.0.1:55446/phase6?currentSchema=play_neml';$env:DB_USER='phase6';$env:DB_PASSWORD=$state.password;$env:JWT_SECRET=$state.jwt
$env:APP_BOOTSTRAP_ENABLED='true';$env:APP_BOOTSTRAP_EMAIL=$state.bootstrapEmail;$env:APP_BOOTSTRAP_PASSWORD=$state.bootstrapPassword
$env:AUTH_COOKIE_SECURE='false';$env:CORS_ORIGINS='http://localhost:53006';$env:SERVER_PORT='58096';$env:SERVER_ADDRESS='127.0.0.1';$env:DATA_SEED_ENABLED='false'
$env:LOG_PATH=Join-Path $repoRoot 'tmp/phase6-logs';$env:PLAYER_PHOTO_DIR=Join-Path $repoRoot 'tmp/phase6-photos'
$env:CERTIFICATES_PUBLIC_ENABLED='true';$env:CERTIFICATES_PUBLIC_BASE_URL='http://localhost:53006'
Copy-Item backend/target/sports-platform-0.0.1-SNAPSHOT.jar tmp/phase6-runtime-backend.jar -Force
$jar=Join-Path $repoRoot 'tmp/phase6-runtime-backend.jar'
$backend=Start-Process (Join-Path $env:USERPROFILE '.jdks/ms-17.0.20.1/bin/java.exe') -ArgumentList '-Xmx512m','-jar',('"'+$jar+'"') -WindowStyle Hidden -PassThru -RedirectStandardOutput "$evidence/backend.log" -RedirectStandardError "$evidence/backend-stderr.log"
$state.backendPid=$backend.Id;$state|ConvertTo-Json|Set-Content $stateFile
$ready=$false
for($i=0;$i -lt 50;$i++){try{$null=Invoke-WebRequest 'http://127.0.0.1:58096/api/health' -TimeoutSec 1;$ready=$true;break}catch{Start-Sleep -Milliseconds 800}}
if(!$ready){throw 'Disposable backend did not become healthy; inspect Phase 6 logs'}
Write-Output 'Started isolated backend on 58096 (512 MiB heap); credentials remain in ignored local state'
