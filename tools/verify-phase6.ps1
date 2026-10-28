param([switch]$SkipBuild,[switch]$SkipBackendTests,[switch]$KeepRunning)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $repoRoot
if(!(Test-Path tmp/phase6-runtime.json)){& ./tools/phase6-runtime.ps1 -Action Init}
$state=Get-Content tmp/phase6-runtime.json -Raw|ConvertFrom-Json
if((docker inspect --format '{{ index .Config.Labels "purpose" }}' $state.container) -ne 'play-neml-phase6'){throw 'Only the labeled disposable Phase 6 database is permitted'}
$evidence=Join-Path $repoRoot 'docs/phase-6/evidence'
$env:JAVA_HOME=Join-Path $env:USERPROFILE '.jdks/ms-17.0.20.1'
$env:DB_URL='jdbc:postgresql://127.0.0.1:55446/phase6?currentSchema=play_neml';$env:DB_USER='phase6';$env:DB_PASSWORD=$state.password;$env:JWT_SECRET=$state.jwt
$env:PHASE6_DATABASE_TESTS='true';$env:PHASE5_DATABASE_TESTS='true';$env:PHASE4_DATABASE_TESTS='true';$env:PLAYER_PHOTO_DIR=Join-Path $repoRoot 'tmp/phase6-test-photos'
$env:APP_BOOTSTRAP_EMAIL=$state.bootstrapEmail;$env:APP_BOOTSTRAP_PASSWORD=$state.bootstrapPassword
$env:PHASE6_API_URL='http://localhost:58096/api';$env:PHASE3_API_URL=$env:PHASE6_API_URL;$env:PHASE1_WEB_URL='http://localhost:53006';$env:UI_EVIDENCE='../docs/phase-6/evidence'
$env:NEXT_PUBLIC_BACKEND_URL='http://localhost:58096';$env:NEXT_PUBLIC_WS_URL='http://localhost:58096'
$frontend=$null
try{
 docker start $state.container|Out-Null
 Push-Location backend
 try{if(!$SkipBackendTests){./mvnw.cmd -q verify *> "$evidence/final-backend.log";if($LASTEXITCODE -ne 0){throw 'Backend verification failed'}}}finally{Pop-Location}
 & ./tools/phase6-runtime.ps1 -Action Start
 Push-Location frontend
 try{
  node --test e2e/auction-model.test.cjs e2e/live-refresh.test.cjs e2e/portal-presentation.test.cjs *> "$evidence/final-model.log";if($LASTEXITCODE -ne 0){throw 'Model tests failed'}
  npm run lint *> "$evidence/final-lint.log";if($LASTEXITCODE -ne 0){throw 'Lint failed'}
  npm run typecheck *> "$evidence/final-types.log";if($LASTEXITCODE -ne 0){throw 'TypeScript failed'}
  if(!$SkipBuild){npm run build *> "$evidence/final-build.log";if($LASTEXITCODE -ne 0){throw 'Build failed'}}
  $next=Join-Path $repoRoot 'frontend/node_modules/next/dist/bin/next'
  $frontend=Start-Process (Get-Command node).Source -ArgumentList ('"'+$next+'"'),'start','-p','53006','-H','127.0.0.1' -WorkingDirectory (Join-Path $repoRoot 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput "$evidence/frontend.log" -RedirectStandardError "$evidence/frontend-stderr.log"
  $ready=$false;for($i=0;$i -lt 30;$i++){try{$null=Invoke-WebRequest 'http://127.0.0.1:53006/login' -TimeoutSec 1;$ready=$true;break}catch{Start-Sleep -Milliseconds 500}}
  if(!$ready){throw 'Frontend did not become healthy'}
  npx playwright test e2e/phase6.spec.ts e2e/phase3.spec.ts *> "$evidence/final-browser.log";if($LASTEXITCODE -ne 0){throw 'Browser verification failed'}
 }finally{Pop-Location}
 node tools/seed-phase6-demo.mjs *> "$evidence/demo-seed.log";if($LASTEXITCODE -ne 0){throw 'Interactive demo setup failed'}
 $env:PHASE6_DEMO_TESTS='true'
 Push-Location frontend
 try{npx playwright test e2e/phase6-demo.spec.ts *> "$evidence/final-demo-browser.log";if($LASTEXITCODE -ne 0){throw 'Prepared demo verification failed'}}finally{Pop-Location}
 if($KeepRunning){Write-Output 'Isolated demo remains running on http://localhost:53006; account details are only in tmp/phase6-demo-access.json'}
}finally{
 if(!$KeepRunning){if($frontend -and !$frontend.HasExited){Stop-Process -Id $frontend.Id};& ./tools/phase6-runtime.ps1 -Action Stop}
}
