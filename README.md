# 扫码机器人 (ScanRobot)

基于 **Android + Jetpack Compose** 的智能扫码工具应用。

[![Release](https://img.shields.io/github/v/release/qq145968/QR-Robot?label=Release&include_prereleases)](https://github.com/qq145968/QR-Robot/releases)
[![CI Build](https://github.com/qq145968/QR-Robot/actions/workflows/build-apk.yml/badge.svg)](https://github.com/qq145968/QR-Robot/actions/workflows/build-apk.yml)

## 下载最新版

👉 [Release 页面](https://github.com/qq145968/QR-Robot/releases) 获取最新 `ScanRobot-v*.apk`

---

## 功能特性

### 扫码
- **4 种扫码模式**：全屏 / 新全屏 / 微信风格 / 半屏
- CameraX + ML Kit 实时识别（条码 / 二维码 / 数据矩阵 等）
- 批量连续扫码自动归档为批次，支持批次详情与管理
- 闪光灯控制、PCM 扫码提示音、震动反馈

### 数据 & 用户
- SharedPreferences 本地持久化扫码历史（JSON 序列化），按批次归档
- 复制到剪贴板 / 导出 CSV
- 登录 / 注册 / 忘记密码 + 滑动验证码 + 头像上传
- 消息通知弹窗 / 版本内更新提示

### 界面
- **Material 3** 明暗主题自适应
- 公告无缝跑马灯、再按一次退出
- Kotlin DSL + MVVM（ViewModel + StateFlow）

---

## 技术栈

| 类别 | 技术 |
|---|---|
| UI | Jetpack Compose 2024.06 + Material 3 |
| 相机 | AndroidX CameraX 1.3.1 |
| 扫码 | Google ML Kit Barcode Scanning 17.2.0 |
| 网络 | OkHttp 4.12 + Gson 2.10.1 |
| 图片 | Coil Compose 2.5.0 |
| 构建 | Gradle 8.7 · AGP 8.5.0 · Kotlin 1.9.24 · JDK 17 · compileSdk 34 |
| CI/CD | GitHub Actions |

---

## 目录结构

```
.
├── app/
│   ├── build.gradle.kts          # Android 模块配置（versionCode=7, versionName=1.0.7）
│   ├── debug.keystore            # release 默认签名（含 gitignore 例外，CI 直接使用）
│   ├── proguard-rules.pro        # R8 规则
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/scanrobot/app/
│       │   ├── MainActivity.kt
│       │   ├── ScanApp.kt
│       │   ├── data/             Models · ScanStore
│       │   ├── network/          ApiClient
│       │   ├── ui/               theme · Auth · Home · Scanner · Detail · CameraPreview · ContentDialogs
│       │   ├── util/             BeepManager · QRCodeGenerator
│       │   └── viewmodel/        ScanViewModel
│       └── res/
├── deploy/
│   ├── build-release.ps1         # 本地增量 Release 构建脚本（推荐）
│   ├── build-debug.ps1           # 本地 Debug 构建脚本
│   ├── deploy.ps1                # SSH/SCP 服务器部署（需 server.env，不上库）
│   ├── deploy-apk.ps1            # 同 deploy.ps1
│   ├── sync-api.ps1              # 同步 PHP API 后端
│   └── README.md
├── php-api/                      # PHP 后端（登录 / 公告 / 用户 / 消息 / 头像）
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── .github/workflows/
│   ├── build-apk.yml             # push main → Release APK → Artifact（保留 30 天）
│   └── release-publish.yml       # tag v* → Release APK + GitHub Release
├── build.gradle.kts              # 根（AGP 8.5.0 + Kotlin 1.9.24）
├── settings.gradle.kts           # 仓库配置（本地用阿里云镜像，CI 自动覆盖）
├── gradle.properties             # Gradle 参数（-Xmx4096m · daemon · parallel · config cache）
├── gradlew · gradlew.bat
├── README.md
└── .gitignore
```

---

## 本地增量构建

推荐直接用项目内的 PowerShell 脚本——JDK 17 + Android SDK 34 + Gradle 8.7 都放在 `tools/` 下（已 `.gitignore`，不会污染仓库），**第一次跑会自动下载依赖并构建，第二次起走 Gradle 增量 + configuration cache，通常几十秒**。

```powershell
# Release 增量构建（默认不 clean）
.\deploy\build-release.ps1

# Debug 增量构建
.\deploy\build-debug.ps1

# 全量 clean + Release
.\deploy\build-release.ps1 -Clean
```

构建产物：`app\build\outputs\apk\release\app-release.apk`（debug 输出同目录下 `app-debug.apk`）。

> 环境要求一次性：Windows 本机没有 JDK / Android SDK 时，把 [Temurin JDK 17](https://adoptium.net/) 的 zip 解压到 `tools\jdk\`，[Android SDK cmdline-tools](https://developer.android.com/studio#command-tools) 解压到 `tools\android-sdk\cmdline-tools\latest\`，然后在 SDK manager 里装 `platforms;android-34` + `build-tools;34.0.0` + `platform-tools`。

---

## GitHub Actions CI

### push 自动构建 Artifact

推到 `main` 分支触发 [build-apk.yml](.github/workflows/build-apk.yml)：
- JDK 17 + Gradle 8.7（`gradle/actions/setup-gradle`）
- `assembleRelease`（R8 压缩 + 资源缩减）
- Artifact 名 `scan-robot-release-apk`，保留 30 天

### 正式发布（打 tag）

```bash
# 1. 改 app/build.gradle.kts 里的 versionCode / versionName
# 2. 打 tag 推送
git tag -a v1.0.7 -m "ScanRobot v1.0.7"
git push && git push origin v1.0.7
```

tag 匹配 `v*` 触发 [release-publish.yml](.github/workflows/release-publish.yml)：
- 自动从 `build.gradle.kts` 取 `versionName`，把 APK 重命名为 `ScanRobot-v{version}.apk`
- 用 `softprops/action-gh-release` 创建 GitHub Release 并上传
- 发布地址：https://github.com/qq145968/QR-Robot/releases/tag/v{version}

签名：两个 workflow 都直接用仓库内 `app/debug.keystore` 签 release（`KEYSTORE_FILE=debug.keystore / android / debug-key / android`，已写死在 `env:` 里，不依赖 GitHub Secrets）。

---

## 版本历史

| 版本 | 说明 |
|---|---|
| **v1.0.7** | 本地构建环境固化到 `tools/`（JDK 17 + Android SDK 34 + Gradle 8.7）；新增 `deploy/build-release.ps1` 增量 Release 构建；重写 README |
| **v1.0.6** | CI 工作流重写：签名硬编码进 `env:`、移除服务器部署步骤；Gradle distributionUrl 改官方源 |
| 1.0.5 | 4 种扫码模式 / 用户认证 + 滑动验证码 / 消息通知 / 一键清除数据 / 公告跑马灯 / 退出美化 |
| 1.0.0 | 首个正式版本：扫码 / 批次管理 / 导出 / 公告 / 头像上传 |

---

## 权限说明

| 权限 | 用途 |
|---|---|
| CAMERA | 扫码 |
| VIBRATE | 扫码成功震动 |
| INTERNET | 登录 / 公告 / 消息 |
| ACCESS_NETWORK_STATE | 检测网络 |
| REQUEST_INSTALL_PACKAGES | 应用内更新（预留） |

---

## License

仅供学习交流使用。
