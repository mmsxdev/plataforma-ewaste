[CmdletBinding()]
param(
    [Parameter(Mandatory)][string[]]$TeamAddresses,
    [Parameter(Mandatory)][string]$HostAddress,
    [int]$Port = 5432
)
$ErrorActionPreference = 'Stop'
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]$identity).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'Execute em PowerShell Administrador na anfitria; o script nao altera a categoria da rede.'
}
# Somente IPv4 individuais da equipe; nenhum Any, faixa ampla ou endereco publico.
$allowed = @('127.0.0.1', $HostAddress) + $TeamAddresses
$numbers = @($allowed | ForEach-Object {
    $ip = [Net.IPAddress]::Parse($_)
    $bytes = $ip.GetAddressBytes()
    if ($bytes.Length -ne 4 -or -not ($bytes[0] -eq 10 -or ($bytes[0] -eq 172 -and $bytes[1] -ge 16 -and $bytes[1] -le 31) -or ($bytes[0] -eq 192 -and $bytes[1] -eq 168) -or $_ -eq '127.0.0.1')) {
        throw 'Informe apenas IPv4 privados individuais (LAN/VPN).'
    }
    [Array]::Reverse($bytes)
    [uint64][BitConverter]::ToUInt32($bytes, 0)
} | Sort-Object -Unique)
function Format-IPv4([uint64]$n) {
    $b = [BitConverter]::GetBytes([uint32]$n)
    [Array]::Reverse($b)
    ([Net.IPAddress]::new($b)).ToString()
}
# Bloqueios explicitos complementares: nao dependemos de regras permissivas preexistentes do Docker.
$blocked = [Collections.Generic.List[string]]::new()
[uint64]$next = 0
foreach ($n in $numbers) {
    if ($n -gt $next) { $blocked.Add("$(Format-IPv4 $next)-$(Format-IPv4 ($n - 1))") }
    $next = $n + 1
}
if ($next -le 4294967295) { $blocked.Add("$(Format-IPv4 $next)-255.255.255.255") }
if (Get-NetFirewallRule -Name 'Ewaste-Team-Postgres-*' -ErrorAction SilentlyContinue) {
    throw 'Regras E-Waste ja existem. Revise-as antes de substituir; nenhuma regra foi apagada.'
}
New-NetFirewallRule -Name 'Ewaste-Team-Postgres-BlockOthers' -DisplayName 'E-Waste PostgreSQL bloqueia outros IPv4' -Direction Inbound -Action Block -Protocol TCP -LocalPort $Port -RemoteAddress $blocked.ToArray() -Profile Any | Out-Null
New-NetFirewallRule -Name 'Ewaste-Team-Postgres-AllowPeers' -DisplayName 'E-Waste PostgreSQL equipe' -Direction Inbound -Action Allow -Protocol TCP -LocalPort $Port -LocalAddress $HostAddress -RemoteAddress $allowed -Profile Any | Out-Null
Write-Output 'Regras restritas criadas. Teste a partir de outro computador; Docker Desktop/WSL tambem pode exigir regra especifica.'
