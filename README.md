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

## Security

- Do not commit real API keys.
- Keep secrets only in `local.properties` (already git-ignored in typical Android setups).

