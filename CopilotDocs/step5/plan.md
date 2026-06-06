# Step5 Implementation Plan: 正式主页面与准备演奏入口

## 目标
- 将 `MainActivity` 从 Step4 临时验收入口改为正式 Compose 主页面。
- 主页面暂时自动使用排序后的第一首已存曲谱，不做曲谱选择、多选、导入或删除。
- 播放配置暂时固定使用 `PlaybackConfig()`；配置入口跳转到占位页面，Step7 再实现真实配置。
- “准备演奏”只有在曲谱缓存已预加载、悬浮窗权限已授予、无障碍服务已连接时启用。

## 范围内
1. 主页面状态模型：
   - 第一首曲谱名。
   - 缓存预加载状态。
   - 加载中状态。
   - 错误信息。
   - 悬浮窗权限状态。
   - 无障碍连接状态。
   - 当前页面。
2. 主页面 Compose UI：
   - 展示当前自动选中的第一首曲谱。
   - 曲谱管理入口按钮，跳转轻量占位页。
   - 自定义播放配置入口按钮，跳转轻量占位页。
   - “预加载曲谱”按钮，在 IO 线程完成解析、预烘焙和缓存保存。
   - “准备演奏”按钮，启动 `OverlayPlaybackService`。
3. 页面刷新：
   - 调用 `ScoreStorage.listAndNormalizeScores(filesDir)` 获取第一首曲谱。
   - 调用 `ScoreStorage.cleanExpiredCaches(...)` 清理过期缓存。
   - 若第一首曲谱已有有效缓存，则标记为已预加载。
   - 刷新悬浮窗权限和无障碍连接状态。
4. 最小权限入口：
   - 悬浮窗未授权时显示授权按钮并打开系统设置。
   - 无障碍未连接时只显示状态提示，完整引导留给 Step8。

## 范围外
- 不实现曲谱导入、删除、编辑或列表选择。
- 不实现播放配置表单。
- 不新增 Navigation Compose 或 ViewModel 依赖。
- 不实现 Step8 的完整权限说明和引导流程。

## 实现约束
- 不改变 `PlaybackSessionRequest`、`OverlayPlaybackService`、`PlaybackConfig` 的现有契约。
- 将“列出第一首曲谱、清理缓存、判断/构建缓存”的逻辑从 Compose UI 中抽离，便于 JVM 单元测试。
- `MainActivity` 用内部 Compose 状态和 `lifecycleScope` 控制异步加载。
- 缓存预加载失败时保留错误状态，并禁止准备演奏。

## 验收
- 无曲谱时预加载和准备演奏不可用。
- 有曲谱但无缓存时，预加载成功后写入 `filesDir/cache/<name>.json`。
- 有有效缓存时，刷新后直接允许准备演奏。
- 缓存比曲谱旧时，刷新会清理旧缓存并要求重新预加载。
- 曲谱 JSON 无效时，预加载失败并显示错误。
- 曲谱管理/播放配置入口可进入占位页并返回。
- 运行 `gradlew.bat :app:testDebugUnitTest :app:assembleDebug` 通过。
