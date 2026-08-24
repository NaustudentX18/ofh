# 🌲 OFH — Build Plan

> Independent agent harness for Android. Own engine, own UI, no DSH dependency.
> Best ideas from DSH + Hermes Bot + Grok Bot. Provider-agnostic.

## Guiding principles
1. **Own everything** — agent loop, tool system, UI, config. Nothing from `@deepseek-ai/dsh`.
2. **Provider-agnostic** — one OpenAI-compatible adapter covers Ollama Cloud/Local, MiniMax, Z.AI, Qwen, DeepSeek, OpenAI, Custom.
3. **Local-first** — runs on-device; offline where possible.
4. **Real-time + personality** — live web grounding and a distinctive tone (Grok-style).
5. **Extensible** — skills + plugins (Hermes/DSH-style).

## Phases
### Phase 0 — Foundation
- [x] Research synthesis (DSH / Hermes / Grok) → `docs/ARCHITECTURE.md` (Grok done; DSH+Hermes in progress)
- [x] Project scaffold (Gradle Android app, package `com.ofh.harness`)
- [x] Core config model (providers, credentials, settings) — `ProviderConfig.kt`
- [x] Provider adapter (OpenAI-compatible, streaming, thinking) — `ProviderAdapter.kt`
- [x] Agent loop (plan → act → observe) — `AgentLoop.kt`
- [x] Tool system (register + dispatch) — `ToolRegistry.kt` + `BuiltinTools.kt`
- [x] Minimal web UI (chat surface) — `assets/web/index.html`
- [x] Android shell (WebView host + bridge) — `WebViewActivity.kt`
- [ ] Ship v0.1.0 (debug APK builds; needs provider wiring + on-device test)

### Phase 1 — Core surface
- [ ] Skills (SKILL.md loader)
- [ ] Memory / context compaction
- [ ] Real-time web search + fetch
- [ ] Personality presets (Bot Mode)
- [ ] Share receiver, widgets, quick-reply, STT, screenshot
- [ ] First-run wizard (provider picker)
- [ ] Ship v0.2.0

### Phase 2 — Polish
- [ ] Quick-settings tile, PiP, drag-drop, periodic tasks
- [ ] Crash reporting, i18n, biometric gate, dynamic colour
- [ ] Accessibility observability
- [ ] MCP shim, provider fallback
- [ ] Ship v0.3.0

### Phase 3 — Wild
- [ ] Wake word, Model Cookbook, sync, TTS, butler, Wear, DeX, marketplace, voice call, live observer
- [ ] Ship v1.0.0

## Ship gate
- `./gradlew :app:assembleRelease` clean
- realign + v2/v3 sign, 0 misalignment
- `aapt2 dump badging` → `label='OpenForest'`
- copy to `/sdcard/Download/OFH-v<version>.apk` + `dist/`
