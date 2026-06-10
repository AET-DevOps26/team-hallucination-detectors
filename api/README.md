# API contracts — single source of truth

This directory holds the OpenAPI contracts for VibeShield. **The spec comes
first:** endpoints are designed and reviewed here *before* any controller,
DTO, or client code is written. Implementation code is generated *from* these
files — never the other way around.

| File | Contract |
|---|---|
| `openapi.yaml` | Public REST API (`/api/v1/...`), served to the client through the nginx gateway / k8s ingress. Auth endpoints are implemented by the auth-service, everything else by the api-service. |
| `scanner-internal.yaml` | Internal api-service ↔ scanner-service contract (issue #27). Not exposed through the gateway. |
| `openapitools.json` | Pins the OpenAPI Generator version so output is reproducible. |
| `scripts/gen-all.sh` | Regenerates every language artefact from the specs. |

## Workflow for changing the API

1. Edit `openapi.yaml` / `scanner-internal.yaml` on a feature branch.
2. Lint: `npx --yes @redocly/cli@1 lint` (also runs as a pre-commit hook —
   install once with `pip install pre-commit && pre-commit install`).
3. Regenerate: `./api/scripts/gen-all.sh`.
4. Commit the spec **and** the regenerated files together; open a PR. The
   spec diff is the design review.

CI enforces both steps: a lint failure or any drift between the spec and the
committed generated code fails the `openapi` job and blocks merge.

## Generated artefacts (never edit by hand)

| Output | Consumer | Generator |
|---|---|---|
| `services/api-service/src/generated/java` | Spring server interfaces (`WebsitesApi`, `ScansApi`) + all models | `openapi-generator` (`spring`, interface-only) |
| `client/src/api/schema.ts` | TypeScript types for all paths/schemas | `openapi-typescript` |

Controllers in the api-service must `implement` the generated interfaces;
hand-written DTOs are not allowed. The auth-service predates the contract and
still uses hand-written DTOs — its migration to generated interfaces is
tracked as a separate issue.

A Python client for the langchain-service (`openapi-python-client`) gets added
to `gen-all.sh` when the backend→GenAI integration starts (issue #26).

## Mock server for frontend development

Frontend work does not need a running backend — serve the contract as a mock:

```bash
npx @stoplight/prism-cli mock api/openapi.yaml  # http://localhost:4010
```

## Conventions baked into the contract

- All paths versioned under `/api/v1`.
- Unified error shape `{ code, message, details }` on every non-2xx response.
- Bearer JWT (issued by `POST /api/v1/auth/login`) on everything except
  register/login.
- Resources owned by another user return **404**, not 403, so IDs cannot be
  enumerated.
- Enum casing: scan status `Pending | Running | Completed | Failed`, severity
  `Critical | High | Medium | Low | Info`, finding status `Open | Fixed |
  Ignored`; scan check IDs (`https`, `headers`, `adminPaths`, ...) match the
  client UI's scan options.
