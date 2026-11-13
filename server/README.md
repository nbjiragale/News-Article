# ArthaReader API server

A small [Cloudflare Worker](https://developers.cloudflare.com/workers/) that holds the
OpenRouter and Deepgram keys. The Android app calls this server instead of calling
OpenRouter and Deepgram directly, so the keys are never inside the APK.

## What it allows

| Route | Forwards to | Limits |
|---|---|---|
| `POST /v1/chat` | OpenRouter chat completions | system/user messages only, at most 4; 150,000 characters total; model and JSON mode fixed by the server; at most 32,000 output tokens |
| `POST /v1/speak?voice=aura-…` | Deepgram text-to-speech | Aura voices only; 2,000 characters |
| `POST /v1/listen` | Deepgram speech-to-text | MP3 up to 3 MB |
| `GET /health` | — | — |

Every API call needs:

- `X-App-Token`: must equal the `APP_TOKEN` secret.
- `X-Install-Id`: a random id the app creates on first launch. Each install is limited
  per minute (30 chat, 60 speak, 60 listen requests).

## Deploy (one time)

You need a free Cloudflare account and Node.js.

```bash
cd server
npx wrangler login
npx wrangler secret put OPENROUTER_API_KEY   # paste a NEW OpenRouter key
npx wrangler secret put DEEPGRAM_API_KEY     # paste a NEW Deepgram key (optional)
npx wrangler secret put APP_TOKEN            # any long random string
npx wrangler deploy
```

`wrangler deploy` prints the server address (`https://arthareader-api.<you>.workers.dev`).
Put it and the same `APP_TOKEN` in the app's `local.properties`:

```properties
api.base.url=https://arthareader-api.<you>.workers.dev
api.app.token=<the APP_TOKEN value>
```

Change the model in `wrangler.toml` (`OPENROUTER_MODEL`) and run `npx wrangler deploy`
again. No app update is needed.

## Important: rotate the old keys

Every APK built before this change contains the OpenRouter and Deepgram keys in plain
text. **Revoke those keys** in the OpenRouter and Deepgram dashboards and create new ones
for this server.

## What this protects, and what it does not

- **Protected:** the paid keys. They exist only as Worker secrets.
- **Limited:** misuse of the server itself. The app token ships in the app, so a
  determined person can extract it and call these routes. They can only do what the
  app does (fixed model, size limits), at the per-install rate limit. If the token
  leaks, set a new `APP_TOKEN` and ship an app update.
- **Not guaranteed:** the rate-limit counters live in each Worker instance's memory,
  so they are a brake, not a hard cap, and someone can invent new install ids.
  **Set a spending limit** on the OpenRouter key and a budget on the Deepgram project:
  those are the real ceiling. Stronger protection (checking that requests come from your
  genuine app) needs Google Play Integrity, which this server does not do yet.

## Test

```bash
cd server
npm test
```
