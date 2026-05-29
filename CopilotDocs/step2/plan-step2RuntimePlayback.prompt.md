## Plan: Step2 运行时调度与交互响应

以运行时播放调度为核心，采用 `SystemClock.uptimeMillis` 作为轻量时间源并实现延迟-自旋混合休眠；统一所有可调数值为配置常量，默认值沿用 JS；提供“开始/暂停/停止/上一首/下一首/抬起所有触控”控制接口，暂停重置当前曲目进度；队列边界按用户模式回绕或停止；`releaseAllTouches` 覆盖 `README.md` 键位表。明确后续原生 UI 必须通过这些接口传递用户配置与交互事件。

### Steps 5
1. 归纳运行时模块契约与输入输出，链接 `OriginScripts/CuSimpAutoGenshinLyre/player.js` 的时间轴扫描语义。
2. 规划时间源与混合休眠策略，采用 `SystemClock.uptimeMillis` 并设定可配置休眠阈值默认 5ms。
3. 统一可配置常量清单与默认值，覆盖所有可自定义数值并沿用 JS 行为基线。
4. 设计控制接口与状态机，定义暂停重置当前曲目进度、切歌立即暂停并切换、停止与切歌先调用 `releaseAllTouches`。
5. 衔接用户配置与 UI 传递要求，明确后续原生 UI 需收集配置并调用 step2 接口传入运行时模块。

### Further Considerations 1
1. `releaseAllTouches` 覆盖 `README.md` 键位表全量按键触控点，并与无障碍注入实现保持一致。

