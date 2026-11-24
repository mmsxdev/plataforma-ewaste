#Requires -Version 7.0
[CmdletBinding()]
param(
    [string]$RepositoryRoot = (Split-Path $PSScriptRoot -Parent),
    [string]$EnvFile,
    [string]$BackupRoot,
    [string]$LegacyUploadsPath
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Team-Common.ps1')
$context = Get-TeamContext $RepositoryRoot $EnvFile
if (-not $BackupRoot) { $BackupRoot = Join-Path $context.Root 'backups' }
$stamp = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$directory = Join-Path $BackupRoot $stamp
if (Test-Path -LiteralPath $directory) { throw 'Diretório de backup já existe; nada foi sobrescrito.' }
New-Item -ItemType Directory -Path $directory -ErrorAction Stop | Out-Null
$session = $null
$started = [DateTime]::UtcNow
$remoteFile = '/tmp/ewaste-backup-' + [guid]::NewGuid().ToString('N') + '.dump'
try {
    $session = Open-TeamSnapshot $context
    $tables = @(Get-TeamTableMetadata $context $context.Database $session.Id)
    Invoke-TeamDocker $context @('exec', '-T', 'db', 'pg_dump', '--no-password',
        '--username', $context.Owner, '--dbname', $context.Database, '--format=custom',
        '--snapshot', $session.Id, '--file', $remoteFile) | Out-Null
    $dumpPath = Join-Path $directory 'database.dump'
    Invoke-TeamDocker $context @('cp', ('db:' + $remoteFile), $dumpPath) | Out-Null
    $dumpHash = (Get-FileHash -LiteralPath $dumpPath -Algorithm SHA256).Hash
    $remoteHash = ((Invoke-TeamDocker $context @('exec', '-T', 'db', 'sha256sum', $remoteFile)) -join '').Split(' ')[0]
    if ($dumpHash -ne $remoteHash.ToUpperInvariant()) { throw 'Hash do dump copiado não confere.' }
    $gitSha = (& git -C $context.Root rev-parse HEAD 2>$null) -join ''
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível registrar a revisão Git.' }
    $legacy = @()
    if (-not $LegacyUploadsPath) { $LegacyUploadsPath = Join-Path $context.Root 'uploads' }
    if (Test-Path -LiteralPath $LegacyUploadsPath -PathType Container) {
        $legacyRoot = (Resolve-Path -LiteralPath $LegacyUploadsPath).Path
        $legacyDestination = Join-Path $directory 'uploads-legado'
        Copy-Item -LiteralPath $legacyRoot -Destination $legacyDestination -Recurse -ErrorAction Stop
        $legacy = @(Get-ChildItem -LiteralPath $legacyDestination -File -Recurse | ForEach-Object {
            [pscustomobject]@{ path = [IO.Path]::GetRelativePath($legacyDestination, $_.FullName)
                bytes = $_.Length; sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
        })
    }
    $manifest = [ordered]@{
        format_version = 1; created_utc = $started.ToString('o'); finished_utc = [DateTime]::UtcNow.ToString('o')
        database = $context.Database; git_sha = $gitSha; snapshot = $session.Id
        dump_file = 'database.dump'; dump_sha256 = $dumpHash; dump_bytes = (Get-Item -LiteralPath $dumpPath).Length
        tables = $tables; legacy_uploads = $legacy
        legacy_consistency = 'Cópia filesystem requer ausência de escrita concorrente; não participa do snapshot SQL.'
        container_temporary_dump = $remoteFile
    }
    $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $directory 'manifest.json') -Encoding utf8
    Write-Output "Backup concluído: $directory"
    Write-Output 'Inclui todas as tabelas e BYTEA de arquivo_compartilhado presentes no snapshot SQL.'
} finally { Close-TeamSnapshot $session }
