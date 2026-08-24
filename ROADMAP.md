# 🌲 Open Forest Harness — Roadmap

> **How to read this.** The first section is the **recommended build order** — what to start working on first, phase by phase. The section after it is the **full feature backlog** (every idea from a 5-agent research sweep across agent-core, mobile UX, provider/model layers, the DSH/Hermes/Grok/MCP ecosystem, and repo professionalism), grouped by area with per-item priorities.
>
> Priorities: **P0** = must-have for a trustworthy daily driver · **P1** = high-value next · **P2** = nice-to-have · **P3** = later / experimental.

---

## 🚦 Recommended build order — start here

Ordered by my judgment of what turns OFH from a working demo into a trustworthy, delightful, everyday phone agent **in the smallest number of steps**. Each phase is independently shippable.

### Phase 0 · Prove it runs (the gate) — *do first, in parallel with Phase 1*
Nothing on this roadmap matters until the app has actually run against a live provider on a device.
- ✅ On-device smoke test against **Ollama Local** and one cloud provider (MiniMax/OpenAI) — `docs/SMOKE-TEST.md` items, every row green. **[the gate]**
- ✅ Get CI green on `main` (build + 3/3 unit tests + lint) so the live CI badge is honest.
- ✅ Confirm release APK rebuilds from source and matches the released `SHA256SUMS`.

### Phase 1 · Trust & reliability core — *must ship before anything that acts*
A phone agent that can send, search, or buy **must** gate its actions and survive flaky mobile networks.
- **Human-in-the-loop / permission gates** — `canUseTool`-style confirmation card before any sensitive tool (SMS, write, purchase, device-affecting). [P0]
- **Provider fallback router with retry + exponential backoff** — 429/5xx/timeout/context-overflow → transparently retry on a backup provider; never crash a turn. [P0]
- **Token accounting + context budget** — client-side token counting (incl. reasoning/cached tokens), cap at ~80% of the window so streaming never 400s. [P0]
- **Tool authorization + allow/deny lists + dangerous-action guardrails.** [P0]
- **Lifecycle hooks** (Claude-SDK-style `SessionStart` / `PreToolUse` / `PostToolUse` / `Stop`) as the one universal extension point for observability, permissions, and side-effects. [P0]
- **Streaming robustness** — hardened SSE parser (multi-line `data:`, `[DONE]`, keep-alives), stream→non-stream fallback. [P0]
- **Retry-with-error-context + tool-call repair** — failed calls come back into the loop once instead of dying. [P0]
- **Hardened errors** — inline error + Retry everywhere; structured error codes.

### Phase 2 · Delightful input & output
> Make it a pleasure to talk to, not a chore.
- **Streaming UI polish** — blinking cursor, instant always-visible **Stop**, stable markdown (no repaint flicker). [P0]
- **On-device speech-to-text** (mic button, live waveform, push-to-talk + hold-to-talk) — the killer input on a phone. [P0]
- **Full markdown rendering** + **copy-on-code-block** + scrollable syntax-highlighted code. [P0]
- **Back/gesture nav hierarchy** (keyboard → drawer → exit) + config-change resilience (rotation never drops the in-flight stream). [P0]
- **Chat history list + auto-titles + search** + pin/delete with confirm. [P1]
- **Edit / regenerate / retry** on any message. [P1]
- **Text-to-speech** read-aloud with stop control. [P1]
- **Share intent → OFH** (text/URL/image) with a content-preview card before send. [P1]
- **Voice recognition** (STT) and **TTS** as built-in tools, not just UI.

### Phase 3 · Memory & retrieval — the compounding differentiator
> A phone that is always with you should get smarter about you. This is the biggest moat.
- **Recent-notes injection + layered memory** (working / short / long-term). [P0]
- **On-device vector + BM25 hybrid retrieval over notes** (SQLite + FTS5, optional native ANN upgrade path). [P0]
- **Conversation compression / summarization on budget** (rolling "what happened so far" block). [P0]
- **Embeddings client** + per-provider defaults + on-device embedding option. [P0]
- **Skills with front-matter schemas + triggers + install trust-scan** (OpenHands-style discovery sources; skillspector-style gate). [P0]
- **Memory lifecycle / TTL** + anchored pins + conflict resolution. [P2]

### Phase 4 · Proactive & multi-agent — from assistant to helper
- **Background curator** (Hermes-style): idle-time pruning/consolidation of memory & skills on charge. [P0]
- **Cron / scheduled tasks** + delivery to a chosen channel + self-messaging (daily briefing, backups, audits). [P1]
- **Sub-agents + planner/executor + reflection pass** (bounded budgets; 2–3 max concurrency for battery). [P1]
- **MCP client** (stdio/HTTP/SSE) so OFH reaches the ecosystem + the existing Pi MCP fleet. [P1]
- **Gateway** to Telegram/Discord/Slack/etc. + voice-memo transcription — meet the user in their chats. [P1]
- **Multimodal vision** (camera/gallery/screenshot → `image_url`) with per-provider capability gate + local-stays-local. [P1]

### Phase 5 · Scale & distribution
- **More providers** (Groq, OpenRouter, Mistral, Together, xAI, Moonshot/Kimi, Cerebras, Gemini-via-proxy) + a **model cookbook**. [P1]
- **Home-screen widgets, app shortcuts, Quick-Settings tile, deep links, notifications.** [P2]
- **Pi/tailnet integration** — heavy local model on the Pi (`http://<pi>:11434`), MCP fleet, phone automation. [P2]
- **Play Store readiness** — reproducible builds, R8/proguard hygiene, verified badge, docs→Pages site. [P2]
- **Polished docs** — convert `docs/` to user-facing QUICKSTART / CONFIGURATION / SECURITY / FAQ. [P2]

---

## 🗂️ Full feature backlog (every researched idea)

### 1. Agent core & loop
- Layered memory store (working / short-term / long-term) [P0]
- Rolling context window with bounded slice [P0]
- Recent-notes injection [P0]
- Conversation compression / summarization on budget [P0]
- Recursive summarization (summary-of-summaries) [P2]
- Fenced memory recall with token cap + "memory block" [P0]
- Episodic session indexing / reopen-as-context [P2]
- Key-value durable memory (facts store) [P2]
- Memory conflict resolution (surface both, user picks) [P3]
- Anchored / user-curated pins (immune to pruning) [P2]
- Reflection / self-critique pass before presenting [P1]
- Plan-then-act loop + step-budget UI [P1]
- Interrupt / pause mid-plan and resume [P2]
- Task templates / recipes [P2]
- Checkpointers + cross-thread store (survive app-kill) [P1]
- Deferred/background plan execution + notify-on-finish [P1]
- Run-budget handoff across agents [P2]

### 2. Tools & function calling
- Parallel tool calls (with `tool_use_id` correlation) [P0]
- Streaming tool events (`tool_start` → `tool_result`) [P0]
- Strict typed tool schemas + validation [P0]
- Tool output size caps + truncation/summarization [P0]
- Automatic tool-call repair on failure [P0]
- Tool error taxonomy (retryable vs fatal) [P1]
- Tool authorization / confirmation gates [P0]
- Allow/deny lists + dangerous-action guardrails [P1]
- Tool description upkeep from source-of-truth [P2]
- Nested/sub-tool invocation [P2]
- Tool metrics/logging for introspection [P3]
- Skills-as-tools (markdown + executable plugins) [P1]
- MQTT client tool (smart-home) [P1]
- SMS send/read tool [P2]
- WhatsApp Web gateway [P3]

### 3. Memory & RAG
- On-device vector store with SQLite (+ optional native ANN upgrade) [P0]
- Chunking strategy sized to small context windows [P0]
- Document ingestion pipeline + dedupe [P1]
- Hybrid retrieval (BM25 keyword + vector) [P1]
- Metadata-filtered retrieval [P1]
- On-device vs API embedding dual-mode [P1]
- Ranking / re-ranking pass [P2]
- Embedding caching + incremental indexing [P2]
- Retrieval-quality telemetry (local-only) [P3]

### 4. Multi-agent & workflows
- Sub-agent / delegate pattern (bounded budget) [P1]
- Reviewer/curator agent (Hermes-style) [P1]
- Supervisor / planner-executor split [P2]
- Parallel agents for independent subtasks (2–3 max) [P2]
- Handoffs (OpenAI Agents SDK) [P1]
- Agents-as-tools [P2]
- Shared memory bus between agents [P2]
- Output contracts between agents [P2]

### 5. Providers & model layer
- OpenAI-compatible adapter + per-provider "spec" table (paths, auth, streaming, vision) [P0]
- Tool-call / structured-output normalization (Ollama/Qwen/Zhipu/GLM/DeepSeek quirks) [P0]
- `max_tokens` vs `max_completion_tokens` normalization [P1]
- Reasoning-effort parameter + strip `reasoning_content` [P1]
- Fallback router (A→B on 429/5xx/network/context-overflow) [P0]
- Context-overflow fallback to larger-window provider [P1]
- Latency/cost-aware routing + telemetry (TTFT, cost, error) [P1]
- Per-task model selection (task classifier) [P1]
- Health checks + proactive routing [P2]
- Cost/token accounting + max-budget guard [P0]
- Request-log store (SQLite) [P2]
- Vision support (image_url / base64; per-provider gate) [P1]
- Local models: Ollama management (pull/run/status) as a baseURL [P1]
- GGUF/llama.cpp on-device engine + quantize advisor [P1]
- NNAPI/NPU acceleration hook [P3]
- New providers: Groq, OpenRouter, Mistral, Together, xAI, Moonshot, Cerebras, Gemini [P1–P3]

### 6. Mobile UX & product
- Streaming cursor + instant Stop [P0]
- On-device STT (waveform, push-to-talk) [P0]
- TTS read-aloud + stop [P1]
- Markdown rendering, code copy, tables, KaTeX math [P0/P1/P2]
- Chat history, auto-titles, search, pin, date separators [P1]
- Edit / regenerate / retry [P1]
- Branching conversations [P2]
- Export/import (JSON + Markdown) + share-reply [P1]
- Offline mode + notice + queued sends [P2]
- Incognito mode (`FLAG_SECURE`) [P1]
- Quick prompts / command palette [P1]
- Persona presets (reshape system prompt) [P1]
- Theming: light / dark / forest + accent [P1/P3]
- Onboarding tutorial [P1]
- Rate-limit / cost indicator [P1]
- Back/gesture nav hierarchy + config-change resilience [P0]
- WebView best practices (clean bridge, file chooser, memory) [P1]
- Accessibility / TalkBack pass [P1]
- Widgets, app shortcuts, Quick-Settings tile, deep links [P2]
- Notifications + channels [P1]
- Empty-state design + consistent retry [P1]

### 7. Privacy & security
- Local-first default; no cloud sync [P0]
- "Sent to provider" disclosure + consent-on-attachment [P1]
- Privacy center + one-tap clear-all [P1]
- Pause-sending switch [P1]
- Opt-in telemetry only [P1]
- Encrypted-at-rest history [P2]
- Sensitive-mode redaction (PII/keys/secrets) [P2]
- Biometric gate for incognito/sensitive tools [P2]
- Screenshot-safe mode (`FLAG_SECURE`) [P2]
- Skill-install trust scan (skillspector-style) [P0]
- Permission prompts/modes + `canUseTool` [P0]
- Sandbox for tools (FS/network isolation) [P1]

### 8. Reliability & engineering
- Hardened SSE parser + stream→non-stream fallback [P0]
- Retry with backoff + jitter [P0]
- Structured errors + consistent retry pattern [P0]
- Lifecycle hooks + event-stream [P0]
- Debug trace / session-log retention + resume [P1]
- OpenTelemetry observability [P2]
- Reproducible builds + released SHA-256 [P0]
- CI = build + tests + lint [P0]
- Dependency scanning (Dependabot) + CodeQL + secret scanning [P1]

---

## ✅ Already shipped (v0.1 → v0.2)
- From-scratch pure-Kotlin `AgentLoop` (plan→act→observe), `ProviderAdapter` (OpenAI-compatible, streaming + tool calls), `ToolRegistry`, `BuiltinTools` (`web_search` / `web_fetch` / `device_info` / `memory` / `skills`), WebView chat UI, provider presets, DSH-style `~/.ofh/` config.
- Provider wizard, provider/model switcher, tool-call cards, **120s run budget**, memory notes, SKILL.md loader.
- JVM integration test (3/3) proving streaming + tool round-trip.

---

## 📏 Definition of done (per phase)
- **Phase 0:** smoke test green on a device; CI green on `main`; released APK matches its checksum.
- **Phase 1:** every sensitive tool gated by a confirmation; a provider outage never silently kills a turn; token usage visible.
- **Phase 2:** a new user can install, speak, get a beautiful streaming answer, and recover from any nav/config change without losing a reply.
- **Phase 3:** the agent recalls prior notes and compresses long chats; memory is searchable on-device.
- **Phase 4:** a scheduled + background + delegated task completes end-to-end without blocking interactive use.
- **Phase 5:** the app is on the Play store (or a distributable build path) with reproducible, checksum-verified releases.
