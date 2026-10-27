#requires -Version 5
<#
  ScanRobot SSH deploy (non-interactive, host key pinned, password via -pw)
  plink.exe supports -hostkey (pin fingerprint, skip host key prompt) and -pw (password non-interactive).
  Usage:
    .\deploy\deploy.ps1 -Cmd "uptime"
    .\deploy\deploy.ps1 -Upload ".\app\build\outputs\apk\release\app-release.apk" "/www/wwwroot/xxx/app.apk"
  Add -DryRun to preview without executing.
#>
[CmdletBinding()] param(
  [string]$Cmd,
  [string]$UploadLocal,
  [string]$UploadRemote,
  [switch]$DryRun
)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot

$envFile = Join-Path $here 'server.env'
if (-not (Test-Path $envFile)) { throw "Missing credentials file: $envFile (gitignored, keep local)" }
Get-Content $envFile | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith('#') -and $line -match '^([A-Z_]+)=(.*)$') {
    Set-Item -Path "env:$($Matches[1])" -Value $Matches[2].Trim()
  }
}

$plink = Join-Path $here 'bin\plink.exe'
$pscp  = Join-Path $here 'bin\pscp.exe'
if (-not (Test-Path $plink)) { throw "Missing plink.exe: $plink (copy from QR-system deploy_tools)" }

$user = $env:SRV_USER
$hostaddr = $env:SRV_HOST
$target = "${user}@${hostaddr}"
$port = $env:SRV_PORT
$pass = $env:SRV_PASS
$hostkey = $env:SRV_HOSTKEY

# -batch    disable interactive prompts
# -hostkey  pin fingerprint, skip host key confirmation (equivalent to your -hostkey pin)
# -pw       password non-interactive
$common = @('-batch','-hostkey',$hostkey,'-P',$port,'-pw',$pass)

if ($UploadLocal -and $UploadRemote) {
  if (-not (Test-Path $pscp)) { throw "Missing pscp.exe: $pscp" }
  Write-Host "[UPLOAD] $UploadLocal -> ${target}:${UploadRemote}"
  if ($DryRun) { Write-Host "[DRY] $pscp $($common -join ' ') `"$UploadLocal`" `"${target}:${UploadRemote}`""; return }
  & $pscp @common "$UploadLocal" "${target}:${UploadRemote}"
}
elseif ($Cmd) {
  Write-Host "[EXEC] ${target} : $Cmd"
  if ($DryRun) { Write-Host "[DRY] $plink $($common -join ' ') ${target} $Cmd"; return }
  & $plink @common $target $Cmd
}
else {
  Write-Host "Usage: -Cmd <remote-command>  or  -Upload <local> <remote>  (optional -DryRun)"
}
