# Security Policy

OFH is an **on-device, provider-agnostic** agent harness. Your API keys and conversations live on your phone — we want to keep it that way.

## Supported versions
| Version | Supported |
|---|---|
| latest (v0.2.x) | ✅ |
| earlier | ❌ |

## Reporting a vulnerability
**Do not** open a public issue for security problems.
Email us, or if you're comfortable, open a private report:
- Create a **security advisory** on the repo: `Security` tab → "Report a vulnerability".

Please include:
- Affected file/version and a short description.
- Steps to reproduce (if any).
- Impact assessment.

We aim to acknowledge within 48h and to patch in the nearest release.

## What we take seriously
- **Cleartext / network security:** requests go directly to the provider you pick; no third-party middle server. Local-first = Ollama Local never leaves your network.
- **Secret handling:** API keys are stored in `~/.ofh/.credentials.yaml` — never baked into the APK, never uploaded. We review PRs to ensure `release.keystore` and keys are never committed.
- **Dependencies:** kept current; CI runs lint and we plan CodeQL/dependency scanning.

## A note on local model safety
If you use **Ollama Local**, the model runs entirely on your device/network. Review any model you choose to run — the agent can invoke tools you allow it to use.
