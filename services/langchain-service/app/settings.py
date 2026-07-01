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
    logos_model_name: str = "gpt-4o-mini"

    port: int = 8000
    reload: bool = False

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


settings = Settings()
