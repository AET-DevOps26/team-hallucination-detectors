from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "vibeshield-langchain-service"

    # OpenAI (openai.com) provider. Keys are optional so the service can run
    # against a single provider — the endpoint returns a clean 503 if a caller
    # selects a provider whose key was never configured.
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    model_name: str = "gpt-4o-mini"  # OpenAI model (env: MODEL_NAME)

    # TUM Logos gateway — OpenAI-compatible, provided by the course.
    logos_api_key: str = ""
    logos_base_url: str = "https://logos.aet.cit.tum.de/v1"
    logos_model_name: str = "openai/gpt-oss-120b"

    # Self-hosted model we run ourselves (Ollama), OpenAI-compatible. The key is a
    # dummy Ollama ignores — it defaults non-empty so the provider counts as
    # "configured" wherever the Ollama runtime is deployed. Blank it to disable
    # the provider (callers then get the standard 503 PROVIDER_NOT_CONFIGURED).
    selfhosted_api_key: str = "ollama"
    selfhosted_base_url: str = "http://ollama:11434/v1"
    selfhosted_model_name: str = "llama3.2:3b"

    # Shared HMAC secret used to VALIDATE the JWTs the auth-service issues — must
    # match APP_JWT_SECRET across all services (see docs/auth.md). Env: APP_JWT_SECRET.
    # Same dev default as docker-compose so the compose stack authenticates out of
    # the box; deployed environments inject the real secret.
    app_jwt_secret: str = "dev-secret-change-me-minimum-32-chars-required"

    port: int = 8000
    reload: bool = False

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


settings = Settings()
