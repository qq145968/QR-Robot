#requires -Version 5
<#
  Deploy APK to server (non-interactive, hostkey pinned)
  Usage:
    .\deploy\deploy-apk.ps1 -ApkPath ".\app\build\outputs\apk\debug\app-debug.apk"
    .\deploy\deploy-apk.ps1 -ApkPath ".\app\build\outputs\apk\release\app-release.apk" -Version "1.0.6"
  Reads credentials from deploy\server.env
#>
[CmdletBinding()] param(
  [Parameter(Mandatory=$true)][string]$ApkPath,
  [string]$Version,
  [switch]$DryRun
)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$projectRoot = Split-Path $here -Parent

# Load credentials
$envFile = Join-Path $here 'server.env'
if (-not (Test-Path $envFile)) { throw "Missing: $envFile" }
Get-Content $envFile | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith('#') -and $line -match '^([A-Z_]+)=(.*)$') {
    Set-Item -Path "env:$($Matches[1])" -Value $Matches[2].Trim()
  }
}

$pscp  = Join-Path $here 'bin\pscp.exe'
$plink = Join-Path $here 'bin\plink.exe'
if (-not (Test-Path $pscp)) { throw "Missing pscp.exe: $pscp" }

$target = "$($env:SRV_USER)@$($env:SRV_HOST)"
$port = $env:SRV_PORT
$pass = $env:SRV_PASS
$hostkey = $env:SRV_HOSTKEY

# Resolve APK path
if (-not (Test-Path $ApkPath)) {
  $fullPath = Join-Path $projectRoot $ApkPath
  if (-not (Test-Path $fullPath)) { throw "APK not found: $ApkPath" }
  $ApkPath = $fullPath
}

# Determine remote filename
$fileName = Split-Path $ApkPath -Leaf
if ($Version) {
  $fileName = "ScanRobot-v$Version.apk"
}

$remotePath = "/www/wwwroot/qr.wzdi.cn/apks/$fileName"
$common = @('-batch','-hostkey',$hostkey,'-P',$port,'-pw',$pass)

Write-Host "=== Deploy APK to Server ==="
Write-Host "Local:  $ApkPath"
Write-Host "Remote:  ${target}:${remotePath}"
$size = [math]::Round((Get-Item $ApkPath).Length / 1MB, 2)
Write-Host "Size:    $size MB"
Write-Host ""

if ($DryRun) {
  Write-Host "[DRY] $pscp $($common -join ' ') `"$ApkPath`" `"${target}:${remotePath}`""
  return
}

# Ensure apks directory exists
& $plink @common $target "mkdir -p /www/wwwroot/qr.wzdi.cn/apks/" 2>&1 | ForEach-Object { Write-Host "  $_" }

# Upload
& $pscp @common "$ApkPath" "${target}:${remotePath}" 2>&1 | ForEach-Object { Write-Host "  $_" }

if ($LASTEXITCODE -eq 0) {
  # Set permissions
  & $plink @common $target "chown www:www '$remotePath' && chmod 644 '$remotePath'" 2>&1 | ForEach-Object { Write-Host "  $_" }
  Write-Host ""
  Write-Host "DEPLOY SUCCESS"
  Write-Host "APK URL: https://qr.wzdi.cn/apks/$fileName"
} else {
  Write-Host ""
  Write-Host "DEPLOY FAILED (exit code: $LASTEXITCODE)"
  exit $LASTEXITCODE
}
