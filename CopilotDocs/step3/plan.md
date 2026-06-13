## Step3 Implementation Plan: 触控注入与无障碍服务接入

### 目标
- 接入 Android `AccessibilityService`，通过 `dispatchGesture` 实现触控注入。
- 按 README 的 1920x1080 基准坐标与缩放公式实现坐标映射（使用 `WindowManager.getCurrentWindowMetrics`）。
- 提供 Android 侧 `TouchInjector` 实现以对接 core 的 `RuntimePlaybackEngine`。
- 服务生命周期由 UI 控制（本步仅提供可被 UI 调用的连接与状态能力）。

### 范围内
1. 无障碍服务注册与生命周期接入。
2. 触控注入实现（多指并行按下并保持到 keyUp）。
3. 坐标映射与按键坐标查询。
4. 与 core 触控接口对接（实现 `TouchInjector` 适配器）。

### 不在本步（明确推迟）
- 无障碍权限引导 UI/提示流程（后续 step 完成）。
- 前台服务保活与后台运行保障。

### 关键约束与输入
- `minSdk = 31`，允许使用 `dispatchGesture` 与 `StrokeDescription.continueStroke`。
- 和弦/多键需“多指同时按下并保持到 keyUp”。
- 坐标映射公式严格按 `README.md` 附录：
  - `Scale = max(W/1920, H/1080)`
  - `x = xBase * Scale + (W - 1920 * Scale) / 2`
  - `y = yBase * Scale + (H - 1080 * Scale)`

### 产出文件（建议）
- `app/src/main/java/com/culoo/cusagl_4android/accessibility/LyreAccessibilityService.kt`
  - 继承 `AccessibilityService`。
  - 暴露 `dispatchGesture` 的安全调用入口与服务可用状态。
  - 在 `onServiceConnected`/`onDestroy` 更新服务连接状态。

- `app/src/main/java/com/culoo/cusagl_4android/accessibility/AccessibilityTouchInjector.kt`
  - 实现 core 的 `TouchInjector`。
  - `keyDown` 创建并持有长按手势；`keyUp` 使用 `continueStroke` 结束。
  - 维护 `activeStrokes: MutableMap<String, StrokeDescription>` 防止重复按下与遗漏抬起。

- `app/src/main/java/com/culoo/cusagl_4android/accessibility/TouchCoordinateMapper.kt`
  - 依赖 `WindowManager` 计算屏幕宽高。
  - 将 `KeyLayout.baseCoordinates` 映射到实际屏幕坐标。

- `app/src/main/java/com/culoo/cusagl_4android/accessibility/AccessibilityServiceBridge.kt`
  - 提供服务连接状态/实例获取（供 UI 层在后续 step 控制）。
  - 不包含权限引导逻辑。

- `app/src/main/res/xml/accessibility_service_config.xml`
  - 配置 `android:canPerformGestures="true"` 等能力。

- `app/src/main/AndroidManifest.xml`
  - 注册无障碍服务与对应 `meta-data`。

### 实现要点
1. **服务接入**
   - 在 Service 中集中封装 `dispatchGesture`，避免外部直接持有 Service 引用。
   - 服务断开时清理 `activeStrokes`，并通知 `TouchInjector` 失效。

2. **触控注入（多指）**
   - `keyDown`:
     - 根据映射坐标创建 `Path`，生成 `StrokeDescription`（持续时间设置为安全上限，例如 10s）。
     - 记录到 `activeStrokes`，以支持后续 `keyUp`。
   - `keyUp`:
     - 取出对应 `StrokeDescription`，调用 `continueStroke` 结束。
     - 对于不存在的 key，记录警告日志并忽略。

3. **坐标映射**
   - 使用 `WindowManager.getCurrentWindowMetrics()` 获得宽高。
   - 将 `KeyLayout.baseCoordinates` 映射成实际屏幕坐标，并可缓存。

4. **与 core 对接**
   - `RuntimePlaybackEngine` 通过 `TouchInjector` 注入点完成桥接。
   - 若服务不可用，`AccessibilityTouchInjector` 记录日志并跳过注入。

### 验收标准
- 服务开启后，单键与和弦按下/抬起能正确触发系统手势。
- 未开启服务时不会崩溃，且有可定位日志。
- 坐标映射在 1920x1080 下保持原坐标不变，其他分辨率符合缩放公式。

### 风险与注意事项
- `dispatchGesture` 在部分 ROM 上可能存在节流或并发限制；需保留日志以便排查。
- 服务重启会丢失 `activeStrokes`，需要在断开时强制释放逻辑（由 `releaseAllTouches` 兜底）。

### 备注
- 本步完成后再更新 `README.md` 的“开发节点的记录”。

