# Android 原生移植开发指南 (Android Port Guide)

本指南针对无 Android 开发经验的开发者，提供将基于 BetterGI (Windows) 的原神自动弹琴 JS 脚本移植至 Android 平台的完整路线图。

## 方案简述：混合架构 (JS 预处理 + Kotlin 原生演奏)

为了最大化性能并保证跨端运行极高的定时精度，我们采用**混合架构**：
1. **JS 层预处理 (QuickJS)**：复用 `main.js` 中的曲谱解析、和弦合并及生成 `mergedTimeline` 缓存的逻辑。当按键指令时间轴对象数组在内存中生成完毕后，将其直接通过桥接接口移交回 Kotlin 层，JS 脚本即完成计算使命。
2. **Kotlin 原生层 (执行器)**：Kotlin 端完全接管并替代原 JS 中的 `playCachedTimeline` 运行时扫描播放器。原生协程利用 Android 底层高精度时钟（`System.nanoTime`）遍历时间轴数组，并通过无障碍服务高频、精准地执行屏幕并发点击操作。消除跨语言高频通信带来的延迟与抖动。

---

## 核心实现机制映射

| 核心实现机制 | PC端 (Better GI 宿主环境)                           | Android 端 (我们即将打造的新宿主环境)            | 说明                                                                                                                                 |
| -- | --------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **窗体UI控件** | **窗体UI控件**                                      | `WindowManager` 悬浮窗                           | 用于在原神游戏画面之上悬浮显示“播放/暂停”面板。需申请 `SYSTEM_ALERT_WINDOW` 权限。                                                   |
| **键盘按键模拟** | **键盘按键模拟** (`PostMessage.keyDown`)            | `AccessibilityService.dispatchGesture`           | 无障碍手势服务，由 Kotlin 执行器直接调度，不再直接暴露给 JS。 |
| **JS 预处理环境** | **JS 执行环境** (`ClearScript`)                     | `QuickJS-Android` (由 CashApp 提供)              | 仅用于在准备阶段执行 `main.js`，读取曲谱并完成时间轴矩阵计算，算出结果数组后立即交还给 Kotlin 宿主。                                                |
| **文件/日志 API** | **文件读写及日志集成** | 绑定自定义 `file` 和 `log` 对象到 QuickJS 环境 | 封装混合 AssetManager 和内部存储的 JS 桥接，保证 JS 可以完整顺畅地读取乐谱、读取/修改 `settings.json` 并持久化缓存。|
| **原生播放调度** | **JS 全局 `sleep` 与自旋循环**                      | Kotlin Coroutines + `System.nanoTime()` 调度机制 | 完全由 Kotlin 接管原本脚本中基于自旋 (`while`) 和 `sleep` 的指令调度派发，消除跨 JNI 调用的高频性能开销与 GC 停顿影响。                                   |

---

## 里程碑与开发流程拆解

建议采用分步骤实现策略，切勿一开始就试图导入整个 `main.js`：

### 阶段 1：跑通无障碍点按服务 (Accessibility Foundation)

**目标**：App 能够跨应用点击屏幕指定的坐标。

1. 在 `AndroidManifest.xml` 注册 `AccessibilityService`。
2. 创建 `accessibility_service_config.xml` 配置，指定可以监听全部应用。
3. 在 Service 内部实现 `fun clickAt(x: Float, y: Float, action: Int)`，封装 `Path` 和 `GestureDescription`，实现精准触控。

### 阶段 2：跑通悬浮控制窗 (Float Window Foundation)

**目标**：App 能够显示在桌面（甚至游戏画面）上方。

1. 申请悬浮窗权限并处理权限回调。
2. 使用 `WindowManager` 和 `PhoneWindow` 在后台 Service 中绘制一个简易布局 (Xml: 一个按钮)。
3. 给按钮加上点击事件，点击后调用阶段1中的屏幕点击（找一个坐标如 500,500 进行测试）。

### 阶段 3：建立 JS 与 Kotlin 桥梁 (QuickJS Integration)

**目标**：配置 JS 环境生成数据并回传。

1. 导入库 `implementation("app.cash.quickjs:quickjs-android:xxx")`。
2. **关键操作：注入并映射 API**。
   需要桥接脚本使用的全局变量至 Kotlin 对象：
   - `log`: `error/info/warn/debug` -> 映射到 `android.util.Log`。
   - `file`: `isFolder/readPathSync/readTextSync/...` -> 映射到 Asset 与内部存储。
3. **改造 `main.js` 回传机制**：修改原有 JS 播放循环，新增如 `AndroidPlayBridge.submitTimeline(...)` 方法，将 `mergedTimeline` 数组直接序列化后回传到 Kotlin。

### 阶段 4：Kotlin 高精度时间轴适配与播放

1. 在 Android 侧反序列化 JS 提交的 `mergedTimeline` 参数大对象。
2. 在 Kotlin 内部编写高精度扫描播放器（采用协程 `delay` 及高精度纳秒时间自旋补偿机制）。
3. 调整和校验“PC 键盘”和“Android 屏幕虚拟琴键”之间的矩阵坐标，利用 `AccessibilityService` 的连续执行或多指手势实现和弦的打点派发。

---

## 给新项目 Copilot 的提示 (Tips for AI prompt)

当你开启一个新的 Android Studio 项目时，请将一份 `.copilot-instructions.md` 放入根目录。你可以使用以下提示词唤醒 AI 进行辅助：

> "当前我正在使用 Kotlin 开发一个通过无障碍服务和 QuickJS 实现游戏屏幕触摸宏的 App。请先查阅 `copilot-instructions.md`（或本指南），然后帮我写一段利用 WindowManager 显示悬浮窗的基础代码。"

## 疑难重点预警

1. **多指按下 (Chord/和弦)**: 原神自动弹琴经常遇到和弦并发。Android `dispatchGesture` 支持 `StrokeDescription` 并发数组，请确保 `PostMessage.keyDown` 被正确聚合成多指并发手势而不是排队发送，否则会导致和弦变成了快速的乱按。
2. **GC 卡顿**: 在演奏进程中频繁创建对象会导致垃圾回收产生短暂停顿卡节奏。所有坐标映射、时间戳对象、手势对象应尽量在**预处理（Pre-bake）阶段**完成构建，在 `playCachedTimeline` 循环时复用对象。

---

## 接下来行动计划 (Next Steps)

当前项目刚刚初始化完成 (基于 Jetpack Compose 的空项目)。根据规划，我们接下来的行动方案如下：

### 第 1 步: 配置依赖 
在 `app/build.gradle.kts` 中添加所需的依赖（如 QuickJS Android `app.cash.quickjs:quickjs-android`），并完成同步。

### 第 2 步: 搭建无障碍服务骨架
创建 `AccessibilityService` 的实现类 `CuSAGLAccessibilityService`，并在 `AndroidManifest.xml` 与 res/xml 目录下完成相应的配置注册。
实现 `clickAt` 与跨点触控分发逻辑。

### 第 3 步: 搭建悬浮窗与悬浮按钮
实现申请 `SYSTEM_ALERT_WINDOW` 权限的逻辑，并使用 Compose 与 `WindowManager` 结合提供一个小型的悬浮播放暂停按钮，作为调试测试入口。

### 第 4 步: 整合并暴露 Kotlin API 到 QuickJS
在 App 内部创建 QuickJS 运行环境，注入文件读写 API (`file`)、日志 API (`log`) 与播放动作移交 API。修改 `main.js` 屏蔽其默认播放行为并使其执行后能向 Kotlin 发送完整的预处理结果。

### 第 5 步: Kotlin 原生播放与坐标映射验证
通过配置获取不同设备的屏幕比例，实现按键名如 'Q' 到屏幕 XY 的坐标映射算法。在 Kotlin 端实现高精度执行调度器，验证单音、琶音与并发和弦在原神内的执行精准度。
