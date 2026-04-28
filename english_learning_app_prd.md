# 📄 Product Requirements Document (PRD)

## 🧠 Product Name
Contextual English Learning App (Personal Use)

---

## 🎯 Goal

Build a simple Android app that helps **learn English vocabulary in real context** using news articles.

The app should:
- Show full article
- Allow user to tap any word
- Provide **contextual meaning in Kannada** (not dictionary meaning)

---

## 👤 Target User

- Single user (you)
- Kannada speaker learning English

---

## 🚀 Core Features (MVP)

### 1. Paste Article
- User can paste full article text
- Article is displayed in readable format

---

### 2. Word Tap Detection
- User taps on any word
- App detects:
  - tapped word
  - full sentence
  - surrounding context

---

### 3. Contextual Meaning (AI)

When a word is tapped:

App sends:
- full article text
- tapped sentence
- tapped word

AI returns:
- Kannada meaning (context-based)
- Simple English meaning
- Part of speech
- Explanation
- Example sentence

---

### 4. Display Meaning UI

Show result in:
- Bottom sheet OR dialog

Display:
- Word
- Kannada meaning
- English meaning
- Explanation
- Example

---

## 🏗️ Architecture (Simple)

```
Android App → OpenRouter API
```

No backend (personal use only)

---

## 🔌 API Details

### Endpoint
```
https://openrouter.ai/api/v1/chat/completions
```

### Headers
```
Authorization: Bearer YOUR_API_KEY
Content-Type: application/json
```

---

## 🧠 Prompt Design

```
You are an English teacher for Kannada speakers.

Article context:
{articleText}

Tapped sentence:
{sentence}

Tapped word:
{word}

Explain the meaning of the tapped word ONLY in this context.

Return JSON:
{
  "word": "",
  "meaningKannada": "",
  "simpleEnglish": "",
  "partOfSpeech": "",
  "explanationKannada": "",
  "exampleEnglish": "",
  "exampleKannada": ""
}
```

---

## 📱 UI Requirements

### Screen 1: Article Input
- Text field to paste article
- Button: "Load Article"

### Screen 2: Article Viewer
- Scrollable text
- Words clickable

### Interaction
- Tap word → show bottom sheet

---

## ⚙️ Technical Stack

- Language: Kotlin
- UI: Jetpack Compose
- Networking: Retrofit / OkHttp
- JSON: Gson / Kotlinx Serialization

---

## 🔐 API Key Handling

- Store in `local.properties`
- Do NOT hardcode in code
- Keys used:
  - `openrouter.api.key` — OpenRouter (article cleaning, summary, word meaning)
  - `openrouter.model` — optional override; defaults to `google/gemini-2.5-flash`
  - `deepgram.api.key` — Deepgram Aura TTS for high-quality English article narration. Without this key the app silently falls back to the on-device Android TTS.
  - `deepgram.voice` — optional voice override; defaults to `aura-asteria-en`
  - `newsapi.key` — News API key for browsing top headlines and searching articles. Get one free at https://newsapi.org/register. Without this key the "News" tab will show an error when fetching articles.

---

## ⚡ Performance Considerations

- Cache results:
  ```
  word + sentence → meaning
  ```

- Avoid repeated API calls

---

## 📦 Future Enhancements

- Save vocabulary list
- Word history
- Daily news fetch
- Quiz mode
- Pronunciation audio
- Sentence explanation

---

## ❌ Out of Scope (for now)

- User login
- Cloud sync
- Backend server
- Publishing to Play Store

---

## 🧪 Example Flow

1. User pastes article
2. App displays article
3. User taps word: "backed"
4. App sends request to OpenRouter
5. AI returns:
   - Kannada: ಬೆಂಬಲಿಸಿದರು
   - English: supported
6. App shows result in UI

---

## 🏁 Success Criteria

- Word tap works correctly
- AI returns contextual meaning
- Kannada meaning is accurate
- UI is responsive

---

## 🧠 Key Insight

> The app should answer:
> "What does this word mean HERE?"

Not:
> "What does this word mean generally?"

---

## 🚀 MVP Definition

✔ Paste article
✔ Tap word
✔ Show contextual Kannada meaning

That's it.

---

## 🔚 End of PRD
