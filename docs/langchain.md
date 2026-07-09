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

> **Unauthenticated.** No endpoint checks a JWT; it relies on being unpublished
> (reachable only via the gateway). It's otherwise stateless except for one optional
> piece: a small Postgres-backed RAG knowledge base for the fix-prompt endpoint (see
> [RAG knowledge base](#rag-knowledge-base) below). Retrieval is additive grounding,
> not a hard dependency — with no `DATABASE_URL` configured the service behaves
> exactly as it did before that table existed.

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

All endpoints are on the root app (no routers) and require **no authentication**.

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
  for a non-technical *vibecoder*; requires the output be only the prompt text (no
  markdown fences); and forbids inventing files/routes/APIs, suggesting a security
  control be disabled or a finding hidden, and requires a verification step. It
  interpolates three things computed before the chain is invoked (`app/main.py`):
  - `{builder}` / `{builder_guidance}` — the target builder (**Lovable, Cursor, v0,
    Bolt, Replit, Generic**) and a guidance line rendered from that builder's entry
    in `BUILDER_PROFILES` (tone, output style, must-include/avoid), so builder
    choice actually changes the prompt instead of being a cosmetic label.
  - `{retrieved_context}` — a short, cited grounding snippet from the [RAG
    knowledge base](#rag-knowledge-base), or the literal string `(none)`.
- `FIX_PROMPT_HUMAN` interpolates the finding fields (`title`, `severity`, `check`,
  `affected`, `summary`, `impact`).

The chain is invoked with `.ainvoke(...)`, and the response is checked against
`app/guardrails.py` before it's returned: max length, no markdown fences, must
contain a verification step, must not contain a dangerous instruction (e.g.
"disable HTTPS") unless the finding itself already says so, and must reference the
affected target when one is known. A prompt that fails is regenerated once; a
second failure surfaces as `502 FIX_PROMPT_GENERATION_FAILED` instead of returning
an unsafe prompt. The request itself is validated too: `severity` must be one of
`Critical/High/Medium/Low/Info` (case-insensitive, else `422`), `builder` is
normalized to a known name (falling back to `Generic` rather than rejecting), and
`title` is rejected if empty or symbol-only.

Chains are cached per provider with `@lru_cache`, but a build that raises
`ProviderNotConfigured` is deliberately not cached, so it retries once the key is
set. The `/chat` endpoint uses an analogous `build_chat_chain` with a generic system
prompt (temperature 0.2) and none of the above guardrails or retrieval.

## RAG knowledge base

`app/retrieval.py` grounds the fix-prompt generator in a handful of short, cited
security write-ups (OWASP/CWE/MDN), so the model can reference a real source (e.g.
"per OWASP A05:2021") instead of inventing a severity rationale. It's one
provider-agnostic step shared by all three LLM providers — retrieval happens once,
upstream of picking openai/logos/selfhosted, rather than being duplicated or
restricted to the cloud path.

- **Storage:** a `fix_prompt_knowledge` table (`check_type`, `source`, `title`,
  `content`, `embedding vector(1536)`) in its own `langchain_service` schema in the
  shared vibeshield Postgres, using the `pgvector` extension (`pgvector/pgvector:pg16`
  image — a drop-in replacement for `postgres:16`, same on-disk format). Created
  automatically on startup by `app/db.py::ensure_schema()`, which is a no-op if
  `DATABASE_URL` isn't set.
- **Content:** `app/knowledge_data.py` — curated entries keyed to VibeShield's
  scanner check types (`https`, `headers`, `adminPaths`, `secrets`,
  `sensitiveFiles`, `cookies`).
- **Indexing (offline, manual):** embed and upsert the content once `DATABASE_URL`
  and an embedding key are configured:

  ```bash
  cd services/langchain-service
  python -m app.embed_knowledge
  ```

  Safe to re-run — entries are matched by `title` and updated in place, so editing
  `knowledge_data.py` and re-running keeps the table in sync.
- **Retrieval (per request):** embeds the finding's `check`/`title`/`summary`,
  then a pgvector cosine-distance search scoped to that `check_type` (falling back
  to a plain similarity search across everything if that check type has no
  entries), capped to 2 chunks / 700 characters total — the cap applies to every
  provider, not just the slow self-hosted one, since it's the total prompt length
  that matters for CPU prefill time, not which provider answers.
- **Failure mode:** any failure here (DB unreachable, no embedding key, query
  error) logs a warning and returns `(none)` rather than failing the fix-prompt
  request — RAG is additive grounding, not a hard dependency.
- **Embeddings:** a single fixed config (`EMBEDDING_API_KEY` — falls back to
  `OPENAI_API_KEY` if unset — `EMBEDDING_BASE_URL`, `EMBEDDING_MODEL_NAME`,
  default `text-embedding-3-small`), independent of which of the three chat
  providers answers the request.

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
| Database URL (RAG) | `DATABASE_URL` | `""` (retrieval disabled) |
| Embedding key (RAG) | `EMBEDDING_API_KEY` | `""` (falls back to `OPENAI_API_KEY`) |
| Embedding base URL (RAG) | `EMBEDDING_BASE_URL` | `https://api.openai.com/v1` |
| Embedding model (RAG) | `EMBEDDING_MODEL_NAME` | `text-embedding-3-small` |
| Port | `PORT` | `8000` |
| Uvicorn reload | `RELOAD` | `false` |

In Kubernetes, `OPENAI_API_KEY` and `LOGOS_API_KEY` come from the
`vibeshield-secrets` secret (both `optional: true`); base URLs and model names are
templated through Helm values. The self-hosted provider points at the in-cluster
`ollama` Service. `DATABASE_URL` is assembled from the same `POSTGRES_PASSWORD`
secret the Java services use, pointed at the shared `database` Service.

## Source map

| Concern | File (under `services/langchain-service/`) |
|---|---|
| App, endpoints, request/response & error models, exception handlers, metrics | `app/main.py` |
| Uvicorn runner | `app/server.py` |
| Chains, prompt templates, builder profiles, provider selection, `ProviderNotConfigured` | `app/chains.py` |
| Input validation, output validation (`FixPromptGenerationFailed`) | `app/guardrails.py` |
| RAG: SQLAlchemy engine/session, `KnowledgeChunk` model, schema setup | `app/db.py` |
| RAG: embedding + pgvector similarity search | `app/retrieval.py` |
| RAG: curated OWASP/CWE/MDN content | `app/knowledge_data.py` |
| RAG: offline embed/upsert script | `app/embed_knowledge.py` |
| Config / env | `app/settings.py` |
| Tests | `tests/` (pytest; run via `python -m pytest` from this directory) |
| Dependencies | `requirements.txt`, `requirements-dev.txt` |
| Container image | `Dockerfile` |

## Known gaps

- **Default-provider disagreement:** `.env.example` comments TUM Logos as the
  "default provider", but the code default is `"openai"` in `ChatRequest` /
  `FixPromptRequest`. (The client, separately, defaults its provider toggle to
  Logos — see [client.md](client.md).)
- **Env-var naming mismatch** for the OpenAI model: internal `MODEL_NAME` vs.
  compose/`.env` indirection `LANGCHAIN_MODEL_NAME`.
- **No authentication** on any endpoint — the service relies entirely on being
  unpublished and gateway-only.
