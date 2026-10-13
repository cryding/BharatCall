import http from "node:http";
import { randomUUID } from "node:crypto";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

const SESSION_TTL_MS = 30 * 60 * 1000;
const MAX_SESSIONS = 500;
const MAX_AUTH_FAILURES = 20;
const AUTH_WINDOW_MS = 60 * 1000;

const json = (res, status, body, requestId) => {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff",
    "x-request-id": requestId
  });
  res.end(JSON.stringify({ requestId, ...body }));
};

async function readJson(req) {
  let raw = "";
  for await (const chunk of req) {
    raw += chunk;
    if (raw.length > 32_768) throw Object.assign(new Error("Request body too large"), { status: 413 });
  }
  if (!raw) return {};
  try { return JSON.parse(raw); }
  catch { throw Object.assign(new Error("Invalid JSON body"), { status: 400 }); }
}

export function createApp({
  token = process.env.BHARATCALL_API_TOKEN,
  mode = process.env.TELEPHONY_MODE || "mock",
  now = () => Date.now()
} = {}) {
  const sessions = new Map();
  const authFailures = new Map();

  function pruneSessions() {
    const cutoff = now() - SESSION_TTL_MS;
    for (const [id, session] of sessions) {
      if (session.state === "ENDED" || Date.parse(session.createdAt) < cutoff) sessions.delete(id);
    }
    while (sessions.size >= MAX_SESSIONS) {
      const oldestId = sessions.keys().next().value;
      if (!oldestId) break;
      sessions.delete(oldestId);
    }
  }

  function authLimited(ip) {
    const time = now();
    const record = authFailures.get(ip);
    if (!record || time - record.start >= AUTH_WINDOW_MS) {
      authFailures.set(ip, { start: time, count: 0 });
      return false;
    }
    return record.count >= MAX_AUTH_FAILURES;
  }

  const server = http.createServer(async (req, res) => {
    const requestId = randomUUID();
    const url = new URL(req.url || "/", "http://localhost");
    const ip = req.socket.remoteAddress || "unknown";

    if (req.method === "GET" && url.pathname === "/health") {
      return json(res, 200, {
        ok: true,
        service: "bharatcall-telephony-api",
        mode,
        telephonyConnected: false,
        capabilities: { carrierCalls: false, liveAudio: false, aiSpeech: false, humanHandoff: false }
      }, requestId);
    }

    if (!token || token.length < 32) {
      return json(res, 503, {
        error: "SERVER_NOT_CONFIGURED",
        message: "Set BHARATCALL_API_TOKEN to a random secret of at least 32 characters."
      }, requestId);
    }

    if (authLimited(ip)) {
      res.setHeader("retry-after", "60");
      return json(res, 429, { error: "AUTH_RATE_LIMITED", message: "Too many failed authentication attempts. Retry later." }, requestId);
    }
    if (req.headers.authorization !== `Bearer ${token}`) {
      const record = authFailures.get(ip) || { start: now(), count: 0 };
      record.count += 1;
      authFailures.set(ip, record);
      return json(res, 401, { error: "UNAUTHORIZED" }, requestId);
    }
    authFailures.delete(ip);

    try {
      if (req.method === "GET" && url.pathname === "/v1/telephony/readiness") {
        return json(res, 200, {
          state: "CONFIGURED_NOT_CONNECTED",
          mode,
          canPlaceCalls: false,
          mediaConnected: false,
          capabilities: { carrierCalls: false, liveAudio: false, aiSpeech: false, humanHandoff: false },
          message: "No telephony provider/media gateway is configured. No calls can be placed."
        }, requestId);
      }

      if (req.method === "POST" && url.pathname === "/v1/telephony/sessions") {
        const body = await readJson(req);
        if (!["INCOMING", "OUTGOING"].includes(body.direction)) {
          return json(res, 400, { error: "INVALID_DIRECTION", expected: ["INCOMING", "OUTGOING"] }, requestId);
        }
        if (typeof body.agentId !== "string" || !body.agentId.trim() || body.agentId.length > 128) {
          return json(res, 400, { error: "INVALID_AGENT_ID" }, requestId);
        }
        if (typeof body.disclosureText !== "string" || body.disclosureText.trim().length < 5 || body.disclosureText.length > 500) {
          return json(res, 400, { error: "DISCLOSURE_REQUIRED" }, requestId);
        }
        pruneSessions();
        const session = {
          sessionId: randomUUID(),
          direction: body.direction,
          agentId: body.agentId.trim(),
          state: "CONFIGURED_NOT_CONNECTED",
          statusMessage: "Mock mode only: provider call and audio media are not connected.",
          createdAt: new Date(now()).toISOString(),
          expiresAt: new Date(now() + SESSION_TTL_MS).toISOString(),
          canPlaceCalls: false,
          mediaConnected: false
        };
        sessions.set(session.sessionId, session);
        return json(res, 201, session, requestId);
      }

      const match = url.pathname.match(/^\/v1\/telephony\/sessions\/([0-9a-f-]+)(?:\/(handoff|end))?$/i);
      if (match) {
        const [, id, action] = match;
        const session = sessions.get(id);
        if (!session || (now() - Date.parse(session.createdAt) >= SESSION_TTL_MS)) {
          sessions.delete(id);
          return json(res, 404, { error: "SESSION_NOT_FOUND_OR_EXPIRED" }, requestId);
        }
        if (req.method === "GET" && !action) return json(res, 200, session, requestId);
        if (req.method === "POST" && action === "handoff") {
          if (session.state === "ENDED") return json(res, 409, { error: "SESSION_ENDED" }, requestId);
          return json(res, 409, {
            error: "HUMAN_HANDOFF_UNAVAILABLE",
            message: "No live carrier/provider call exists to transfer. No handoff was attempted.",
            session
          }, requestId);
        }
        if (req.method === "POST" && action === "end") {
          session.state = "ENDED";
          session.statusMessage = "Application session ended; no carrier call was placed.";
          session.endedAt = new Date(now()).toISOString();
          return json(res, 200, session, requestId);
        }
      }
      return json(res, 404, { error: "NOT_FOUND" }, requestId);
    } catch (error) {
      return json(res, error.status || 500, {
        error: error.status ? "BAD_REQUEST" : "INTERNAL_ERROR",
        message: error.status ? error.message : "Request could not be processed."
      }, requestId);
    }
  });
  return server;
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const port = Number(process.env.PORT || 8080);
  const server = createApp();
  server.listen(port, "0.0.0.0", () => {
    console.log(`BharatCall telephony API listening on port ${port}; mode=${process.env.TELEPHONY_MODE || "mock"}`);
    console.log("Mock mode does not place calls or connect audio.");
  });
}
