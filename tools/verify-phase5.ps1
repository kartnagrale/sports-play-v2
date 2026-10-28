param([switch]$SkipBuild,[switch]$SkipBackendTests,[switch]$StageBOnly)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $repoRoot
$evidence=Join-Path $repoRoot 'docs/phase-5/evidence'
New-Item -ItemType Directory -Force -Path $evidence | Out-Null
$container='play-neml-phase5-'+[guid]::NewGuid().ToString('N').Substring(0,8)
$env:DB_PASSWORD=[guid]::NewGuid().ToString('N');$env:DB_USER='phase5';$env:POSTGRES_PASSWORD=$env:DB_PASSWORD
$env:DB_URL='jdbc:postgresql://127.0.0.1:55445/phase5?currentSchema=play_neml'
$env:JWT_SECRET=[guid]::NewGuid().ToString()+[guid]::NewGuid().ToString()
$env:APP_BOOTSTRAP_ENABLED='true';$env:APP_BOOTSTRAP_EMAIL='phase5-'+[guid]::NewGuid().ToString('N')+'@example.invalid';$env:APP_BOOTSTRAP_PASSWORD=[guid]::NewGuid().ToString()
$env:CERTIFICATES_PUBLIC_ENABLED='true';$env:CERTIFICATES_PUBLIC_BASE_URL='http://localhost:53005';$env:DATA_SEED_ENABLED='false';$env:AUTH_COOKIE_SECURE='false';$env:CORS_ORIGINS='http://localhost:53005';$env:SERVER_PORT='58095';$env:SERVER_ADDRESS='127.0.0.1'
$env:LOG_PATH=Join-Path $repoRoot 'tmp/phase5-runtime-logs'
$env:PLAYER_PHOTO_DIR=Join-Path $repoRoot "tmp/phase5-photos-$container"
$env:PHASE5_API_URL='http://localhost:58095/api';$env:PHASE1_WEB_URL='http://localhost:53005';$env:UI_EVIDENCE='../docs/phase-5/evidence'
$env:NEXT_PUBLIC_BACKEND_URL='http://localhost:58095';$env:NEXT_PUBLIC_WS_URL='http://localhost:58095'
$backend=$null;$frontend=$null
$env:PHASE5_DATABASE_TESTS='true'
$env:PHASE5_STAGE_B_ONLY=if($StageBOnly){'true'}else{'false'}
try{
 docker run -d --name $container --label purpose=play-neml-phase5 -p 127.0.0.1:55445:5432 -e POSTGRES_USER=phase5 -e POSTGRES_DB=phase5 -e POSTGRES_PASSWORD `
 --mount "type=bind,source=$repoRoot/docs/sql/sports-play-complete-schema.sql,target=/docker-entrypoint-initdb.d/01-schema.sql,readonly" `
 --mount "type=bind,source=$repoRoot/docs/sql/phase1-init.sh,target=/docker-entrypoint-initdb.d/02-security.sh,readonly" `
 --mount "type=bind,source=$repoRoot/docs/sql,target=/opt/play-neml-migrations,readonly" postgres:15-alpine | Out-Null
 if($LASTEXITCODE -ne 0){throw 'Disposable database creation failed'}
 $ready=$false
 for($i=0;$i -lt 45;$i++){$v=docker exec $container psql -U phase5 -d phase5 -Atc "select count(*) from play_neml.schema_migrations where version='2026-10-28-phase5-certificate-batches'" 2>$null;if($LASTEXITCODE -eq 0 -and $v -eq '1'){$ready=$true;break};Start-Sleep -Seconds 1}
 if(!$ready){throw 'Fresh schema initialization failed'}
 & ./tools/verify-phase5-migrations.ps1 -Container $container
 $env:JAVA_HOME=Join-Path $env:USERPROFILE '.jdks/ms-17.0.20.1'
 Push-Location backend
 try{if($SkipBackendTests){./mvnw.cmd -q -DskipTests package *> "$evidence/backend-package.log"}else{./mvnw.cmd -q '-Dtest=Certificate*' verify *> "$evidence/backend-tests.log"};if($LASTEXITCODE -ne 0){throw 'Backend verification failed'}}finally{Pop-Location}
 $java=Join-Path $env:JAVA_HOME 'bin/java.exe';$jar=Join-Path $repoRoot 'backend/target/sports-platform-0.0.1-SNAPSHOT.jar'
 $backend=Start-Process $java -ArgumentList '-jar',('"'+$jar+'"') -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput "$evidence/backend.log" -RedirectStandardError "$evidence/backend-stderr.log"
 $ready=$false
 for($i=0;$i -lt 45;$i++){try{$null=Invoke-WebRequest 'http://127.0.0.1:58095/api/health' -TimeoutSec 2;$ready=$true;break}catch{Start-Sleep -Seconds 1}}
 if(!$ready){throw 'Backend failed to start'}
 Push-Location frontend
 try{
  npm run lint *> "$evidence/lint.log";if($LASTEXITCODE -ne 0){throw 'Lint failed'}
  npm run typecheck *> "$evidence/types.log";if($LASTEXITCODE -ne 0){throw 'Types failed'}
  if(!$SkipBuild){npm run build *> "$evidence/build.log";if($LASTEXITCODE -ne 0){throw 'Build failed'}}
  $next=Join-Path $repoRoot 'frontend/node_modules/next/dist/bin/next'
  $frontend=Start-Process (Get-Command node).Source -ArgumentList ('"'+$next+'"'),'start','-p','53005','-H','127.0.0.1' -WorkingDirectory (Join-Path $repoRoot 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput "$evidence/frontend.log" -RedirectStandardError "$evidence/frontend-stderr.log"
  $ready=$false
  for($i=0;$i -lt 30;$i++){try{$null=Invoke-WebRequest 'http://127.0.0.1:53005/login' -TimeoutSec 2;$ready=$true;break}catch{Start-Sleep -Seconds 1}}
  if(!$ready){throw 'Frontend failed to start'}
  npx playwright test e2e/phase5.spec.ts *> "$evidence/browser-tests.log";if($LASTEXITCODE -ne 0){throw 'Browser verification failed'}
 }finally{Pop-Location}
}finally{
 if($frontend -and !$frontend.HasExited){Stop-Process -Id $frontend.Id}
 if($backend -and !$backend.HasExited){Stop-Process -Id $backend.Id}
 docker stop $container 2>$null | Out-Null
 Write-Output "Disposable test container retained stopped: $container"
}
