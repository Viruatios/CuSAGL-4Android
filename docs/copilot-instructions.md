# Copilot Instructions for Android Porting (Genshin Lyre Auto Player)

## Project Context

The user is migrating a Windows-based BetterGI JavaScript macro (for Genshin Impact Lyre Auto-playing) to a pure Android native application using Kotlin. The target architecture is "Android Native App + QuickJS Engine".

## Core Architecture

- **Language**: Kotlin (for Android Native) + standard JavaScript (ES6+ via QuickJS engine).
- **UI & Controls**: `WindowManager` (System Alert Window / 悬浮窗) to display the controller overlay on top of the Genshin Impact game.
- **Execution Engine**: `QuickJS` (or Duktape) embedded in Android to execute the existing `main.js` and parse `score_file` (JSON) assets.
- **Input Simulation**: `AccessibilityService` (无障碍服务) with `dispatchGesture` API to simulate exact (X, Y) screen touch coordinates (replacing PC keyboard inputs).

## Migration Guidelines & Rules for Copilot

1. **DO NOT Rewrite JS Business Logic (Unless necessary)**: Keep the parsing logic, timeline prebaking, and the Jitter/Sleep synchronization inside `main.js` untouched as much as possible to ensure highest code reusability.
2. **Focus on Bridging**: Whenever the user asks to implement the script execution, prioritize writing the Kotlin-JS Bridge.
   You need to mock or map these specific BetterGI APIs injected in JS to Kotlin functions:
   - `file.readTextSync`, `file.isFolder`, `file.readPathSync` -> Map to Android `AssetManager` or App-specific `Context.filesDir` I/O.
   - `System.IO...` fallback APIs -> Replace with standard Android file I/O or expose a custom Kotlin IO object.
   - `class PostMessage { keyDown(k); keyUp(k) }` -> Must be provided to the QuickJS context. Map `keyDown(k)` to `AccessibilityService` simulated screen finger down at specific mapped coordinates, and `keyUp` to finger release.
   - `sleep(ms)` -> BetterGI provides a global `sleep` that returns a Promise. QuickJS requires an asynchronous bridge or using Kotlin Coroutines `delay()` exposed as a JS Promise.
3. **Coordinate Mapping (Keyboard -> Screen)**: The JS code passes string keys (like `'Q'`, `'W'`, `'E'`). You must help the user implement a coordinate mapping matrix in Kotlin based on standard 16:9 screen ratios for Genshin Impact's lyre UI.
4. **Android Setup First**: If the user is starting fresh, guide them through setting up `AndroidManifest.xml` (permissions for `SYSTEM_ALERT_WINDOW` and `BIND_ACCESSIBILITY_SERVICE`), `accessibility_service_config.xml`, and the barebones MVP (Minimum Viable Product).

## When generating Android code

- Always use **Kotlin**.
- Provide `build.gradle.kts` exact dependencies for QuickJS (e.g., `app.cash.quickjs:quickjs-android`).
- Remind the user about Android UI thread vs Background thread (JS execution & Accessibility gestures should happen smoothly without blocking the main UI).
