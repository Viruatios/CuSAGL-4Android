## Step2 Implementation Plan: Runtime Playback Scheduler + UI Control Hooks

聚焦实现运行时播放调度与控制接口，完成延迟-自旋混合休眠策略，并在核心层提供可供后续悬浮窗 UI 调用的播放控制方法。用户配置解析属于次要内容，目标是建立最小可用的配置数据结构与约束，确保后续原生 UI 能将配置传入运行时模块。

### 当前范围
1. 运行时播放调度：对齐 `OriginScripts/CuSimpAutoGenshinLyre/player.js` 的时间轴扫描与执行时序。
2. 时间基准与休眠策略：采用 `SystemClock.uptimeMillis` 与“延迟-自旋”混合等待。
3. 控制接口：`start`/`pause`/`stop`/`previous`/`next` + `releaseAllTouches`。
4. 关键语义约束：
   - `pause` 重置当前曲目进度，下一次 `start` 从当前曲目起始位置播放。
   - `previous`/`next` 触发时立刻暂停并切换曲目。
   - `pause`/`stop`/`previous`/`next` 必须立刻调用 `releaseAllTouches`。
   - 队列边界：队列循环模式回绕；队列单次模式到边界停止。
5. 用户配置解析：建立播放配置与队列数据模型，提供参数规范化方法，后续 UI 将配置输入传递到运行时模块。

### 产出文件（核心代码）
- `app/src/main/java/com/culoo/cusagl_4android/core/RuntimePlaybackConfig.kt`
  - `PlayType`（单曲单次/单曲循环/队列单次/队列循环）。
  - `PlaybackConfig`：包含 `startTimeEpochMs`、`queueIntervalMs`、`repeatTimes`、`repeatIntervalMs` 等。
  - 可配置常量与默认值（沿用 JS 行为基线）。

- `app/src/main/java/com/culoo/cusagl_4android/core/RuntimePlaybackEngine.kt`
  - `RuntimePlaybackEngine`：运行时调度主控制器。
  - 控制方法：`start()`、`pause()`、`stop()`、`previous()`、`next()`、`releaseAllTouches()`。
  - 状态机：`IDLE`/`PLAYING`/`PAUSED`/`STOPPED`。

- `app/src/main/java/com/culoo/cusagl_4android/core/RuntimePlaybackInterfaces.kt`
  - `TimeSource`：默认使用 `SystemClock.uptimeMillis`。
  - `Sleeper`：默认使用 `Thread.sleep`。
  - `TouchInjector`：抽象按键下压/抬起。
  - `CacheProvider`：运行时按曲目名加载缓存（必要时回退到解析+生成缓存）。

- `app/src/main/java/com/culoo/cusagl_4android/core/KeyLayout.kt`
  - `README.md` 键位表常量（按键名与 1920x1080 基准坐标）。
  - `ALL_KEYS` 用于 `releaseAllTouches` 覆盖范围。

### 行为细节（实现要点）
1. 延迟-自旋策略
   - 对齐 JS 行为：当 `remain > SPIN_THRESHOLD_MS` 先 `sleep(remain - SPIN_THRESHOLD_MS)`，随后自旋等待到目标时间。
   - `SPIN_THRESHOLD_MS` 为可配置常量，默认 5ms（JS 规则）。

2. 时间源与最终保护等待
   - 时间源：`SystemClock.uptimeMillis`。
   - 尾部保护等待：`finalRemain = start + total + gap * FINAL_GAP_MULTIPLIER - now`，默认倍数为 8。

3. 播放控制接口与状态机
   - `start()`：从当前曲目起点开始播放，等待 `startTimeEpochMs`（若在未来）。
   - `pause()`：停止调度线程，重置当前曲目进度，调用 `releaseAllTouches()`。
   - `stop()`：停止调度线程，重置当前曲目进度与队列索引，调用 `releaseAllTouches()`。
   - `previous()`/`next()`：立刻执行 `pause()`，再切换到前/后曲目（队列边界按播放模式处理）。

4. 用户配置解析（最小契约）
   - `PlaybackConfig` 对外暴露 `normalize()`，确保参数非负、默认值兜底。
   - 后续原生 UI 必须将用户配置转换为 `PlaybackConfig` 并传入运行时模块。

### 可配置常量清单（默认值沿用 JS）
- `DEFAULT_SPIN_THRESHOLD_MS = 5`
- `DEFAULT_FINAL_GAP_MULTIPLIER = 8`
- `DEFAULT_START_WAIT_SAFETY_MARGIN_MS = 100`
- `DEFAULT_START_WAIT_POLL_MS = 5`
- `DEFAULT_QUEUE_INTERVAL_MS = 0`
- `DEFAULT_REPEAT_TIMES = 0`（0 表示无限循环）
- `DEFAULT_REPEAT_INTERVAL_MS = 0`

### 后续步骤记录（本步不实现）
1. 无障碍服务注入实现（基于 `KeyLayout` 的坐标映射和触控注入）。
2. 悬浮窗 UI 与配置 UI 的原生交互实现，并通过 step2 的控制接口传递交互事件与配置。
3. 更完善的调度压力测试与播放精度评测（不同设备与刷新率）。

