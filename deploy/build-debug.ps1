#requires -Version 5
<#
  Local debug build script for ScanRobot
  Usage: .\deploy\build-debug.ps1
  Uses JDK17 + Android SDK + Gradle from yilaiwenjian
#>
[CmdletBinding()] param(
  [switch]$Clean,
  [switch]$Install
)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$projectRoot = Split-Path $here -Parent

# 环境路径（项目内 tools 目录，已被 .gitignore 忽略）
$tools       = Join-Path $projectRoot 'tools'
$javaHome    = Join-Path $tools 'jdk'
$androidHome = Join-Path $tools 'android-sdk'
$gradleUserHome = Join-Path $tools 'gradle_home'

# Set environment for this process
$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $androidHome
$env:ANDROID_SDK_ROOT = $androidHome
$env:GRADLE_USER_HOME = $gradleUserHome
$env:PATH = "$javaHome\bin;$androidHome\platform-tools;$env:PATH"

Write-Host "=== ScanRobot Debug Build ==="
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "ANDROID_HOME: $env:ANDROID_HOME"
Write-Host "GRADLE_USER_HOME: $env:GRADLE_USER_HOME"
Write-Host ""

# Verify tools
if (-not (Test-Path "$javaHome\bin\java.exe")) { throw "JDK not found: $javaHome" }
if (-not (Test-Path "$androidHome\platforms")) { throw "Android SDK not found: $androidHome" }

# Gradle wrapper
$gradlew = Join-Path $projectRoot 'gradlew.bat'
if (-not (Test-Path $gradlew)) { throw "gradlew.bat not found: $gradlew" }

# Build command
$tasks = @('assembleDebug')
if ($Clean) { $tasks = @('clean') + $tasks }
if ($Install) { $tasks += 'installDebug' }

$gradleArgs = @($tasks) + @(
  '--daemon',
  '--configuration-cache',
  '--stacktrace'
)

Write-Host "Running: gradlew $($gradleArgs -join ' ')"
Write-Host ""

Push-Location $projectRoot
& $gradlew @gradleArgs
$exitCode = $LASTEXITCODE
Pop-Location

if ($exitCode -eq 0) {
  $apkPath = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
  if (Test-Path $apkPath) {
    $size = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
    Write-Host ""
    Write-Host "BUILD SUCCESS"
    Write-Host "APK: $apkPath ($size MB)"
  } else {
    Write-Host ""
    Write-Host "BUILD SUCCESS (APK path may differ)"
  }
} else {
  Write-Host ""
  Write-Host "BUILD FAILED (exit code: $exitCode)"
  exit $exitCode
}
