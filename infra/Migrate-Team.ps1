#Requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^[a-fA-F0-9]{40}$')][string]$ApprovedRevision,
    [switch]$IncludeDemoData,
    [switch]$ImportLegacyUploads
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$head = (& git -C $root rev-parse HEAD) -join ''
if ($LASTEXITCODE -ne 0 -or $head -ne $ApprovedRevision) { throw 'HEAD difere da revisao combinada; nenhum banco foi alterado.' }
& git -C $root diff --quiet -- src/main pom.xml
if ($LASTEXITCODE -ne 0) { throw 'Fontes/configuracao alteradas sem commit; fixe a revisao antes de migrar.' }
$profiles = @('migrate')
if ($IncludeDemoData) { $profiles += 'dev' }
if ($ImportLegacyUploads) { $profiles += 'import-uploads' }
Push-Location $root
try {
    # Empacotado sem DevTools: erro de inicializacao retorna erro real ao operador.
    & (Join-Path $root 'mvnw.cmd') -B -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Empacotamento falhou; banco preservado.' }
    & java -jar (Join-Path $root 'target/plataforma-ewaste-0.0.1-SNAPSHOT.jar') ("--spring.profiles.active=" + ($profiles -join ','))
    if ($LASTEXITCODE -ne 0) { throw 'Migracao/importacao falhou. Verifique o log; nao execute repair ou limpeza automaticamente.' }
} finally { Pop-Location }
