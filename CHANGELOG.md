# Changelog

All notable changes to OFH (Open Forest Harness) are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/) and semantic versioning.

## [0.2.0] - 2026-08-24
### Added
- First-run **provider setup wizard** (`SetupWizardActivity`).
- Persistent **provider / model switcher** in the chat header.
- **Tool-call rendering** (🔧 cards with name + args + result).
- **120s wall-clock run budget** in `AgentLoop` (prevents runaway loops).
- **Memory** tool (`Memory.kt`) with long-term note storage & recall.
- **Skills** (`Skills.kt`) — load instruction sets from `SKILL.md`.
- Real `device_info` (was a placeholder).
- JVM integration test (`HarnessIntegrationTest`) — 3/3 passing.

### Fixed
- Streaming tokens now reach the UI (`window.OFH.onDelta` / `onDone`).
- Ollama Local connects over cleartext (`usesCleartextTraffic`).
- Custom / edited `baseURL` now persists and is read back.
- Release signing keystore path is absolute (no more module-dir mis-resolve).

### Security
- Signing keystore excluded from VCS; API keys stored on-device only.

## [0.1.0] - 2026-08-24
### Added
- Initial from-scratch harness skeleton: pure-Kotlin `AgentLoop`,
  OpenAI-compatible `ProviderAdapter` (streaming + tool calls), `ToolRegistry`,
  `BuiltinTools` (`web_search`/`web_fetch`), WebView chat UI, provider presets,
  DSH-style `~/.ofh/` config.
