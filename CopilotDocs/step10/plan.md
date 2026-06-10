# Step10 Implementation Plan: 结构优化与资源整理

## 目标
- 将主界面 Compose 页面从 `MainActivity` 中拆出，降低入口 Activity 的阅读负担。
- 将用户可见文本迁移到 Android 字符串资源，为后续多语言维护打基础。
- 将可调参数和目录/网络常量按领域集中，保留现有播放、权限、更新、曲谱和配置行为不变。

## 范围内
1. 字符串资源化：
   - 将首页、曲谱管理、手动创建、播放配置、关于页、弹窗、按钮、状态提示、错误前缀等 Compose 用户可见文本迁移到 `res/values/strings.xml`。
   - 同步补齐 `res/values-en/strings.xml`，保留已有 key。
   - Compose 使用 `stringResource()` 和格式化字符串；纯 Kotlin controller 的业务消息暂不强制依赖 Android `Context`。
2. 常量集中：
   - 新增 `core/CoreConstants.kt`、`accessibility/AccessibilityConstants.kt`、`overlay/OverlayConstants.kt`、`main/MainConstants.kt`。
   - 抽离最小抬键间隔、存储目录名、运行时默认值、触控/悬浮窗基准尺寸、悬浮窗服务参数、更新 URL/asset/缓存目录/网络超时等常量。
   - `BuildConfig.VERSION_NAME` 继续作为版本号来源；Intent extra key 和日志 tag 保持就近定义。
3. 页面拆分：
   - `MainActivity.kt` 保留生命周期、状态持有、launcher、协程调度、系统 Intent/FileProvider 和服务启动。
   - 页面拆到 `app/src/main/java/com/culoo/cusagl_4android/main/ui/`：
     - `MainScreen.kt`
     - `HomeScreen.kt`
     - `ScoreManagementScreen.kt`
     - `PlaybackConfigScreen.kt`
     - `AboutScreen.kt`
     - `CommonUi.kt`
   - `PendingScoreSave` 仍保持 Activity 私有；UI 只接收 `pendingOverwriteTitle`。
   - `AboutUiState` 移到 `main/AboutUiState.kt`。

## 范围外
- 不引入 Navigation Compose、ViewModel、新图标库或新依赖。
- 不实现 Step11 的动画、JSON 错误定位、保存 Snackbar 或队列复选框。
- 不改变已有 controller、runtime playback、overlay service、permission guide 的行为契约。

## 验收
- 首页、曲谱管理、手动创建、播放配置、关于页可正常切换与返回。
- 权限弹窗、覆盖确认弹窗、更新检查/下载状态文本仍正常显示。
- 曲谱导入/删除/覆盖、播放配置保存、预加载、准备演奏入口行为不变。
- `gradlew.bat :app:testDebugUnitTest :app:assembleDebug` 通过。
