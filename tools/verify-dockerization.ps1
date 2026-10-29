param([switch]$SkipBuild,[switch]$SkipBrowser,[switch]$KeepRunning)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $repoRoot
New-Item -ItemType Directory -Force tmp,tmp/docker-evidence | Out-Null
$statePath=Join-Path $repoRoot 'tmp/docker-project.txt'
$configPath=Join-Path $repoRoot 'tmp/docker-test.env'
if(!(Test-Path $statePath)){
 if(Test-Path $configPath){throw 'Private configuration exists without ownership state; inspect manually'}
 $project='playneml65-'+[guid]::NewGuid().ToString('N').Substring(0,8)
 if((docker ps -aq --filter "label=com.docker.compose.project=$project") -or (docker volume ls -q --filter "label=com.docker.compose.project=$project")){throw 'Random project already exists; refusing to adopt data'}
 @{containers=@(docker ps -aq);volumes=@(docker volume ls -q)} | ConvertTo-Json | Set-Content tmp/docker-before.json
 function New-Secret { [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)) }
 $values=@("COMPOSE_PROJECT_NAME=$project",'POSTGRES_DB=sports_play','POSTGRES_USER=sports_play_owner',('POSTGRES_PASSWORD='+(New-Secret)),'DB_USER=sports_play_app',('DB_PASSWORD='+(New-Secret)),('JWT_SECRET='+(New-Secret)),'HTTP_PORT=53165','PUBLIC_ORIGIN=http://localhost:53165','PUBLIC_SCHEME=http','AUTH_COOKIE_SECURE=false','APP_BOOTSTRAP_ENABLED=true',('APP_BOOTSTRAP_EMAIL=docker-'+[guid]::NewGuid().ToString('N')+'@example.invalid'),('APP_BOOTSTRAP_PASSWORD='+(New-Secret)),'CERTIFICATES_PUBLIC_ENABLED=true','CERTIFICATES_PUBLIC_BASE_URL=http://localhost:53165')
 [IO.File]::WriteAllLines($configPath,$values,[Text.UTF8Encoding]::new($false))
 $project | Set-Content $statePath
}
$project=(Get-Content $statePath -Raw).Trim()
if($project -notmatch '^playneml65-[a-f0-9]{8}$'){throw 'Unexpected project; refusing to operate'}
$settings=@{}
foreach($line in Get-Content $configPath){if($line -match '^([^#=]+)=(.*)$'){$settings[$Matches[1]]=$Matches[2]}}
if($settings.COMPOSE_PROJECT_NAME -ne $project){throw 'Project ownership mismatch'}
# Prevent inherited host variables from overriding this disposable configuration.
$savedEnvironment=@{}
foreach($key in $settings.Keys){
 $savedEnvironment[$key]=[Environment]::GetEnvironmentVariable($key,'Process')
 [Environment]::SetEnvironmentVariable($key,$settings[$key],'Process')
}
foreach($key in @('PHASE6_API_URL','PHASE3_API_URL','PHASE1_WEB_URL','UI_EVIDENCE')){
 $savedEnvironment[$key]=[Environment]::GetEnvironmentVariable($key,'Process')
}
function dc { & docker compose --env-file $configPath -p $project @args; if($LASTEXITCODE -ne 0){throw 'Compose command failed; inspect private logs'} }
function Check-Private([string]$Name,[scriptblock]$Work){
 & $Work *> (Join-Path $repoRoot "tmp/docker-$Name.log")
 if($LASTEXITCODE -ne 0){throw "$Name failed; see ignored tmp/docker-$Name.log"}
 Write-Output "Passed: $Name"
}
function Wait-Gateway {
 $ready=$false
 for($i=0;$i -lt 120;$i++){
  & curl.exe --fail --silent --max-time 2 ($settings.PUBLIC_ORIGIN+'/api/health') *> $null
  if($LASTEXITCODE -eq 0){$ready=$true;break}
  Start-Sleep -Milliseconds 1000
 }
 if(!$ready){throw 'Gateway did not become healthy'}
}
try{
 dc config --quiet
 if(!$SkipBuild){Check-Private build {dc build --pull}}
 Check-Private startup {dc up -d --force-recreate --wait --wait-timeout 240}
 $db=dc ps -q db
 if((docker inspect --format '{{ index .Config.Labels "purpose" }}' $db) -ne 'play-neml-phase6.5'){throw 'Unexpected database purpose label'}
 # Browser tests run on the host against the actual containerized application.
 $env:APP_BOOTSTRAP_EMAIL=$settings.APP_BOOTSTRAP_EMAIL
 $env:APP_BOOTSTRAP_PASSWORD=$settings.APP_BOOTSTRAP_PASSWORD
 $env:PHASE6_API_URL=$settings.PUBLIC_ORIGIN+'/api'
 $env:PHASE3_API_URL=$env:PHASE6_API_URL
 $env:PHASE1_WEB_URL=$settings.PUBLIC_ORIGIN
 $env:UI_EVIDENCE='../tmp/docker-evidence'
 if(!$SkipBrowser){
  Push-Location frontend
  try{
   if(!(Test-Path node_modules/@playwright/test)){Check-Private npm {npm ci}}
   Check-Private lint {npm run lint}
   Check-Private types {npm run typecheck}
   Check-Private model {node --test e2e/auction-model.test.cjs e2e/live-refresh.test.cjs e2e/portal-presentation.test.cjs}
   Check-Private browser {npx playwright test e2e/phase6.spec.ts e2e/phase3.spec.ts}
  }finally{Pop-Location}
 }
 Check-Private security {node tools/dockerization-checks.mjs security}
 Check-Private contexts {node tools/check-docker-contexts.mjs}
 Check-Private migrations {node tools/dockerization-checks.mjs migrations}
 Check-Private snapshot {node tools/dockerization-checks.mjs snapshot}
 Check-Private restart {dc restart db backend frontend gateway}
 Wait-Gateway
 Check-Private restart-data {node tools/dockerization-checks.mjs persistence restartPersistence}
 # Recreate just the backend first to verify gateway DNS recovers a changed address.
 Check-Private recreate-backend {dc up -d --no-deps --force-recreate --wait --wait-timeout 240 backend}
 Wait-Gateway
 Check-Private recreated-backend-data {node tools/dockerization-checks.mjs persistence recreatedBackendPersistence}
 Check-Private down {dc down}
 Check-Private recreate {dc up -d --wait --wait-timeout 240}
 Check-Private recreate-data {node tools/dockerization-checks.mjs persistence recreatedStackPersistence}
 Check-Private backup {node tools/dockerization-checks.mjs backup}
 $previousSecure=$env:AUTH_COOKIE_SECURE
 try{
  $env:AUTH_COOKIE_SECURE='true'
  Check-Private secure-backend {dc up -d --no-deps --force-recreate --wait --wait-timeout 240 backend}
  Wait-Gateway
  Check-Private secure-cookie {node tools/dockerization-checks.mjs secure-cookie}
 }finally{
  $env:AUTH_COOKIE_SECURE=$previousSecure
  Check-Private restore-http {dc up -d --no-deps --force-recreate --wait --wait-timeout 240 backend}
 }
 $before=Get-Content tmp/docker-before.json -Raw | ConvertFrom-Json
 $currentContainers=@(docker ps -aq);$currentVolumes=@(docker volume ls -q)
 foreach($id in $before.containers){if($id -notin $currentContainers){throw 'An original container is missing'}}
 foreach($name in $before.volumes){if($name -notin $currentVolumes){throw 'An original volume is missing'}}
 Write-Output "Verified isolated project $project. Original containers/volumes preserved. Sanitized results: tmp/docker-verification.json"
}finally{
 try{
  if(!$KeepRunning){dc stop | Out-Null;Write-Output 'Only this test project stopped; its named data volumes are retained.'}
 }finally{
  foreach($key in $savedEnvironment.Keys){[Environment]::SetEnvironmentVariable($key,$savedEnvironment[$key],'Process')}
 }
}
