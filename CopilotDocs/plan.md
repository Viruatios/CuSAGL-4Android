# General Plan - CuSAGL-4Android

## 目标与定位
- 目标：将 `OriginScripts/CuSimpAutoGenshinLyre` 的核心能力完整移植为 Android 原生 Kotlin 逻辑，并以可维护的 Compose UI + 无障碍触控注入完成端到端演奏。
- 原则：先纯 Kotlin 核心，再 Android 绑定；核心逻辑与 UI 解耦；先对齐 JS 规则，再做结构优化。

## 当前进度摘要
- Step1：曲谱解析、缓存与预烘焙已完成（`core` 下相关模块）。
- Step2：运行时播放调度与控制接口已完成（延迟-自旋、状态机、触控接口）。

## 关键资料与规则来源
- 读谱与调度规则：`OriginScripts/CuSimpAutoGenshinLyre/README.md`。
- Android 触控映射：`README.md` 附录（1920x1080 基准坐标 + 缩放公式）。
- 代码入口与核心目录：`app/src/main/java/com/culoo/cusagl_4android/core/`。

## 总体路线（高层）
1. 核心逻辑对齐：保持解析、预烘焙、调度与缓存行为和 JS 等价。
2. Android 绑定：实现触控注入与权限管理，接入 Compose UI 控制流程。
3. 体验打磨：补充配置/管理 UI、悬浮窗控制、稳定性与性能验证。
4. 质量保障：完善单元测试与关键路径日志，保障跨设备稳定性。

## 后续分解步骤
- Step3：触控注入与无障碍服务接入（权限、服务生命周期、坐标映射）。
- Step4：Compose UI 与悬浮窗控制面板（曲谱管理、播放配置、运行态控制）。
- Step5：配置与队列管理完善（UI->PlaybackConfig 传入与校验）。
- Step6：测试与性能验证（不同分辨率与刷新率设备）。

## 约束与约定
- 新需求必须落到 `CopilotDocs/stepX/plan.md`，避免范围漂移。
- 仅在项目结构或关键接口发生重大变化时更新 `AGENTS.md`。
- 每完成一个 stepX 的实现后，补写 `README.md` 的“开发节点的记录”。

