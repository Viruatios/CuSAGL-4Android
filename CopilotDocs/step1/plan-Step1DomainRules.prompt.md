## Plan: 曲谱解析与缓存提取完善

固定内部存储根为 `filesDir`、沿用 `score_file/` 与 `cache/` 分目录、缓存过期基于文件修改时间、异常仅记录带标签日志。输出字段表+流程表规格，覆盖解析规则、缓存生命周期、懒加载与重命名逻辑，并形成 Kotlin 模块接口草案，确保与 JS 行为一致。

### Steps 5
1. 归纳曲谱 JSON 字段与默认值映射，依据 `OriginScripts/CuSimpAutoGenshinLyre/README.md` 与 `OriginScripts/CuSimpAutoGenshinLyre/main.js` 的 `getMusicInfo`，保留必填约束与类型注意项。
2. 拆解 `keySheetSerialization` 规则，明确停止符语义与 `rest|single|chord|arpeggio` 单位结构，形成解析流程表。
3. 抽取缓存生成与懒加载流程，定义 `cacheData` 字段与生命周期，落地到 `filesDir/cache/`。
4. 迁移文件名自动重命名与缓存过期清理策略，使用 `filesDir/score_file/` 与 `filesDir/cache/` 的修改时间判定失效。
5. 统一错误处理与日志标签规范，异常仅记录日志并附带标签（如 `PARSE_FAIL`、`FILE_MISSING`、`CACHE_INVALID`），目录命名规范仅在注释说明。

### Further Considerations 1
1. Kotlin 模块接口草案明确“输入为文件名列表、输出为曲谱元信息与缓存元信息”的边界契约。

