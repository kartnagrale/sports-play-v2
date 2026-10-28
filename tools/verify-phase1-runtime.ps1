param([switch]$Browser)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $repoRoot
if($env:PHASE1_DB_URL -notmatch '^jdbc:postgresql://(127\.0\.0\.1|localhost):55441/phase1(\?.*)?$'){
    throw 'This runner requires the dedicated Phase 1 database on localhost:55441/phase1'
}
$evidence=Join-Path $repoRoot 'docs/phase-1/evidence'
New-Item -ItemType Directory -Force -Path $evidence | Out-Null
$env:DB_URL=$env:PHASE1_DB_URL
$env:DB_USER='phase1'
$env:DB_PASSWORD=$env:PHASE1_DB_PASSWORD
$env:JWT_SECRET=[guid]::NewGuid().ToString()+[guid]::NewGuid().ToString()
$env:DATA_SEED_ENABLED='false'
$env:APP_BOOTSTRAP_ENABLED='true'
$env:APP_BOOTSTRAP_EMAIL='bootstrap-'+[guid]::NewGuid().ToString()+'@example.invalid'
$env:APP_BOOTSTRAP_PASSWORD=[guid]::NewGuid().ToString()
$env:AUTH_COOKIE_SECURE='false'
$env:CORS_ORIGINS='http://localhost:53001,http://localhost:3000'
$env:SERVER_PORT='58092'
$env:SERVER_ADDRESS='127.0.0.1'
$env:LOG_PATH=Join-Path $repoRoot 'tmp/phase1-runtime-logs'
$env:PHASE1_API_URL='http://localhost:58092/api'
$env:PHASE1_WEB_URL='http://localhost:53001'
$env:NEXT_PUBLIC_BACKEND_URL='http://localhost:58092'
$env:NEXT_PUBLIC_WS_URL='http://localhost:58092'
$java=Join-Path $env:JAVA_HOME 'bin/java.exe'
if(!(Test-Path -LiteralPath $java)){throw 'Set JAVA_HOME to Java 17 first'}
$jar=Join-Path $repoRoot 'backend/target/sports-platform-0.0.1-SNAPSHOT.jar'
$backend=Start-Process -FilePath $java -ArgumentList '-jar',('"'+$jar+'"') -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput "$evidence/backend-runtime.log" -RedirectStandardError "$evidence/backend-runtime-stderr.log"
$frontend=$null
try{
    $ready=$false
    for($i=0;$i -lt 45;$i++){
        try{$null=Invoke-WebRequest -Uri 'http://127.0.0.1:58092/api/health' -TimeoutSec 2;$ready=$true;break}catch{Start-Sleep -Seconds 1}
    }
    if(!$ready){throw 'Backend failed to start; inspect runtime evidence'}
    node tools/runtime-security-checks.mjs
    $runtimeExit=$LASTEXITCODE
    if($Browser){
        Push-Location frontend
        try{
            npm run build *> "$evidence/frontend-runtime-build.log"
            if($LASTEXITCODE -ne 0){throw 'Frontend runtime build failed'}
            $next=Join-Path $repoRoot 'frontend/node_modules/next/dist/bin/next'
            $frontend=Start-Process -FilePath (Get-Command node).Source -ArgumentList ('"'+$next+'"'),'start','-p','53001','-H','127.0.0.1' -WorkingDirectory (Join-Path $repoRoot 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput "$evidence/frontend-runtime.log" -RedirectStandardError "$evidence/frontend-runtime-stderr.log"
            $webReady=$false
            for($i=0;$i -lt 20;$i++){
                try{$null=Invoke-WebRequest -Uri 'http://127.0.0.1:53001/login' -TimeoutSec 2;$webReady=$true;break}catch{Start-Sleep -Seconds 1}
            }
            if(!$webReady){throw 'Frontend failed to start'}
            npx playwright test *> "$evidence/browser-tests.log"
            if($LASTEXITCODE -ne 0){throw 'Browser verification failed'}
        }finally{Pop-Location}
    }
    if($runtimeExit -ne 0){throw 'API/WebSocket verification failed'}
}finally{
    if($null -ne $frontend -and !$frontend.HasExited){Stop-Process -Id $frontend.Id}
    if(!$backend.HasExited){Stop-Process -Id $backend.Id}
}
