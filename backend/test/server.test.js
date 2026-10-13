import test from "node:test";
import assert from "node:assert/strict";
import { createApp } from "../src/server.js";

const TOKEN = "test-token-with-at-least-thirty-two-characters";
async function withServer(fn, options = {}) {
  const server = createApp({ token: TOKEN, mode: "mock", ...options });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  try { await fn(`http://127.0.0.1:${address.port}`); }
  finally { await new Promise((resolve, reject) => server.close(e => e ? reject(e) : resolve())); }
}
const auth = { authorization: `Bearer ${TOKEN}`, "content-type": "application/json" };

test("health is public, traceable, and never claims live telephony", async () => withServer(async base => {
  const res = await fetch(base + "/health");
  assert.equal(res.status, 200);
  assert.ok(res.headers.get("x-request-id"));
  const data = await res.json();
  assert.ok(data.requestId);
  assert.equal(data.telephonyConnected, false);
  assert.equal(data.mode, "mock");
  assert.equal(data.capabilities.carrierCalls, false);
  assert.equal(data.capabilities.liveAudio, false);
}));

test("telephony endpoints reject missing credentials", async () => withServer(async base => {
  const res = await fetch(base + "/v1/telephony/readiness");
  assert.equal(res.status, 401);
  assert.equal((await res.json()).error, "UNAUTHORIZED");
}));

test("readiness never claims mock mode can place calls", async () => withServer(async base => {
  const res = await fetch(base + "/v1/telephony/readiness", { headers: auth });
  assert.equal(res.status, 200);
  const data = await res.json();
  assert.equal(data.state, "CONFIGURED_NOT_CONNECTED");
  assert.equal(data.canPlaceCalls, false);
  assert.equal(data.mediaConnected, false);
  assert.equal(data.capabilities.aiSpeech, false);
}));

test("session creation validates direction and required disclosure", async () => withServer(async base => {
  let res = await fetch(base + "/v1/telephony/sessions", { method: "POST", headers: auth, body: JSON.stringify({ direction: "SIDEWAYS", agentId: "agent", disclosureText: "AI assistant" }) });
  assert.equal(res.status, 400);
  res = await fetch(base + "/v1/telephony/sessions", { method: "POST", headers: auth, body: JSON.stringify({ direction: "INCOMING", agentId: "agent", disclosureText: "" }) });
  assert.equal(res.status, 400);
}));

test("session is explicitly disconnected; handoff cannot fake a live call", async () => withServer(async base => {
  const res = await fetch(base + "/v1/telephony/sessions", { method: "POST", headers: auth, body: JSON.stringify({ direction: "OUTGOING", agentId: "demo-agent", disclosureText: "This is an AI assistant." }) });
  assert.equal(res.status, 201);
  const session = await res.json();
  assert.ok(session.requestId);
  assert.ok(session.expiresAt);
  assert.equal(session.state, "CONFIGURED_NOT_CONNECTED");
  assert.equal(session.canPlaceCalls, false);
  const read = await fetch(base + `/v1/telephony/sessions/${session.sessionId}`, { headers: auth });
  assert.equal(read.status, 200);
  assert.equal((await read.json()).sessionId, session.sessionId);
  const handoff = await fetch(base + `/v1/telephony/sessions/${session.sessionId}/handoff`, { method: "POST", headers: auth });
  assert.equal(handoff.status, 409);
  assert.equal((await handoff.json()).error, "HUMAN_HANDOFF_UNAVAILABLE");
  const end = await fetch(base + `/v1/telephony/sessions/${session.sessionId}/end`, { method: "POST", headers: auth });
  assert.equal(end.status, 200);
  assert.equal((await end.json()).state, "ENDED");
}));

test("failed authentication attempts are rate limited", async () => withServer(async base => {
  for (let i = 0; i < 20; i++) {
    const res = await fetch(base + "/v1/telephony/readiness");
    assert.equal(res.status, 401);
  }
  const limited = await fetch(base + "/v1/telephony/readiness");
  assert.equal(limited.status, 429);
  assert.equal(limited.headers.get("retry-after"), "60");
  assert.equal((await limited.json()).error, "AUTH_RATE_LIMITED");
}));

test("expired mock sessions cannot be read or handed off", async () => {
  let time = Date.parse("2026-01-01T00:00:00.000Z");
  await withServer(async base => {
    const res = await fetch(base + "/v1/telephony/sessions", { method: "POST", headers: auth, body: JSON.stringify({ direction: "OUTGOING", agentId: "demo-agent", disclosureText: "This is an AI assistant." }) });
    assert.equal(res.status, 201);
    const session = await res.json();
    time += 31 * 60 * 1000;
    const expired = await fetch(base + `/v1/telephony/sessions/${session.sessionId}`, { headers: auth });
    assert.equal(expired.status, 404);
    assert.equal((await expired.json()).error, "SESSION_NOT_FOUND_OR_EXPIRED");
  }, { now: () => time });
});
