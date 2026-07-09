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

    # RAG knowledge base (app/db.py, app/retrieval.py). Deliberately a single,
    # fixed config independent of which of the three LLM providers above
    # answers a given request — retrieval is one provider-agnostic step shared
    # by all three, not a per-provider concern. Empty database_url disables
    # retrieval entirely; it's additive grounding, not a hard dependency.
    database_url: str = ""
    embedding_api_key: str = ""  # falls back to openai_api_key if unset
    embedding_base_url: str = "https://api.openai.com/v1"
    embedding_model_name: str = "text-embedding-3-small"

    port: int = 8000
    reload: bool = False

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    @property
    def resolved_embedding_api_key(self) -> str:
        return self.embedding_api_key or self.openai_api_key


settings = Settings()
