from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from app.settings import settings


def build_chat_chain():
    model = ChatOpenAI(
        model=settings.model_name,
        api_key=settings.openai_api_key,
        temperature=0.2,
    )

    prompt = ChatPromptTemplate.from_messages(
        [
            (
                "system",
                "You are a helpful AI microservice for the Vibeshield app. "
                "Answer clearly, safely, and concisely.",
            ),
            ("human", "{message}"),
        ]
    )

    return prompt | model