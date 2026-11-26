#Requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$BackupDirectory,
    [string]$RepositoryRoot = (Split-Path $PSScriptRoot -Parent),
    [string]$EnvFile
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Team-Common.ps1')
$context = Get-TeamContext $RepositoryRoot $EnvFile
$backup = (Resolve-Path -LiteralPath $BackupDirectory -ErrorAction Stop).Path
$manifest = Get-Content -LiteralPath (Join-Path $backup 'manifest.json') -Raw | ConvertFrom-Json
if ($manifest.format_version -ne 1 -or $manifest.dump_file -ne 'database.dump') { throw 'Manifesto não suportado.' }
$dumpPath = Join-Path $backup 'database.dump'
if ((Get-FileHash -LiteralPath $dumpPath -Algorithm SHA256).Hash -ne $manifest.dump_sha256) {
    throw 'Hash do dump não confere; nenhum banco foi criado.'
}
$database = 'ewaste_restore_' + [guid]::NewGuid().ToString('N')
if ($database -eq $context.Database -or $database -notmatch '^ewaste_restore_[0-9a-f]{32}$') {
    throw 'Destino de restauração inseguro.'
}
$remoteFile = '/tmp/ewaste-restore-' + [guid]::NewGuid().ToString('N') + '.dump'
$started = [DateTime]::UtcNow
$timer = [Diagnostics.Stopwatch]::StartNew()
$result = [ordered]@{ started_utc = $started.ToString('o'); restore_database = $database
    source_backup = $backup; source_git_sha = $manifest.git_sha; passed = $false }
try {
    # Name is always new, generated here. Never restore into DB_NAME or overwrite an existing database.
    Invoke-TeamDocker $context @('exec', '-T', 'db', 'createdb', '--no-password',
        '--username', $context.Owner, '--owner', $context.Owner, '--template=template0', $database) | Out-Null
    Invoke-TeamDocker $context @('cp', $dumpPath, ('db:' + $remoteFile)) | Out-Null
    Invoke-TeamDocker $context @('exec', '-T', 'db', 'pg_restore', '--no-password',
        '--username', $context.Owner, '--dbname', $database, '--no-owner', '--no-privileges',
        '--exit-on-error', '--single-transaction', $remoteFile) | Out-Null
    $actual = @(Get-TeamTableMetadata $context $database)
    $expectedTables = @($manifest.tables | Sort-Object table)
    $actualTables = @($actual | Sort-Object table)
    if ($expectedTables.Count -ne $actualTables.Count) { throw 'Número de tabelas restauradas diverge.' }
    for ($i = 0; $i -lt $expectedTables.Count; $i++) {
        $expected = $expectedTables[$i]; $found = $actualTables[$i]
        foreach ($property in @('table', 'rows', 'signature', 'bytea_bytes')) {
            if ($expected.$property -ne $found.$property) {
                throw "Diferença após restauração: tabela $($expected.table), campo $property."
            }
        }
    }
    foreach ($file in @($manifest.legacy_uploads)) {
        $legacyRoot = [IO.Path]::GetFullPath((Join-Path $backup 'uploads-legado'))
        $path = [IO.Path]::GetFullPath((Join-Path $legacyRoot $file.path))
        if (-not $path.StartsWith($legacyRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Caminho de arquivo legado fora do backup.'
        }
        if ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $file.sha256) {
            throw 'Hash de arquivo legado não confere.'
        }
    }
    $result.passed = $true
    $result.tables = $actualTables
    $result.legacy_files_checked = @($manifest.legacy_uploads).Count
    Write-Output "Restauração e comparação concluídas: $database"
} catch {
    $result.error = $_.Exception.Message
    throw
} finally {
    $timer.Stop()
    $result.elapsed_seconds = [Math]::Round($timer.Elapsed.TotalSeconds, 3)
    $result.finished_utc = [DateTime]::UtcNow.ToString('o')
    $result.database_preserved = $true
    $result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $backup ($database + '.json')) -Encoding utf8
    Write-Output 'O banco restaurado foi preservado. Nenhum DROP foi executado.'
}
