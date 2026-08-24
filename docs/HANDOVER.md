# OFH — Handover Doc (for the next build session)

> Read this first. It is the single source of truth for resuming OFH work.
> Companion docs: `README.md` (vision), `docs/ARCHITECTURE.md` (design), `docs/PLAN.md` (roadmap),
> `docs/SMOKE-TEST.md` (on-device checklist). The canonical briefing is `/root/OFH_HANDOVER.md`.

---

## What OFH is

**Open Forest Harness (OFH)** — a fully independent, from-scratch Android agent harness.
**It is NOT a wrapper around DeepSeek Harness (DSH).** It has its own pure-Kotlin agent loop,
its own OpenAI-compatible provider client, its own tool registry, its own WebView UI.

Pillars (from README): **independent · provider-agnostic · local-first · real-time ·
personality · extensible.**

---

## Current state (verified 2026-08-24 — re-audit passed)

- **`./gradlew :app:assembleRelease` → BUILD SUCCESSFUL from `clean`** (re-verified). Fresh signed
  release APK produced; `apksigner verify` passes (v2, CN=OpenForest), matches `release.keystore` cert.
- **Shipped:** `dist/OFH-v0.2.0.apk` + `/sdcard/Download/OFH-v0.2.0.apk` (freshly copied from the
  verified build). `dist/OFH-v0.1.0.apk` also present. Both pass `apksigner verify`.
- **`./gradlew :app:testDebugUnitTest` → 3/3 PASS** (real `ProviderAdapter`+`AgentLoop`+`ToolRegistry`
  against a mock OpenAI-compatible server: streaming tool-call round-trip, non-streaming complete(),
  unknown-tool error). Test: `app/src/test/java/com/ofh/harness/HarnessIntegrationTest.kt`.
- **OFH keystore:** `/root/OFH/release.keystore` (alias `ofh`, pass `ofhlauncher123`).
- Build-tools: `/root/Android/build-tools/35.0.0/`. `apksigner` works; **`zipalign` is x86-64 and won't
  run on this aarch64 proot — rely on Gradle's built-in zipalign, do NOT manual-zipalign.**
- The harness still has **never been run on a device against a live provider** — on-device smoke test is
  the only remaining gate (checklist: `docs/SMOKE-TEST.md`; Ollama Local is the zero-key option).

### Shipped this session (P0 + P1 + P2)
- **P0 fixed:** streaming `onDelta` + `onDone` reach the UI; `usesCleartextTraffic="true"` added;
  `baseURL` persisted/read via `ProviderConfig.readBaseUrl` + `setCurrent`.
- **P1 done:** first-run `SetupWizardActivity` (provider → key/model/baseURL → save); provider switcher
  in the chat header; tool-call rendering (`onTool` → 🔧 bubbles).
- **P2 done:** 120s wall-clock run budget in `AgentLoop`; `Memory.kt` (+ `memory` tool, recent-notes
  injection); `Skills.kt` (SKILL.md loader + `skills` tool); `device_info` returns real device data.
- **New files:** `SetupWizardActivity.kt`, `Memory.kt`, `Skills.kt`, `docs/SMOKE-TEST.md`,
  `app/src/test/java/com/ofh/harness/HarnessIntegrationTest.kt`.

---

## Build config (critical, do not change casually)
- **AGP 8.11.1 + Kotlin 2.2.20** (ARM aarch64 proot-compatible). compileSdk **35**, minSdk **24**,
  targetSdk **28** (lint `ExpiredTargetSdkVersion` disabled). Java/Kotlin target **17**.
  `android.builder.sdkDownload=false` in `gradle.properties`.
- **Release signing** in `app/build.gradle.kts`: env `OFH_KEYSTORE_FILE` (default
  **`rootProject.file("release.keystore")`** → `/root/OFH/release.keystore`), `OFH_KEYSTORE_PASS`
  (default `ofhlauncher123`), keyAlias `ofh`, keyPassword `ofhlauncher123`. **Works with NO env vars.**
  Falls back to debug signing if keystore missing.
- ⚠️ **Fixed:** keystore path is now **absolute** via `rootProject.file(...)`. AGP re-resolves relative
  `storeFile` paths against the `app/` module dir, so a relative `"release.keystore"` broke
  `assembleRelease` (looked in `/root/OFH/app/`). Do not revert to a relative path.

---

## Audit findings — ALL RESOLVED (from earlier full source review)
1. Streaming deltas didn't render (no `OFH.onDelta`; JS callback ignored). ✅ fixed in `index.html`.
2. Cleartext HTTP blocked for Ollama Local. ✅ `usesCleartextTraffic="true"` added.
3. Custom/edited `baseURL` wasn't persisted/read. ✅ `readBaseUrl`/`setCurrent` added.
Non-blocking notes: `AgentLoop` now has a 120s budget; `device_info` returns real data; `web_search`
backends return raw JSON (fine for grounding).

---

## Provider presets (final, in `ProviderConfig.kt`)
| Route | baseURL | default model | needsKey |
|---|---|---|---|
| ollamacloud | `https://ollama.com/v1` | `deepseek-v4-flash:0731` | yes |
| ollama | `http://127.0.0.1:11434/v1` | `qwen3:8b` | no |
| minimax | `https://api.minimaxi.com/v1` | `MiniMax-M2` | yes |
| zai | `https://api.z.ai/api/paas/v4` | `glm-4.7` | yes |
| qwen | `https://dashscope-intl.aliyuncs.com/compatible-mode/v1` | `qwen3-coder-plus` | yes |
| deepseek | `https://api.deepseek.com/v1` | `deepseek-chat` | yes |
| openai | `https://api.openai.com/v1` | `gpt-4.1` | yes |
| custom | user-entered baseURL | user-entered | yes |

Config mirrors DSH: `~/.ofh/` (on Android: `filesDir/.ofh/`), `.credentials.yaml`
(`refs: { KEY_ENV: "…" }`) and `settings.yaml` (providers + `agent-default-model`).

---

## Research folded into the design
- **Grok** → live web grounding (`web_search`/`web_fetch`), personality, tool-use-first, multimodal.
- **Hermes** → provider-profile registry, tool registry + toolsets, iteration + wall-clock budgets,
  memory-provider abstraction, fenced memory injection, skills-as-markdown, background curator/aux model.
- **DSH** → provider-adapter config, agent loop, credentials format, WebView UI pattern.

---

## Next steps
- ⏳ **On-device smoke test (the gate):** install `dist/OFH-v0.2.0.apk`, run `docs/SMOKE-TEST.md`
  against a live provider (Ollama Local first, zero-key). Fix anything surfaced.
- **P3 (optional):** personality presets, multimodal input, background curator/aux model, toolsets, MCP.
- **Polish:** summarize `web_search` raw JSON; tool-output size limits; confirm wizard UI on device.

---

## Gotchas
- **`write`/`edit` tools can leave dangling symlinks for new files** in this env. If a file vanishes or
  `edit` says "file not found", recreate it via a **`bash` heredoc** (reliable). This bit the HANDOVER
  docs themselves — rewrite via heredoc.
- **No image-capable model / no MINIMAX_API_KEY in-session** → can't visually verify the goat icon here.
- Each `bash` call is a fresh shell; Gradle wrapper + caches under `GRADLE_USER_HOME` persist, but the
  daemon doesn't — expect re-JIT per build.
- targetSdk 28 + minSdk 24 installs fine here but fails Play Store requirements (by design).
- `zipalign` in build-tools is x86-64 → not runnable here; rely on Gradle's built-in zipalign.
- Legacy `/root/dsh-mobile-s25` OpenForest (v2.3.0) is done except user on-device verification — don't
  touch it unless asked.
