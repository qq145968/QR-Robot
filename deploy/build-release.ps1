#requires -Version 5
<#
  ScanRobot 本地增量 Release 构建脚本
  用法: .\deploy\build-release.ps1 [-Clean] [-Install]
  依赖: 项目 tools 目录下的 JDK17 + Android SDK（已加入用户环境变量）
#>
[CmdletBinding()] param(
  [switch]$Clean,
  [switch]$Install
)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$projectRoot = Split-Path $here -Parent
$tools = Join-Path $projectRoot 'tools'

$env:JAVA_HOME      = Join-Path $tools 'jdk'
$env:ANDROID_HOME   = Join-Path $tools 'android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = Join-Path $tools 'gradle_home'
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

if (-not (Test-Path "$env:JAVA_HOME\bin\java.exe")) { throw "JDK 未找到: $env:JAVA_HOME" }
if (-not (Test-Path "$env:ANDROID_HOME\platforms\android-34")) { throw "Android SDK 34 未找到: $env:ANDROID_HOME" }

$gradlew = Join-Path $projectRoot 'gradlew.bat'
if (-not (Test-Path $gradlew)) { throw "gradlew.bat 未找到: $gradlew" }

Write-Host "=== ScanRobot Release 增量构建 ==="
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "ANDROID_HOME: $env:ANDROID_HOME"
Write-Host "GRADLE_USER_HOME: $env:GRADLE_USER_HOME"
Write-Host ""

# 默认不执行 clean，走 Gradle 增量编译；-Clean 时才全量
$tasks = @()
if ($Clean) { $tasks += 'clean' }
$tasks += 'assembleRelease'
if ($Install) { $tasks += 'installRelease' }

$gradleArgs = $tasks + @('--daemon', '--configuration-cache', '--stacktrace')

Write-Host "Running: gradlew $($gradleArgs -join ' ')"
Push-Location $projectRoot
try { & $gradlew @gradleArgs } finally { Pop-Location }
$exitCode = $LASTEXITCODE

if ($exitCode -ne 0) { Write-Host "BUILD FAILED (exit $exitCode)"; exit $exitCode }

$apkDir = Join-Path $projectRoot 'app\build\outputs\apk\release'
$apk = Get-ChildItem $apkDir -Filter *.apk -ErrorAction SilentlyContinue | Select-Object -First 1
if ($apk) {
  Write-Host ""
  Write-Host "BUILD SUCCESS"
  Write-Host ("APK: {0} ({1} MB)" -f $apk.FullName, [math]::Round($apk.Length / 1MB, 2))
} else {
  Write-Host "BUILD SUCCESS (未找到 APK 输出)"
}
