# Contributing to OFH

Thanks for helping make the Open Forest Harness better. This project is provider-agnostic, local-first, and built to be easy to extend — your contributions make it stronger.

## How to help
- **Report bugs** — open an issue with the checklist from `docs/SMOKE-TEST.md` filled in where relevant.
- **Request features** — open a feature request; tell us the provider/UX/agent behaviour and why it matters.
- **Add a provider** — add a `Preset` in `ProviderConfig.kt` and confirm it works with the OpenAI-compatible client.
- **Add a tool** — register it in `BuiltinTools.kt` (name, description, JSON schema, handler).
- **Write a skill** — a `SKILL.md` + references that the agent can load.
- **Improve docs** — README, `docs/`, this repo. Docs are as important as code.

## Getting started
```bash
# requires JDK 17 + Android SDK (compileSdk 35)
./gradlew :app:assembleDebug      # build
./gradlew :app:testDebugUnitTest  # 3/3 integration tests must pass
./gradlew :app:lintDebug          # no new lint errors
```

## Before you submit
- Keep the build + tests green (`testDebugUnitTest` passing is the bar).
- Follow the existing code style (Kotlin, 4-space indent, KDoc on public APIs).
- Include a short description in your PR; link any related issue.
- **Never commit** `release.keystore` or any API key / secret.

## Commit message style
`area: short summary` — e.g. `provider: add Groq preset` or `agent: add run budget`.

Questions? Open an issue and tag it `question`.
