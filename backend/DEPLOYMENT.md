# Deploy the mock API on Render Free

This is an optional deployment path for testing the API URL and Android health-check UI. It does not provide AI phone calls, SIP/PSTN, or live audio. Render's free service can sleep when idle; startup may be delayed.

**Phone-only Bengali walkthrough:** see [RENDER_PHONE_GUIDE_BN.md](./RENDER_PHONE_GUIDE_BN.md). No terminal commands are required.

## Deploy

1. Open the repository's [Render Blueprint](https://render.com/docs/blueprint-spec) workflow and connect `cryding/BharatCall`.
2. Choose branch `ai-telephony-bridge` for the initial test.
3. Review `render.yaml`. Keep the plan at `free`; do not upgrade or enable paid billing.
4. When prompted for `BHARATCALL_API_TOKEN`, generate a random secret with at least 32 characters and enter it as a private environment variable. Do not put the token in GitHub, a URL, screenshots, or the Android APK.
5. Deploy and wait until Render reports the service healthy.
6. Open `https://YOUR-SERVICE.onrender.com/health`. A valid response should include `\"ok\":true` and `\"telephonyConnected\":false`.
7. In BharatCall → Settings → AI telephony backend URL, save the HTTPS service URL and use Test telephony backend.

## Important security limitation

The current mock API's protected routes use a single bearer token. Do **not** embed that shared token in a publicly distributed Android APK; APK secrets can be extracted. The current Android UI only checks the public health endpoint and does not send a token. Before enabling protected session APIs from Android, replace the shared-token scheme with per-user authentication and short-lived server-issued tokens, rate limiting, and server-side provider credentials.

## Free-tier and billing safety

A free tier can change and may sleep or have usage limits. Select only free resources, do not add a payment method if not required, and do not approve upgrades. No telephony provider is configured by this blueprint.
