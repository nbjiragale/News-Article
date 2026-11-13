import { test, beforeEach } from "node:test";
import assert from "node:assert/strict";
import worker, { LIMITS, resetRateLimits } from "../src/index.js";

const env = {
  OPENROUTER_API_KEY: "or-secret",
  DEEPGRAM_API_KEY: "dg-secret",
  APP_TOKEN: "app-token",
  OPENROUTER_MODEL: "google/gemini-2.5-flash"
};
const INSTALL = "3f6c1d2e-1111-4a2b-9c3d-abcdefabcdef";

let upstreamCalls;
beforeEach(() => {
  resetRateLimits();
  upstreamCalls = [];
  globalThis.fetch = async (url, init) => {
    upstreamCalls.push({ url: String(url), init });
    if (String(url).includes("openrouter.ai")) {
      return new Response(JSON.stringify({ choices: [{ message: { content: "{}" } }] }), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      });
    }
    if (String(url).includes("/v1/speak")) {
      return new Response(new Uint8Array([1, 2, 3]), { status: 200, headers: { "Content-Type": "audio/mpeg" } });
    }
    return new Response(JSON.stringify({ results: {} }), { status: 200, headers: { "Content-Type": "application/json" } });
  };
});

function call(path, { method = "POST", body, headers = {}, token = "app-token", install = INSTALL } = {}) {
  const h = new Headers(headers);
  if (token !== null) h.set("X-App-Token", token);
  if (install !== null) h.set("X-Install-Id", install);
  return worker.fetch(new Request(`https://proxy.example${path}`, { method, body, headers: h }), env);
}

const chatBody = (content = "Hello") =>
  JSON.stringify({ messages: [{ role: "system", content: "Return JSON." }, { role: "user", content }] });

test("chat forwards with the server key and the server's model", async () => {
  const res = await call("/v1/chat", { body: chatBody() });
  assert.equal(res.status, 200);
  assert.equal(upstreamCalls.length, 1);
  const sent = JSON.parse(upstreamCalls[0].init.body);
  assert.equal(upstreamCalls[0].init.headers.Authorization, "Bearer or-secret");
  assert.equal(sent.model, "google/gemini-2.5-flash");
  assert.deepEqual(sent.response_format, { type: "json_object" });
});

test("the app cannot choose the model or add other fields", async () => {
  const body = JSON.stringify({
    model: "expensive/model",
    messages: [{ role: "user", content: "Hi", extra: "x" }],
    max_tokens: 999999
  });
  await call("/v1/chat", { body });
  const sent = JSON.parse(upstreamCalls[0].init.body);
  assert.equal(sent.model, "google/gemini-2.5-flash");
  assert.equal(sent.max_tokens, LIMITS.chatMaxOutputTokens);
  assert.deepEqual(sent.messages, [{ role: "user", content: "Hi" }]);
});

test("wrong or missing app token is rejected before any paid call", async () => {
  assert.equal((await call("/v1/chat", { body: chatBody(), token: "wrong" })).status, 401);
  assert.equal((await call("/v1/chat", { body: chatBody(), token: null })).status, 401);
  assert.equal(upstreamCalls.length, 0);
});

test("missing install id is rejected", async () => {
  assert.equal((await call("/v1/chat", { body: chatBody(), install: null })).status, 400);
  assert.equal(upstreamCalls.length, 0);
});

test("oversized and malformed chat requests are rejected", async () => {
  assert.equal((await call("/v1/chat", { body: chatBody("x".repeat(LIMITS.chatMaxChars + 1)) })).status, 413);
  assert.equal((await call("/v1/chat", { body: "not json" })).status, 400);
  const assistant = JSON.stringify({ messages: [{ role: "assistant", content: "x" }] });
  assert.equal((await call("/v1/chat", { body: assistant })).status, 400);
  assert.equal(upstreamCalls.length, 0);
});

test("speak checks the voice and length, and adds the Deepgram key", async () => {
  const ok = await call("/v1/speak?voice=aura-asteria-en", { body: JSON.stringify({ text: "Hello there." }) });
  assert.equal(ok.status, 200);
  assert.equal(ok.headers.get("Content-Type"), "audio/mpeg");
  assert.equal(upstreamCalls[0].init.headers.Authorization, "Token dg-secret");
  assert.match(upstreamCalls[0].url, /model=aura-asteria-en&encoding=mp3/);

  assert.equal((await call("/v1/speak?voice=nova-3", { body: JSON.stringify({ text: "x" }) })).status, 400);
  const long = JSON.stringify({ text: "x".repeat(LIMITS.speakMaxChars + 1) });
  assert.equal((await call("/v1/speak?voice=aura-asteria-en", { body: long })).status, 413);
  assert.equal(upstreamCalls.length, 1);
});

test("listen forwards audio and rejects oversize audio", async () => {
  const ok = await call("/v1/listen", { body: new Uint8Array([1, 2, 3]), headers: { "Content-Type": "audio/mpeg" } });
  assert.equal(ok.status, 200);
  assert.match(upstreamCalls[0].url, /model=nova-3/);
  const big = new Uint8Array(LIMITS.listenMaxBytes + 1);
  assert.equal((await call("/v1/listen", { body: big, headers: { "Content-Type": "audio/mpeg" } })).status, 413);
});

test("each install is rate limited per minute", async () => {
  for (let i = 0; i < LIMITS.perMinute.chat; i++) {
    assert.equal((await call("/v1/chat", { body: chatBody() })).status, 200);
  }
  const limited = await call("/v1/chat", { body: chatBody() });
  assert.equal(limited.status, 429);
  assert.ok(Number(limited.headers.get("Retry-After")) > 0);
  // Another install is unaffected.
  const other = await call("/v1/chat", { body: chatBody(), install: "another-install-id-123" });
  assert.equal(other.status, 200);
});

test("upstream 429 passes through; a rejected server key becomes 502", async () => {
  globalThis.fetch = async () => new Response("{}", { status: 429, headers: { "Retry-After": "5" } });
  const busy = await call("/v1/chat", { body: chatBody() });
  assert.equal(busy.status, 429);
  assert.equal(busy.headers.get("Retry-After"), "5");

  globalThis.fetch = async () => new Response("{}", { status: 401 });
  assert.equal((await call("/v1/chat", { body: chatBody() })).status, 502);
});

test("unknown routes and GET on API routes are refused; health is open", async () => {
  assert.equal((await call("/v1/other", { body: "{}" })).status, 404);
  assert.equal((await call("/v1/chat", { method: "GET" })).status, 405);
  assert.equal((await worker.fetch(new Request("https://proxy.example/health"), env)).status, 200);
});
