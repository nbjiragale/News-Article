# ArthaReader

ArthaReader is an Android app for contextual English learning (with Kannada support).

You can:
- Paste article text and clean it into readable article format
- Import YouTube captions directly from a video link (no AI rewrite for captions)
- Tap words/sentences and get context-aware meaning (Kannada + simple English)
- Save words, review history, and practice with spaced repetition style stats
- Listen to content with TTS (Deepgram Aura when configured, Android TTS fallback)

---

## Tech Stack

- Kotlin
- Jetpack Compose
- Room (local persistence)
- OpenRouter API (article cleanup, summaries, meanings, phrase extraction)
- Deepgram API (optional high-quality English narration)

---

## Requirements

- Android Studio (latest stable recommended)
- Android SDK 36
- Min SDK 24
- JDK 11

---

## Setup

1. Clone the repo.
2. Open the project in Android Studio.
3. Add/update `local.properties` in project root:

```properties
openrouter.api.key=YOUR_OPENROUTER_KEY
openrouter.model=google/gemini-2.5-flash

deepgram.api.key=YOUR_DEEPGRAM_KEY
deepgram.voice=aura-asteria-en
deepgram.audio.offset.ms=0
```

Notes:
- `openrouter.api.key` is required for AI-powered article cleanup/meaning/summary flows.
- `deepgram.api.key` is optional. If missing, app falls back to Android system TTS.

---

## Run

From Android Studio:
- Run `app` on device/emulator.

From terminal (Windows PowerShell):

```powershell
.\gradlew.bat assembleDebug
```

---

## Tests

Run all local unit tests:

```powershell
.\gradlew.bat testDebugUnitTest --console=plain
```

Run YouTube live transcription test (real network call):

```powershell
$env:RUN_LIVE_YOUTUBE_TESTS='true'
.\gradlew.bat testDebugUnitTest --tests com.niranjan.englisharticle.YouTubeTranscriptServiceTest.fetchTranscript_liveVideo_producesTranscriptForProvidedLink --console=plain
```

The live test is skipped unless `RUN_LIVE_YOUTUBE_TESTS=true`.

---

## YouTube Transcription Notes

- The app uses a YouTube watch page + Innertube player flow to resolve caption tracks.
- Captions are imported directly as transcript text.
- Some networks/IPs may receive HTTP 429 from YouTube (temporary blocking/rate limit).
- Service logs use tag: `YouTubeTranscriptSvc` (check Logcat for step-by-step diagnostics).

---

## App Navigation

- Home: paste article / import YouTube / open reader
- Saved: saved words and meanings
- Practice: quiz/practice mode with saved words
- Recents: previously processed articles

---

## Project Structure

- `app/src/main/java/com/niranjan/englisharticle/ui` - Compose screens, view model, navigation
- `app/src/main/java/com/niranjan/englisharticle/data` - AI service, YouTube transcript service, local store
- `app/src/main/java/com/niranjan/englisharticle/data/local` - Room entities/DAO/database
- `app/src/main/java/com/niranjan/englisharticle/domain` - domain models and formatter utilities
- `app/src/test/java/com/niranjan/englisharticle` - unit tests

---

## Distribution

**Do not share, upload, or publish the APK.** Install it on your own devices only.

ArthaReader compiles its OpenRouter and Deepgram keys into the app as `BuildConfig`
string constants. Release builds run R8, which makes those constants harder to locate —
but obfuscation is not encryption. Anyone holding the APK can still recover both keys,
and there is no per-user quota, no rate limit, and no way to revoke access short of
rotating the key and rebuilding. Every request made with a leaked key is billed to you.

This is a deliberate, accepted trade-off for a single-user app, consistent with the PRD
("Target User: Single user", "Out of scope: publishing to Play Store"). It is the correct
call for personal use and the wrong call for anything else.

**If that ever changes** — if you want to hand the app to someone else, publish it, or
open the source with keys still wired this way — the keys must move behind a server you
control before you do:

```
Android app → your proxy (auth + quota + key custody) → OpenRouter / Deepgram
```

See §3.1 of `docs/PRODUCTION_READINESS_REVIEW.md` for a worked sketch. Treat this section
as the gate: distributing the current build is not a shortcut, it is handing out your
billing credentials.

### Spend limits

Because the keys are yours and the app has no server-side quota, set a hard ceiling at
the provider before you build:

- **OpenRouter** — set a credit or spend limit on the account, and prefer a scoped key
  used only by this app so it can be rotated without touching anything else.
- **Deepgram** — set a usage limit and billing alerts on the project.

The app applies one client-side guard of its own: pasted articles and imported
transcripts over 100k characters (~15,000 words) are rejected with a message rather than
sent. That bounds the cost of a single stray paste. It does not bound total spend — only
the provider-side limits do that.

Cost scales with article length and with how many words you tap: a word lookup currently
sends the whole article as context. Reducing that is tracked as H3 in the review.

---

## Security

- Do not commit real API keys.
- Keep secrets only in `local.properties` (already git-ignored).
- Release signing material (`keystore.properties`, `*.jks`, `*.keystore`) is git-ignored
  too. Committing a keystore hands out the ability to publish updates under this app's
  identity.

