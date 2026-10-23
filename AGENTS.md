# Blancall-Std — 工程规则

本工程是 **Blancall** 的无 AI 标准版。

共享规则见 reference **`blancall-shared`**（`D:\Blancall\_shared\AGENTS-shared.md`），
涵盖技术栈版本、构建命令、ABI 约束、签名机制、资源约定。
**先读那份**，本文件只写本工程的差异和不在共享文件里的知识。

## 本工程身份

| 项 | 值 |
|---|---|
| applicationId | `com.ilyskyo.blancall.noai` |
| versionName | `7.5.1-NoAI`（以 `app/build.gradle.kts` 为准） |
| versionCode | 50 |
| namespace | `com.ilyskyo.blancall`（与 Pro 相同） |

与 `Blancall Pro` 的 gradle 差异只有 applicationId 和 versionName 两行。

## 本工程独有

- **没有** `.qoder/` 和 `.trae/` 目录（Pro 工程才有）
- `gradle.properties` 与 Pro 侧**逐字一致**：`org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8`
  就在本文件 `:13`。此前本条误记为「Std 没有这一行」，据实修正 —— 两版 gradle 配置的真实
  差异只有 applicationId 与 versionName（见上表），排查构建行为不一致时别按旧说法找
- 历史 APK 在 `apk-output/`，另有一份归档在 `D:\_asm_std\apk-output\`
  （2.8 → 5.5 的完整序列，比本目录的更全，排查历史版本时看那边）
- `local.properties` 里同样有 release 签名四项，指向同一个 keystore

## applicationId 的意义

`com.ilyskyo.blancall.noai` 与 Pro 的 `com.ilyskyo.blancall` 是**不同应用**，
可以同时装在一台设备上。这是有意设计的，不要「统一」成同一个 id。

反过来说，如果要验证 Pro 版，必须先卸载旧版 —— 签名相同但 applicationId 不同，
Android 会当成两个应用，互不覆盖。

## 打发布包的完整流程

```sh
# 1. 确认 local.properties 四项齐全
# 2. 构建
gradlew.bat assembleRelease
# 3. 验证签名（不能省）
keytool -printcert -jarfile app\build\outputs\apk\release\app-release.apk
# 4. 产物带-NoAI 后缀，注意别和 Pro 的包搞混
```

## 相关位置

```
正式签名密钥   D:\Blancall\keystore\blancall-release.jks
签名备份       D:\Blancall-keystore-backup\blancall-release.jks
共享规则       D:\Blancall\_shared\AGENTS-shared.md
完整版工程     D:\Blancall\Blancall Pro\
历史APK 全序列 D:\_asm_std\apk-output\
```