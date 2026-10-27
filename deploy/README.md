# 扫码机器人 部署配置（deploy/）

本目录存放扫码机器人的部署与 SSH 配置。**凭据文件已被 `.gitignore` 忽略，不会提交到公开仓库。**

## 文件说明

| 文件 | 用途 | 是否提交 |
|------|------|----------|
| `server.env` | 服务器/宝塔/GitHub 凭据（明文，本地持有） | ❌ 已 gitignore |
| `known_hosts` | 服务器 host key（已预扫写死） | ✅ 可提交（仅公钥） |
| `ssh_config` | OpenSSH ssh.exe 配置（主机别名） | ✅ 可提交 |
| `deploy.ps1` | 非交互部署脚本（plink -hostkey -pw） | ✅ 可提交 |
| `bin/plink.exe` | PuTTY 命令行客户端（支持 -hostkey/-pw） | ⚠ 二进制，按需 |
| `bin/pscp.exe` | PuTTY scp 客户端（上传文件） | ⚠ 二进制，按需 |

## SSH 连接方式（非交互，host key 写死）

用 plink.exe：`-batch` 禁交互、`-hostkey` 写死指纹跳过 host key 确认、`-pw` 密码非交互传入。

```powershell
# 预览（不实际连接）
.\deploy\deploy.ps1 -Cmd "uptime" -DryRun

# 实际执行远程命令（需你后续给指令才跑）
.\deploy\deploy.ps1 -Cmd "uptime"

# 上传文件
.\deploy\deploy.ps1 -Upload ".\app\build\outputs\apk\release\app-release.apk" "/www/wwwroot/xxx/app.apk"
```

## host key 指纹（写死）

- ED25519: `SHA256:eT3vCM2a8xEKKdp7fIlz6kFrwwrDh6hlkiLWE68m4lc`
- RSA: `SHA256:pQb/VlIJfmEw3GCYJ7eiPvHFp6gneWVaKSwoZqND1fc`

## 安全提醒

`server.env` 含明文密码与 GitHub 令牌，**切勿提交**。已在 `.gitignore` 中忽略 `deploy/server.env`、`deploy/secrets/`、`deploy/*.ppk` 等。建议后续轮换已暴露的凭据。
