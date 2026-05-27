## Step 1 Implementation: Score Parsing + Cache Mechanism

This document refines `CopilotDocs/plan-Step1DomainRules.prompt.md` into actionable Kotlin-facing specs. It mirrors JS behavior in `OriginScripts/CuSimpAutoGenshinLyre/main.js` and `OriginScripts/CuSimpAutoGenshinLyre/README.md` while adopting Android private storage (`filesDir`).

### Storage Layout (Android)
- Root: `filesDir`.
- Score files: `filesDir/score_file/`.
- Cache files: `filesDir/cache/`.
- Naming convention is identical to JS and documented in code comments only.
- Exceptions are logged with tags; no UI prompt in this step.

### Score JSON Field Table

| Field | JSON Type | Kotlin Type | Required (README) | Default (JS) | Notes |
|---|---|---|---|---|---|
| name | string | String | Yes | "未知曲名" | Missing in JSON is tolerated by JS defaults; still required by README. |
| author | string | String | No | "未知作者" | Optional metadata. |
| instrument | string | String | No | "无建议乐器" | Optional metadata. |
| description | string | String | No | "无描述" | Optional metadata. |
| type | string | String | Yes | "keyboard" | JS forces `"keyboard"` regardless of JSON; keep note. |
| bpm | string or number | Int | Yes | 120 | JSON files may store as string (e.g., "110"). Parse to Int. |
| time_signature | string | String | Yes | "4/4" | Format `N/D`, e.g., `3/4`, `6/8`. |
| composer | string | String | No | "未知作曲者" | Optional metadata. |
| arranger | string | String | No | "未知编曲者" | Optional metadata. |
| notes | string | String | Yes | (none) | Missing `notes` is a parse failure (`PARSE_FAIL`). |

### Parsed Domain Structures

#### Bar (Kotlin concept)
- Structure: `[barLength, unit1, unit2, ...]` (JS output format).
- `barLength`: Int, number of beats in this bar (equals number of `/` splits).
- Unit object:
  - `kind`: `rest | single | chord | arpeggio`
  - `keys`: List<String> or List<List<String>>
  - `time`: Float (beat fraction, e.g., 1/2)

#### Unit Semantics
- `rest`: no keys, time still consumes duration within beat.
- `single`: one key.
- `chord`: multiple keys pressed together.
- `arpeggio`: ordered blocks, each block can be single or chord.

### Parsing Flow Table: `keySheetSerialization`

| Step | Input | Output | Rule |
|---|---|---|---|
| 1 | `notes` string | lines | Split by `\n` (newline). Empty lines ignored. |
| 2 | line | normalized line | Remove `\r`. Replace multiple `/` with single `/`. |
| 3 | normalized line | beats | `line.split('/')`. `barLength = beats.length`. |
| 4 | beat string | units | `trimEnd()`; if last beat is empty, skip it. Split by single space `' '`. Empty unit -> `@` (rest). |
| 5 | unit | kind+keys+time | `unitDuration = 1 / units.length`. For `@`, emit rest. Otherwise extract parts by regex `/\([A-Za-z]+\)|[A-Za-z]/g`. |
| 6 | parts | unit object | If no parts, emit rest. If one part: `(` -> chord, else single. If multiple parts: arpeggio with `keys = parts.map(toValidKeys)` where `toValidKeys` uppercases and returns `[A-Z]+`. |

### Cache JSON Field Table

| Field | JSON Type | Kotlin Type | Required | Source | Notes |
|---|---|---|---|---|---|
| name | string | String | Yes | Score | Copied from score. |
| author | string | String | Yes | Score | Copied from score. |
| barCount | number | Int | Yes | Score | `notes.length`. |
| eventBatchCount | number | Int | Yes | Prebake | `mergedTimeline.length`. |
| expectedDuration | number | Long | Yes | Prebake | Total calculated time (ms). |
| create_time | number | Long | Yes | System | Epoch ms at cache creation. |
| gap | number | Double | Yes | BPM + time_signature | Per-beat duration in ms. |
| mergedTimeline | array | List<MergedEvent> | Yes | Prebake | List of merged time events. |

#### MergedEvent Structure
- `time`: Int (ms)
- `action`: "down" | "up"
- `keys`: List<String>

### Cache Generation Flow Table

| Step | Input | Output | Rule |
|---|---|---|---|
| 1 | score JSON | score model | Parse and validate fields, apply defaults. |
| 2 | `bpm`, `time_signature` | `gap` | `gapMultiplier = (den==8 && num%3==0) ? 1.5 : (4/den)`; `gap = 60000 / bpm * gapMultiplier`. |
| 3 | parsed bars + `gap` | `mergedTimeline`, `totalCalculatedTime` | Call `prebakeTimeline` (timeline rules are in JS; keep behavior). |
| 4 | score + timeline | `cacheData` | Build cache object using fields above. |
| 5 | `cacheData` | cache file | Save to `filesDir/cache/<musicName>.json`. |

### Cache Load & Lazy Load Rules
- For each selected music name, try `filesDir/cache/<name>.json` first.
- Cache JSON is parsed only when the track is actually about to play (lazy load).
- After playback, release `mergedTimeline` reference to reduce memory.

### Cache Expiration & Cleanup Flow

| Step | Input | Output | Rule |
|---|---|---|---|
| 1 | `filesDir/cache/` | cache list | List `*.json` files. |
| 2 | cache file + `filesDir/score_file/` | validity | If corresponding score file is missing -> delete cache. |
| 3 | cache file + score file | validity | If `cache.lastModified < score.lastModified` -> delete cache. |

### `musicList` Renaming & Ordering Rules
- Read all `*.json` under `filesDir/score_file/`.
- Valid filename: `^\d{4}\..*\.json$`.
- If valid: keep as-is.
- If invalid: choose smallest unused 4-digit prefix; rename to `${prefix}.${baseName}.json`.
- Return list **without** `.json` extension.
- Sort by numeric prefix ascending.

### Logging & Error Tags
- `PARSE_FAIL`: JSON missing required content (`notes`), or JSON parse failure.
- `FILE_MISSING`: score file missing for a cache entry.
- `CACHE_INVALID`: cache expired by modified time comparison.

### Kotlin Module Interface Sketch

- `listAndNormalizeScores(filesDir: File): List<String>`
  - Returns normalized names without `.json`.
- `loadScoreByName(filesDir: File, name: String): ScoreInfo?`
  - Parses JSON, applies defaults, validates `notes`.
- `parseNotes(notes: String): List<Bar>`
  - Implements `keySheetSerialization` rules.
- `loadCache(filesDir: File, name: String): CacheData?`
- `buildCache(score: ScoreInfo): CacheData`
- `saveCache(filesDir: File, name: String, cache: CacheData)`
- `cleanExpiredCaches(filesDir: File, scoreNames: Set<String>)`

---

Implementation note: keep directory names (`score_file`, `cache`) in comments; they are fixed and should not be user-facing settings in this step.

