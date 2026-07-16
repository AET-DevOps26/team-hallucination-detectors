# Gateway

Nginx reverse proxy that fronts the whole stack on a single origin. Everything the
browser touches — the SPA, the API, auth, and the GenAI service — is served through
the gateway, so there are no cross-origin requests (which is why the services carry
no CORS config). Locally this is the `nginx:alpine` container in `docker-compose.yml`;
in Kubernetes an ingress-nginx **ingress** plays the same role (see below).

Config: `gateway/nginx.conf` — two `server` blocks (HTTP `:80` and HTTPS `:443`)
with identical location blocks.

## Ports

| Listener | Container port | Published (compose) |
|---|---|---|
| HTTP | `80` | `3000` |
| HTTPS (`ssl http2`) | `443` | `3443` |

TLS certs are mounted from `./gateway/certs` (`gateway.crt` / `gateway.key`). So
locally the app is `http://localhost:3000` (or `https://localhost:3443`).

## Routing

Nginx picks the **longest-prefix** match (and `=` is an exact match), so ordering in
the file doesn't determine precedence — specificity does:

| Location | Match | Upstream | Rewrite |
|---|---|---|---|
| `/api/v1/auth/` | prefix (beats `/api/`) | `auth-service:8080` | none — served natively |
| `/api/` | prefix | `api-service:8080` | none |
| `= /openapi.yaml` | exact | `api-service:8080` | none — feeds Swagger UI |
| `/auth/` | prefix | `auth-service:8080` | `^/auth/?(.*)$ → /api/v1/auth/$1` |
| `/langchain/` | prefix | `langchain-service:8000` | `^/langchain/?(.*)$ → /$1` (strips prefix) |
| `/` | prefix (fallback) | `client:3000` | none — SPA and everything else |

Notes:

- **Auth has two entry points:** the canonical `/api/v1/auth/*` (no rewrite — the
  auth-service serves that path natively) and a convenience alias `/auth/*` that is
  rewritten to it. The client uses the `/auth` alias by default (`VITE_AUTH_BASE_URL`).
- **`/langchain/*` strips its prefix**, so the client's `POST /langchain/fix-prompt`
  arrives at the GenAI service as `POST /fix-prompt`. The client calls this service
  directly — the api-service is not involved.
- **The [scanner-service](scanner.md) has no route** — it is internal-only, reached
  only by the api-service on the container network.
- **`/openapi.yaml`** is routed to the api-service so the Swagger UI can load the raw
  spec.

## Headers

Both server blocks set `Host`, `X-Real-IP`, `X-Forwarded-For`, and
`X-Forwarded-Proto` (`$scheme` on `:80`, hardcoded `https` on `:443`) on proxied
requests. No CORS headers (single-origin design), no explicit timeouts (Nginx
defaults). Access/error logs go to `/var/log/nginx/*` (errors at `warn`).

## Kubernetes: ingress instead of this gateway

In the cluster, this compose gateway is replaced by ingress-nginx resources under
`helm/vibeshield/templates/ingress*.yml`. They provide the same logical routing with
different mechanics:

- Ingress backends target each service on **port 80**; routing to the real container
  port (`8080` / `8000` / `3000`) happens in the k8s Service definitions.
- The main ingress adds the `/openapi.yaml` (`Exact`) route and TLS via cert-manager
  / Let's Encrypt (`secretName: vibeshield-tls`, host from `.Values.global.host`).
- The `/auth` and `/langchain` **rewrites are split into separate ingress objects**
  (`ingress-auth-rewrite.yml`, `ingress-langchain-rewrite.yml`) using
  `nginx.ingress.kubernetes.io/rewrite-target` + `use-regex`, rather than inline
  `rewrite` directives.

The deployed app is `https://ge65poj.stud.k8s.aet.cit.tum.de` (API docs at
`/api/swagger-ui.html`).
