# Android 原生移植开发指南 (Android Port Guide)

本指南针对无 Android 开发经验的开发者，提供将基于 BetterGI (Windows) 的原神自动弹琴 JS 脚本移植至 Android 平台的完整路线图。

## 方案简述：Android (Kotlin) + 悬浮窗 + 无障碍服务 + QuickJS

现有系统在 Windows 平台上使用 C# 注入的 BetterGI 宿主环境提供底层文件 IO 与键盘 API。在 Android 端，我们将通过开发一个原生 App，结合跨应用 UI（悬浮窗）、屏幕触控模拟（无障碍服务）、以及嵌入式脚本动态解析（QuickJS），构建一个全新的宿主运行环境，确保绝大部分 `main.js` 代码实现**零修改复用**。

---

## 核心实现机制映射

| PC端 (Better GI 宿主环境)                           | Android 端 (我们即将打造的新宿主环境)            | 说明                                                                                                                                 |
| --------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **窗体UI控件**                                      | `WindowManager` 悬浮窗                           | 用于在原神游戏画面之上悬浮显示“播放/暂停”面板。需申请 `SYSTEM_ALERT_WINDOW` 权限。                                                   |
| **键盘按键模拟** (`PostMessage.keyDown`)            | `AccessibilityService.dispatchGesture`           | 无障碍手势服务，可不依赖 Root 权限直接在屏幕固定坐标 (X, Y) 模拟手指按下和抬起。需申请 `BIND_ACCESSIBILITY_SERVICE` 并引导用户开启。 |
| **JS 执行环境** (`ClearScript`)                     | `QuickJS-Android` (由 CashApp 提供)              | 在 App 内部执行 `main.js` 并保持 V8 / ES6 兼容。                                                                                     |
| **文件读写 API** (`file.readTextSync`, `System.IO`) | `Context.assets` (读取乐谱) / Kotlin 内部存储 IO | 将原项目 `assets/score_file/` 放入 Android `src/main/assets` 目录，并将原生 IO 桥接到 JS 环境暴露同名对象。                          |
| **全局 `sleep()` 函数**                             | Kotlin Coroutines `suspend` / QuickJS 异步桥接   | 提供精确的微秒/毫秒级等待能力，确保弹琴不断流。                                                                                      |

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

**目标**：用 JS 触发原生操作。

1. 导入库 `implementation("app.cash.quickjs:quickjs-android:xxx")`。
2. 将 `main.js` 和部分 `score_file/*.json` 存入 `assets`。
3. **关键操作：注入 API**。
   ```kotlin
   // 在 Kotlin 中定义接口
   interface BetterGIMock {
      fun nativeKeyDown(key: String)
      fun nativeKeyUp(key: String)
      fun readTextSync(path: String): String
   }
   // 将实现类绑定给 QuickJS，暴露全局变量
   ```
4. 让 `main.js` 中的 `new PostMessage().keyDown('Q')` 实际调用映射好的 Kotlin 方法，Kotlin 方法再根据预先设定的 `{ "Q": Point(200, 300) }` 坐标矩阵，调用无障碍打点。

### 阶段 4：异步与时间轴适配

1. `main.js` 依赖 `async/await` 和 `sleep` 进行演奏。
2. 在 Android 侧，需要正确处理 JS Promise，或者在 Kotlin 提供阻塞函数并允许 JS 调度。
3. 调整和校验“PC 键盘”和“Android 屏幕虚拟琴键”之间的矩阵坐标，考虑刘海屏偏移及全面屏长宽比。

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
在 App 内部创建 QuickJS 运行环境，注入文件读写与无障碍触发相关的 Mock API。调试确保能够直接运行 `main.js`。

### 第 5 步: 进行坐标映射与性能优化
通过配置获取不同设备的屏幕比例，实现按键名如 'Q' 到屏幕 XY 的坐标映射算法。调试并发逻辑，避免和弦失效或卡顿。
