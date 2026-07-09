# API Service

Spring Boot microservice that is the public REST backend for VibeShield: it owns
websites, scans, findings, and reports. It validates the JWTs issued by the
[auth-service](auth.md), persists to the shared PostgreSQL database under the
service-owned `api_service` schema, and drives scans by calling the internal
[scanner-service](scanner.md) from a background worker.

> **Contract-first.** Every public endpoint is designed in `api/openapi.yaml`
> first; the controllers `implement` OpenAPI-generated interfaces and all DTOs
> are generated. Hand-written DTOs are not allowed here (the two exceptions,
> `ErrorResponse` and `HelloResponse`, predate or sit outside the contract). See
> `api/README.md` for the regeneration workflow.

> **The api-service does not call the GenAI service.** AI fix-prompt generation
> lives in the [langchain-service](langchain.md), which the client reaches
> directly through the gateway at `/langchain/*`. This service only stores each
> finding's plain-language `suggestedFix` text — the raw material the client
> later feeds to the GenAI service.

## Stack

| | |
|---|---|
| Framework | Spring Boot 3.4.4, Java 21 (Gradle Kotlin DSL) |
| Persistence | Spring Data JPA → PostgreSQL 16, schema `api_service` (schema-per-service) |
| Migrations | Flyway versioned SQL in `src/main/resources/db/migration` (V1–V3) |
| Tokens | JWT **validation** via `jjwt` 0.12.6, HMAC — never issues tokens |
| Scanner client | Spring `RestClient` → `scanner-service` (types generated from `api/scanner-internal.yaml`) |
| Docs | springdoc OpenAPI → Swagger UI |
| Metrics | Spring Actuator + Micrometer (Prometheus) |

Hibernate runs with `ddl-auto: validate`, so schema drift fails startup instead
of mutating the database at runtime.

## Running

The service listens on **container port 8080** and is **not** published to the
host — it is reached only through the nginx gateway (`/api/*` → `api-service:8080`).
Locally the gateway exposes everything on `http://localhost:3000`.

```bash
# whole stack
docker compose up --build

# api service only (needs the database and scanner)
docker compose up --build api-service scanner-service database
```

Interactive API docs (through the gateway): `http://localhost:3000/api/swagger-ui.html`
(the raw spec is served at `/openapi.yaml`; OpenAPI JSON at `/api/v3/api-docs`).

## Endpoints

Every route requires a Bearer JWT (issued by `POST /api/v1/auth/login`) except the
scaffold health route. Resources owned by another user return **404**, not 403, so
IDs cannot be enumerated. Full request/response schemas are in `api/openapi.yaml`.

### Websites

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| `POST` | `/api/v1/websites` | `{ url, name? }` | `201` `Website` | `400 VALIDATION_ERROR`, `401`, `409 WEBSITE_ALREADY_REGISTERED` |
| `GET`  | `/api/v1/websites` | — | `200` `Website[]` (newest first) | `401` |

### Scans

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| `POST` | `/api/v1/websites/{websiteId}/scans` | `ScanRequest` (optional) | `202` `Scan` + `Location` | `401`, `404 WEBSITE_NOT_FOUND`, `409 SCAN_IN_PROGRESS` |
| `GET`  | `/api/v1/websites/{websiteId}/scans` | — | `200` `Scan[]` (newest first) | `401`, `404` |
| `GET`  | `/api/v1/websites/{websiteId}/scans/latest` | — | `200` `Scan` | `401`, `404 SCAN_NOT_FOUND` |
| `GET`  | `/api/v1/scans/{scanId}` | — | `200` `Scan` (poll while `Pending`/`Running`) | `401`, `404` |
| `GET`  | `/api/v1/scans/{scanId}/findings` | — | `200` `Finding[]` (severity desc) | `401`, `404` |
| `POST` | `/api/v1/scans/{scanId}/rescan` | — | `202` `Scan` + `Location` | `401`, `404`, `409 SCAN_IN_PROGRESS` |
| `GET`  | `/api/v1/scans/{scanId}/comparison` | — | `200` `ScanComparison` | `401`, `404` |

### Reports

| Method | Path | Success | Errors |
|---|---|---|---|
| `GET` | `/api/v1/scans/{scanId}/report/data` | `200` `ReportData` (JSON) | `401`, `404` |
| `GET` | `/api/v1/scans/{scanId}/report/summary.html` | `200` `text/html` (attachment) | `401`, `404` |
| `GET` | `/api/v1/scans/{scanId}/report/summary.pdf` | `200` `application/pdf` (attachment) | `401`, `404` |
| `GET` | `/api/v1/scans/{scanId}/report/full.pdf` | `200` `application/pdf` (attachment) | `401`, `404` |

All errors use the unified schema `{ code, message, details }` (`dto/ErrorResponse.java`),
shared across every VibeShield service. Codes emitted here: `VALIDATION_ERROR`,
`WEBSITE_ALREADY_REGISTERED`, `WEBSITE_NOT_FOUND`, `SCAN_NOT_FOUND`,
`SCAN_IN_PROGRESS`, `UNAUTHORIZED`, `INVALID_TOKEN`, and `INTERNAL_ERROR`.
`details` is populated for bean-validation failures (a field→message map) and is
otherwise `null`.

### Async scan workflow

`POST .../scans` returns **`202 Accepted`** immediately with the scan in status
`Pending` — the request thread does no scanning. A scheduled background worker
executes the scan and the client polls `GET /api/v1/scans/{scanId}` until the
status is `Completed` or `Failed`, then fetches the findings.

```
1. POST .../scans        ScanService.trigger — validate ownership, reject if a
                         scan is already Pending/Running (409), persist Pending
                         → 202 returned to caller

2. @Scheduled poll       ScanWorker.processNextPendingScan (every 2s):
   (background worker)      - pick oldest Pending
                            - skip this tick if scanner /health is down
                              (scan stays Pending — graceful degradation)
                            - atomically claim Pending → Running
                            - ScannerClient.execute(...)  POST scanner /scan  (sync)
                            - persist findings + mark Completed, or mark Failed

3. @Scheduled recovery   ScanWorker.recoverStaleScans (every 60s):
                            fail scans stuck Running past running-timeout-ms (5 min)
```

Each status transition is its own short transaction (`ScanProcessingService`) — no
transaction is held open across the scanner HTTP call. A unique index
(`uq_scans_one_in_flight_per_website`) enforces at most one in-flight scan per
website at the database level, backing the `409 SCAN_IN_PROGRESS` guard. **Rescan**
clones an existing scan's configuration into a new `Pending` scan; **comparison**
diffs a completed scan against the previous completed scan for the same website
(by `checkType|affected|title` fingerprint → `Fixed` / `Still present` /
`Newly introduced`) and produces a prioritized action plan. Reports are read-only
projections rendered by `report/HtmlReportRenderer` and `report/PdfReportRenderer`.

## JWT validation

This service **validates but never issues** tokens. It verifies the HMAC signature
of tokens minted by the auth-service using a **shared secret** — the two services
must be configured with the same `APP_JWT_SECRET`.

- `security/JwtService.java` — verifies the signature/expiry and reads the `userId`
  claim and subject (`email`) into an `AuthenticatedUser`.
- `security/JwtAuthFilter.java` — a plain servlet `OncePerRequestFilter` (there is
  no Spring Security starter) that guards `/api/**`. It skips the swagger paths and
  the public `/api/v1/hello` scaffold route. A missing/non-Bearer header → `401
  UNAUTHORIZED`; an invalid/expired token → `401 INVALID_TOKEN`. These 401s are
  written directly by the filter (before the MVC exception advice runs).
- `security/CurrentUser.java` — how controllers read the authenticated identity,
  since the generated interfaces can't take extra parameters.

| Property | Env / key | Default |
|---|---|---|
| Signing secret | `app.jwt.secret` | `dev-secret-key-change-me-minimum-32-chars!!` (dev only) |

> The default secret is a development placeholder and **must** be overridden in any
> deployed environment. It must be identical to the auth-service's secret, or
> token validation fails. (Note the compose fallback value differs textually from
> this in-code default, so relying on defaults across services would break
> validation — always set `APP_JWT_SECRET` explicitly.)

## Data model

Schema `api_service`, created by Flyway migrations V1–V3. The api-service owns
`websites`, `scans`, and `findings`; `owner_id` is a logical reference to the
auth-service's users with **no cross-schema foreign key**.

**`websites`** (`V1__create_websites.sql`)
| Column | Type | Notes |
|---|---|---|
| `id` | `bigint` | PK, identity |
| `owner_id` | `bigint` | JWT `userId`; indexed |
| `url` | `varchar(2048)` | |
| `name` | `varchar(255)` | defaults to the URL host |
| `created_at` | `timestamptz` | |
| | | unique `(owner_id, url)` |

**`scans`** (`V2__create_scans_and_findings.sql`, `V3__add_scan_started_at.sql`)
| Column | Type | Notes |
|---|---|---|
| `id` | `bigint` | PK, identity |
| `website_id` | `bigint` | FK → `websites(id)` `ON DELETE CASCADE` |
| `status` | `varchar(16)` | `PENDING` / `RUNNING` / `COMPLETED` / `FAILED` |
| `requested_checks` | `varchar(255)` | comma-list via `ScanCheckListConverter` |
| `crawl_depth` | `int` | |
| `include_subdomains` | `boolean` | |
| `created_at` | `timestamptz` | |
| `completed_at` | `timestamptz` | set on `Completed`/`Failed` |
| `error_message` | `varchar(2048)` | set on `Failed` |
| `in_flight_website_id` | `bigint` | unique — enforces one active scan per website |
| `started_at` | `timestamptz` | claim time, for stale-running recovery |

**`findings`** (`V2__create_scans_and_findings.sql`)
| Column | Type | Notes |
|---|---|---|
| `id` | `bigint` | PK, identity |
| `scan_id` | `bigint` | FK → `scans(id)` `ON DELETE CASCADE` |
| `check_type` | `varchar(32)` | `ScanCheck` enum |
| `title` | `varchar(255)` | |
| `severity` | `varchar(16)` | `CRITICAL` / `HIGH` / `MEDIUM` / `LOW` |
| `affected` | `varchar(2048)` | |
| `explanation` | `varchar(4000)` | non-technical risk explanation |
| `suggested_fix` | `varchar(4000)` | plain-language fix — input for GenAI fix prompts |
| `status` | `varchar(16)` | `FindingStatus`; always `OPEN` on creation |

Enums are stored as their Java constant names (`PENDING`, `CRITICAL`); the contract
casing (`Pending`, `Critical`) is applied at serialization.

## Source map

| Concern | File(s) (under `src/main/java/de/tum/devops/vibeshield/`) |
|---|---|
| Entry point | `VibeShieldApiApplication.java` (`@EnableScheduling`) |
| HTTP endpoints | `controller/{WebsiteController,ScanController,ReportController}.java` |
| Generated API interfaces | `src/generated/java/.../generated/api/{WebsitesApi,ScansApi,ReportsApi}.java` |
| Business logic | `service/{WebsiteService,ScanService,ScanProcessingService,RescanService,ReportService,FindingPrioritization}.java` |
| Scan worker + scanner client | `scanner/{ScanWorker,ScannerClient}.java` |
| Entities | `model/{Website,Scan,Finding}.java` |
| Persistence | `repository/{WebsiteRepository,ScanRepository,FindingRepository}.java` |
| Security / JWT | `security/{JwtService,JwtAuthFilter,CurrentUser,AuthenticatedUser}.java` |
| Error handling | `exception/GlobalExceptionHandler.java`, `dto/ErrorResponse.java` |
| Migrations | `src/main/resources/db/migration/V1..V3` |

## Known gaps

These deviate from the project conventions and are worth tracking:

- **Hardcoded config:** the datasource defaults (`username: vibeshield`,
  `password: vibeshield`) and the dev JWT secret live in `application.yml`. These
  should be externalised to env vars/Secrets in any deployed environment.
- **Divergent JWT-secret defaults:** the in-code default and the docker-compose
  fallback for `APP_JWT_SECRET` are not textually identical. Cross-service
  validation only works when both services are given the same explicit secret.
- **Leftover scaffold:** `HelloController` / `GET /api/v1/hello` (and
  `dto/HelloResponse`) are scaffold-only, allow-listed as public, and not part of
  the OpenAPI contract. `HelloControllerTest` still targets it. The client's health
  badge currently pings this endpoint.
- **Report renderers are not beans:** `HtmlReportRenderer` / `PdfReportRenderer`
  are `new`-ed directly inside `ReportController` rather than injected.
