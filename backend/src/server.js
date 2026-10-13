import http from "node:http";
import { randomUUID } from "node:crypto";

const json = (res, status, body) => {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff"
  });
  res.end(JSON.stringify(body));
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

export function createApp({ token = process.env.BHARATCALL_API_TOKEN, mode = process.env.TELEPHONY_MODE || "mock" } = {}) {
  const sessions = new Map();
  const server = http.createServer(async (req, res) => {
    const url = new URL(req.url || "/", "http://localhost");
    if (req.method === "GET" && url.pathname === "/health") {
      return json(res, 200, { ok: true, service: "bharatcall-telephony-api", mode, telephonyConnected: false });
    }

    if (!token || token.length < 32) {
      return json(res, 503, { error: "SERVER_NOT_CONFIGURED", message: "Set BHARATCALL_API_TOKEN to a random secret of at least 32 characters." });
    }
    if (req.headers.authorization !== `Bearer ${token}`) {
      return json(res, 401, { error: "UNAUTHORIZED" });
    }

    try {
      if (req.method === "GET" && url.pathname === "/v1/telephony/readiness") {
        return json(res, 200, {
          state: "CONFIGURED_NOT_CONNECTED",
          mode,
          canPlaceCalls: false,
          mediaConnected: false,
          message: "No telephony provider/media gateway is configured. No calls can be placed."
        });
      }

      if (req.method === "POST" && url.pathname === "/v1/telephony/sessions") {
        const body = await readJson(req);
        if (!["INCOMING", "OUTGOING"].includes(body.direction)) {
          return json(res, 400, { error: "INVALID_DIRECTION", expected: ["INCOMING", "OUTGOING"] });
        }
        if (typeof body.agentId !== "string" || !body.agentId.trim() || body.agentId.length > 128) {
          return json(res, 400, { error: "INVALID_AGENT_ID" });
        }
        if (typeof body.disclosureText !== "string" || body.disclosureText.trim().length < 5 || body.disclosureText.length > 500) {
          return json(res, 400, { error: "DISCLOSURE_REQUIRED" });
        }
        const session = {
          sessionId: randomUUID(),
          direction: body.direction,
          agentId: body.agentId.trim(),
          state: "CONFIGURED_NOT_CONNECTED",
          statusMessage: "Mock mode only: provider call and audio media are not connected.",
          createdAt: new Date().toISOString()
        };
        sessions.set(session.sessionId, session);
        return json(res, 201, { ...session, canPlaceCalls: false, mediaConnected: false });
      }

      const match = url.pathname.match(/^\/v1\/telephony\/sessions\/([0-9a-f-]+)(?:\/(handoff|end))?$/i);
      if (match) {
        const [, id, action] = match;
        const session = sessions.get(id);
        if (!session) return json(res, 404, { error: "SESSION_NOT_FOUND" });
        if (req.method === "GET" && !action) return json(res, 200, session);
        if (req.method === "POST" && action === "handoff") {
          if (session.state === "ENDED") return json(res, 409, { error: "SESSION_ENDED" });
          return json(res, 409, {
            error: "HUMAN_HANDOFF_UNAVAILABLE",
            message: "No live carrier/provider call exists to transfer. No handoff was attempted.",
            session
          });
        }
        if (req.method === "POST" && action === "end") {
          session.state = "ENDED";
          session.statusMessage = "Application session ended; no carrier call was placed.";
          return json(res, 200, session);
        }
      }
      return json(res, 404, { error: "NOT_FOUND" });
    } catch (error) {
      return json(res, error.status || 500, { error: error.status ? "BAD_REQUEST" : "INTERNAL_ERROR", message: error.status ? error.message : "Request could not be processed." });
    }
  });
  return server;
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const port = Number(process.env.PORT || 8080);
  const server = createApp();
  server.listen(port, "0.0.0.0", () => {
    console.log(`BharatCall telephony API listening on port ${port}; mode=${process.env.TELEPHONY_MODE || "mock"}`);
    console.log("Mock mode does not place calls or connect audio.");
  });
}
