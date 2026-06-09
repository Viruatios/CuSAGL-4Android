# Step9 Implementation Plan: 关于页面与应用内更新检查

## 目标
- 新增“关于”页面，展示当前版本号、仓库地址和更新检查入口。
- 在主页面标题行右侧添加三点“···”入口，点击后直接进入“关于”页面。
- 通过 GitHub 最新正式 Release 与本地 `versionName` 做语义版本比较；发现新版本时下载 `app-debug.apk` 并交给系统安装器。

## 范围内
1. 关于页与主页面入口：
   - `MainPage` 增加 `ABOUT`，继续沿用单 `MainActivity` 的 Compose 页面切换模式。
   - 首页标题区展示应用名与右侧 48dp 三点按钮，按钮无障碍说明为“关于”。
   - 关于页显示当前版本、可点击跳转的仓库地址、更新状态、最新版本和下载/安装提示。
2. Release 检查：
   - 使用 `BuildConfig.VERSION_NAME` 作为本地版本。
   - 请求 `https://api.github.com/repos/Viruatios/CuSAGL-4Android/releases/latest`。
   - 解析 `tag_name`、`html_url` 和名为 `app-debug.apk` 的 asset 下载地址。
   - 去掉版本前缀 `v/V` 后按 `major.minor.patch` 数字比较。
3. 下载与安装：
   - 使用 `HttpURLConnection` 和 `org.json`，不新增 OkHttp。
   - APK 下载到 `cacheDir/updates/app-debug.apk`，下载中使用 `app-debug.apk.part`。
   - 每次下载前清理旧更新缓存；下载失败删除临时文件。
   - 拉起系统安装器后不立即删除 APK；用户回到 App 的 `onResume` 中清理本次安装缓存。
   - App 下次启动时兜底清理 `cacheDir/updates` 中残留缓存。
   - 不预检未知来源安装权限，由系统安装器处理授权和失败提示。

## 范围外
- 不实现静默安装。
- 不支持 prerelease、draft release 或 tag-only 更新。
- 不在 App 内展示 release notes。
- 不新增导航框架、下载管理器或第三方网络库。

## 验收
- 关于页可从首页右上角三点进入，并可返回首页。
- 当前版本显示为 Gradle `versionName`。
- 无更新、发现更新、Release JSON 缺字段、缺少 `app-debug.apk` asset、网络/下载失败均有明确状态。
- 检测到更新后能下载私有缓存 APK 并拉起系统安装器。
- JVM 单元测试覆盖版本比较、Release JSON 解析和更新缓存路径/清理策略。
- 运行 `gradlew.bat :app:testDebugUnitTest :app:assembleDebug` 通过。
