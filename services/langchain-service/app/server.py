import uvicorn

from app.settings import settings


def start():
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.port,
        reload=settings.reload,
    )


if __name__ == "__main__":
    start()