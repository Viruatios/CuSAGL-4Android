# bugfix2: 新建曲谱独立页面与 v1.0.1

## 目标
- 将“新建曲谱”从曲谱管理页内展开改为独立页面。
- 将当前手动创建曲谱表单迁移到新页面。
- 清空手动创建曲谱草稿的所有默认填充值。
- 更新 Android 版本号到 `v1.0.1`。

## 修复方案
- 在 `MainPage` 增加 `MANUAL_SCORE_CREATE` 页面状态，继续使用现有页面状态机，不引入 Navigation Compose。
- 曲谱管理页只保留导入、新建入口和曲谱列表；点击“新建曲谱”时清空草稿与提示，并跳转到新建页面。
- 新建 `ManualScoreCreateScreen` 承载 `ManualScoreForm`，保存失败或覆盖确认时停留在新建页面，取消和系统返回回到曲谱管理页并重置草稿。
- 保存成功后回到曲谱管理页，刷新曲谱列表、播放配置与首页准备状态。
- 将 `ManualScoreDraft()` 的所有字段默认值改为空字符串。
- 将 `app/build.gradle.kts` 的 `versionCode` 更新为 `2`，`versionName` 更新为 `"v1.0.1"`。

## 测试与验收
- 补充 JVM 单元测试，验证 `ManualScoreDraft()` 默认字段全部为空。
- 运行 `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`。
- 手动确认曲谱管理页点击“新建曲谱”进入独立页面，表单初始为空，取消/返回回到曲谱管理页，合法曲谱保存后刷新列表。

## 记录规则
- 本次属于问题修复，记录为 `bugfix2`。
- 完成实现后更新 `README.md` 的“开发节点的记录”。
