# BharatCall Android ↔ backend API contract

Base URL must be HTTPS outside local development. All `/v1/*` requests require `Authorization: Bearer <server-issued-token>`; never bundle a long-lived server token in the APK. The current backend is a mock scaffold and does not make carrier calls.

## Readiness

`GET /v1/telephony/readiness`

A safe mock response is `200` with `state=CONFIGURED_NOT_CONNECTED`, `canPlaceCalls=false`, and `mediaConnected=false`. UI must not display “AI answered” or “active” from this response.

## Start session

`POST /v1/telephony/sessions`

```json
{
  "direction": "INCOMING",
  "agentId": "configured-agent-id",
  "disclosureText": "This is an AI assistant."
}
```

Direction is `INCOMING` or `OUTGOING`. Response `201` contains a session ID and `state=CONFIGURED_NOT_CONNECTED` in mock mode. This creates only an application session, not a telephone call.

## Session lifecycle

- `GET /v1/telephony/sessions/{sessionId}`
- `POST /v1/telephony/sessions/{sessionId}/handoff`
- `POST /v1/telephony/sessions/{sessionId}/end`

Mock handoff returns `409 HUMAN_HANDOFF_UNAVAILABLE`; this is intentional. Only a future provider adapter may report `ACTIVE`, and only after confirming provider call control and bidirectional media connectivity. All failure paths must preserve a clear human-answer/return-to-human path and must never silently intercept emergency calls.
