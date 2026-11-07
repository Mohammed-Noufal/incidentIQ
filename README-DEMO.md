# IncidentIQ — Hackathon Demo

A lightweight incident investigation demo built on the existing Spring Boot backend. It recalls prior incident context from Hindsight, asks Groq GPT-OSS 20B to analyze the current incident, then attempts to retain the new case in Hindsight.

## Requirements
- Java 21
- Maven
- MySQL (use the existing `docker-compose.yml`)
- Existing environment variables: `GROQ_API_KEY`, `HINDSIGHT_API_KEY`
- Authentication environment variables: `AUTH_USERNAME`, `AUTH_PASSWORD`

Optional environment variables:
- `GROQ_MODEL` (default `openai/gpt-oss-20b`)
- `HINDSIGHT_BASE_URL` (default `https://api.hindsight.vectorize.io`)
- `HINDSIGHT_BANK_ID` (default `incident-iq-demo`)
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_TTL_SECONDS` (default `3600`; minimum `60`)

All API routes require a bearer token except `POST /api/auth/login`,
`POST /api/auth/register`, and `GET /api/health`. Users can create accounts from the
sign-in screen; registration returns a token and signs the new user in. User passwords
are stored as BCrypt hashes in the `user_account` table (created by Flyway migration V2).
Set `AUTH_USERNAME` and `AUTH_PASSWORD` to provision the bootstrap administrator account.
The demo JWT signing key is Base64-configured at `app.security.jwt.secret` in
`application.yml` and is validated to decode to at least 32 bytes. Replace it with a
securely managed secret before deploying beyond local demos. The bootstrap account
credentials remain required at startup.

The login endpoint accepts `{"username":"...","password":"..."}` and returns an
`accessToken`. Send it on API requests as `Authorization: Bearer <accessToken>`.
Registration uses the same JSON shape at `/api/auth/register`; usernames must be 3–50
letters, numbers, dots, underscores, or hyphens, and passwords must be at least 12
characters (up to 72 UTF-8 bytes).

## Run backend
Start MySQL using the existing compose file, then run:

```bash
mvn spring-boot:run
```

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

## Demo API
`POST http://localhost:8080/api/demo/investigate`

```json
{
  "serviceName": "Payment API",
  "title": "Elevated payment latency",
  "severity": "HIGH",
  "symptoms": "p95 latency increased and 502 responses are rising after deployment."
}
```

## Demo limitations
This is a demonstration build, not a production incident automation system. AI output is advisory and must be reviewed by a human. The demo endpoint does not execute remediation actions. Hindsight recall/retain request shapes and account permissions should be verified against the provisioned Hindsight workspace; external API behavior may require adjustment. The original `failureFamily` field remains seed/evaluation metadata and is not sent to either provider.
