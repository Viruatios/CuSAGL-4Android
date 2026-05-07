# Copilot Instructions for Android Porting (Genshin Lyre Auto Player)

## Project Context

The user is migrating a Windows-based BetterGI JavaScript macro (for Genshin Impact Lyre Auto-playing) to a pure Android native application using Kotlin. The target architecture is "Android Native App + QuickJS Engine".
The original scripts are placed under `app/src/main/assets/scripts/CuSimpAutoGenshinLyre/`, including `main.js`, `manifest.json`, and directories like `assets/score_file/`.

## Core Architecture

- **Language**: Kotlin (for Android Native) + standard JavaScript (ES6+ via QuickJS engine).
- **UI & Controls**: `WindowManager` (System Alert Window / 悬浮窗) to display the controller overlay on top of the Genshin Impact game.
- **Execution Engine**: `QuickJS` embedded in Android, used **strictly for pre-processing**. It executes `main.js` to read score files, manage cache, apply custom settings mapping, and generate the optimal playback timeline array.
- **Input Simulation & Playback**: Kotlin Coroutines + `AccessibilityService`. The Android layer takes the fully generated `mergedTimeline` output from JS, controls the high-precision delay loop natively, and dispatches multi-touch gestures to exactly simulate logic.

## Migration Guidelines & Rules for Copilot

1. **Hybrid Architecture (JS Pre-processing -> Kotlin Playback)**: Keep the parsing logic and timeline prebaking in `main.js` untouched mostly. However, **remove or explicitly bypass the original JS `playCachedTimeline` runtime player**. After generating `mergedTimeline`, JS must hand the data over to the Kotlin boundary and complete its lifecycle to free CPU overhead.
2. **Focus on Bridging**: Whenever the user asks to implement the script execution, prioritize writing the Kotlin->JS Bridge to prep the environment.
   You need to mock or map these specific BetterGI APIs injected in JS to Kotlin functions:
   - Global `file` object: `isFolder(path)`, `readPathSync(dir)`, `readTextSync(path)`, `writeTextSync(path, text)`, `renamePathSync(oldPath, newPath)`. These paths are typically relative to the script roots. Our Kotlin bridge needs to combine Android `AssetManager` (for static JSON configs) with App `Context.filesDir` (for writable `settings.json` and cache).
   - Global `log` object: `error(msg)`, `info(msg)`, `warn(msg)`, `debug(msg)`. Map these to Android's `Log.e`, `Log.i`, etc.
   - **Native Bridge Export**: Inject a custom Kotlin object into JS (e.g., `NativeBridge.submitTimeline(timelineJson)`) so JS can pass its artifacts to Android.
   - Global `sleep` / `PostMessage`: In the new Hybrid system, manual low-level mocking of `sleep` or keystrokes during playback inside JS is no longer necessary as the playback responsibility is wholly moved to Kotlin. Wait/Delay logic during script boot can be faked or ignored as long as it doesn't break initialization.

3. **Coordinate Mapping (Keyboard -> Screen)**: The JS code outputs key characters like `'Q'`, `'W'`, `'E'`. The coordinate transformation logic has already been sketched out in `TimelinePlayer.kt` using a `1920x1080` base resolution. It automatically handles scaling with center-horizontal and bottom-vertical alignments. You should strictly use `TimelinePlayer.kt` when mapping keys, and help the user fill in the `TODO` base dictionary once 1920x1080 raw coordinates are provided.
4. **Android Setup First**: If the user is starting fresh, guide them through setting up `AndroidManifest.xml` (permissions for `SYSTEM_ALERT_WINDOW` and `BIND_ACCESSIBILITY_SERVICE`), `accessibility_service_config.xml`, and the barebones MVP (Minimum Viable Product).

## When generating Android code

- Always use **Kotlin**.
- Provide `build.gradle.kts` exact dependencies for QuickJS (e.g., `app.cash.quickjs:quickjs-android`).
- Remind the user about Android UI thread vs Background thread (JS execution & Accessibility gestures should happen smoothly without blocking the main UI).
