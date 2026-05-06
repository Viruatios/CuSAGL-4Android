# Copilot Instructions for Android Porting (Genshin Lyre Auto Player)

## Project Context

The user is migrating a Windows-based BetterGI JavaScript macro (for Genshin Impact Lyre Auto-playing) to a pure Android native application using Kotlin. The target architecture is "Android Native App + QuickJS Engine".
The original scripts are placed under `app/src/main/assets/scripts/CuSimpAutoGenshinLyre/`, including `main.js`, `manifest.json`, and directories like `assets/score_file/`.

## Core Architecture

- **Language**: Kotlin (for Android Native) + standard JavaScript (ES6+ via QuickJS engine).
- **UI & Controls**: `WindowManager` (System Alert Window / 悬浮窗) to display the controller overlay on top of the Genshin Impact game.
- **Execution Engine**: `QuickJS` (or Duktape) embedded in Android to execute the existing `main.js` and parse `score_file` (JSON) assets.
- **Input Simulation**: `AccessibilityService` (无障碍服务) with `dispatchGesture` API to simulate exact (X, Y) screen touch coordinates (replacing PC keyboard inputs).

## Migration Guidelines & Rules for Copilot

1. **DO NOT Rewrite JS Business Logic (Unless necessary)**: Keep the parsing logic, timeline prebaking, and the Jitter/Sleep synchronization inside `main.js` untouched as much as possible to ensure highest code reusability.
2. **Focus on Bridging**: Whenever the user asks to implement the script execution, prioritize writing the Kotlin-JS Bridge.
   You need to mock or map these specific BetterGI APIs injected in JS to Kotlin functions:
   - Global `sleep` function: BetterGI provides a global `sleep(ms)` returning a Promise. QuickJS requires Kotlin Coroutines suspended functions or JS-exposed Promise wrapping.
   - Global `file` object: `isFolder(path)`, `readPathSync(dir)`, `readTextSync(path)`, `writeTextSync(path, text)`, `renamePathSync(oldPath, newPath)`. These paths are typically relative to the script roots. Our Kotlin bridge needs to combine Android `AssetManager` (for static JSON configs) with App `Context.filesDir` (for writable files like `settings.json` and cache).
   - Global `log` object: `error(msg)`, `info(msg)`, `warn(msg)`, `debug(msg)`. Map these to Android's `Log.e`, `Log.i`, etc.
   - Global `PostMessage` class logic: JS does `const postMessage = new PostMessage(); postMessage.keyDown(k)`. We can expose a Kotlin `postMessage` global object acting similarly. Map `keyDown(k)` to `AccessibilityService` simulated screen finger down at specific mapped coordinates.
   - Note: The JS script gracefully handles missing `System.IO` if `typeof System === 'undefined'`, so bridging `System.IO` is NOT strictly required as long as `file` object works perfectly.

3. **Coordinate Mapping (Keyboard -> Screen)**: The JS code passes string keys (like `'Q'`, `'W'`, `'E'`). You must help the user implement a coordinate mapping matrix in Kotlin based on standard 16:9 screen ratios for Genshin Impact's lyre UI.
4. **Android Setup First**: If the user is starting fresh, guide them through setting up `AndroidManifest.xml` (permissions for `SYSTEM_ALERT_WINDOW` and `BIND_ACCESSIBILITY_SERVICE`), `accessibility_service_config.xml`, and the barebones MVP (Minimum Viable Product).

## When generating Android code

- Always use **Kotlin**.
- Provide `build.gradle.kts` exact dependencies for QuickJS (e.g., `app.cash.quickjs:quickjs-android`).
- Remind the user about Android UI thread vs Background thread (JS execution & Accessibility gestures should happen smoothly without blocking the main UI).
