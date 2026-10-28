param([switch]$SkipBuild,[switch]$PortalOnly,[switch]$Auction,[switch]$AuctionOnly)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $repoRoot
$evidence=Join-Path $repoRoot 'docs/phase-3/evidence'
New-Item -ItemType Directory -Force -Path $evidence | Out-Null
$container='play-neml-phase3-'+[guid]::NewGuid().ToString('N').Substring(0,8)
$env:DB_PASSWORD=[guid]::NewGuid().ToString('N')
$env:DB_USER='phase2'
$env:DB_URL='jdbc:postgresql://127.0.0.1:55442/phase2?currentSchema=play_neml'
$env:POSTGRES_PASSWORD=$env:DB_PASSWORD
$env:JWT_SECRET=[guid]::NewGuid().ToString()+[guid]::NewGuid().ToString()
$env:APP_BOOTSTRAP_ENABLED='true'
$env:APP_BOOTSTRAP_EMAIL='phase3-'+[guid]::NewGuid().ToString('N')+'@example.invalid'
$env:APP_BOOTSTRAP_PASSWORD=[guid]::NewGuid().ToString()
$env:DATA_SEED_ENABLED='false'
$env:AUTH_COOKIE_SECURE='false'
$env:CORS_ORIGINS='http://localhost:53002'
$env:SERVER_PORT='58093'
$env:SERVER_ADDRESS='127.0.0.1'
$env:LOG_PATH=Join-Path $repoRoot 'tmp/phase3-runtime-logs'
$env:PHASE2_API_URL='http://localhost:58093/api'
$env:PHASE1_WEB_URL='http://localhost:53002'
$env:NEXT_PUBLIC_BACKEND_URL='http://localhost:58093'
$env:NEXT_PUBLIC_WS_URL='http://localhost:58093'
$env:UI_EVIDENCE='../docs/phase-3/evidence'
$env:PHASE3_API_URL='http://localhost:58093/api'
$backend=$null; $frontend=$null
try {
    # New container, new database; do not start, reset or migrate an existing database.
    docker run -d --name $container --label purpose=play-neml-phase2 -p 127.0.0.1:55442:5432 -e POSTGRES_USER=phase2 -e POSTGRES_DB=phase2 -e POSTGRES_PASSWORD `
      --mount "type=bind,source=$repoRoot/docs/sql/sports-play-complete-schema.sql,target=/docker-entrypoint-initdb.d/01-schema.sql,readonly" `
      --mount "type=bind,source=$repoRoot/docs/sql/phase1-init.sh,target=/docker-entrypoint-initdb.d/02-security.sh,readonly" `
      --mount "type=bind,source=$repoRoot/docs/sql,target=/opt/play-neml-migrations,readonly" postgres:15-alpine | Out-Null
    if($LASTEXITCODE -ne 0){throw 'Could not create disposable database'}
    $ready=$false
    for($i=0;$i -lt 40;$i++) {
      $value=docker exec $container psql -U phase2 -d phase2 -Atc 'SELECT count(*) FROM play_neml.schema_migrations' 2>$null
      if($LASTEXITCODE -eq 0 -and $value -eq '1'){$ready=$true;break}; Start-Sleep -Seconds 1
    }
    if(!$ready){throw 'Disposable schema initialization failed'}
    $java=Join-Path $env:USERPROFILE '.jdks/ms-17.0.20.1/bin/java.exe'
    $jar=Join-Path $repoRoot 'backend/target/sports-platform-0.0.1-SNAPSHOT.jar'
    $backend=Start-Process -FilePath $java -ArgumentList '-jar',('"'+$jar+'"') -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput "$evidence/backend.log" -RedirectStandardError "$evidence/backend-stderr.log"
    $ready=$false
    for($i=0;$i -lt 40;$i++){try{$null=Invoke-WebRequest 'http://127.0.0.1:58093/api/health' -TimeoutSec 2;$ready=$true;break}catch{Start-Sleep -Seconds 1}}
    if(!$ready){throw 'Backend failed to start'}
    Push-Location frontend
    try {
      if(!$SkipBuild){npm run build *> "$evidence/build.log";if($LASTEXITCODE -ne 0){throw 'Build failed'}}
      node e2e/build-ui-harness.cjs *> "$evidence/harness-build.log"
      if($LASTEXITCODE -ne 0){throw 'Component harness build failed'}
      $next=Join-Path $repoRoot 'frontend/node_modules/next/dist/bin/next'
      $frontend=Start-Process -FilePath (Get-Command node).Source -ArgumentList ('"'+$next+'"'),'start','-p','53002','-H','127.0.0.1' -WorkingDirectory (Join-Path $repoRoot 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput "$evidence/frontend.log" -RedirectStandardError "$evidence/frontend-stderr.log"
      $ready=$false
      for($i=0;$i -lt 30;$i++){try{$null=Invoke-WebRequest 'http://127.0.0.1:53002/login' -TimeoutSec 2;$ready=$true;break}catch{Start-Sleep -Seconds 1}}
      if(!$ready){throw 'Frontend failed to start'}
      if($PortalOnly){npx playwright test e2e/phase2.spec.ts --grep 'platform:|admin:|captain:|viewer:' *> "$evidence/stage-a-browser.log"}
      elseif($AuctionOnly){npx playwright test e2e/phase3.spec.ts *> "$evidence/auction-browser-tests.log"}
      elseif($Auction){npx playwright test e2e/phase2.spec.ts e2e/phase3.spec.ts *> "$evidence/browser-tests.log"}
      else{npx playwright test e2e/phase2.spec.ts *> "$evidence/browser-tests.log"}
      if($LASTEXITCODE -ne 0){throw 'Browser verification failed; inspect docs/phase-3/evidence/browser-tests.log'}
    } finally { Pop-Location }
} finally {
    if($frontend -and !$frontend.HasExited){Stop-Process -Id $frontend.Id}
    if($backend -and !$backend.HasExited){Stop-Process -Id $backend.Id}
    docker stop $container 2>$null | Out-Null
    Write-Output "Disposable test container retained stopped: $container"
}
