from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "vibeshield-langchain-service"

    # Optional at startup so the service starts and /health responds even without
    # a key. Requests to /chat and /fix-prompt will fail with a clear 503 if unset.
    openai_api_key: str = ""
    model_name: str = "gpt-4o-mini"

    port: int = 8000
    reload: bool = False

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    @property
    def openai_configured(self) -> bool:
        return bool(self.openai_api_key and not self.openai_api_key.startswith("sk-proj-..."))


settings = Settings()