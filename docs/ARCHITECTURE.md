# 🌲 OFH — Open Forest Harness: Architecture

> A fully **independent** agent harness for Android. Own engine, own UI, no
> `@deepseek-ai/dsh` dependency. Synthesised from the best of **DSH**, **Hermes
> Bot**, and **Grok Bot**.

## 1. What we steal from each

### From DSH (the engine we love the look/feel of)
- **Provider-agnostic LLM adapter** — one OpenAI-compatible profile per provider
  (route → `baseURL`, `apiKeyEnv`, `models`, `api` protocol). Covers Ollama
  Cloud/Local, MiniMax, Z.AI, Qwen, DeepSeek, OpenAI, Custom.
- **Clean config layout** — `~/.ofh/settings.yaml` (`llm-pi-ai.providers` +
  `agent-default-model`) + `.credentials.yaml` (`refs: { KEY: "…" }`).
- **Agent loop** — plan → call LLM → execute tool → observe → loop.
- **Tool system** — tools registered and dispatched by the model's tool-calls.
- **Skills** — `SKILL.md` files that change behaviour.
- **Web UI in a WebView** with a native↔web bridge (`window.__OFH_*`).
- **Plugin seam** — small self-contained plugins (web search, status bridge, …).

### From Hermes Bot
- **Multi-provider profiles as first-class config** (OpenAI-compatible + more).
- **Memory / context management** — compaction, long-term memory.
- **Skills + plugins** as the extension model.
- **Agentic tool-use-first** design.

### From Grok Bot
- **Live grounding as a first-class tool** — real-time web search wired into
  every turn (recency is the killer feature).
- **Deliberate personality** — a defined, consistent voice, tuned on purpose.
- **Tool-use-first architecture** — tools as native primitives.
- **Multimodal in/out** — vision understanding + generation + voice.
- **Fast tier + full tier** — a "mini" model for latency-sensitive turns.
- **Reasoning-mode toggle** — fast path for easy, deep path for hard.

## 2. Core components

```
┌────────────────────────────────────────────────────────────┐
│  Android shell (Kotlin)                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ WebView  │ │ Provider │ │ Agent    │ │ Tool         │  │
│  │ host +   │ │ adapter  │ │ loop     │ │ registry     │  │
│  │ bridge   │ │ (LLM)    │ │          │ │ + dispatcher │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ Skills   │ │ Memory / │ │ Real-time│ │ Personality  │  │
│  │ loader   │ │ context  │ │ search   │ │ presets      │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │
└────────────────────────────────────────────────────────────┘
```

### 2.1 Provider adapter (`ProviderAdapter`)
- One OpenAI-compatible client (chat completions + streaming + tool calls).
- `ProviderConfig` = list of presets (route, label, baseURL, defaultModel,
  keyEnv, needsKey). Same presets as the current app.
- Reads `~/.ofh/settings.yaml` + `.credentials.yaml`.
- Supports `thinking`/reasoning format where the provider exposes it.

### 2.2 Agent loop (`AgentLoop`)
- `runTurn(userMsg)`:
  1. Build messages (system persona + history + tools).
  2. Call provider (streaming).
  3. If tool-call → dispatch via `ToolRegistry` → append result → loop.
  4. Else → return final answer.
- Max iterations guard, cancellation, streaming to UI.

### 2.3 Tool system (`ToolRegistry`)
- Tools are `(name, description, schema, handler)`.
- Built-ins: `web_search`, `web_fetch`, `device_apps`, `device_clipboard`,
  `screenshot`, `memory`, `mcp_call`.
- Skills add tools/behaviour via `SKILL.md`.

### 2.4 Web UI + bridge
- A bundled web app (chat surface) served from assets, loaded in WebView.
- Bridge: `window.__OFH_*` for send/receive/stream, plus native actions
  (STT, screenshot, share).

### 2.5 Memory / context
- Conversation history with compaction (drop old turns, summarise).
- Long-term memory: key-value store + file notes.

### 2.6 Real-time grounding
- `web_search` tool (free backends: Wikipedia, StackOverflow, Hacker News, npm,
  MDN, web.archive.org) — Grok-style recency.

### 2.7 Personality
- `persona` system prompt presets (Bot Mode): default, researcher, builder,
  analyst, scribe. A distinctive default voice.

## 3. Tech stack
- **Kotlin + Android** (minSdk 24, targetSdk 28, compileSdk 35) — same as current.
- **No Node / no DSH** — pure Kotlin harness. HTTP via `HttpURLConnection`/OkHttp.
- **Web UI** — bundled static HTML/JS/CSS in `assets/web/`.
- **Config** — YAML (`~/.ofh/`), parsed with a small YAML reader (or snakeyaml).

## 4. Config layout (`~/.ofh/`)
```
~/.ofh/
  settings.yaml        # llm-pi-ai.providers.* + agent-default-model + persona
  .credentials.yaml    # refs: { KEY_ENV: "…" }
  skills/              # SKILL.md files
  memory/              # long-term notes
  sessions/            # conversation JSONL
```

## 5. Build phases
See `docs/PLAN.md`. Phase 0 = foundation (provider adapter → agent loop →
tool system → web UI → Android shell → ship v0.1.0).

## 6. Non-goals (v0.x)
- No on-device model inference (defer to Model Cookbook phase).
- No image generation (defer).
- No account/sync (defer).
