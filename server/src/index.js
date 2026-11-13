/**
 * ArthaReader API proxy.
 *
 * The Android app used to ship its OpenRouter and Deepgram keys inside the APK, where
 * anyone could extract them and spend on the owner's accounts without limit. The keys
 * now live only here, as Worker secrets. The app calls these three routes; each one is
 * shaped to exactly what the app needs, with size limits, so the proxy cannot be used as
 * a general-purpose gateway to either service:
 *
 *   POST /v1/chat    OpenRouter chat completion. Model and JSON mode are fixed here.
 *   POST /v1/speak   Deepgram text-to-speech (Aura voices only).
 *   POST /v1/listen  Deepgram speech-to-text on an MP3 the app just received.
 *
 * Abuse limits:
 *   - APP_TOKEN: requests must send it in X-App-Token. It ships in the app, so it can be
 *     extracted; it stops casual misuse and can be rotated without touching the paid keys.
 *   - X-Install-Id: a random id per install, rate limited per minute (see LIMITS). The
 *     counters live in this Worker instance's memory, so they are a best-effort brake,
 *     not a hard guarantee. Set spending limits on the OpenRouter key and the Deepgram
 *     project as the real ceiling.
 */

export const LIMITS = {
  chatMaxChars: 150_000,
  chatMaxMessages: 4,
  chatMaxOutputTokens: 32_000,
  speakMaxChars: 2_000,
  listenMaxBytes: 3_000_000,
  perMinute: { chat: 30, speak: 60, listen: 60 }
};

const OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
const DEEPGRAM_SPEAK_URL = "https://api.deepgram.com/v1/speak";
const DEEPGRAM_LISTEN_URL =
  "https://api.deepgram.com/v1/listen?model=nova-3&punctuate=true&smart_format=false&filler_words=false";
const DEFAULT_MODEL = "google/gemini-2.5-flash";
const VOICE_PATTERN = /^aura-[a-z0-9-]{1,48}$/;
const INSTALL_ID_PATTERN = /^[A-Za-z0-9-]{8,64}$/;

/** installId|route -> { windowStart, count }. Per Worker instance; see the header note. */
const counters = new Map();

export function resetRateLimits() {
  counters.clear();
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    try {
      if (request.method === "GET" && url.pathname === "/health") {
        return json({ ok: true });
      }

      const routes = { "/v1/chat": chat, "/v1/speak": speak, "/v1/listen": listen };
      const handler = routes[url.pathname];
      if (!handler) return error(404, "Not found.");
      if (request.method !== "POST") return error(405, "Use POST.");

      if (env.APP_TOKEN && request.headers.get("X-App-Token") !== env.APP_TOKEN) {
        return error(401, "Unknown app.");
      }
      const installId = request.headers.get("X-Install-Id") || "";
      if (!INSTALL_ID_PATTERN.test(installId)) return error(400, "Missing install id.");

      const route = url.pathname.slice("/v1/".length);
      const retryAfter = rateLimit(installId, route, LIMITS.perMinute[route], Date.now());
      if (retryAfter > 0) {
        return error(429, "Too many requests. Please wait a moment.", { "Retry-After": String(retryAfter) });
      }

      return await handler(request, env, url);
    } catch (err) {
      return error(500, "Proxy error.");
    }
  }
};

async function chat(request, env) {
  if (!env.OPENROUTER_API_KEY) return error(503, "The AI service is not configured.");
  const body = await readJson(request);
  if (!body) return error(400, "Body must be JSON.");

  const messages = body.messages;
  if (!Array.isArray(messages) || messages.length === 0 || messages.length > LIMITS.chatMaxMessages) {
    return error(400, "Invalid messages.");
  }
  let totalChars = 0;
  for (const message of messages) {
    if (!message || !["system", "user"].includes(message.role) || typeof message.content !== "string") {
      return error(400, "Invalid messages.");
    }
    totalChars += message.content.length;
  }
  if (totalChars > LIMITS.chatMaxChars) return error(413, "Text is too long.");

  const upstream = await fetch(OPENROUTER_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${env.OPENROUTER_API_KEY}`,
      "Content-Type": "application/json",
      "X-Title": "ArthaReader"
    },
    body: JSON.stringify({
      model: env.OPENROUTER_MODEL || DEFAULT_MODEL,
      messages: messages.map(({ role, content }) => ({ role, content })),
      response_format: { type: "json_object" },
      max_tokens: LIMITS.chatMaxOutputTokens
    })
  });
  return passThrough(upstream, "application/json");
}

async function speak(request, env, url) {
  if (!env.DEEPGRAM_API_KEY) return error(503, "Read-aloud is not configured.");
  const voice = url.searchParams.get("voice") || "aura-asteria-en";
  if (!VOICE_PATTERN.test(voice)) return error(400, "Unknown voice.");

  const body = await readJson(request);
  const text = body && typeof body.text === "string" ? body.text : "";
  if (!text.trim()) return error(400, "Missing text.");
  if (text.length > LIMITS.speakMaxChars) return error(413, "Text is too long.");

  const upstream = await fetch(`${DEEPGRAM_SPEAK_URL}?model=${voice}&encoding=mp3`, {
    method: "POST",
    headers: {
      Authorization: `Token ${env.DEEPGRAM_API_KEY}`,
      "Content-Type": "application/json",
      Accept: "audio/mpeg"
    },
    body: JSON.stringify({ text })
  });
  return passThrough(upstream, "audio/mpeg");
}

async function listen(request, env) {
  if (!env.DEEPGRAM_API_KEY) return error(503, "Read-aloud is not configured.");
  const declared = Number(request.headers.get("Content-Length") || "0");
  if (declared > LIMITS.listenMaxBytes) return error(413, "Audio is too long.");

  const audio = await request.arrayBuffer();
  if (audio.byteLength === 0) return error(400, "Missing audio.");
  if (audio.byteLength > LIMITS.listenMaxBytes) return error(413, "Audio is too long.");

  const upstream = await fetch(DEEPGRAM_LISTEN_URL, {
    method: "POST",
    headers: { Authorization: `Token ${env.DEEPGRAM_API_KEY}`, "Content-Type": "audio/mpeg" },
    body: audio
  });
  return passThrough(upstream, "application/json");
}

/**
 * Returns 0 when the request may go ahead, or the seconds to wait. Fixed one-minute
 * windows per install id and route.
 */
export function rateLimit(installId, route, limit, now) {
  const key = `${installId}|${route}`;
  const windowStart = Math.floor(now / 60_000) * 60_000;
  const entry = counters.get(key);
  if (!entry || entry.windowStart !== windowStart) {
    counters.set(key, { windowStart, count: 1 });
    if (counters.size > 50_000) pruneCounters(windowStart);
    return 0;
  }
  if (entry.count >= limit) return Math.ceil((windowStart + 60_000 - now) / 1000);
  entry.count += 1;
  return 0;
}

function pruneCounters(currentWindow) {
  for (const [key, entry] of counters) {
    if (entry.windowStart !== currentWindow) counters.delete(key);
  }
}

/** Relays the upstream status and body. 429/5xx keep their codes so the app's retry rules still apply. */
function passThrough(upstream, fallbackType) {
  const headers = new Headers({ "Content-Type": upstream.headers.get("Content-Type") || fallbackType });
  const retryAfter = upstream.headers.get("Retry-After");
  if (retryAfter) headers.set("Retry-After", retryAfter);
  // A rejected server key is the server's problem, not the app's: report it as a bad gateway.
  const status = upstream.status === 401 || upstream.status === 403 ? 502 : upstream.status;
  return new Response(upstream.body, { status, headers });
}

async function readJson(request) {
  try {
    return await request.json();
  } catch {
    return null;
  }
}

function json(value, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "Content-Type": "application/json", ...extraHeaders }
  });
}

function error(status, message, extraHeaders = {}) {
  return json({ error: message }, status, extraHeaders);
}
