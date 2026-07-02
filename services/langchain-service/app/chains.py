from typing import Literal

from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from app.settings import settings


# The LLM backends VibeShield can talk to. Both are OpenAI-compatible, so they
# differ only by base URL, API key, and model name.
Provider = Literal["openai", "logos"]


class ProviderNotConfigured(Exception):
    """The selected provider has no API key configured (e.g. Logos key missing)."""

    def __init__(self, provider: str) -> None:
        self.provider = provider
        super().__init__(f"LLM provider '{provider}' is not configured.")


def _provider_config(provider: Provider) -> tuple[str, str, str]:
    """Return (api_key, base_url, model) for the given provider."""
    if provider == "logos":
        return (
            settings.logos_api_key,
            settings.logos_base_url,
            settings.logos_model_name,
        )
    return settings.openai_api_key, settings.openai_base_url, settings.model_name


def _model(provider: Provider = "openai", temperature: float = 0.2) -> ChatOpenAI:
    api_key, base_url, model = _provider_config(provider)
    if not api_key:
        raise ProviderNotConfigured(provider)
    return ChatOpenAI(
        model=model,
        api_key=api_key,
        base_url=base_url,
        temperature=temperature,
    )


def build_chat_chain(provider: Provider = "openai"):
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

    return prompt | _model(provider)


# VibeShield's core GenAI capability: turn one security finding into a single,
# ready-to-paste prompt the user can hand straight to the AI builder that made
# their site. The user is non-technical and never reads or writes code, so the
# output must be the prompt itself — nothing else.
FIX_PROMPT_SYSTEM = (
    "You are VibeShield's fix-prompt generator. The user is a non-technical "
    '"vibecoder" who built their website with an AI builder and does not read '
    "or write code. Given one security finding from a scan, write a single "
    "prompt the user can paste directly into their AI builder to fix the issue.\n\n"
    "The target AI builder is: {builder}.\n\n"
    "Tailor the prompt for that builder:\n"
    "- Lovable: conversational, reference the Lovable project editor.\n"
    "- Cursor: reference files/code directly, use precise technical language.\n"
    "- v0: focus on React/Next.js components and Vercel config.\n"
    "- Bolt: full-stack context, reference StackBlitz environment.\n"
    "- Replit: mention the Replit project and its file structure.\n"
    "- Generic: platform-agnostic, plain clear instructions.\n\n"
    "The prompt you write must:\n"
    "- Address the AI builder directly (second person), as an instruction.\n"
    "- State the security problem in plain, concrete language.\n"
    "- Say exactly what to change, and where, based on the finding.\n"
    "- Tell it to apply the fix safely without breaking existing functionality.\n"
    "- Be fully self-contained, so it works pasted on its own.\n\n"
    "Output ONLY the prompt text. No preamble, no sign-off, no commentary, and "
    "no surrounding markdown code fences."
)

FIX_PROMPT_HUMAN = (
    "Write the fix prompt for this finding:\n\n"
    "Title: {title}\n"
    "Severity: {severity}\n"
    "Check that flagged it: {check}\n"
    "Affected URL, file, route, or endpoint: {affected}\n"
    "What happened: {summary}\n"
    "Potential impact: {impact}"
)


def build_fix_prompt_chain(provider: Provider = "openai"):
    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", FIX_PROMPT_SYSTEM),
            ("human", FIX_PROMPT_HUMAN),
        ]
    )

    return prompt | _model(provider, temperature=0.3)
