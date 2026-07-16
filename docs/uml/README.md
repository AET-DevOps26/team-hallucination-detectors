# UML diagrams

Rendered architecture diagrams for VibeShield. Only the PNG renders are committed (no `.puml` sources).

Last verified against `main` on 2026-07-16.

Diagrams in this directory:

- `subsystems.png` — subsystem decomposition (component diagram)
- `usecase-diagram.png` — use case diagram
- `analysis-object-model.png` — analysis object model (class diagram)

The ERD lives one level up at `docs/db-schema.png` (embedded by the top-level README).

## Verification notes

All facts were cross-checked against the actual code: controllers, entities and Flyway migrations of api- and auth-service, the scanner check implementations, the langchain-service modules, `gateway/nginx.conf`, `k8s/ingress.yml`, `docker-compose.yml`, and the client's pages and API calls. Rendered with PlantUML 1.2025.4; every render was visually inspected for overlapping or unreadable elements.

### `subsystems.png`
- Schema-per-service on a single Postgres 16; api-service uses the `api_service` schema.
- Scanner shows its real 7 checks (https, headers, adminPaths, secrets, sensitiveFiles, cookies, cors) plus the depth-1 same-origin crawl.
- GenAI REST seam is JWT-protected and called directly by the browser via `/langchain` (no api-service → GenAI edge).
- Shows the cloud LLM APIs (OpenAI `gpt-4o-mini`, TUM Logos `gpt-oss-120b`) and the optional self-hosted Ollama (`llama3.2:3b`, compose profile `ollama`).
- Legend documents the shared `APP_JWT_SECRET` issue/validate split and the nginx-vs-k8s-Ingress routing differences (`/auth` alias and `/openapi.yaml` are nginx-only).

### `usecase-diagram.png`
- 10 implemented use cases, including Reset password, Rescan a site (`<<include>>` Run a scan) and Download report (PDF/HTML) (`<<extend>>` View findings).
- The local LLM path is no longer deferred: Ollama is part of the LLM-provider choice.

### `analysis-object-model.png`
- Shows only classes that exist in code; the GenAI service is plain Python modules (`app.main`, `app.chains`, `app.guardrails`, `app.auth`) with a per-request `Provider` literal (`openai | logos | selfhosted`).
- Includes `ScannerClient`, `JwtAuthFilter`, `PasswordResetService`, `PageDiscovery`, `SiteFetcher`, `SsrfGuard`, the 7 `SecurityCheck` implementations and the full 8-value `ScanCheck` enum.
- The SPA calls the GenAI service directly, so there is no `ScanWorker` → GenAI edge.

### `../db-schema.png` (ERD)
- Columns match the Flyway migrations; shows `ON DELETE CASCADE`, indexes, the enum-constant-name storage convention, the FK-less cross-schema `users`/`websites` ownership edge, and a note on the `in_flight_website_id` work-queue claim.
