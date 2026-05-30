# AGENTS

## Project snapshot
- Android native port of the JS script in `OriginScripts/CuSimpAutoGenshinLyre/`; that folder is reference-only and is not packaged (see `README.md`).
- Single Android app module in `app/` using Jetpack Compose; current UI is a placeholder in `app/src/main/java/com/culoo/cusagl_4android/MainActivity.kt`.
- Core Kotlin logic now lives under `app/src/main/java/com/culoo/cusagl_4android/core/` (score parsing, storage/cache, timeline prebake).
- Runtime playback scheduling is implemented in core (`RuntimePlaybackEngine`, `PlaybackConfig`, `RuntimePlaybackInterfaces`).

## Architecture and data flow
- Two-layer approach: port parsing/processing into pure Kotlin first, then bind to Compose UI and Android services later (see `README.md`).
- JS reference split: `OriginScripts/CuSimpAutoGenshinLyre/main.js` for settings/parse/cache flow; `OriginScripts/CuSimpAutoGenshinLyre/player.js` for timeline playback behavior (see `OriginScripts/CuSimpAutoGenshinLyre/README.md`).
- Typical pipeline: score JSON -> Kotlin `data class` models -> preprocess timeline / cache -> playback scheduler.
- Implemented core pipeline: `ScoreParser` -> `ScoreStorage.buildCache` -> `TimelinePrebaker.prebakeTimeline` (see `app/src/main/java/com/culoo/cusagl_4android/core/`).
- Runtime playback uses cache -> `RuntimePlaybackEngine` -> `TouchInjector` with a `CacheProvider` (see `app/src/main/java/com/culoo/cusagl_4android/core/RuntimePlaybackEngine.kt`).

## Domain rules (music playback)
- Playback must be serial by "basic unit" (not concurrent note timers) to avoid timing drift and key-up races; see v0.1.1 in `OriginScripts/CuSimpAutoGenshinLyre/README.md`.
- Enforce a short key-up gap to prevent swallowed repeated notes; rationale and mitigation are described in v0.1.3/v0.1.9 of `OriginScripts/CuSimpAutoGenshinLyre/README.md`.
- Kotlin prebake enforces a minimum key-up gap (`MIN_GAP_TIME_MS = 25`) and serializes events in `TimelinePrebaker`.
- Score parsing rules (rest/single/chord/arpeggio), stop symbols (space and "/"), and time signature handling are the source of truth in `OriginScripts/CuSimpAutoGenshinLyre/README.md`.
- Runtime playback scheduling uses `SystemClock.uptimeMillis()` plus sleep+spin (`spinThresholdMs`) for event alignment (see `RuntimePlaybackEngine`).

## Touch coordinate mapping
- Use the 1920x1080 baseline with `Scale = max(W/1920, H/1080)`, X centered, Y bottom-aligned (see appendix in `README.md`).
- Base key coordinates are listed in `README.md` (e.g., `Q -> PointF(455f, 670f)`, `M -> PointF(1460f, 940f)`) and should be treated as canonical input positions.
- Base key coordinates are mirrored in `app/src/main/java/com/culoo/cusagl_4android/core/KeyLayout.kt` as `KeyLayout.baseCoordinates` and `KeyLayout.allKeys`.

## Project-specific conventions
- JSON-heavy inputs should be modeled as Kotlin `data class` types before translating logic (explicitly recommended in `README.md`).
- JS async patterns map to Kotlin coroutines (`suspend`) when porting file IO or preprocessing work.
- Prefer extracting pure Kotlin logic from UI; use unit tests under `app/src/test` to validate against known JS behavior (see `README.md`).
- Score files are stored under `filesDir/score_file` and normalized to `####.name.json`; cache files live under `filesDir/cache` (see `ScoreStorage`).
- Cache JSON is produced via `ScoreStorage.serializeCache` using `org.json` and stores merged timeline batches (`CacheData`).
- Core unit tests already exist in `app/src/test/java/com/culoo/cusagl_4android/core/ScoreParserTest.kt`.
- Runtime playback injects dependencies via `TimeSource`, `Sleeper`, and `TouchInjector` to keep core logic platform-agnostic (see `RuntimePlaybackInterfaces.kt`).
- Cache loading for playback goes through `ScoreCacheProvider`, which builds cache on demand when missing (see `RuntimePlaybackInterfaces.kt`).
- Core logging stays platform-agnostic via `Logger`/`LogTags` in `app/src/main/java/com/culoo/cusagl_4android/core/Logger.kt` (e.g., `ScoreStorage.listAndNormalizeScores` accepts a `Logger`).

## Developer workflow notes
- Use the Gradle wrapper (`gradlew`/`gradlew.bat`); the only module is `:app`.

## Communication and rules
- Use Chinese while communicating with the user.
- - `X` is a placeholder for the current step number (e.g., `step1`, `step2`); update as needed when new steps are added.
- For each stepX, write down `CopilotDocs/stepX/plan.md` first to focus on the current requirement and avoid scope creep; if new requirements arise, note them down in `CopilotDocs/stepX/plan.md` for future implementation.
- Before starting a new stepX, read the `CopilotDocs/GenetalPlan.md` to confirm what to do. Then check the `CopilotDocs/step(X-N)/plan.md` of those previous steps for any relevant context or pending requirements that may affect the new step.
- Write a brief summary of what you did after completing  the implementation of each stepX, and update at `README.md ##开发节点的记录`.

## Key references
- Android plan and touch mapping: `README.md`.
- JS parsing/playback rules and historical fixes: `OriginScripts/CuSimpAutoGenshinLyre/README.md`.
