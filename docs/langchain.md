# GenAI Service (langchain-service)

Python + FastAPI microservice that turns a security finding into a ready-to-paste
fix prompt for the user's AI website builder. It also exposes a general chat
endpoint. It talks to OpenAI-compatible LLM backends through LangChain, and supports
three selectable providers: **OpenAI** (cloud), **TUM Logos** (the course-provided
gateway), and **self-hosted** (a local Ollama model we run ourselves).

> **Cloud + local model.** The course requires the GenAI service to support both a
> cloud model and a local model. OpenAI is the cloud path; the `selfhosted` provider
> is the local path — an Ollama runtime (`llama3.2:3b`) deployed alongside the
> service, reached in-cluster over its OpenAI-compatible API. TUM Logos is an
> additional third option.

> **The client calls this service directly** through the gateway at `/langchain/*` —
> it is *not* proxied by the [api-service](api.md). The api-service only stores the
> plain-language `suggestedFix` text that the client sends here as input.

> **Stateless.** The service has no database. Its user-facing endpoints (`/chat`,
> `/fix-prompt`) require a valid JWT — the same shared-secret HMAC token the
> auth-service issues and the Java services validate (see [auth.md](auth.md)) —
> so the GenAI capability isn't open to the internet even though it's reachable
> through the gateway. `/health` and `/metrics` stay unauthenticated for probes
> and Prometheus scraping.

## Stack

| | |
|---|---|
| Runtime | Python 3.12 |
| Framework | FastAPI 0.115.6, served by uvicorn 0.32.1 |
| LLM | LangChain 0.3.14 + `langchain-openai` 0.2.14 (`ChatOpenAI`) |
| Config | `pydantic-settings` `BaseSettings` (reads env / `.env`) |
| Metrics | `prometheus-fastapi-instrumentator` 7.0.0 → `/metrics` |

Dependencies are pinned in `requirements.txt` (there is no `pyproject.toml`). Both
providers are driven through `langchain_openai.ChatOpenAI`; they differ only by API
key, base URL, and model name.

## Running

The service listens on **container port 8000** with **no published host port**. The
gateway routes `/langchain/*` → `langchain-service:8000`, stripping the `/langchain`
prefix. So the client calls e.g. `POST /langchain/fix-prompt`, which arrives at the
service as `POST /fix-prompt`.

```bash
# whole stack
docker compose up --build

# genai service only
docker compose up --build langchain-service
```

Provider API keys are optional at startup — a provider selected without its key
returns a clean `503 PROVIDER_NOT_CONFIGURED` rather than failing to boot.

## Endpoints

All endpoints are on the root app (no routers). `/chat` and `/fix-prompt` require a
valid `Authorization: Bearer <jwt>` header; `/health` and `/metrics` are public.

| Method | Path | Body | Success |
|---|---|---|---|
| `GET`  | `/health` | — | `200` `{ status: "ok", service }` |
| `POST` | `/chat` | `ChatRequest` | `200` `{ response }` |
| `POST` | `/fix-prompt` | `FixPromptRequest` | `200` `{ prompt }` |
| `GET`  | `/metrics` | — | Prometheus exposition (text) |

**`FixPromptRequest`**: `title` and `severity` (required), plus optional `check`,
`affected`, `summary`, `impact`, `builder` (default `Generic`), and `provider`
(default `openai`). Empty optional fields are defaulted to `"n/a"` before the model
is invoked.

**`ChatRequest`**: `message` (required), `provider` (default `openai`).

Errors use the unified schema `{ code, message, details }` — the same shape as the
Java services. Codes emitted:

| Exception | Status | Code |
|---|---|---|
| Request validation | `422` | `VALIDATION_ERROR` (`details` = field errors) |
| HTTP error (e.g. 404) | passthrough | `HTTP_ERROR` |
| Provider has no API key | `503` | `PROVIDER_NOT_CONFIGURED` (`details` = `{ provider }`) |
| Unexpected failure | `500` | `INTERNAL_ERROR` (internals hidden) |

## Fix-prompt generation

`POST /fix-prompt` takes one finding and returns a single prompt the user can paste
into their AI builder. The chain (`app/chains.py`) is an LCEL composition:

```
ChatPromptTemplate([system, human])  |  ChatOpenAI(provider, temperature=0.3)
```

- `FIX_PROMPT_SYSTEM` casts the model as "VibeShield's fix-prompt generator" writing
  for a non-technical *vibecoder*, tailors guidance per builder — **Lovable, Cursor,
  v0, Bolt, Replit, Generic** — and requires the output be only the prompt text (no
  markdown fences).
- `FIX_PROMPT_HUMAN` interpolates the finding fields (`title`, `severity`, `check`,
  `affected`, `summary`, `impact`).

The chain is invoked with `.ainvoke(...)` and the response content is stripped.
Chains are cached per provider with `@lru_cache`, but a build that raises
`ProviderNotConfigured` is deliberately not cached, so it retries once the key is
set. The `/chat` endpoint uses an analogous `build_chat_chain` with a generic system
prompt (temperature 0.2).

## Provider selection

The provider is chosen **per request** via the `provider` field
(`Literal["openai", "logos", "selfhosted"]`), defaulting to `"openai"` in code.
`_provider_config()` maps the provider to `(api_key, base_url, model)`; `_model()`
raises `ProviderNotConfigured` if the selected provider's key is empty.

| Provider | Model (default) | Base URL (default) | API key env |
|---|---|---|---|
| OpenAI (cloud) | `gpt-4o-mini` (`MODEL_NAME`) | `https://api.openai.com/v1` (`OPENAI_BASE_URL`) | `OPENAI_API_KEY` |
| TUM Logos | `openai/gpt-oss-120b` (`LOGOS_MODEL_NAME`) | `https://logos.aet.cit.tum.de/v1` (`LOGOS_BASE_URL`) | `LOGOS_API_KEY` |
| Self-hosted (local) | `llama3.2:3b` (`SELFHOSTED_MODEL_NAME`) | `http://ollama:11434/v1` (`SELFHOSTED_BASE_URL`) | `SELFHOSTED_API_KEY` (dummy `ollama`) |

### Self-hosted model (Ollama)

The `selfhosted` provider is backed by an **Ollama** runtime that VibeShield runs
itself — the local-model path for the course requirement:

- **Docker Compose:** an `ollama` service (`ollama/ollama:latest`, container
  `vibeshield-ollama`) that on first boot pulls `SELFHOSTED_MODEL_NAME` and serves
  the OpenAI-compatible API on `:11434`. Weights are cached in the `ollama_models`
  volume so restarts are fast. It is `expose`d only (no host port); the
  langchain-service `depends_on` it. CPU-only inference is slow — keep to small
  models (1B–3B).
- **Kubernetes:** an `ollama` Deployment + Service (port `11434`) with an
  `ollama-models` PersistentVolumeClaim (8Gi) so the pulled weights survive
  restarts.
- The API key is a **dummy value** (`ollama`) that Ollama ignores; it defaults
  non-empty so the provider counts as "configured". Blank `SELFHOSTED_API_KEY` to
  disable the provider (callers then get `503 PROVIDER_NOT_CONFIGURED`).

> **Env-var name trap:** inside the container the OpenAI model variable is
> `MODEL_NAME`, but `docker-compose.yml` and `.env.example` feed it from a
> differently named host var: `MODEL_NAME=${LANGCHAIN_MODEL_NAME:-gpt-4o-mini}`. Set
> `LANGCHAIN_MODEL_NAME` in your `.env`; it becomes `MODEL_NAME` in the container.

## Configuration

All settings come from `app/settings.py` (pydantic `Settings`, `extra="ignore"`):

| Setting | Env | Default |
|---|---|---|
| App name | `APP_NAME` | `vibeshield-langchain-service` |
| OpenAI key | `OPENAI_API_KEY` | `""` |
| OpenAI base URL | `OPENAI_BASE_URL` | `https://api.openai.com/v1` |
| OpenAI model | `MODEL_NAME` | `gpt-4o-mini` |
| Logos key | `LOGOS_API_KEY` | `""` |
| Logos base URL | `LOGOS_BASE_URL` | `https://logos.aet.cit.tum.de/v1` |
| Logos model | `LOGOS_MODEL_NAME` | `openai/gpt-oss-120b` |
| Self-hosted key | `SELFHOSTED_API_KEY` | `ollama` (dummy) |
| Self-hosted base URL | `SELFHOSTED_BASE_URL` | `http://ollama:11434/v1` |
| Self-hosted model | `SELFHOSTED_MODEL_NAME` | `llama3.2:3b` |
| Port | `PORT` | `8000` |
| Uvicorn reload | `RELOAD` | `false` |

In Kubernetes, `OPENAI_API_KEY` and `LOGOS_API_KEY` come from the
`vibeshield-secrets` secret (both `optional: true`); base URLs and model names are
templated through Helm values. The self-hosted provider points at the in-cluster
`ollama` Service.

## Source map

| Concern | File (under `services/langchain-service/`) |
|---|---|
| App, endpoints, request/response & error models, exception handlers, metrics | `app/main.py` |
| Uvicorn runner | `app/server.py` |
| Chains, prompt templates, provider selection, `ProviderNotConfigured` | `app/chains.py` |
| Config / env | `app/settings.py` |
| Dependencies | `requirements.txt` |
| Container image | `Dockerfile` |

## Known gaps

- **No tests exist.** The root `README.md` documents `pytest` for this service, but
  there are no test files and `pytest` is not even in `requirements.txt`. This is a
  documentation/CI claim not backed by code.
- **Default-provider disagreement:** `.env.example` comments TUM Logos as the
  "default provider", but the code default is `"openai"` in `ChatRequest` /
  `FixPromptRequest`. (The client, separately, defaults its provider toggle to
  Logos — see [client.md](client.md).)
- **Env-var naming mismatch** for the OpenAI model: internal `MODEL_NAME` vs.
  compose/`.env` indirection `LANGCHAIN_MODEL_NAME`.
- **JWT-protected** user endpoints (`/chat`, `/fix-prompt`) via the shared
  `APP_JWT_SECRET`; `/health` and `/metrics` remain public for probes/scraping.
