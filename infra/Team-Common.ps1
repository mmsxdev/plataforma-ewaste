#Requires -Version 7.0
Set-StrictMode -Version Latest

function Get-TeamContext {
    param([string]$RepositoryRoot, [string]$EnvFile)
    $root = (Resolve-Path -LiteralPath $RepositoryRoot -ErrorAction Stop).Path
    if (-not $EnvFile) { $EnvFile = Join-Path $root '.env' }
    $envPath = (Resolve-Path -LiteralPath $EnvFile -ErrorAction Stop).Path
    $values = @{}
    # Never execute .env as a script, and never print its contents.
    foreach ($line in [IO.File]::ReadAllLines($envPath)) {
        if ($line -match '^\s*(DB_NAME|DB_USER)\s*=\s*(.*?)\s*$') {
            $values[$Matches[1]] = $Matches[2].Trim('"').Trim("'")
        }
    }
    foreach ($key in @('DB_NAME', 'DB_USER')) {
        $override = [Environment]::GetEnvironmentVariable($key)
        if ($override) { $values[$key] = $override }
        if (-not $values.ContainsKey($key)) { $values[$key] = 'ewaste' }
        if ($values[$key] -notmatch '^[A-Za-z_][A-Za-z0-9_]{0,62}$') {
            throw "Use um identificador PostgreSQL simples em $key (sem espaços ou interpolação)."
        }
    }
    [pscustomobject]@{
        Root = $root
        EnvFile = $envPath
        Database = $values.DB_NAME
        Owner = $values.DB_USER
        Docker = (Get-Command docker -ErrorAction Stop).Source
        Prefix = @('compose', '--project-directory', $root, '--env-file', $envPath,
            '-f', (Join-Path $root 'docker-compose.yml'))
    }
}

function Invoke-TeamDocker {
    param($Context, [string[]]$DockerArguments)
    $output = & $Context.Docker @($Context.Prefix) @DockerArguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Docker falhou (código $LASTEXITCODE): $($output -join [Environment]::NewLine)"
    }
    return $output
}

function Invoke-TeamSql {
    param($Context, [string]$Database, [string]$Sql, [string]$Snapshot)
    $prefix = "SET TIME ZONE 'UTC'; SET bytea_output = 'hex'; SET extra_float_digits = 3; "
    if ($Snapshot) {
        if ($Snapshot -notmatch '^[0-9A-Fa-f-]+$') { throw 'Snapshot inválido.' }
        $prefix = "BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY; SET TRANSACTION SNAPSHOT '$Snapshot'; " + $prefix
        $Sql += '; COMMIT;'
    }
    Invoke-TeamDocker $Context @('exec', '-T', 'db', 'psql', '-X', '-qAt',
        '--no-password', '-v', 'ON_ERROR_STOP=1', '-U', $Context.Owner, '-d', $Database,
        '-c', ($prefix + $Sql))
}

function Open-TeamSnapshot {
    param($Context)
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $Context.Docker
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardInput = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = [Text.Encoding]::UTF8
    $info.StandardErrorEncoding = [Text.Encoding]::UTF8
    foreach ($argument in @($Context.Prefix) + @('exec', '-T', 'db', 'psql', '-X', '-qAt',
        '--no-password', '-v', 'ON_ERROR_STOP=1', '-U', $Context.Owner, '-d', $Context.Database)) {
        $info.ArgumentList.Add($argument)
    }
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $info
    if (-not $process.Start()) { throw 'Não foi possível abrir a sessão de snapshot.' }
    try {
        $process.StandardInput.WriteLine('BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;')
        $process.StandardInput.WriteLine('SELECT pg_export_snapshot();')
        $process.StandardInput.Flush()
        $read = $process.StandardOutput.ReadLineAsync()
        if (-not $read.Wait(15000)) { throw 'Timeout ao exportar snapshot PostgreSQL.' }
        $snapshot = $read.Result
        if (-not $snapshot -or $snapshot -notmatch '^[0-9A-Fa-f-]+$') {
            throw 'O PostgreSQL não devolveu um snapshot válido.'
        }
        return [pscustomobject]@{ Process = $process; Id = $snapshot }
    } catch {
        if (-not $process.HasExited) { $process.Kill() }
        $process.Dispose()
        throw
    }
}

function Close-TeamSnapshot {
    param($SnapshotSession)
    if ($null -eq $SnapshotSession) { return }
    $process = $SnapshotSession.Process
    try {
        if (-not $process.HasExited) {
            $process.StandardInput.WriteLine('ROLLBACK;')
            $process.StandardInput.WriteLine('\q')
            $process.StandardInput.Close()
            if (-not $process.WaitForExit(5000)) { $process.Kill() }
        }
    } finally { $process.Dispose() }
}

function Get-TeamTableMetadata {
    param($Context, [string]$Database, [string]$Snapshot)
    $inventorySql = @'
SELECT json_build_object('table', t.table_name, 'bytea_columns', COALESCE((
  SELECT json_agg(c.column_name ORDER BY c.ordinal_position)
  FROM information_schema.columns c WHERE c.table_schema = 'public'
    AND c.table_name = t.table_name AND c.data_type = 'bytea'), '[]'::json))
FROM information_schema.tables t
WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
ORDER BY t.table_name
'@
    $inventory = @(Invoke-TeamSql $Context $Database $inventorySql $Snapshot |
        Where-Object { $_.ToString().StartsWith('{') } | ForEach-Object { $_ | ConvertFrom-Json })
    $metadata = @()
    foreach ($table in $inventory) {
        $quotedTable = '"' + $table.table.Replace('"', '""') + '"'
        $literalTable = $table.table.Replace("'", "''")
        $byteTerms = @($table.bytea_columns | ForEach-Object {
            'COALESCE(octet_length(t."' + $_.Replace('"', '""') + '"),0)'
        })
        $bytes = if ($byteTerms.Count) { 'COALESCE(sum(' + ($byteTerms -join ' + ') + '),0)' } else { '0' }
        $sql = "SELECT json_build_object('table','$literalTable','rows',count(*)," +
            "'signature',md5(COALESCE(string_agg(md5(t::text),'' ORDER BY md5(t::text)),''))," +
            "'bytea_bytes',$bytes) FROM public.$quotedTable t"
        $metadata += @(Invoke-TeamSql $Context $Database $sql $Snapshot |
            Where-Object { $_.ToString().StartsWith('{') } | ForEach-Object { $_ | ConvertFrom-Json })
    }
    return $metadata
}
