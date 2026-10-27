#requires -Version 5
<#
  Sync PHP API files to server (non-interactive, hostkey pinned)
  Usage: .\deploy\sync-api.ps1
  Reads credentials from deploy\server.env
#>
[CmdletBinding()] param(
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

$plink = Join-Path $here 'bin\plink.exe'
$pscp  = Join-Path $here 'bin\pscp.exe'
if (-not (Test-Path $pscp)) { throw "Missing pscp.exe: $pscp" }

$target = "$($env:SRV_USER)@$($env:SRV_HOST)"
$port = $env:SRV_PORT
$pass = $env:SRV_PASS
$hostkey = $env:SRV_HOSTKEY

# Source: local php-api directory
$srcDir = Join-Path $projectRoot 'php-api'
if (-not (Test-Path $srcDir)) { throw "php-api directory not found: $srcDir" }

# Destination: server api directory
$destPath = '/www/wwwroot/qr.wzdi.cn/api/'

$common = @('-batch','-hostkey',$hostkey,'-P',$port,'-pw',$pass)

Write-Host "=== Sync PHP API to Server ==="
Write-Host "Source: $srcDir"
Write-Host "Target: ${target}:${destPath}"
Write-Host ""

# Ensure target directory exists
Write-Host "Ensuring target directory exists..."
if ($DryRun) {
  Write-Host "[DRY] mkdir -p $destPath"
} else {
  & $plink @common $target "mkdir -p $destPath" 2>&1 | ForEach-Object { Write-Host "  $_" }
}

# Upload all PHP files
Write-Host "Uploading PHP files..."
$phpFiles = Get-ChildItem $srcDir -Filter '*.php' -File
foreach ($f in $phpFiles) {
  $localFile = $f.FullName
  $remoteFile = "$destPath$($f.Name)"
  Write-Host "  -> $($f.Name)"
  if ($DryRun) {
    Write-Host "     [DRY] $pscp $($common -join ' ') `"$localFile`" `"${target}:${remoteFile}`""
  } else {
    & $pscp @common "$localFile" "${target}:${remoteFile}" 2>&1 | ForEach-Object { Write-Host "     $_" }
  }
}

# Upload subdirectories (admin/)
$adminDir = Join-Path $srcDir 'admin'
if (Test-Path $adminDir) {
  Write-Host "Uploading admin/ directory..."
  $adminFiles = Get-ChildItem $adminDir -Filter '*.php' -File
  if ($DryRun) {
    Write-Host "  [DRY] mkdir -p ${destPath}admin/"
  } else {
    & $plink @common $target "mkdir -p ${destPath}admin/" 2>&1 | ForEach-Object { Write-Host "  $_" }
  }
  foreach ($f in $adminFiles) {
    Write-Host "  -> admin/$($f.Name)"
    if ($DryRun) {
      Write-Host "     [DRY]"
    } else {
      & $pscp @common $f.FullName "${target}:${destPath}admin/$($f.Name)" 2>&1 | ForEach-Object { Write-Host "     $_" }
    }
  }
}

# Set permissions
Write-Host "Setting permissions..."
if ($DryRun) {
  Write-Host "[DRY] chown -R www:www $destPath"
} else {
  & $plink @common $target "chown -R www:www $destPath && chmod -R 755 $destPath" 2>&1 | ForEach-Object { Write-Host "  $_" }
}

Write-Host ""
Write-Host "SYNC COMPLETE"
