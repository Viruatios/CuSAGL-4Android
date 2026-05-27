## Plan: Android Auto Touch Playback Port

Translate the JS core into platform-agnostic Kotlin modules first, then define the Android/Compose integration points and touch injection pipeline. We will map data models and parsing rules from the JS and README, mirror the serial timeline scheduler and key-up gap behavior, and specify caching/IO flows. This document is the master plan outline for recording requirements and direction; each concrete future requirement will have its own detailed plan saved separately in `CopilotDocs/plan.md`.

### Steps 4
1. Extract core domain rules and data models from `OriginScripts/CuSimpAutoGenshinLyre/main.js`, `OriginScripts/CuSimpAutoGenshinLyre/player.js`, and `OriginScripts/CuSimpAutoGenshinLyre/README.md` into a Kotlin module outline with `data class` and `suspend` boundaries.
2. Define the playback pipeline: parse notes → prebake timeline → cached timeline → serial scheduler, matching the min-gap and hybrid sleep semantics, plus the queue/repeat rules from `OriginScripts/CuSimpAutoGenshinLyre/main.js`.
3. Specify Android integration points: touch coordinate mapping from `README.md` (appendix), input injection strategy, and UI entry points in `app/src/main/java/com/culoo/cusagl_4android/MainActivity.kt` for future Compose wiring.
4. Record plan-ownership rules: `AGENTS.md` is updated only for major project changes (structure changes, new key classes/methods) or by explicit command; concrete requirement plans live in `CopilotDocs/plan.md`.

### Requirement Breakdown 4
1. Core data model + parsing: JSON models, note-string parsing, and validation rules.
2. Timeline compilation: prebake timeline generation, key-up gap handling, time signature handling.
3. Playback runtime: serial scheduler, caching IO, queue/repeat orchestration, and time-source strategy.
4. Android integration: touch coordinate mapping, input injection (accessibility/gesture), Compose UI wiring, and permissions/foreground behavior.

### Further Notes
1. Touch input mechanism: AccessibilityService gestures.
