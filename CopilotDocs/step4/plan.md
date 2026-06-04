# Step4 Implementation Plan: 悬浮窗播放控制与前台服务

## 目标
- 实现由前台服务承载的 Compose 悬浮控制面板，支持播放控制、状态显示和安全的位置调整。
- 悬浮窗使用 1920x1080 基准坐标、与琴键相同的缩放及水平居中思路，但垂直方向顶部对齐。
- 悬浮窗仅能在顶部安全区域移动，且播放期间锁定位置。

## 播放状态契约
- 为 `RuntimePlaybackEngine` 增加线程安全的 `PlaybackSnapshot` 与监听接口。
- 快照包含播放状态、当前曲谱、队列索引、切歌可用性及最近错误。
- 曲谱加载失败时队列模式跳过失败曲目；单曲或全部失败后进入停止状态并保留错误。
- `AccessibilityServiceBridge` 支持多个监听者；触控注入器提供显式注销与清理入口。

## 前台悬浮服务与面板
- 新增 `OverlayPlaybackService`，持有播放引擎、触控注入器、Compose 面板和常驻通知。
- 使用 `PlaybackSessionRequest(queue, config)` 作为后续主页面启动播放会话的正式输入。
- 服务使用 `START_NOT_STICKY`；自然播放完成后保留面板；手动停止、无障碍断开、权限撤销或面板创建失败时立即退出。
- 面板显示当前曲谱、状态、最近错误及上一首、开始/暂停、停止、下一首按钮。
- 当前占位 `MainActivity` 提供使用第一首已存储曲谱和默认配置的临时验收入口。

## 悬浮窗位置规则
- 缩放比例：`Scale = max(W / 1920, H / 1080)`。
- 水平映射：`xTarget = xBase * Scale + (W - 1920 * Scale) / 2`。
- 顶部对齐垂直映射：`yTarget = yBase * Scale`。
- 基准初始顶部中心锚点为 `(960, 40)`。
- 首行琴键基准 Y 为 `670`，预留 `80px` 安全间距；安全底边由转换后的首行琴键 Y 减去转换后的间距得到，1920x1080 下为 `590`。
- `IDLE`、`PAUSED`、`STOPPED` 允许拖动；`PLAYING` 时立即取消拖动并锁定位置。
- 拖动和屏幕尺寸变化后将面板约束在可见水平范围和顶部安全区域内；不持久化位置。

## Android 配置
- 声明 `SYSTEM_ALERT_WINDOW`、`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_SPECIAL_USE`。
- 前台服务声明 `specialUse` 类型及自动化演奏用途说明。
- 使用低打扰常驻通知，通知不提供控制动作。
- Step8 再实现完整权限解释和引导体验。

## 测试与验收
- 单元测试覆盖播放快照、状态监听、切歌边界、曲谱失败，以及顶部对齐映射、安全区域约束和拖动状态规则。
- 手工验证悬浮窗初始位置、拖动约束、播放锁定、后台运行及停止清理。
- 运行 `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`。

## 范围边界
- 不提前实现 Step5 正式主页面。
- 不实现 Step8 完整权限引导或游戏界面状态检测。

## Step4 后维护任务：AGENTS.md 更新技能
- 创建 workspace skill `update-agent-instructions`，用于基于代码库现状对项目根目录现有 `AGENTS.md` 做最小、可验证的增量更新。
- 技能必须先完整读取目标 `AGENTS.md`，再以一次 glob 搜索收集常见 AI 指令文件和 README，随后分析代码库差异。
- 仅记录可发现、对编码代理有直接帮助的变化；保留仍准确的原文、结构、章节顺序和格式。
- 更新后检查 diff，并向用户总结新增、修改与删除内容。
