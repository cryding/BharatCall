# BharatCall AI telephony API (scaffold)

This is a dependency-free Node.js API scaffold for testing the Android-to-backend contract. **It is not a carrier/SIP integration.** It does not dial, answer, record, stream audio, invoke Gemini, or transfer a real call. Its mock responses deliberately never report an active media session.

## Run locally

Requires Node.js 20+.

1. Set `BHARATCALL_API_TOKEN` to a private random secret of at least 32 characters.
2. Optionally set `PORT=8080` and `TELEPHONY_MODE=mock`.
3. Run `npm test`, then `npm start`.

Do not expose this plain-HTTP development server directly to the internet. Deploy only behind HTTPS/TLS termination and add production-grade secret management, per-user authorization, rate limits, audit logging, and operational monitoring before real use.

## Endpoints

- `GET /health` — public health check; reports `telephonyConnected: false` and explicit false capabilities for carrier calls, live audio, AI speech, and handoff.
- `GET /v1/telephony/readiness` — bearer-token protected readiness; explicitly returns `canPlaceCalls: false`.
- `POST /v1/telephony/sessions` — validates direction, agent ID and AI disclosure, then creates a disconnected mock session with a 30-minute expiry.
- `GET /v1/telephony/sessions/:id` — read session state until expiry.
- `POST /v1/telephony/sessions/:id/handoff` — returns 409 because no live provider call exists.
- `POST /v1/telephony/sessions/:id/end` — ends the local mock session.

Send `Authorization: Bearer <BHARATCALL_API_TOKEN>` for all `/v1/*` endpoints. Responses include a unique `requestId` and `X-Request-Id` header for debugging. Repeated failed authentication attempts from the same IP are rate-limited. These in-memory safeguards are basic development protections, not a substitute for a production API gateway or distributed rate limiter.

## Security limitations

The shared-token scheme is suitable only for controlled testing. Do **not** embed the shared token in a publicly distributed Android APK; APK secrets can be extracted. The current Android UI only checks the public health endpoint and does not send a token. Before enabling protected session APIs from Android, replace the shared-token scheme with per-user authentication and short-lived server-issued tokens, authorization scoped to each user, persistent session storage, and production-grade abuse controls.

## Still required for actual AI calling

A selected SIP/PSTN provider and credentials, inbound webhook/signature verification, verified outbound caller ID, a media gateway that genuinely bridges provider audio to a real-time speech/AI pipeline, consent/disclosure controls, DTMF and interruption handling, handoff routing, abuse controls, and end-to-end tests with an authorized test number. Public carrier calls can incur charges. Do not switch on a provider or paid billing without explicit approval. The current backend must not be represented as a working phone agent.
