# Auth Service

Spring Boot microservice that handles user identity for VibeShield: registration, login,
and current-user lookup. It issues HMAC-signed JWTs that other services can validate, and
stores users in the shared PostgreSQL database.

> **Note:** The MVP uses this self-contained JWT service rather than Keycloak. If the team
> later adopts Keycloak/OIDC, this service is the seam that would be replaced.

## Stack

| | |
|---|---|
| Framework | Spring Boot 3.3.4, Java 21 (Gradle Kotlin DSL) |
| Persistence | Spring Data JPA → PostgreSQL 16 (`users` table) |
| Passwords | BCrypt (`spring-security-crypto`) |
| Tokens | JWT via `jjwt` 0.12.6, HMAC-signed |
| Docs | springdoc OpenAPI → Swagger UI |
| Metrics | Spring Actuator + Micrometer (Prometheus) |

## Running

The service listens on **container port 8080**, published to the host as **8081** via
`docker-compose.yml` (`8081:8080`). The frontend reaches it through `VITE_AUTH_BASE_URL`
(default `http://localhost:8081`).

```bash
# whole stack
docker compose up --build

# auth service only
docker compose up --build auth-service database
```

## Endpoints

All routes are served under `/api/v1/auth`.

| Method | Path | Auth | Body | Success | Errors |
|---|---|---|---|---|---|
| `GET`  | `/health`   | — | — | `200` `"Auth service running"` (text) | — |
| `POST` | `/register` | — | `{ email, password }` | `200` `{ message }` | `400` if email already registered |
| `POST` | `/login`    | — | `{ email, password }` | `200` `{ token, email }` | `401` on bad credentials |
| `GET`  | `/me`       | Bearer token | — | `200` `{ email }` | `401` if header missing/invalid |

Send the token returned by `/login` as `Authorization: Bearer <token>` to `/me`.

All errors use the unified schema `{ code, message, details }` (see `dto/ErrorResponse.java`),
shared across every VibeShield service. Codes emitted here: `EMAIL_ALREADY_REGISTERED`,
`INVALID_CREDENTIALS`, `UNAUTHORIZED`, `INVALID_TOKEN`, and `INTERNAL_ERROR` for unexpected
failures. `details` is `null` unless an error carries structured context.

Interactive API docs (when running): `http://localhost:8081/swagger-ui.html`
(OpenAPI JSON at `/v3/api-docs`).

## Data model

`users` table (Hibernate `ddl-auto: update`):

| Column | Type | Notes |
|---|---|---|
| `id` | `bigint` | PK, identity |
| `email` | `varchar` | unique — login identifier |
| `password` | `varchar` | BCrypt hash (never the plaintext) |

## JWT

- **Algorithm:** HMAC over the configured secret.
- **Claims:** `sub` = user email, `userId`, `iat`, `exp`.
- **Validation:** `JwtService#isTokenValid` checks signature and expiry; `extractEmail`
  reads the subject.

| Property | Env / key | Default |
|---|---|---|
| Signing secret | `app.jwt.secret` | `dev-secret-key-change-me-minimum-32-chars!!` (dev only) |
| Token lifetime | `app.jwt.expiration-ms` | `86400000` (24h) |

> The default secret is a development placeholder and **must** be overridden in any deployed
> environment. Every service that validates these tokens must share the same secret.

## Source map

| Concern | File |
|---|---|
| HTTP endpoints | `controller/AuthController.java` |
| Token issue/verify | `service/JwtService.java` |
| User entity | `model/User.java` |
| Persistence | `repository/UserRepository.java` |
| Request/response bodies | `dto/RegisterRequest.java`, `dto/LoginRequest.java`, `dto/AuthResponse.java` |
| CORS | `config/WebConfig.java` |
| Error handling | `exception/GlobalExceptionHandler.java` |
| Entry point | `AuthServiceApplication.java` |

## Known gaps

These deviate from the project conventions and are worth tracking:

- **`application.yml` identity is wrong:** `spring.application.name` is `api-service` and the
  datasource block is copied from the API service. The name should be `auth-service` — it
  labels Prometheus metrics, so fixing it matters for observability.
- **Hardcoded config:** DB credentials live in `application.yml` and CORS origins are hardcoded
  in `WebConfig`. These should be externalised to env vars/Secrets.
- **Leftover test:** `HelloControllerTest` calls `/api/v1/hello`, which this service does not
  expose; it boots the context but the assertions target the wrong endpoint.
