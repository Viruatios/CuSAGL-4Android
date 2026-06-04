---
name: update-agent-instructions
description: Update an existing project-root AGENTS.md from the current repository with minimal, evidence-based changes. Use when asked to refresh, synchronize, audit, or bring AGENTS.md up to date after architecture, workflow, convention, dependency, integration, file-layout, build, test, or debugging changes.
---

# Update Agent Instructions

Refresh the existing project-root `AGENTS.md` without rewriting accurate guidance. Treat the current repository as evidence and keep the resulting diff small, concrete, and useful to coding agents.

## Workflow

### 1. Establish the baseline

- Treat the current workspace or repository root as the target project root.
- Make the root `AGENTS.md` the first repository content read. Read it in full before inspecting other repository files or running analysis.
- If the root `AGENTS.md` does not exist, stop and tell the user. Do not create one unless the user explicitly changes the request.
- Understand its structure, section order, formatting, scope, and claims. Assume every existing statement is correct until repository evidence proves it stale, incomplete, or misleading.

### 2. Discover existing AI conventions once

Perform one glob or file-discovery search using this combined pattern:

```text
**/{.github/copilot-instructions.md,AGENT.md,AGENTS.md,CLAUDE.md,.cursorrules,.windsurfrules,.clinerules,.cursor/rules/**,.windsurf/rules/**,.clinerules/**,README.md}
```

- Honor repository ignore rules where the available search tool supports them.
- Skip generated, dependency, vendor, cache, and build-output directories.
- Read only relevant matches after the single discovery search.
- Treat nested instruction files as scoped evidence. Promote their guidance to the root `AGENTS.md` only when it applies broadly to the repository.
- Resolve conflicts using repository behavior and configuration as the source of truth; do not blindly merge competing instructions.

### 3. Identify material differences

Inspect the codebase selectively to verify what changed or is missing compared with the baseline. Focus on facts that let an agent become productive quickly:

- Architecture: added or removed components, service boundaries, ownership, entry points, and data flows.
- Workflow: actual build, test, lint, run, debug, generation, and release commands.
- Conventions: established patterns, renamed paths, deprecated practices, and project-specific constraints.
- Integrations: external services, runtime dependencies, generated artifacts, configuration, and required environment setup.

Use direct evidence from source files, configuration, scripts, tests, and documentation. Treat the current working tree as the repository state unless a file is clearly transient or generated.

Do not document:

- Generic engineering advice.
- Aspirational practices that the repository does not yet follow.
- Speculation based only on names or incomplete clues.
- Incidental implementation details that do not help future agents.
- Facts already documented accurately in `AGENTS.md`.

### 4. Apply surgical edits

- Preserve every still-accurate line exactly as written.
- Preserve the existing structure, section order, heading style, list style, tone, and formatting.
- Add guidance to the most logical existing section. Create a new section only when no existing section can hold an important repository-wide fact.
- Modify or remove text only when evidence proves it outdated, incorrect, or actively misleading.
- Write concise, actionable bullets with specific repository examples and paths.
- Keep instructions scoped to this repository and explain unusual behavior where an agent could otherwise make a wrong assumption.
- Do not reflow, polish, reorganize, or rephrase unrelated content.

### 5. Verify the result

- Re-read the complete updated `AGENTS.md`.
- Inspect the diff and remove unrelated formatting churn.
- Confirm every changed claim is supported by repository evidence.
- Confirm no accurate instruction, project-specific constraint, or scoped convention was accidentally lost.
- Run a Markdown or repository documentation check only when the project already provides one relevant to `AGENTS.md`.

## Final Response

Summarize the result under these categories when applicable:

- Added
- Modified
- Removed

State explicitly when a category has no changes. Mention validation performed and any uncertainty that prevented a possible update.
