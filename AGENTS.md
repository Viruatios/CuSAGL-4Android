# AGENTS Guide

## Project Summary
- Android native port of `OriginScripts/CuSimpAutoGenshinLyre` (JS) into Kotlin; `OriginScripts/` is reference material and is not packaged (see `README.md`).
- UI is Jetpack Compose; core logic should stay platform-agnostic where possible and be wired to UI later (see porting notes in `README.md`).

## Architecture & Data Flow
- **Two-layer approach**: keep parsing/processing logic in pure Kotlin first, then bind to Compose UI and Android services later.
- **JS reference split**: use `OriginScripts/CuSimpAutoGenshinLyre/main.js` for settings/parse/cache flow and `OriginScripts/CuSimpAutoGenshinLyre/player.js` for timeline playback behavior.

## Porting Conventions
- JSON-heavy inputs should become Kotlin `data class` models before logic translation (explicitly recommended in `README.md`).
- JS async patterns map to Kotlin coroutines (`suspend`), especially around file IO or pre-processing steps described in the JS scripts.

## Domain Rules (Music Playback)
- Timeline execution is designed around a serial, unit-based scheduler (see the JS README’s evolution notes); avoid concurrent note timers when porting to Kotlin.
- The JS script enforces a short key-up gap to avoid “swallowed” inputs; mirror this behavior in Kotlin when implementing key/tap output logic (see `OriginScripts/CuSimpAutoGenshinLyre/README.md`).

## Touch Coordinate Mapping
- Touch coordinates are based on a 1920x1080 baseline; scaling uses `Scale = max(W_target/1920, H_target/1080)` with X centered and Y bottom-aligned (see `README.md` “附录”).
- Baseline key positions are listed in `README.md` under the base coordinate table and should be used as the source of truth.

## Project Structure Pointers
- Android app module: `app/` (Compose enabled in `app/build.gradle.kts`).
- Current Kotlin sources are minimal: `app/src/main/java/com/culoo/cusagl_4android/MainActivity.kt` and theme files; expect to add core logic packages alongside them.

