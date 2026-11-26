#Requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^[a-fA-F0-9]{40}$')][string]$ApprovedRevision,
    [switch]$IncludeDemoData,
    [switch]$ImportLegacyUploads
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$protectedPaths = @('src/main', 'pom.xml', '.mvn', 'mvnw', 'mvnw.cmd', 'infra/Migrate-Team.ps1')

function Assert-ApprovedInputs {
    $head = (& git -C $root rev-parse HEAD) -join ''
    if ($LASTEXITCODE -ne 0 -or $head -ne $ApprovedRevision) {
        throw 'HEAD difere da revisao combinada; nenhum banco foi alterado.'
    }
    & git -C $root diff --quiet -- @protectedPaths
    if ($LASTEXITCODE -ne 0) { throw 'Worktree de fontes/configuracao alterado; nenhum banco foi alterado.' }
    & git -C $root diff --cached --quiet -- @protectedPaths
    if ($LASTEXITCODE -ne 0) { throw 'Index de fontes/configuracao alterado; nenhum banco foi alterado.' }
    # Sem --exclude-standard: um SQL/Java ignorado tambem pode entrar no empacotamento.
    $newInputs = @(& git -C $root ls-files --others -- @protectedPaths)
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel conferir arquivos novos; operacao interrompida.' }
    if ($newInputs.Count -gt 0) { throw 'Arquivos de entrada nao rastreados; nenhum banco foi alterado.' }
}

function Get-BuildFingerprint([string]$Directory) {
    $files = @(foreach ($relative in $protectedPaths) {
        $path = Join-Path $Directory $relative
        if (Test-Path -LiteralPath $path -PathType Container) {
            Get-ChildItem -LiteralPath $path -File -Recurse -Force
        } elseif (Test-Path -LiteralPath $path -PathType Leaf) {
            Get-Item -LiteralPath $path -Force
        }
    })
    $fingerprints = @($files | Sort-Object FullName -Unique | ForEach-Object {
        [ordered]@{
            path = [IO.Path]::GetRelativePath($Directory, $_.FullName)
            sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
        }
    })
    return ConvertTo-Json -InputObject $fingerprints -Compress -Depth 3
}

Assert-ApprovedInputs
$profiles = @('migrate')
if ($IncludeDemoData) { $profiles += 'dev' }
if ($ImportLegacyUploads) { $profiles += 'import-uploads' }
$buildRoot = Join-Path ([IO.Path]::GetTempPath()) ('ewaste-migration-' + [guid]::NewGuid().ToString('N'))
$source = Join-Path $buildRoot 'source'
Push-Location $root
try {
    New-Item -ItemType Directory -Path $buildRoot -ErrorAction Stop | Out-Null
    $archive = Join-Path $buildRoot 'approved.zip'
    & git -C $root archive --format=zip ("--output=" + $archive) $ApprovedRevision
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel extrair a revisao aprovada; banco preservado.' }
    Expand-Archive -LiteralPath $archive -DestinationPath $source -ErrorAction Stop
    $beforeBuild = Get-BuildFingerprint $source
    Push-Location $source
    try {
        # Snapshot sem .env ou target locais e sem DevTools no JAR executado.
        & (Join-Path $source 'mvnw.cmd') -B -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'Empacotamento falhou; banco preservado.' }
    } finally { Pop-Location }
    if ($beforeBuild -ne (Get-BuildFingerprint $source)) {
        throw 'O empacotamento alterou fontes/configuracao da revisao; nenhum banco foi alterado.'
    }
    Assert-ApprovedInputs
    $jar = Join-Path $source 'target/plataforma-ewaste-0.0.1-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw 'JAR esperado nao foi produzido; banco preservado.' }
    Write-Output ('Revisao: ' + $ApprovedRevision + '; SHA256 do JAR: ' + (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash)
    # A configuracao privada e lida na raiz original, sem copiar segredos para o snapshot.
    & java -jar $jar ("--spring.profiles.active=" + ($profiles -join ','))
    if ($LASTEXITCODE -ne 0) { throw 'Migracao/importacao falhou. Verifique o log; nao execute repair ou limpeza automaticamente.' }
} finally {
    Pop-Location
    # Somente o diretorio temporario criado por esta chamada pode ser removido.
    $temporaryBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    $resolvedBuild = [IO.Path]::GetFullPath($buildRoot)
    if ($resolvedBuild.StartsWith($temporaryBase, [StringComparison]::OrdinalIgnoreCase) -and
        [IO.Path]::GetFileName($resolvedBuild) -match '^ewaste-migration-[0-9a-f]{32}$' -and
        (Test-Path -LiteralPath $resolvedBuild)) {
        try { Remove-Item -LiteralPath $resolvedBuild -Recurse -Force -ErrorAction Stop }
        catch { Write-Warning ('Build temporario preservado porque a limpeza falhou: ' + $resolvedBuild) }
    }
}
