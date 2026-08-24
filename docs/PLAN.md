# 🌲 OFH — Build Plan

> Independent agent harness for Android. Own engine, own UI, no DSH dependency.
> Provider-agnostic, local-first. The prioritized feature backlog lives in [`ROADMAP.md`](../ROADMAP.md).

## Status
- **v0.1.0** ✅ shipped — harness core (agent loop, provider adapter, tool registry, web UI, Android shell).
- **v0.2.0** ✅ shipped — provider wizard, provider/model switcher, tool-call cards, 120s run budget, memory notes, skills (SKILL.md), real `device_info`, integration test 3/3.
- **Gate ⛔** — **on-device smoke test against a live provider** is the only remaining item before Phase 1. See `docs/SMOKE-TEST.md`.

## Phase 0 · Prove it runs *(the gate — first)*
- [x] Research synthesis → `docs/ARCHITECTURE.md`
- [x] Project scaffold (Gradle, package `com.ofh.harness`)
- [x] `ProviderConfig.kt`, `ProviderAdapter.kt`, `AgentLoop.kt`, `ToolRegistry.kt`, `BuiltinTools.kt`
- [x] Minimal web chat UI + `WebViewActivity.kt` shell
- [x] Signing + clean release build; APK signed + checksum published
- [ ] **On-device smoke test vs Ollama Local + a cloud provider** ← the gate

## Phase 1 · Trust & reliability core
- Permission / confirmation gates for sensitive tools (human-in-the-loop)
- Provider fallback router + retry/backoff
- Token accounting + context budget
- Tool authorization + allow/deny lists
- Lifecycle hooks (SessionStart / PreToolUse / PostToolUse / Stop)
- Streaming robustness (hardened SSE, stream→non-stream fallback)
- Retry-with-error-context + tool-call repair

## Phase 2 · Delightful input & output
- Streaming UI polish (cursor, Stop, stable markdown)
- On-device STT (waveform, push-to-talk) + TTS read-aloud
- Markdown/code rendering + copy-on-block
- Back/gesture nav + config-change resilience
- Chat history + auto-titles + search + pin/delete
- Edit / regenerate / retry per message
- Share intent → OFH with preview card

## Phase 3 · Memory & retrieval
- Recent-notes injection + layered memory
- On-device vector + BM25 hybrid retrieval (SQLite/FTS5)
- Conversation compression / summarization
- Embeddings client (+ on-device option)
- Skills with schemas + triggers + install trust-scan

## Phase 4 · Proactive & multi-agent
- Background curator (idle-time memory/skill upkeep)
- Cron / scheduled tasks + delivery to a channel
- Sub-agents + planner/executor + reflection
- MCP client (reach the ecosystem + the Pi MCP fleet)
- Gateway (Telegram/Discord/etc.) + voice-memo transcription
- Multimodal vision (camera/screenshot)

## Phase 5 · Scale & distribution
- More providers (Groq, OpenRouter, Mistral, Together, xAI, Moonshot…)
- Widgets, shortcuts, Quick-Settings tile, deep links, notifications
- Pi/tailnet integration (heavy local model + MCP fleet + phone automation)
- Play-Store readiness (reproducible builds, R8 hygiene, verified badge)
- User-facing docs (QUICKSTART / CONFIGURATION / SECURITY / FAQ)

## Ship gate
- `./gradlew :app:assembleRelease` clean
- APKv2 sign (CN=OpenForest), 0 misalignment
- `aapt2 dump badging` → `label='OpenForest'`
- copy to `/sdcard/Download/OFH-v<version>.apk` + `dist/`
- publish `SHA256SUMS` with the release
