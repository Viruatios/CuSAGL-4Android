# AGENTS Guide

## Project snapshot
- Android native port of the JS script in `OriginScripts/CuSimpAutoGenshinLyre/`; that folder is reference-only and not packaged (see `README.md`).
- Single Android app module `app/` with Jetpack Compose; current UI is a placeholder in `app/src/main/java/com/culoo/cusagl_4android/MainActivity.kt`.

## Architecture and data flow
- Two-layer approach: port parsing/processing into pure Kotlin first, then bind to Compose UI and Android services later (see `README.md`).
- JS reference split: `OriginScripts/CuSimpAutoGenshinLyre/main.js` for settings/parse/cache flow; `OriginScripts/CuSimpAutoGenshinLyre/player.js` for timeline playback behavior.
- Typical pipeline: score JSON -> Kotlin `data class` models -> preprocess timeline / cache -> playback scheduler (details in `OriginScripts/CuSimpAutoGenshinLyre/README.md`).

## Domain rules (music playback)
- Playback must be serial by "basic unit" (not concurrent note timers) to avoid timing drift and key-up races.
- Enforce a short key-up gap to prevent "swallowed" repeated notes; JS README explains the input engine behavior and the mitigation.
- Score parsing rules, unit kinds (rest/single/chord/arpeggio), and time signature handling are the source of truth in `OriginScripts/CuSimpAutoGenshinLyre/README.md`.

## Touch coordinate mapping
- Use the 1920x1080 baseline with `Scale = max(W/1920, H/1080)`, X centered, Y bottom-aligned (see `README.md` appendix).
- Base key coordinates are listed in `README.md` and should be treated as canonical input positions.

## Project-specific conventions
- JSON-heavy inputs should be modeled as Kotlin `data class` types before translating logic (explicitly recommended in `README.md`).
- JS async patterns map to Kotlin coroutines (`suspend`) when porting file IO or preprocessing work.

## Developer workflow notes
- Use the Gradle wrapper (`gradlew`/`gradlew.bat`) for app builds/tests; the only module is `:app`.
- Unit tests are expected under `app/src/test`; parity tests can compare Kotlin outputs against known JS behavior (see `README.md`).

## Communication rules
- Use Chinese while communicating with the user; English is fine for code comments and technical terms when clearer.
- Assume the user is new to the topic when explanations are requested and briefly define technical terms.
