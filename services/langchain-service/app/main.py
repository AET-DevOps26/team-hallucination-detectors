from fastapi import FastAPI
from pydantic import BaseModel, Field

from app.chains import build_chat_chain
from app.settings import settings


app = FastAPI(title=settings.app_name)

chat_chain = build_chat_chain()


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1)


class ChatResponse(BaseModel):
    response: str


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": settings.app_name,
    }


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    result = await chat_chain.ainvoke({"message": request.message})

    return ChatResponse(response=result.content)