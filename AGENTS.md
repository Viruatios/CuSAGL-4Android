# AGENTS

## Project snapshot
- Android native app originally ported from the JS script; the porting phase is complete, so current work should focus on maintaining and updating this Android project rather than re-reading `OriginScripts`.
- Single Android app module in `app/` using Jetpack Compose; `MainActivity.kt` is the Activity/state orchestration entry, while main Compose screens live under `app/src/main/java/com/culoo/cusagl_4android/main/ui/`.
- Core Kotlin logic now lives under `app/src/main/java/com/culoo/cusagl_4android/core/` (score parsing, storage/cache, timeline prebake).
- Main-screen state, UI state, preload helpers, permission-guidance helpers, score-management helpers, playback-configuration helpers, update helpers, and main-screen constants live under `app/src/main/java/com/culoo/cusagl_4android/main/` (`MainScreenController`, `MainScreenState`, `AboutUiState`, `PermissionGuideController`, `ScoreManagementController`, `PlaybackConfigController`, `AboutController`, `MainConstants`).
- Runtime playback scheduling is implemented in core (`RuntimePlaybackEngine`, `PlaybackConfig`, `RuntimePlaybackInterfaces`).
- Touch injection, accessibility service wiring, and accessibility settings entry helpers live under `app/src/main/java/com/culoo/cusagl_4android/accessibility/` (`LyreAccessibilityService`, `AccessibilityTouchInjector`, `TouchCoordinateMapper`, `AccessibilityPermission`).
- Foreground playback service and Compose overlay controls live under `app/src/main/java/com/culoo/cusagl_4android/overlay/` (`OverlayPlaybackService`, `PlaybackSessionRequest`, `OverlayPositionMapper`, `OverlayPlaybackPanel`).
- Tunable constants are grouped by domain in `CoreConstants`, `AccessibilityConstants`, `OverlayConstants`, and `MainConstants`; `BuildConfig.VERSION_NAME` remains the app version source.

## Architecture and data flow
- Current work is Android-native maintenance and feature evolution: keep pure Kotlin core logic decoupled from Compose UI and Android services, and use `CopilotDocs/GeneralPlan.md` as the primary planning reference.
- Treat historical JS materials under `OriginScripts` as legacy background only; do not use them as the default source for new implementation decisions.
- Typical pipeline: score JSON -> Kotlin `data class` models -> preprocess timeline / cache -> playback scheduler.
- Implemented core pipeline: `ScoreParser` -> `ScoreStorage.buildCache` -> `TimelinePrebaker.prebakeTimeline` (see `app/src/main/java/com/culoo/cusagl_4android/core/`).
- Runtime playback uses cache -> `RuntimePlaybackEngine` -> `TouchInjector` with a `CacheProvider` (see `app/src/main/java/com/culoo/cusagl_4android/core/RuntimePlaybackEngine.kt`).
- Android touch injection path: `RuntimePlaybackEngine` -> `TouchInjector` -> `AccessibilityTouchInjector` -> `AccessibilityServiceBridge`/`LyreAccessibilityService` (gesture dispatch).
- Main-page preparation path: `MainActivity` -> `PlaybackConfigController.loadApplied/applyAndSave` -> `MainScreenController.refresh(configuredQueue)` / `PlaybackConfigController.preloadScores` -> `ScoreStorage`/`ScoreParser`, with `PermissionGuideController` deriving permission todos and prepare-blocking reasons, then `PlaybackSessionRequest` -> `OverlayPlaybackService` when ready.
- Score-management path: `MainActivity` -> `ScoreManagementController` -> `ScoreStorage`/`ScoreParser`; it handles system document import, manual score creation, duplicate overwrite confirmation, and cache cleanup on overwrite/delete.
- Playback-configuration path: `MainActivity` -> `PlaybackConfigController` -> `PlaybackConfigDraft`/`AppliedPlaybackConfig`; it persists app-private JSON config, maps supported play modes to `PlayType`, resolves single-score/queue selection, and builds `PlaybackSessionRequest`.
- Overlay playback path: `PlaybackSessionRequest` -> `OverlayPlaybackService` -> `RuntimePlaybackEngine`; the service observes `PlaybackSnapshot` to update the Compose panel and foreground notification.

## Domain rules (music playback)
- Playback must be serial by "basic unit" (not concurrent note timers) to avoid timing drift and key-up races.
- Enforce a short key-up gap to prevent swallowed repeated notes.
- Kotlin prebake enforces a minimum key-up gap (`CoreConstants.MIN_KEY_UP_GAP_MS = 25`) and serializes events in `TimelinePrebaker`.
- Score parsing rules (rest/single/chord/arpeggio), stop symbols (space and "/"), and time signature handling are implemented in `ScoreParser` and covered by tests; consult code/tests first for current behavior.
- Runtime playback scheduling uses `SystemClock.uptimeMillis()` plus sleep+spin (`spinThresholdMs`) for event alignment (see `RuntimePlaybackEngine`).

## Touch coordinate mapping
- Use the 1920x1080 baseline with `Scale = max(W/1920, H/1080)`, X centered, Y bottom-aligned.
- Base key coordinates live in `app/src/main/java/com/culoo/cusagl_4android/core/KeyLayout.kt` (e.g., `Q -> PointF(455f, 670f)`, `M -> PointF(1460f, 940f)`) and should be treated as canonical input positions.
- `KeyLayout` exposes coordinates through `KeyLayout.baseCoordinates` and key order through `KeyLayout.allKeys`.
- Runtime mapping uses `TouchCoordinateMapper` in `app/src/main/java/com/culoo/cusagl_4android/accessibility/TouchCoordinateMapper.kt` (WindowManager `currentWindowMetrics` + cached mapping).
- Overlay positioning uses the same 1920x1080 scale and centered X mapping but top-aligns Y; `OverlayPositionMapper` constrains the panel above the mapped first key row with an 80px-base safety margin.
- Overlay dragging is allowed only while playback is `IDLE`, `PAUSED`, or `STOPPED`; `PLAYING` locks the panel position.

## Project-specific conventions
- JSON-heavy inputs should be modeled as Kotlin `data class` types.
- Prefer extracting pure Kotlin logic from UI; use unit tests under `app/src/test` to validate current Android behavior.
- Score files are stored under `filesDir/score_file` and normalized to `####.name.json`; cache files live under `filesDir/cache` (see `ScoreStorage`).
- Playback configuration is implemented in Step7: the main UI uses the applied config queue for preload/prepare playback, with single-score fallback and queue parsing rules documented in `CopilotDocs/step7/plan.md`.
- Permission guidance is implemented in Step8: the main UI shows overlay/accessibility permission todos, a once-per-foreground home permission dialog, and prepare-blocking reasons derived by `PermissionGuideController` (see `CopilotDocs/step8/plan.md`).
- Playback config is persisted as app-private `filesDir/playback_config.json`; debug mode is stored in the draft but does not yet change `Logger` behavior.
- Score import/manual creation uses strict validation via `ScoreParser.parseScoreTextStrict`: non-empty `name`, positive integer `bpm`, `N/D` time signature with power-of-two denominator, non-empty `notes`, and non-empty parsed notes.
- Cache JSON is produced via `ScoreStorage.serializeCache` using `org.json` and stores merged timeline batches (`CacheData`).
- Core unit tests already exist in `app/src/test/java/com/culoo/cusagl_4android/core/ScoreParserTest.kt`.
- Playback snapshot tests live in `app/src/test/java/com/culoo/cusagl_4android/core/RuntimePlaybackEngineTest.kt`; overlay geometry tests live under `app/src/test/java/com/culoo/cusagl_4android/overlay/`; main-screen preload/cache, permission-guidance, score-management, and playback-config tests live under `app/src/test/java/com/culoo/cusagl_4android/main/`.
- Runtime playback injects dependencies via `TimeSource`, `Sleeper`, and `TouchInjector` to keep core logic platform-agnostic (see `RuntimePlaybackInterfaces.kt`).
- Cache loading for playback goes through `ScoreCacheProvider`, which builds cache on demand when missing (see `RuntimePlaybackInterfaces.kt`).
- Core logging stays platform-agnostic via `Logger`/`LogTags` in `app/src/main/java/com/culoo/cusagl_4android/core/Logger.kt` (e.g., `ScoreStorage.listAndNormalizeScores` accepts a `Logger`).
- Main Compose UI text should use Android string resources in `res/values/strings.xml` and `res/values-en/strings.xml`; pure Kotlin controllers may continue returning business messages without depending on Android `Context`.

## Developer workflow notes
- Use the Gradle wrapper (`gradlew`/`gradlew.bat`); the only module is `:app`.
- Run `gradlew.bat :app:testDebugUnitTest :app:assembleDebug` to verify local unit tests and the debug build.
- The project compiles against Android API 36.1; keep `androidx.core-ktx` on a release compatible with that SDK (`1.18.0` currently), because `1.19.0` requires API 37.
- Accessibility service config lives in `app/src/main/res/xml/accessibility_service_config.xml` and is declared in `app/src/main/AndroidManifest.xml`.
- `OverlayPlaybackService` is a `specialUse` foreground service declared in `app/src/main/AndroidManifest.xml`; it requires overlay permission and a connected accessibility service before starting.

## Communication and rules
- Use Chinese while communicating with the user.
- UTF-8 encoding for all files while reading/writing. 
- `X` is a placeholder for the current step number (e.g., `step1`, `step2`); update as needed when new steps are added.
- For each stepX, write down `CopilotDocs/stepX/plan.md` first to focus on the current requirement and avoid scope creep; if new requirements arise, note them down in `CopilotDocs/stepX/plan.md` for future implementation.
- Before starting a new stepX, read the `CopilotDocs/GeneralPlan.md` to confirm what to do. Then check the `CopilotDocs/step(X-N)/plan.md` of those previous steps for any relevant context or pending requirements that may affect the new step.
- For bug fixes, use `bugfixX` instead of `stepX` and write `CopilotDocs/bugfixX/plan.md` first.
- Write a brief summary of what you did after completing  the implementation of each stepX or bugfixX, update at `README.md ##开发节点的记录`, and update the version number accordingly.

## Key references
- Primary maintenance and development plan: `CopilotDocs/GeneralPlan.md`.
- Current Android behavior references: source code under `app/src/main/java/com/culoo/cusagl_4android/` and tests under `app/src/test/`; use `README.md` mainly for user-facing notes and development records.
