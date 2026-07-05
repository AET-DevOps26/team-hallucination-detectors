# Scanner Service

Spring Boot microservice that performs the actual surface-level security scans. It
is **internal-only**: the [api-service](api.md) calls it on the container network,
and it is deliberately not published to the host or routed through the gateway. The
scanner is **stateless** — same request in, same findings out — with no database of
its own. The api-service owns queueing, retries, persistence, and the public
polling workflow.

> **Passive by design.** The scanner only sends `GET`/`HEAD` requests to the target
> site, with per-request timeouts and a hard request cap. No payload injection, no
> active probing, and an SSRF guard blocks internal/metadata addresses (see below).

> **Contract-first.** The api-service ↔ scanner contract is `api/scanner-internal.yaml`.
> The controller implements a generated interface and all DTOs are generated;
> regenerate with `api/scripts/gen-all.sh`.

## Stack

| | |
|---|---|
| Framework | Spring Boot 3.4.4, Java 21 (Gradle Kotlin DSL) |
| Persistence | none — fully stateless |
| HTTP client | JDK built-in `java.net.http.HttpClient` (no jsoup / Selenium / Playwright) |
| Docs | springdoc OpenAPI → Swagger UI |
| Metrics | Spring Actuator + Micrometer (Prometheus) |

## Running

The service listens on **container port 8080** with **no published host port** — in
`docker-compose.yml` it uses `expose: ["8080"]` only. The api-service reaches it at
`http://scanner-service:8080`.

```bash
# scanner comes up with the whole stack
docker compose up --build

# or on its own (the api-service depends on it)
docker compose up --build scanner-service
```

Interactive API docs (when running): `http://localhost:8080/swagger-ui.html`
(OpenAPI JSON at `/v3/api-docs`). Since the scanner has no host port, reaching this
requires exec-ing into the container or temporarily publishing the port.

## Endpoints

Contract: `api/scanner-internal.yaml`. **No authentication** — the scanner is only
reachable on the internal network, so no auth scheme is defined.

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| `POST` | `/scan` | `ScanExecutionRequest` | `200` `ScanExecutionResult` | `400 VALIDATION_ERROR` |
| `GET`  | `/health` | — | `200` `{ status: "ok", service: "vibeshield-scanner-service" }` | — |

**`POST /scan`** runs synchronously — one request per scan job, the caller waits for
the result:

- Request `{ url, checks[], crawlDepth?, includeSubdomains? }`.
- `200` carries `status` (`Completed` / `Failed`), `executedChecks` (which checks
  actually ran — may be a subset of requested), `pages` (URLs visited), `findings`,
  and `errorMessage` (set only on `Failed`).

A **scan-level failure** (target unreachable, or blocked by the SSRF guard) is a
normal `200` with `status: Failed` and an `errorMessage` — not an error response.
Only malformed requests return `400`. Transport-level failures (scanner down,
timeout) surface to the caller as a connection failure, not a defined response.

The `/health` route returns a hardcoded liveness body and is distinct from the
actuator `/actuator/health` that the docker healthcheck probes.

All errors use the unified schema `{ code, message, details }`. Codes emitted:
`VALIDATION_ERROR` (malformed request / bad body) and `INTERNAL_ERROR` (unexpected
failure). `details` is currently always `null`.

## Security checks

The `ScanCheck` enum defines six check types — `crawl`, `https`, `headers`,
`adminPaths`, `secrets`, `sensitiveFiles` — but **only three are implemented for the
MVP.** Each check is a `@Component` implementing `SecurityCheck` (`type()` +
`run(URI, SiteFetcher)`); `ScanExecutor` runs only the requested checks that have an
implementation and lists the ones that ran in `executedChecks`. A requested check
with no implementation is **silently skipped** and simply omitted from
`executedChecks` — callers must not assume a requested check ran unless it appears
there.

### `https` — `HttpsCheck`
Fetches both the `http://` and `https://` variants of the target.
| Condition | Finding | Severity |
|---|---|---|
| Plain HTTP reachable and not redirecting to HTTPS | Site is reachable over unencrypted HTTP | **High** |
| HTTPS not reachable at all | HTTPS is not available | **Critical** |
| HTTPS reachable but no `Strict-Transport-Security` header | Missing HSTS header | **Medium** |

### `headers` — `HeadersCheck`
Inspects the response headers of the start page.
| Condition | Finding | Severity |
|---|---|---|
| Missing `Content-Security-Policy` | Missing CSP header | **Medium** |
| No framing protection (`X-Frame-Options` absent and no CSP `frame-ancestors`) | Site can be embedded in other pages (clickjacking) | **Medium** |
| `X-Content-Type-Options` not `nosniff` | Missing X-Content-Type-Options header | **Low** |
| Missing `Referrer-Policy` | Missing Referrer-Policy header | **Low** |
| Missing `Permissions-Policy` | Missing Permissions-Policy header | **Info** |

### `sensitiveFiles` — `SensitiveFilesCheck`
Probes a fixed list of well-known paths. A finding requires **both** an HTTP `200`
**and** a content-heuristic match on the body (to avoid false positives from SPA
app-shell responses).
| Path | Finding | Severity |
|---|---|---|
| `/.env` | Environment file (.env) is publicly downloadable | **Critical** |
| `/.git/config` | Git repository metadata (.git/config) is exposed | **High** |
| `/.git/HEAD` | Git repository metadata (.git/HEAD) is exposed | **High** |
| `/.DS_Store` | macOS folder metadata (.DS_Store) is exposed | **Low** |

### Not implemented (post-MVP)
`crawl`, `adminPaths` (admin pages), and `secrets` (exposed API keys) have enum
values but no check class yet.

## Crawling

**There is no crawler.** The `crawl` check is unimplemented and the `crawlDepth` /
`includeSubdomains` request fields are validated (`crawlDepth` 0–3) but never read.
Per scan, `ScanExecutor` does one reachability probe of the target and then each
requested check makes its own `GET`s against the start URL; `pages` in the result is
always just the single start URL.

## SSRF guard

`http/SsrfGuard.java` resolves the host **at fetch time** (defeating DNS rebinding)
and blocks addresses that are loopback, wildcard/any-local, link-local (including
the `169.254.169.254` cloud-metadata address), private (`10/8`, `172.16/12`,
`192.168/16`), multicast, or IPv6 unique-local (`fc00::/7`). A blocked target yields
a `Failed` scan result rather than an exception. Loopback is only permitted when
`scanner.ssrf.allow-loopback` is `true` — which must stay `false` in every deployed
environment (it exists for integration tests).

## Configuration

`spring.application.name: scanner-service`; `server.port: 8080`. All scanner tuning
is env-overridable:

| Property | Env | Default |
|---|---|---|
| Request/connect timeout | `SCANNER_TIMEOUT_MS` | `5000` |
| Per-scan request cap | `SCANNER_REQUEST_BUDGET` | `25` |
| User agent | `SCANNER_USER_AGENT` | `VibeShield-Scanner/1.0` |
| Allow loopback targets | `SCANNER_SSRF_ALLOW_LOOPBACK` | `false` |

The request budget is a hard cap on `GET`s per scan; exhausting it mid-scan stops
the remaining checks early rather than failing the whole scan. Response bodies are
read up to a 16 KB snippet cap. Actuator exposes `health,info,prometheus`.

## Source map

| Concern | File (under `src/main/java/de/tum/devops/vibeshield/scanner/`) |
|---|---|
| Entry point | `ScannerServiceApplication.java` |
| HTTP endpoints | `controller/ScannerController.java` |
| Scan orchestration | `service/ScanExecutor.java`, `service/FetcherFactory.java` |
| Check interface | `checks/SecurityCheck.java` |
| Check implementations | `checks/{HttpsCheck,HeadersCheck,SensitiveFilesCheck}.java` |
| Finding builder | `checks/Findings.java` |
| HTTP layer | `http/{SiteFetcher,HttpSiteFetcher,FetchResult,SsrfGuard}.java` |
| Domain exceptions | `http/{BlockedAddressException,RequestBudgetExceededException}.java` |
| Error handling | `exception/GlobalExceptionHandler.java` |
| Config | `src/main/resources/application.yml` |
| Contract | `api/scanner-internal.yaml` |

## Known gaps

- **Half the checks are unimplemented:** `crawl`, `adminPaths`, and `secrets` have
  enum values but no logic. There is no crawler, and `crawlDepth` /
  `includeSubdomains` are accepted but ignored (`pages` is always just the start
  URL). Requested-but-unimplemented checks are dropped silently — only
  `executedChecks` reflects what actually ran.
- **`/health` is hardcoded** to always report `ok`; it does not reflect real
  readiness (the docker healthcheck uses `/actuator/health` instead).
- **`Error.details` is never populated** (always `null`).
- **`scanner.ssrf.allow-loopback` must stay `false`** everywhere except integration
  tests.
