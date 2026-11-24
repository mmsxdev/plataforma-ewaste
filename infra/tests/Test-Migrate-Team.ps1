#Requires -Version 7.0
# Repositorios temporarios e Maven/Java simulados: nao conecta a PostgreSQL.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$sourceScript = Join-Path (Split-Path $PSScriptRoot -Parent) 'Migrate-Team.ps1'
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('ewaste-guard-tests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot | Out-Null
$shell = Join-Path $PSHOME 'pwsh.exe'
if (-not (Test-Path -LiteralPath $shell)) { throw 'Este ensaio Windows requer PowerShell 7.' }

function Assert-Condition([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Invoke-FixtureGit([string]$Repository, [string[]]$Arguments) {
    & git -C $Repository @Arguments | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao preparar repositorio de teste.' }
}

# O processo deste teste tem seu proprio comando java simulado.
function global:java {
    Add-Content -LiteralPath $env:PI_GUARD_JAVA_LOG -Value ($args -join '|')
    $global:LASTEXITCODE = 0
}

$cases = @(
    @{ Name = 'limpo'; Allowed = $true },
    @{ Name = 'documentos-e-pdf-locais'; Allowed = $true },
    @{ Name = 'target-residual'; Allowed = $true },
    @{ Name = 'head-diferente'; Allowed = $false; Reason = 'HEAD difere' },
    @{ Name = 'worktree-java'; Allowed = $false; Reason = 'Worktree' },
    @{ Name = 'index-sql'; Allowed = $false; Reason = 'Index' },
    @{ Name = 'untracked-sql'; Allowed = $false; Reason = 'nao rastreados' },
    @{ Name = 'ignored-sql'; Allowed = $false; Reason = 'nao rastreados' },
    @{ Name = 'wrapper-alterado'; Allowed = $false; Reason = 'Worktree' },
    @{ Name = 'build-altera-snapshot'; Allowed = $false; Reason = 'empacotamento alterou' },
    @{ Name = 'build-altera-origem'; Allowed = $false; Reason = 'Worktree' }
)

$results = @()
foreach ($case in $cases) {
    $repository = Join-Path $testRoot $case.Name
    New-Item -ItemType Directory -Path (Join-Path $repository 'infra') -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $repository 'src/main/resources/db/migration') -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $repository 'src/main/java') -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $repository '.mvn') -Force | Out-Null
    Copy-Item -LiteralPath $sourceScript -Destination (Join-Path $repository 'infra/Migrate-Team.ps1')
    Set-Content -LiteralPath (Join-Path $repository '.gitignore') -Value "target/`n.env`nsrc/main/resources/db/migration/ignored.sql"
    Set-Content -LiteralPath (Join-Path $repository 'pom.xml') -Value '<project />'
    Set-Content -LiteralPath (Join-Path $repository 'src/main/java/Fixture.java') -Value 'class Fixture {}'
    Set-Content -LiteralPath (Join-Path $repository 'src/main/resources/db/migration/V1__fixture.sql') -Value '-- fixture inicial'
    Set-Content -LiteralPath (Join-Path $repository '.mvn/maven.config') -Value ''
    Set-Content -LiteralPath (Join-Path $repository 'README.md') -Value 'Fixture de teste, sem dados reais.'
    $buildStub = @'
$ErrorActionPreference = 'Stop'
$source = $PSScriptRoot
if (Test-Path -LiteralPath (Join-Path $source '.git')) { throw 'Build nao esta isolado.' }
if (Test-Path -LiteralPath (Join-Path $source '.env')) { throw 'Segredo local copiado indevidamente.' }
if (Test-Path -LiteralPath (Join-Path $source 'target/residual.sql')) { throw 'Residuo de outra branch entrou no build.' }
Add-Content -LiteralPath $env:PI_GUARD_BUILD_LOG -Value $source
if ($env:PI_GUARD_CASE -eq 'build-altera-snapshot') {
    Add-Content -LiteralPath (Join-Path $source 'src/main/java/Fixture.java') -Value '// formatter alterou fonte'
}
if ($env:PI_GUARD_CASE -eq 'build-altera-origem') {
    Add-Content -LiteralPath (Join-Path $env:PI_GUARD_REPOSITORY 'src/main/java/Fixture.java') -Value '// alteracao concorrente'
}
New-Item -ItemType Directory -Path (Join-Path $source 'target') -Force | Out-Null
Set-Content -LiteralPath (Join-Path $source 'target/plataforma-ewaste-0.0.1-SNAPSHOT.jar') -Value 'JAR simulado, nao executavel'
'@
    Set-Content -LiteralPath (Join-Path $repository 'fake-build.ps1') -Value $buildStub
    $wrapper = "@echo off`r`n`"$shell`" -NoLogo -NoProfile -File `"%~dp0fake-build.ps1`"`r`nexit /b %errorlevel%`r`n"
    [IO.File]::WriteAllText((Join-Path $repository 'mvnw.cmd'), $wrapper)
    Invoke-FixtureGit $repository @('init', '-q', '-b', 'main')
    Invoke-FixtureGit $repository @('config', 'user.name', 'PI Guard Test')
    Invoke-FixtureGit $repository @('config', 'user.email', 'pi-guard@teste.invalid')
    Invoke-FixtureGit $repository @('config', 'commit.gpgsign', 'false')
    Invoke-FixtureGit $repository @('config', 'core.autocrlf', 'false')
    Invoke-FixtureGit $repository @('config', 'core.hooksPath', (Join-Path $testRoot 'no-hooks'))
    Invoke-FixtureGit $repository @('add', '.')
    Invoke-FixtureGit $repository @('commit', '-q', '-m', 'Fixture isolada')
    $revision = (& git -C $repository rev-parse HEAD) -join ''
    $env:PI_GUARD_CASE = $case.Name
    $env:PI_GUARD_REPOSITORY = $repository
    $env:PI_GUARD_BUILD_LOG = Join-Path $testRoot ($case.Name + '-build.log')
    $env:PI_GUARD_JAVA_LOG = Join-Path $testRoot ($case.Name + '-java.log')

    switch ($case.Name) {
        'documentos-e-pdf-locais' {
            Add-Content -LiteralPath (Join-Path $repository 'README.md') -Value 'Anotacao local preservada.'
            Set-Content -LiteralPath (Join-Path $repository 'PDF oficial.pdf') -Value 'Ficticio'
        }
        'target-residual' {
            New-Item -ItemType Directory -Path (Join-Path $repository 'target') | Out-Null
            Set-Content -LiteralPath (Join-Path $repository 'target/residual.sql') -Value '-- nao deve entrar no JAR'
            Set-Content -LiteralPath (Join-Path $repository '.env') -Value 'DB_PASSWORD=ficticia-nao-usada'
        }
        'head-diferente' { $revision = '0000000000000000000000000000000000000000' }
        'worktree-java' { Add-Content -LiteralPath (Join-Path $repository 'src/main/java/Fixture.java') -Value '// mudanca'
        }
        'index-sql' {
            Add-Content -LiteralPath (Join-Path $repository 'src/main/resources/db/migration/V1__fixture.sql') -Value '-- staged'
            Invoke-FixtureGit $repository @('add', 'src/main/resources/db/migration/V1__fixture.sql')
        }
        'untracked-sql' { Set-Content -LiteralPath (Join-Path $repository 'src/main/resources/db/migration/V999__novo.sql') -Value '-- novo' }
        'ignored-sql' { Set-Content -LiteralPath (Join-Path $repository 'src/main/resources/db/migration/ignored.sql') -Value '-- ignorado' }
        'wrapper-alterado' { Add-Content -LiteralPath (Join-Path $repository '.mvn/maven.config') -Value '-Poutro' }
    }
    $failed = $false
    $failure = ''
    try {
        & (Join-Path $repository 'infra/Migrate-Team.ps1') -ApprovedRevision $revision | Out-Null
    } catch {
        $failed = $true
        $failure = $_.Exception.Message
    }
    $javaCalled = Test-Path -LiteralPath $env:PI_GUARD_JAVA_LOG
    if ($case.Allowed) {
        Assert-Condition (-not $failed) ("Cenario limpo falhou: " + $case.Name + ': ' + $failure)
        Assert-Condition $javaCalled ('Java simulado nao executou: ' + $case.Name)
        $buildDirectory = (Get-Content -LiteralPath $env:PI_GUARD_BUILD_LOG -Raw).Trim()
        Assert-Condition ($buildDirectory -ne $repository) 'Build usou a arvore de trabalho original.'
        Assert-Condition (-not (Test-Path -LiteralPath $buildDirectory)) 'Build temporario nao foi removido com seguranca.'
    } else {
        Assert-Condition $failed ('Cenario inseguro foi liberado: ' + $case.Name)
        Assert-Condition ($failure.Contains($case.Reason)) ('Bloqueio inesperado em ' + $case.Name + ': ' + $failure)
        Assert-Condition (-not $javaCalled) ('Java foi chamado antes do bloqueio: ' + $case.Name)
    }
    $results += [pscustomobject]@{ scenario = $case.Name; passed = $true; allowed = $case.Allowed; java_simulated = $javaCalled; block_reason = $failure }
    Write-Output ('PASS ' + $case.Name)
}
$results | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $testRoot 'resultado.json') -Encoding utf8
Write-Output ('Todos os ' + $results.Count + ' cenarios passaram sem PostgreSQL. Evidencias: ' + $testRoot)
