param(
    [switch]$Start,
    [switch]$Build,
    [switch]$Recreate,
    [switch]$VerifyWorkflow,
    [switch]$Supplemental,
    [ValidateSet('playneml-npl-mock','playneml-npl-mock-verified')][string]$ProjectName = 'playneml-npl-mock',
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$RunName = 'full-mock',
    [string]$StopAfter,
    [string]$PrivateDirectory = (Join-Path $env:LOCALAPPDATA 'PlayNeml/npl-full-mock')
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$project = $ProjectName
if(-not $PSBoundParameters.ContainsKey('PrivateDirectory') -and $project -eq 'playneml-npl-mock-verified'){
    $PrivateDirectory=Join-Path $env:LOCALAPPDATA 'PlayNeml/npl-full-mock-verified'
}
$private = [IO.Path]::GetFullPath($PrivateDirectory)
if ($private.StartsWith($repo, [StringComparison]::OrdinalIgnoreCase)) { throw 'Private directory must be outside the repository' }
if (-not (Test-Path -LiteralPath $private)) {
    New-Item -ItemType Directory -Path $private | Out-Null
    $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    icacls $private /inheritance:r /grant:r "${identity}:(OI)(CI)F" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Unable to protect private runtime files' }
}
$envFile = Join-Path $private 'mock.env'
if (-not (Test-Path -LiteralPath $envFile)) {
    if (@(docker volume ls --format '{{.Name}}') -match "^${project}_") { throw 'Existing volumes found without their private environment; refusing to adopt them' }
    function New-MockSecret { $bytes = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes); [Convert]::ToBase64String($bytes) }
    $password = New-MockSecret
    [IO.File]::WriteAllLines($envFile, @(
        "COMPOSE_PROJECT_NAME=$project", 'POSTGRES_DB=npl_mock', 'POSTGRES_USER=npl_mock_owner', 'DB_USER=npl_mock_app',
        "POSTGRES_PASSWORD=$(New-MockSecret)", "DB_PASSWORD=$(New-MockSecret)", "JWT_SECRET=$(New-MockSecret)",
        'APP_BOOTSTRAP_ENABLED=true', 'APP_BOOTSTRAP_EMAIL=superadmin@npl-mock.example.invalid', "APP_BOOTSTRAP_PASSWORD=$password",
        'AUTH_COOKIE_SECURE=false', 'PUBLIC_ORIGIN=http://localhost:18090', 'PUBLIC_SCHEME=http', 'HTTP_PORT=18090'
    ))
    @{ email='superadmin@npl-mock.example.invalid'; password=$password } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $private 'bootstrap.credentials.json')
}
Push-Location $repo
try {
    $compose = @('compose','--env-file',$envFile,'-p',$project)
    # Never print resolved configuration: it contains secrets.
    $config = (& docker @compose config --format json | ConvertFrom-Json)
    if ($LASTEXITCODE -ne 0 -or $config.name -ne $project) { throw 'Unexpected Compose project' }
    if ($config.volumes.postgres_data.name -ne "${project}_postgres_data" -or $config.volumes.player_photos.name -ne "${project}_player_photos") { throw 'Unexpected volume names' }
    $ports = @($config.services.gateway.ports)
    if ($ports.Count -ne 1 -or $ports[0].host_ip -ne '127.0.0.1' -or $ports[0].published -ne '18090') { throw 'Mock must use loopback port 18090' }
    foreach ($service in $config.services.PSObject.Properties) {
        if ($service.Value.container_name -or $service.Value.volumes.type -contains 'bind') { throw 'Custom container names and bind mounts are not accepted' }
    }
    Write-Output "Verified $project; dedicated postgres_data/player_photos volumes; 127.0.0.1:18090"
    if ($Build) { & docker @compose build; if ($LASTEXITCODE -ne 0) { throw 'Build failed' } }
    if ($Start -or $Recreate) {
        $up = @('up','-d','--wait','--wait-timeout','240')
        if ($Recreate) { $up += '--force-recreate' }
        & docker @compose @up
        if ($LASTEXITCODE -ne 0) { throw 'Disposable stack did not become healthy' }
    }
    $gateway = (& docker inspect --format '{{index .Config.Labels "com.docker.compose.project"}} {{.State.Health.Status}}' "$project-gateway-1")
    if ($LASTEXITCODE -ne 0 -or $gateway -ne "$project healthy") { throw 'Expected healthy isolated gateway is absent' }
    $env:NPL_TEST_URL = 'http://localhost:18090/api'
    $env:NPL_FULL_MOCK = 'true'
    $env:NPL_MOCK_ISOLATED_PROJECT = $project
    $env:NPL_MOCK_PRIVATE_DIR = $private
    $statePath=Join-Path $private "$RunName.credentials.json"
    $env:NPL_MOCK_STATE_FILE=$statePath
    $env:NPL_MOCK_STOP_AFTER = $StopAfter
    $env:NPL_MOCK_SUPPLEMENTAL = $Supplemental.ToString().ToLowerInvariant()
    & node tests/npl-championship-api.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Mock stopped at a failing assertion; protected checkpoint retained for repair/resume' }
    # The approved bootstrap is only needed on the first fresh database start.
    $privateLines=Get-Content -LiteralPath $envFile
    $privateLines=$privateLines | Where-Object {$_ -notmatch '^APP_BOOTSTRAP_(EMAIL|PASSWORD)='} | ForEach-Object {if($_ -eq 'APP_BOOTSTRAP_ENABLED=true'){'APP_BOOTSTRAP_ENABLED=false'}else{$_}}
    [IO.File]::WriteAllLines($envFile,$privateLines)
    if ($VerifyWorkflow) {
        $state=Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
        if ($state.completed -notcontains 'certificates') { throw 'Finish the mock before full verification' }
        $env:NPL_TEST_STATE=$statePath
        $env:NPL_TEST_PHOTO=Join-Path $repo 'tmp/npl-mock/fixtures/synthetic-avatar.png'
        $env:NPL_PERSISTENCE_MODE='before'
        & node tests/npl-persistence-api.mjs
        if ($LASTEXITCODE -ne 0) { throw 'Correction and certificate checks failed; checkpoint preserved' }
        $migrationJson=& docker @compose exec -T db psql -X -U npl_mock_owner -d npl_mock -At -c 'SELECT json_agg(m ORDER BY version) FROM (SELECT version,checksum FROM play_neml.schema_migrations) m;'
        if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect mock migrations' }
        $migrationJson=$migrationJson -join "`n"
        $migrations=$migrationJson | ConvertFrom-Json
        if ($migrations.Count -ne 7) { throw 'Expected seven tracked migrations' }
        foreach ($migration in $migrations) {
            $hash=(Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $repo "docs/sql/$($migration.version).sql")).Hash.ToLowerInvariant()
            if ($hash -ne $migration.checksum) { throw "Migration checksum mismatch: $($migration.version)" }
        }
        $migrationJson | Set-Content -LiteralPath (Join-Path $repo 'docs/npl-mock/evidence/migrations.json')
        Get-Content -LiteralPath (Join-Path $repo 'tests/npl-verify.sql') -Raw | & docker @compose exec -T db psql -X -U npl_mock_owner -d npl_mock -v "cid=$($state.cid)" > docs/npl-mock/evidence/database-verification.txt
        if ($LASTEXITCODE -ne 0) { throw 'Independent read-only database assertions failed' }
        $env:NPL_PERSISTENCE_MODE='baseline'
        & node tests/npl-persistence-api.mjs
        if ($LASTEXITCODE -ne 0) { throw 'Pre-recreation baseline failed' }
        & docker @compose up -d --force-recreate --wait --wait-timeout 240
        if ($LASTEXITCODE -ne 0) { throw 'Recreated stack is not healthy; all volumes retained' }
        $env:NPL_PERSISTENCE_MODE='after'
        & node tests/npl-persistence-api.mjs
        if ($LASTEXITCODE -ne 0) { throw 'Post-recreation persistence checks failed' }
        & node tests/npl-championship-api.mjs
        if ($LASTEXITCODE -ne 0) { throw 'Final state refresh failed' }
        & docker @compose ps -a > docs/npl-mock/evidence/docker-status.txt
    }
} finally {
    Remove-Item Env:NPL_MOCK_ISOLATED_PROJECT -ErrorAction SilentlyContinue
    Remove-Item Env:NPL_FULL_MOCK -ErrorAction SilentlyContinue
    Remove-Item Env:NPL_MOCK_PRIVATE_DIR -ErrorAction SilentlyContinue
    Remove-Item Env:NPL_MOCK_STATE_FILE -ErrorAction SilentlyContinue
    Remove-Item Env:NPL_MOCK_STOP_AFTER -ErrorAction SilentlyContinue
    Remove-Item Env:NPL_MOCK_SUPPLEMENTAL -ErrorAction SilentlyContinue
    Remove-Item Env:NPL_TEST_STATE -ErrorAction SilentlyContinue
    Remove-Item Env:NPL_TEST_PHOTO -ErrorAction SilentlyContinue
    Remove-Item Env:NPL_PERSISTENCE_MODE -ErrorAction SilentlyContinue
    Pop-Location
}
