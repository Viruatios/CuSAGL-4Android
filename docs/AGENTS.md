# AI Agent Guidelines for CuSAGL-4Android

## Project Architecture & Core Philosophy
This project creates an Android auto-player for Genshin Impact, porting a Windows JS macro to a **Hybrid Architecture** (Kotlin + QuickJS).
- **Pre-processing (JS)**: QuickJS runs `app/src/main/assets/scripts/CuSimpAutoGenshinLyre/main.js` *only* to read files, calculate merged timelines, and return a fixed set of execution instructions. 
- **Execution (Kotlin)**: Kotlin handles the high-precision playback. Completely bypass the original JS `playCachedTimeline` looping/sleep patterns—JS yields control directly back to Kotlin.

## Key Sub-systems & Conventions

### 1. The JS-to-Kotlin Bridge
- Map legacy BetterGI global APIs (`file`, `log`) to Android equivalents (AssetManager/FilesDir and `android.util.Log`). 
- When working on JS side execution, implement API stubs via `QuickJS` and inject Kotlin bridge objects like `NativeBridge.submitTimeline(...)` to pass pre-calculated data out of JS.

### 2. High-Precision Playback
- **Do not create objects in the playback loop**. To prevent GC pauses from ruining the musical timing, pre-bake all `GestureDescription` strokes and coordinate calculations in advance.
- Playback lives entirely in Kotlin Coroutines utilizing high-frequency `System.nanoTime()` spin-waits for exact nano-second fidelity against the raw timeline bounds.

### 3. Screen Touch Mapping & Chords
- Translate key inputs (e.g., 'Q', 'W', 'E') strictly using the mapped coordinate arrays in `TimelinePlayer.kt`. 
- **Coordinates Logic**: 1920x1080 is the baseline. We derive X via center-alignment scaling and Y via bottom-alignment scaling (see mapping formula in `docs/Android_Port_Guide.md`).
- **Concurrent Touch / Chords**: Accurately group multiple simultaneous points into a single `GestureDescription.Builder()` with multiple `addStroke()`. Never queue strokes sequentially if they occur at the exact same timestamp. Send via `AccessibilityService.dispatchGesture`.

### 4. UI Layer
- **Main App**: Built with Jetpack Compose (`app/build.gradle.kts` setup). Keep it strictly for settings and permissions requests.
- **Overlay**: Uses `WindowManager` (`SYSTEM_ALERT_WINDOW`) and `PhoneWindow` via a background Service. Do not use standard Activities for the player overlay.

## Essential Files
- `docs/Android_Port_Guide.md`: Original mapping rules, architecture definitions, and specific resolution math.
- `app/src/main/assets/scripts/CuSimpAutoGenshinLyre/main.js`: Original macro logic (avoid changing except to yield to Kotlin).
- `gradle/libs.versions.toml`: Contains standard Android dependencies and `app.cash.quickjs:quickjs-android`.
