# Codex (ALTREX CODE V4, Android) — Current Architecture (audit, 2026-11-11)
Source audited: ALTREX-CODE zip, 3.7 MB, 328 files, 25,510 lines TypeScript.
PC stack: Electron 43 + React 19 + TypeScript, pnpm monorepo, Windows v0.1.0.
Layout: packages/core (pure Node: gateway, router, task engine, verification, tools, security, checkpoints), packages/contracts (zod UI-core contract), apps/desktop (Electron main/preload/renderer v4).
Providers already built: Gemini, OpenRouter, Groq, NVIDIA NIM, Ollama, OpenAI-compatible, crax-gpt, Cerebras, SambaNova, Cloudflare, Codex CLI.
What works (PC): one Model Gateway, normalised errors, routing AUTO/FAST/POWERFUL/FREE_ONLY/LOCAL_ONLY/CUSTOM, checkpoints before tasks, permission profiles, evidence-based verification (VERIFIED only on passing checks + review), 715 tests passed / 1 failed (frontend race) in 2026-09-27 final audit, no BLOCKER, no hardcoded keys found.
What is broken / must fix: H1 Custom model sent to active provider not owning provider (privacy + correctness); H2 LOCAL_ONLY can fall back to cloud Codex engine. Heavy work on Electron main thread freezes UI.
What cannot run on Android: Electron, Node fs/child_process, git CLI, Ollama runtime, Codex CLI, safeStorage. Android port is a Kotlin rewrite reusing logic/contracts/docs, not a copy-paste.
