#!/usr/bin/env bash
# Regenerates every language artefact from the OpenAPI contracts in api/.
# Run after EVERY spec change and commit the results — CI fails the build if
# the committed generated code drifts from the spec.
#
# Generated outputs (never edit by hand):
#   services/api-service/src/generated/java  — Spring server interfaces + models
#   client/src/api/schema.ts                 — TypeScript types
#
# The generator version is pinned in api/openapitools.json; output is
# deterministic (hideGenerationTimestamp), so re-running without spec changes
# produces no diff.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "==> Linting contracts"
(cd "$ROOT" && npx --yes @redocly/cli@1 lint)

echo "==> Generating Spring server interfaces (api-service)"
# Only the Websites/Scans tags are generated here; the Auth tag is implemented
# by the auth-service, whose migration to generated interfaces is tracked
# separately. Generation goes through a temp dir so the committed tree only
# ever contains .java sources, no generator metadata.
GEN_DIR="$(mktemp -d)"
trap 'rm -rf "$GEN_DIR"' EXIT
npx --yes @openapitools/openapi-generator-cli@2.15.3 generate \
  --openapitools "$ROOT/api/openapitools.json" \
  -i "$ROOT/api/openapi.yaml" \
  -g spring \
  -o "$GEN_DIR" \
  --api-package de.tum.devops.vibeshield.generated.api \
  --model-package de.tum.devops.vibeshield.generated.model \
  --global-property "apis=Websites:Scans,models,apiDocs=false,modelDocs=false,apiTests=false,modelTests=false" \
  --additional-properties "interfaceOnly=true,useSpringBoot3=true,useTags=true,openApiNullable=false,skipDefaultInterface=true,hideGenerationTimestamp=true,useBeanValidation=true"

TARGET="$ROOT/services/api-service/src/generated/java"
rm -rf "$TARGET"
mkdir -p "$TARGET"
cp -R "$GEN_DIR/src/main/java/." "$TARGET/"

echo "==> Generating TypeScript types (client)"
if [ ! -d "$ROOT/client/node_modules" ]; then
  (cd "$ROOT/client" && npm ci)
fi
(cd "$ROOT/client" && npm run gen:api)

# TODO(#26): generate the Python client for the langchain-service with
# openapi-python-client once the backend->GenAI integration starts.

echo "==> Done. Review and commit the regenerated files."
