---
name: cusagl-step-workflow
description: Use for CuSAGL-4Android stepX or bugfixX planning, implementation, verification, and wrap-up. Trigger when the user asks to plan, start, implement, continue, finish, summarize, version, or record a project step or bug fix; includes writing CopilotDocs/stepX/plan.md or CopilotDocs/bugfixX/plan.md, honoring AGENTS.md constraints, updating README development records, and applying the project versioning rules.
---

# CuSAGL Step Workflow

## Core Rule

Follow the live repository documents instead of copying stale project facts into this skill. Read and write all project documents as UTF-8. Communicate with the user in Chinese.

## Discovery Workflow

1. Read `AGENTS.md` first and treat it as the active project contract.
2. Read `CopilotDocs/GeneralPlan.md` to identify the requested `stepX` or `bugfixX` and its high-level intent.
3. Scan relevant project documents before planning:
   - `CopilotDocs/**/plan.md` for previous-step commitments, deferred work, exclusions, interface decisions, and bugfix context.
   - `README.md` for current version, touch-mapping rules, architecture notes, and `## 开发节点的记录`.
   - Relevant source and test files for current implementation shape.
4. Use `rg`/`rg --files` where available; prefer targeted searches over broad reading.

## Planning Workflow

Before writing or changing a step plan, resolve the current scope:

1. Identify whether the task is a feature step (`stepX`) or a fix (`bugfixX`).
2. Confirm that the target plan path is `CopilotDocs/stepX/plan.md` or `CopilotDocs/bugfixX/plan.md`.
3. Derive what can be derived from the repository. Ask the user only about unresolved product intent, tradeoffs, or requirements that would materially change the plan.
4. Discuss unclear items with the user before writing the plan.
5. Write a focused plan that includes goal, scope boundaries, implementation approach, dependencies/interfaces, edge cases, tests, and acceptance criteria.
6. Record newly discovered out-of-scope requirements in the same plan as future work instead of silently expanding the current step.

## Implementation Workflow

When the user asks to implement a planned step or bugfix:

1. Read the current `stepX` or `bugfixX` plan again before editing code.
2. Keep changes scoped to that plan and to any explicitly approved follow-up requirements.
3. Prefer pure Kotlin logic extracted from UI. Model JSON-heavy inputs as Kotlin `data class` types before translating behavior.
4. Keep UI and Android services aligned with existing Compose, accessibility, overlay, and playback-controller patterns.
5. Use Kotlin coroutines for file I/O or preprocessing work that maps from JS async behavior.
6. Preserve project playback rules: serial basic-unit scheduling, key-up gap handling, JS-compatible score parsing, and canonical touch-coordinate mapping.
7. Add or update focused unit tests under `app/src/test` for core logic, controllers, permission guidance, overlay geometry, or playback behavior as appropriate.
8. Verify with the Gradle wrapper. The standard local check is `gradlew.bat :app:testDebugUnitTest :app:assembleDebug` unless the user narrows the verification.

## Wrap-Up Workflow

After completing each `stepX` or `bugfixX` implementation:

1. Update `README.md` under `## 开发节点的记录` with a brief summary of the completed work.
2. Update the app version according to the change type:
   - Major update or upgrade: `v(x+1).0.0`
   - Regular feature step: `vx.(y+1).0`
   - Bug fix (`bugfixX`): `vx.y.(z+1)`
3. Update `AGENTS.md` only when project structure, critical interfaces, workflow rules, or durable conventions changed.
4. Report the changed files, verification command results, version change, and any remaining risks or follow-up items.

## Project Constraints To Keep Active

- Use the Gradle wrapper; the app module is `:app`.
- Keep `OriginScripts/CuSimpAutoGenshinLyre/` as reference-only.
- Keep core logic platform-agnostic where possible, using injected interfaces such as time, sleeping, cache loading, logging, and touch injection.
- Respect storage conventions: score files under `filesDir/score_file`, cache files under `filesDir/cache`, and playback config under `filesDir/playback_config.json`.
- Do not broaden a step into later-step UI, permission, configuration, performance, or refactor work unless the user explicitly approves it.
