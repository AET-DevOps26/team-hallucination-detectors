from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "vibeshield-langchain-service"

    openai_api_key: str
    model_name: str = "gpt-4o-mini"

    port: int = 8000
    reload: bool = False

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


settings = Settings()