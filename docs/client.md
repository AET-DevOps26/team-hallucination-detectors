# Client

React + TypeScript single-page app — the user-facing UI for VibeShield. Users
register/log in, register a website, run a scan, watch it complete, browse findings
by severity, generate an AI fix prompt per finding, and download reports. It is
built with Vite and served in production as a static bundle behind Nginx.

> **Contract-typed.** API types are generated from `api/openapi.yaml` into
> `src/api/schema.ts` (`npm run gen:api`); the API modules derive their types from
> the generated `components["schemas"]`.

## Stack

| | |
|---|---|
| Framework | React 18.3, TypeScript 5.6 |
| Build | Vite 5.4 (`@vitejs/plugin-react`) |
| Styling | Tailwind CSS 3.4 (PostCSS + autoprefixer) |
| HTTP | axios 1.7 |
| Routing | hand-rolled on the History API (no router library) |
| State | React hooks + one context (`LlmProviderContext`); no Redux/Zustand |
| Tests | Vitest 4 + Testing Library (jsdom) |
| Prod serving | Nginx (`nginxinc/nginx-unprivileged:alpine`) |

## Running

Dev server and container both use **port 3000**. In production the app is a static
build served by Nginx (SPA fallback `try_files … /index.html`, 7-day cache on
static assets). In `docker-compose.yml` the client only `expose`s 3000 and is
reached through the gateway.

```bash
cd client
npm ci
npm run dev        # Vite dev server on http://localhost:3000
npm run build      # tsc -b && vite build → dist/
npm test           # vitest run
npm run gen:api    # regenerate src/api/schema.ts from ../api/openapi.yaml
```

The Docker image is a two-stage build: `node:24-alpine` builds the bundle (baking in
the `VITE_*` args), then `nginxinc/nginx-unprivileged:alpine` serves `dist/`.

## Configuration

`VITE_*` variables are read via `import.meta.env` and **baked in at build time**
(Docker `ARG`/`ENV`, passed from `docker-compose.yml` `client.build.args`):

| Variable | Purpose | Default |
|---|---|---|
| `VITE_API_BASE_URL` | api-service base URL | `""` (same-origin → through the gateway) |
| `VITE_AUTH_BASE_URL` | auth-service base URL | `/auth` |
| `VITE_DEV_AUTHENTICATED` | dev-only login bypass (`import.meta.env.DEV` + `"true"`) | unset |

There is no separate GenAI base URL — fix-prompt calls use the same `apiClient`
(empty base) against the gateway's `/langchain/*` path.

## Routes

Routing is hand-rolled (`hooks/useAppRouter.ts` + `utils/router.ts`, matched in
`App.tsx`). There is no route library and no route guards — when there is no session,
every route except `/reset-password` renders the login page.

| Path | Page | Notes |
|---|---|---|
| `/login` | `LoginPage` | login / register tabs + forgot-password; fallback when no session |
| `/reset-password` | `ResetPasswordPage` | public; reads `?token=` from the query |
| `/` | `LandingRedirect` | → `/profile` if signed in, else `/login` |
| `/profile` | `ProfilePage` | account info, team members (mock), site/analysis list |
| `/analysis` | `AnalysisListPage` | all analyses (scans) |
| `/analysis/new` | `NewAnalysisPage` | URL + checks + crawl depth + subdomains → trigger scan |
| `/analysis/:id` | `AnalysisDetailPage` | findings, severity summary, fix prompts, reports, rescan, comparison |

## API layer

Under `src/api/`, two axios instances are configured in `client.ts`: `apiClient`
(baseURL `VITE_API_BASE_URL`) and `authClient` (baseURL `VITE_AUTH_BASE_URL`), both
with a 5 s timeout and an `ApiError`/`normalizeError` wrapper.

| Module | Covers |
|---|---|
| `client.ts` | axios setup, JWT interceptor, and auth calls (`register`, `login`, `me`, `forgot-password`, `reset-password`, health `hello`) |
| `scans.ts` | websites, scans, findings, rescan, comparison |
| `reports.ts` | report data + HTML/PDF downloads (blob) |
| `genai.ts` | `generateFixPrompt` → `POST /langchain/fix-prompt` (30 s timeout) |
| `schema.ts` | generated contract types |

Contract responses are mapped to the UI's domain types in `utils/apiMapping.ts` —
note it maps API `explanation` → `summary` and `suggestedFix` → `impact`.

## Auth handling

- **Storage:** the session (`{ username, email?, token? }`) is JSON in
  `localStorage` under `vibeshield.session` (`utils/session.ts`).
- **Attaching:** the `apiClient` request interceptor reads the stored token and sets
  `Authorization: Bearer <token>` on every api-service call.
- **Protection:** no router guards — `App.tsx` conditionally renders the login page
  when there's no session. On mount, `useAuthState` validates the stored token via
  `GET /me` and clears the session on failure (skipped under the dev-auth bypass).

## Main flows

- **Register / login** (`useAuthState`): login persists the session and navigates to
  `/profile`; register then switches to login mode; forgot-password shows a message
  that email delivery isn't configured (the token is in the service logs).
- **Add website + scan** (`useAnalysisState.createAnalysis`): `ensureWebsite(url)`
  (reuses an existing site on `WEBSITE_ALREADY_REGISTERED`), then `triggerScan`
  (`202 Pending`), then navigates to `/analysis/{scanId}`.
- **Polling:** `getScan` is polled every 2.5 s while the status is `Pending` /
  `Running`; on `Completed` it loads the findings.
- **View findings** (`AnalysisDetailPage`): findings list, severity summary, report
  data, and scan comparison; reports download as HTML/PDF.
- **Fix prompts:** `generateFixPrompt(finding, builder, provider)` →
  `/langchain/fix-prompt`. The provider toggle (`useLlmProvider`) offers three
  backends — **TUM Logos** (default), **OpenAI**, and **Self-hosted (Ollama)** — and
  the choice is persisted in `localStorage` under `vibeshield.llmProvider` and sent
  with each request. See [langchain.md](langchain.md) for what each provider maps to.

## Source map

| Concern | File (under `client/src/`) |
|---|---|
| Entry / providers | `main.tsx` |
| Root + routing switch | `App.tsx`, `hooks/useAppRouter.ts`, `utils/router.ts` |
| Axios + auth API | `api/client.ts` |
| Scans / websites / findings API | `api/scans.ts` |
| Reports API | `api/reports.ts` |
| GenAI fix prompts | `api/genai.ts` |
| Generated contract types | `api/schema.ts` |
| Auth state | `hooks/useAuthState.ts` |
| Scan state + polling | `hooks/useAnalysisState.ts` |
| LLM provider context | `hooks/useLlmProvider.ts`, `hooks/LlmProviderProvider.tsx` |
| Session storage | `utils/session.ts`, `constants/session.ts` |
| Contract → UI mapping | `utils/apiMapping.ts` |
| Prod Nginx / container | `client/nginx.conf`, `client/Dockerfile` |

## Known gaps

- **Team management has no backend:** `useTeamState` is driven by
  `constants/mockData.ts`; invites are client-side only.
- **Finding triage is not persisted:** Open/Fixed/Ignored changes live in memory and
  reset on reload (`useAnalysisState.updateFinding`, cites tickets #22/#14).
- **Scan config isn't in the read model:** selected checks / crawl depth /
  subdomains are only remembered for scans created in the current session (an
  in-memory ref); otherwise they default to empty/0/false.
- **Password-reset email isn't wired up** — the UI tells users to check the service
  logs for the token (matches the [auth-service](auth.md) gap).
- **No route library or guards** — auth protection is a manual conditional in
  `App.tsx`.
- **Health badge pings `/api/v1/hello`**, the leftover scaffold endpoint on the
  api-service.
