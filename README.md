# VibeShield

Security scanning for sites built with AI — findings come with ready-to-paste fix prompts for your AI builder.

**Live app:** https://ge65poj.stud.k8s.aet.cit.tum.de  
**Grafana dashboard:** https://ge65poj-monitoring.stud.k8s.aet.cit.tum.de  
**API docs:** https://ge65poj.stud.k8s.aet.cit.tum.de/api/swagger-ui.html

---

## What it does

Non-technical "vibecoders" ship live sites with AI tools (Lovable, Cursor, v0, Bolt, Replit) that routinely leak API keys, skip HTTPS, expose admin pages, or leave Supabase tables without RLS — and have no way to tell. VibeShield runs a surface-level security scan, shows findings grouped by severity, and generates a ready-to-paste fix prompt per finding so the user can repair the site by asking the same AI that built it.

---

## Architecture

```
Browser → Nginx gateway → React client (Vite + TypeScript)
                        → api-service    (Spring Boot 3, Java 21)  → PostgreSQL 16
                        → auth-service   (Spring Boot 3, Java 21)  → PostgreSQL 16
                        → langchain-service (Python + FastAPI + LangChain) → OpenAI / local LLM
                        ↑ api-service calls scanner-service internally
                        → scanner-service (Spring Boot 3, Java 21)
```

| Service | Tech | Port | Responsibility |
|---|---|---|---|
| [client](docs/client.md) | React + Vite + TypeScript, Nginx | 3000 | User-facing UI |
| [api-service](docs/api.md) | Spring Boot 3, Java 21 | 8080 | REST API, scan orchestration, JWT validation |
| [auth-service](docs/auth.md) | Spring Boot 3, Java 21 | 8080 | Registration, login, password reset, JWT issuance |
| [scanner-service](docs/scanner.md) | Spring Boot 3, Java 21 | 8080 | Security checks (internal-only) |
| [langchain-service](docs/langchain.md) | Python 3.12, FastAPI, LangChain | 8000 | AI fix-prompt generation (OpenAI cloud + self-hosted Ollama local model + TUM Logos) |
| [gateway](docs/gateway.md) | Nginx | 80 / 443 | Single-origin reverse proxy in front of all services |
| database | PostgreSQL 16 | 5432 | Persistent storage (schema-per-service isolation) |

> Per-service reference docs live in [`docs/`](docs/): [client](docs/client.md) ·
> [api-service](docs/api.md) · [auth-service](docs/auth.md) ·
> [scanner-service](docs/scanner.md) · [langchain-service](docs/langchain.md) ·
> [gateway](docs/gateway.md).

---

## Local setup

**Prerequisites:** Docker + Docker Compose

```bash
cp .env.example .env          # fill in OPENAI_API_KEY (and optionally APP_JWT_SECRET)
docker compose up --build     # starts all services + gateway on http://localhost:3000
```

The gateway exposes everything on port `3000`. API docs are at `http://localhost:3000/api/swagger-ui.html`.

To include the monitoring stack (Prometheus, Grafana, Loki):

```bash
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up --build
```

Grafana runs at `http://localhost:3001` (admin / admin for local dev).

---

## Database schema

Two logical schemas in one PostgreSQL instance. Full ERD: [`docs/db-schema.png`](docs/db-schema.png)

![ERD](docs/db-schema.png)

**`auth_service` schema**
| Table | Key columns |
|---|---|
| `users` | `id`, `email`, `password` (bcrypt) |
| `password_reset_tokens` | `id`, `user_id`, `token`, `expires_at`, `used_at` |

**`public` schema (api-service)**
| Table | Key columns |
|---|---|
| `websites` | `id`, `owner_id` (JWT sub), `url`, `name`, `created_at` |
| `scans` | `id`, `website_id`, `status`, `requested_checks`, `crawl_depth`, `started_at`, `completed_at` |
| `findings` | `id`, `scan_id`, `check_type`, `title`, `severity`, `affected`, `explanation`, `suggested_fix`, `status` |

Schema is managed by Flyway migrations in each service (`services/*/src/main/resources/db/migration/`).

---

## CI/CD

**CI** runs on every PR targeting `main` (`.github/workflows/ci.yml`):
- OpenAPI spec lint (Redocly) + drift check (generated code matches spec)
- Build + test: api-service, auth-service, scanner-service (Gradle), langchain-service (pytest), client (Vitest)

**CD** runs on merge to `main` (`.github/workflows/cd.yml`):
1. Builds and pushes all service images to GHCR tagged `sha-<commit>` + `latest` (semver tags added automatically when a `v*` git tag is pushed)
2. Deploys to Kubernetes via Helm (`helm upgrade --install vibeshield ./helm/vibeshield`)
3. Deploys monitoring stack (`kubectl apply -f k8s/monitoring/`)

Required GitHub Actions secrets: `KUBECONFIG_AET`, `POSTGRES_PASSWORD`, `APP_JWT_SECRET`, `OPENAI_API_KEY`.

---

## Monitoring

Deployed to the `ge65poj` namespace alongside the app.

- **Prometheus** — scrapes `/actuator/prometheus` (Spring Boot) and `/metrics` (FastAPI) from all services
- **Grafana** — live dashboard at https://ge65poj-monitoring.stud.k8s.aet.cit.tum.de (provisioned from `k8s/monitoring/grafana-dashboard-configmap.yml`)
- **Loki + Promtail** — log aggregation from all pods
- **Alert rules** (`k8s/monitoring/prometheus-configmap.yml`): ServiceDown (1 min), HighErrorRate (>5% 5xx), SlowResponseTime (P95 > 2s)

---

## Running tests

```bash
# Spring Boot services
cd services/api-service && ./gradlew test
cd services/auth-service && ./gradlew test
cd services/scanner-service && ./gradlew test

# Python GenAI service
cd services/langchain-service && pip install -r requirements.txt && pytest

# React client
cd client && npm ci && npm test
```

All tests run automatically in CI on every PR.

---

## Kubernetes deployment

The app is deployed to the TUM course cluster (Rancher) under namespace `ge65poj`.

```bash
# Deploy app
helm upgrade --install vibeshield ./helm/vibeshield -n ge65poj \
  --set secrets.jwtSecret=<secret> \
  --set secrets.dbPassword=<password> \
  --set secrets.openaiApiKey=<key>

# Deploy monitoring
kubectl apply -f k8s/monitoring/ -n ge65poj
```

Helm charts are in `helm/vibeshield/`. Kubernetes manifests for monitoring are in `k8s/monitoring/`.

---

## Per-student responsibilities

| Student | Area |
|---|---|
| Aziz Chouria | *(TBD)* |
| Julian Jungnitz | *(TBD)* |
| Tim Dreher | *(TBD)* |

> Fill in areas before the July 17th deadline — per-student responsibilities are an automatic-fail-adjacent deliverable per the course brief.