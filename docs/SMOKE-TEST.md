# 🧪 Open Forest Harness — On-device Smoke-Test Checklist

> OFH (Open Forest Harness) is a **provider-agnostic** Android agent harness
> (package `com.ofh.harness`), pure-Kotlin, with a WebView chat UI and streaming
> responses over the `window.OFH.onDelta / onDone` bridge.
>
> Install the APK, then work top-to-bottom. Tick each box only when the
> **expected result** matches. Anything that fails = a bug to report back.
>
> Providers: `ollamacloud` · `ollama` (local, no key, `http://127.0.0.1:11434/v1`)
> · `minimax` · `zai` · `qwen` · `deepseek` · `openai` · `custom`.
>
> Tools: `web_search` (wikipedia/stackoverflow/hackernews/npm/mdn/archive) ·
> `web_fetch` · `device_info`.
>
> Config lives in `~/.ofh/` (`settings.yaml` + `.credentials.yaml`).

**Build under test:** OFH · `com.ofh.harness` · config dir `~/.ofh/`

---

## 0 · Install, first run & wizard
- [ ] Install the APK → launcher shows **OFH / Open Forest Harness**, opens cleanly (no crash on cold start).
- [ ] First-run **provider setup wizard** appears on a fresh install.
- [ ] Wizard lists all providers: Ollama Cloud / Ollama **Local** / MiniMax / Z.AI / Qwen / DeepSeek / OpenAI / **Custom**.
- [ ] Pick a **cloud provider** → wizard asks for an API key (typed field) and an editable model name.
- [ ] Pick **Ollama Local** → **no key required**; base URL defaults to `http://127.0.0.1:11434/v1`.
- [ ] **Custom** lets you enter provider name, base URL, model, and key (or none).
- [ ] After Save → the agent answers using the chosen provider + model.

## P0 · Must pass
- [ ] **Send a message** → a reply comes back on the selected provider.
- [ ] **Streaming**: text appears **incrementally** (chunks via `window.OFH.onChunk`), not all at once at the end.
- [ ] **onDone**: the bridge fires a terminal signal when streaming finishes; the UI shows a completed state.
- [ ] **Tool call executes**: ask something that uses `web_search` → a real tool call runs and its result feeds the answer.
- [ ] **Ollama Local / cleartext**: with a local Ollama running, **Ollama (local)** connects over cleartext `http://127.0.0.1:11434/v1` **after the cleartext-traffic fix** (no blocked `Cleartext HTTP not permitted`).
- [ ] **Config written**: after setup, `~/.ofh/config.yaml` + `.credentials.yaml` exist; the key is **not** stored in plaintext `config.yaml`.
- [ ] **Kill & relaunch** → app still works (no crash, no re-wizard on a configured install).

## P1 · Switcher, persistence & tool rendering
- [ ] **Provider/model switcher**: main screen → switch provider **and/or** model and base URL → Save → the next run uses the new selection (works anytime, not just first-run).
- [ ] **Persists across relaunch**: switch provider, kill the app, reopen → the switcher still shows the chosen provider/model.
- [ ] **Tool-call rendering** in chat: a tool call is rendered showing **name + args** (and, where practical, its **result**), not just swallowed into the reply text.
- [ ] **Wizard config persists**: complete the wizard, relaunch → wizard does **not** reappear and settings hold.

## P2 · Budget, memory, skills
- [ ] **Wall-clock run budget** caps runaway loops: a pathological/failing prompt stops after the budget elapses (no infinite spinner).
- [ ] **Memory tool** works: tell it something to remember, then reference it in a later session / later in the same session → it answers from memory.
- [ ] **Skills-as-markdown**: place a skill in a `SKILL.md` (as configured) → the agent loads and can use that skill.

## P3 · Nice-to-have
- [ ] **Personality presets** switch behavior/persona and persist.
- [ ] **Multimodal** (image) input is accepted where the provider supports it.
- [ ] **Budget/memory/skills** UI or config is surfaced somewhere in-app (not just in `~/.ofh/`).

---

## Report format
If something fails, note: **item** · **what you did** · **what you saw** · **expected**. That's enough to fix it.
