# AGENTS

## Project snapshot
- Android native port of the JS script in `OriginScripts/CuSimpAutoGenshinLyre/`; that folder is reference-only and is not packaged (see `README.md`).
- Single Android app module in `app/` using Jetpack Compose; current UI is a placeholder in `app/src/main/java/com/culoo/cusagl_4android/MainActivity.kt`.
- Core Kotlin logic now lives under `app/src/main/java/com/culoo/cusagl_4android/core/` (score parsing, storage/cache, timeline prebake).

## Architecture and data flow
- Two-layer approach: port parsing/processing into pure Kotlin first, then bind to Compose UI and Android services later (see `README.md`).
- JS reference split: `OriginScripts/CuSimpAutoGenshinLyre/main.js` for settings/parse/cache flow; `OriginScripts/CuSimpAutoGenshinLyre/player.js` for timeline playback behavior (see `OriginScripts/CuSimpAutoGenshinLyre/README.md`).
- Typical pipeline: score JSON -> Kotlin `data class` models -> preprocess timeline / cache -> playback scheduler.
- Implemented core pipeline: `ScoreParser` -> `ScoreStorage.buildCache` -> `TimelinePrebaker.prebakeTimeline` (see `app/src/main/java/com/culoo/cusagl_4android/core/`).

## Domain rules (music playback)
- Playback must be serial by "basic unit" (not concurrent note timers) to avoid timing drift and key-up races; see v0.1.1 in `OriginScripts/CuSimpAutoGenshinLyre/README.md`.
- Enforce a short key-up gap to prevent swallowed repeated notes; rationale and mitigation are described in v0.1.3/v0.1.9 of `OriginScripts/CuSimpAutoGenshinLyre/README.md`.
- Kotlin prebake enforces a minimum key-up gap (`MIN_GAP_TIME_MS = 25`) and serializes events in `TimelinePrebaker`.
- Score parsing rules (rest/single/chord/arpeggio), stop symbols (space and "/"), and time signature handling are the source of truth in `OriginScripts/CuSimpAutoGenshinLyre/README.md`.

## Touch coordinate mapping
- Use the 1920x1080 baseline with `Scale = max(W/1920, H/1080)`, X centered, Y bottom-aligned (see appendix in `README.md`).
- Base key coordinates are listed in `README.md` (e.g., `Q -> PointF(455f, 670f)`, `M -> PointF(1460f, 940f)`) and should be treated as canonical input positions.

## Project-specific conventions
- JSON-heavy inputs should be modeled as Kotlin `data class` types before translating logic (explicitly recommended in `README.md`).
- JS async patterns map to Kotlin coroutines (`suspend`) when porting file IO or preprocessing work.
- Prefer extracting pure Kotlin logic from UI; use unit tests under `app/src/test` to validate against known JS behavior (see `README.md`).
- Score files are stored under `filesDir/score_file` and normalized to `####.name.json`; cache files live under `filesDir/cache` (see `ScoreStorage`).
- Cache JSON is produced via `ScoreStorage.serializeCache` using `org.json` and stores merged timeline batches (`CacheData`).
- Core unit tests already exist in `app/src/test/java/com/culoo/cusagl_4android/core/ScoreParserTest.kt`.

## Developer workflow notes
- Use the Gradle wrapper (`gradlew`/`gradlew.bat`); the only module is `:app`.

## Communication rules
- Use Chinese while communicating with the user; English is fine for code comments and technical terms when clearer.
- Assume the user is new to the topic when explanations are requested and briefly define technical terms.

## Key references
- Android plan and touch mapping: `README.md`.
- JS parsing/playback rules and historical fixes: `OriginScripts/CuSimpAutoGenshinLyre/README.md`.
