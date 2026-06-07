# bugfix1: 横屏悬浮窗稳定与面板紧凑化

## 目标
- 修复游戏横屏时悬浮窗因顶部安全区不足而自动关闭的问题。
- 缩小悬浮窗控制面板，减少横屏占用空间。
- 移除悬浮窗内错误/日志文本显示，把状态文本移动到控制按钮下方。
- 移除内容区额外背景填充，并在演奏中将悬浮窗整体设置为 50% 半透明。

## 修复方案
- `OverlayPlaybackService` 不再因为 `OverlayPositionMapper.fitsSafeArea` 失败而关闭服务；横竖屏变化后只重新约束悬浮窗位置。
- `OverlayPositionMapper.constrain` 已具备安全区不足时贴顶显示的降级逻辑，服务直接复用该逻辑保持悬浮窗存活。
- `OverlayPlaybackPanel` 改为紧凑布局：减小 padding、按钮高度和间距，隐藏 `snapshot.lastError`，状态显示放到按钮组下方。
- 播放状态为 `PLAYING` 时，对面板整体应用 `alpha = 0.5f`；其他状态保持较清晰显示。

## 测试与验收
- 单元测试覆盖安全区不足时仍返回顶部可见位置。
- 运行 `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`。
- 手工验证横屏打开和竖屏切横屏时悬浮窗不关闭，停止/权限撤销/无障碍断开仍能正常退出。

## 记录规则
- 本次是问题修复，记录为 `bugfix1`，不新增 `stepX`。
- 后续功能开发继续使用 `stepX`；问题修复使用 `bugfixX`。
