# 简易自动化原琴：安卓版 - CuSAGL-4Android / CuSAGL Mobile

[CuSimpAutoGenshinLyre](https://github.com/Viruatios/CuSimpAutoGenshinLyre) 的安卓移植版。

---

# 开发记录

_以下是动工前的规划：_

原本想的是在安卓上实现一个类似于 BetterGI 的 JS 脚本宿主环境，提供脚本需要调用的接口，从而可以复用已有的 JS 处理脚本。后来觉得脚本中某些功能，比如预解析曲谱，完全不必在演奏前那点时间、顶着开了游戏的高负载去做，它可以在一个更灵活的时间来执行。所以脚本里不同功能的代码本身就有拆分并融入到项目中的合理性，这种情况下仍然将脚本视作一个整体去执行是不妥当的。此外，数据类型转换、异步回调、内存管理和异常捕获上这些方面处理也比较麻烦，脚本代码其实并不如预期般容易复用。遂推翻，改为将脚本翻译为 Kotlin 原生实现。

## 预期功能

1. 申请无障碍服务（点击操作）和悬浮窗权限（实时控制）。应用每次启动时需要检查权限是否已授予，若无，则弹窗提示，并引导用户前往设置界面授予权限。此外，应用需要确保自身能够在后台保持运行。
2. 应用的主界面使用 Jetpack Compose 构建。应当具有以下功能：
   - 曲谱管理入口。允许用户导入或删除 JSON 曲谱；或提供工具给用户，协助他们按规定的格式自行创建曲谱 JSON 文件。
   - 曲谱演奏的自定义配置选项入口。提供包括“选定曲谱”、“选定曲谱列表”、“单曲单次演奏”、“单曲循环演奏”、“列表循环演奏”等选项。
   - 提供原 JS 脚本中的“预加载曲谱并保存缓存文件”功能。这个任务现在被单独拆分了出来，在设备负载更低的时候完成。
   - “准备演奏”按钮。只允许在上述自定义配置选项完整，且曲谱预加载已完成时启用。点击后，进入“悬浮窗模式”。
3. 在“悬浮窗模式”下，应用应当在屏幕上显示一个 (可拖动的) 悬浮窗，包含以下功能：
   - 显示当前选定的曲谱名称和演奏状态（如“正在演奏”、“已暂停”等）。
   - 提供“开始/暂停”按钮，允许用户控制演奏的开始和暂停。
   - 提供“停止”按钮，允许用户立即停止演奏并退出悬浮窗模式。
   - 提供“上一首/下一首”按钮，允许用户在已经预加载的曲谱列表中切换当前演奏的曲谱。
   - (可选) 在悬浮窗模式下，持续监测游戏状态（如是否在游戏内、是否在演奏界面等），并根据游戏状态自动调整演奏行为（例如，检测到当前游戏不处于演奏界面时，自动暂停演奏动作）。
4. 触屏模式下，将键盘谱转换为点击屏幕上对应点位的功能，详见 [触屏模式下的键盘谱演奏应该如何确认按键位置](#附录触屏模式下的键盘谱演奏应该如何确认按键位置)。

## 脚本移植的具体计划

等待被移植的 JS 脚本暂时存放于 `OriginScripts\CuSimpAutoGenshinLyre` 文件夹下，它将不会被打包进入最终程序中。

_以下内容部分参考了 Copilot 建议。_

> 直接将 JS 逻辑人工/半自动翻译为 Kotlin 原生代码。

不考虑引入 C++ 或 Rust 来重写业务逻辑，对于零 Android 经验的开发者来说，那会是一场灾难。Kotlin 本身是一门非常现代的语言，它的很多语法特性与现代 JavaScript / TypeScript 惊人地相似。

以下是具体的方案和高效起步的步骤：

### 一、 核心方案：JS 到 Kotlin 的逐行映射

Kotlin 和 JS 具有极高的语法亲和力，核心任务是完成**思维模式的转换**。性能上，直接跑在 Android ART 虚拟机上的 Kotlin 代码，可以胜过任何桥接形式的 JS 引擎，完全满足项目对性能的严苛要求。

**核心语法概念映射表（JS 转 Kotlin）：**

- **变量声明**：`const` 对应 `val` (不可变)，`let` 对应 `var` (可变)。
- **函数声明**：`function` 对应 `fun`。
- **异步操作**：JS 的 `Promise` 和 `async/await` 对应 Kotlin 的 **协程 (Coroutines)** 和 `suspend` 关键字。
- **数组与集合**：JS 的 `[]` 对应 Kotlin 的 `listOf()` (只读) 或 `mutableListOf()` (可写)。JS 的数组高阶函数 `.map()`, `.filter()`, `.reduce()` 在 Kotlin 中**完全一样**且更强大。
- **对象/JSON**：JS 中的字面量对象 `{}` 对应 Kotlin 的 `data class`（数据类）。

---

### 二、 高效落地步骤（Step-by-Step）

为了追求开发效率，按照以下步骤对原有 JS 脚本进行移植：

#### 步骤 1：梳理数据模型（JSON -> Data Class）

JS 脚本通常重度依赖 JSON 数据流转。在强类型的 Kotlin 中，第一步是把这些数据结构固定下来。

- **做法**：在 Android Studio 中安装插件 **"JSON To Kotlin Class"**，把 JS 里涉及的核心 JSON 数据直接复制进去，一键生成 Kotlin 的 `data class`。
- _示例_：
  ```kotlin
  // JS: const user = { id: 1, name: "Alice" }
  data class User(val id: Int, val name: String)
  val user = User(1, "Alice")
  ```

#### 步骤 2：剥离纯逻辑函数，使用 AI 辅助翻译

把 JS 脚本中那些**不涉及 DOM、不涉及网络请求**的纯计算、纯数据处理函数（Pure Functions）提取出来。

- **做法**：因为这是纯逻辑转换，**直接把大段的 JS 代码发给 Copilot**，可以瞬间帮你转化成地道、高性能的 Kotlin 代码。
- _示例_：
  ```javascript
  // JS 代码
  function processData(items) {
  	return items.filter((i) => i.active).map((i) => i.value * 2);
  }
  ```
  ```kotlin
  // 翻译后的 Kotlin 代码
  fun processData(items: List<Item>): List<Int> {
      return items.filter { it.active }.map { it.value * 2 }
  }
  ```

#### 步骤 3：改造异步请求（Promise -> Coroutines）

JS 脚本里有 `fetch` 或者异步的文件读取，在 Kotlin 中则需要用**协程**来处理。

- **做法**：在 Kotlin 中，将异步函数标记为 `suspend`。
- _示例_：
  ```javascript
  // JS 异步
  async function fetchData(url) {
  	let res = await fetch(url);
  	return await res.json();
  }
  ```
  ```kotlin
  // Kotlin 协程 (通常配合 Retrofit 或 OkHttp 等网络库)
  suspend fun fetchData(url: String): MyData {
      // 挂起函数，不会阻塞主线程
      val response = httpClient.get(url).await()
      return parseJson(response)
  }
  ```

#### 步骤 4：单元测试验证（兜底保障）

由于没有 Android 开发经验，如果把逻辑直接塞进 UI 里跑，一旦出错，你很难分辨是 UI 写错了还是逻辑写错了。

- **做法**：把翻译好的 Kotlin 逻辑写在 `src/test/java/...` 目录下，写几个简单的单元测试（JUnit）。
- 将 JS 脚本的输入输出作为测试用例，喂给你的 Kotlin 函数，断言结果一致即可。这样可以脱离 Android 模拟器，在几毫秒内验证逻辑的正确性。

### 总结与建议

抛弃 JS 引擎的包袱后，项目将变得极度轻量且原生。**现在的策略是：将 UI 开发和逻辑迁移分开。**

1. 先建一个纯 Kotlin 的文件（不接触任何 Android UI 元素），利用 AI 工具配合，把你的 JS 算法、处理逻辑完整翻译成 Kotlin 函数和类。
2. 写个简单的 `main` 函数或测试用例跑通。
3. 最后再用 Jetpack Compose 把这些原生函数绑定到按钮和列表中。

## 附录：触屏模式下的键盘谱演奏应该如何确认按键位置

### 《原神》如何针对不同分辨率进行缩放？“风物之诗琴”多分辨率坐标变换公式

基于观察得到的原神的 UI 缩放策略推断，对于任意目标分辨率，坐标映射公式如下：

1. **基准设定**: 基准分辨率为 `W_base = 1920`, `H_base = 1080`。
2. **计算缩放比 (Scale)**:
   为了防止 UI 过小，《原神》采用了宽和高中变化更大的比例作为整体防裁剪缩放比：
   `Scale = max(W_target / W_base, H_target / H_base)`
3. **坐标变换**:
   由于“风物之诗琴”属于底部 UI，排版规则为“X 轴居中，Y 轴底部对齐”。
   设基准点坐标为 `(x_base, y_base)`，目标点坐标为 `(x_target, y_target)`：
   - **X轴转换 (居中对齐)**:
     `x_target = x_base * Scale + (W_target - W_base * Scale) / 2`
   - **Y轴转换 (底部对齐)**:
     `y_target = y_base * Scale + (H_target - H_base * Scale)`

在编写坐标映射代码时，可将预先在 1920x1080 下采集好的琴键中心坐标 `(x_base, y_base)` 代入上述公式，即可适配所有非常规长宽比的 Android 设备屏幕。

### 1920x1080 分辨率下的基准 键盘-触屏 琴键坐标表

```kotlin
private val BASE_COORDINATES = mapOf(
    'Q' to PointF(455f, 670f),
    'W' to PointF(625f, 670f),
    'E' to PointF(790f, 670f),
    'R' to PointF(960f, 670f),
    'T' to PointF(1125f, 670f),
    'Y' to PointF(1295f, 670f),
    'U' to PointF(1460f, 670f),
    'A' to PointF(455f, 805f),
    'S' to PointF(625f, 805f),
    'D' to PointF(790f, 805f),
    'F' to PointF(960f, 805f),
    'G' to PointF(1125f, 805f),
    'H' to PointF(1295f, 805f),
    'J' to PointF(1460f, 805f),
    'Z' to PointF(455f, 940f),
    'X' to PointF(625f, 940f),
    'C' to PointF(790f, 940f),
    'V' to PointF(960f, 940f),
    'B' to PointF(1125f, 940f),
    'N' to PointF(1295f, 940f),
    'M' to PointF(1460f, 940f)
)
```

---

_以下是_
## 开发节点的记录

1. 初始化了项目，编写了 README.md 的规划，将原始 JS 脚本导入到仓库。
2. 要求 Copilot 生成了 AGENTS.md 和 GeneralPlan.md 的初稿。AGENTS 快速介绍项目现况，PLAN 指出后续方向。后续对 GeneralPlan 进行了讨论完善，将其作为总大纲文件。具体的不同需求将再区分到不同的进程中分别去做，不在这一步。
3. 完成 Step1 规划并落地曲谱解析与缓存机制核心：新增 core 模块数据模型、解析/预烘焙/缓存存取与日志标签规则，补充单元测试，并生成 Step1 规格文档；修复 Windows 下资源/源码符号链接导致的 Gradle 快照问题后重跑测试，以测试与构建通过作为质量保障。
   - 在此阶段，将“完成计划后，将一段简短的总结写入开发节点记录”写入了 AGENTS.md。
4. 完成 Step2 运行时播放调度实现：新增运行时播放配置、调度器与触控注入接口，采用 `SystemClock.uptimeMillis` 的延迟-自旋调度，补充开始/暂停/停止/上一首/下一首与 `releaseAllTouches` 控制语义，并落地 `CopilotDocs/step2/plan.md`。
5. 完成 Step3 触控注入与无障碍服务接入：新增无障碍服务、触控注入与坐标映射实现，更新 Manifest 与服务配置，补充坐标映射单元测试，并落地 `CopilotDocs/step3/plan.md`。
6. 完成 Step4 悬浮窗播放控制与前台服务：新增播放状态快照与监听、Compose 悬浮控制面板、`specialUse` 前台服务和临时验收入口；悬浮窗采用顶部对齐坐标映射并限制在琴键安全区域内，仅在非播放状态允许拖动；补充位置映射与播放状态单元测试，并落地 `CopilotDocs/step4/plan.md`。
   - 在此期间，从 Copilot 切换到了 Codex，因此将一些 Copilot 原有的内置技能做了显式的 SKILL 更新，以适配 Codex 的能力。具体地说：克隆了 `agent-customization`。新增 workspace skill `update-agent-instructions`，用于从代码库可验证现状出发，对项目根目录现有 `AGENTS.md` 做最小、保留原有结构的增量更新，并规范一次性发现其他 AI 指令、差异核对和更新后验证流程。该 SKILL 以后有望在全局复用。
7. 完成 Step5 正式主页面与准备演奏入口：将 `MainActivity` 从 Step4 临时验收入口替换为 Compose 主页面，自动使用排序后的第一首曲谱，提供曲谱管理和播放配置占位页，接入曲谱预加载缓存保存流程，并在缓存、悬浮窗权限和无障碍服务均就绪后启动悬浮窗演奏服务；补充主页面缓存状态与预加载单元测试，并落地 `CopilotDocs/step5/plan.md`。
