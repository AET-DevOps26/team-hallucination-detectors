from typing import Literal

from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from app.settings import settings


# The LLM backends VibeShield can talk to. All are OpenAI-compatible, so they
# differ only by base URL, API key, and model name:
# - "openai":     OpenAI's own API.
# - "logos":      the TUM Logos gateway provided by the course.
# - "selfhosted": a self-hosted model we run ourselves (Ollama), reached in-cluster
#                 at http://ollama:11434/v1. Ollama ignores the API key, but a
#                 non-empty dummy is still required by the OpenAI client.
Provider = Literal["openai", "logos", "selfhosted"]


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
    if provider == "selfhosted":
        return (
            settings.selfhosted_api_key,
            settings.selfhosted_base_url,
            settings.selfhosted_model_name,
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


# Per-builder guidance for the fix-prompt generator. Keyed by the same builder
# names the client's AI-builder selector uses (see AI_BUILDERS in
# client/src/pages/AnalysisDetailPage.tsx), so a request's `builder` field maps
# straight onto a profile with no translation layer. This is what makes builder
# selection actually change the generated prompt, instead of a cosmetic label.
BUILDER_PROFILES: dict[str, dict[str, object]] = {
    "Lovable": {
        "tone": "conversational, non-technical, project-editor oriented",
        "output_style": "a single conversational instruction",
        "must_include": (
            "ask it to inspect the relevant app page or route",
            "tell it to preserve the existing visual design",
        ),
        "avoid": ("naming file paths unless the finding already names one",),
    },
    "Cursor": {
        "tone": "technical, precise",
        "output_style": "an implementation prompt that references files and tests",
        "must_include": (
            "ask it to identify the likely file(s) first",
            "ask it to add or update a test that covers the fix",
        ),
        "avoid": ("vague UI-only instructions with no code angle",),
    },
    "v0": {
        "tone": "frontend-focused, React/Next.js vocabulary",
        "output_style": "an instruction focused on the relevant component or config",
        "must_include": (
            "reference the relevant React/Next.js component or config",
            "mention Vercel deployment settings if the finding relates to them",
        ),
        "avoid": ("backend- or database-only instructions with no UI angle",),
    },
    "Bolt": {
        "tone": "full-stack, project-environment oriented",
        "output_style": "an instruction spanning the whole StackBlitz project",
        "must_include": (
            "mention the relevant part of the project structure",
            "consider both client and server code where relevant",
        ),
        "avoid": ("assuming a single-file fix when the issue may span layers",),
    },
    "Replit": {
        "tone": "project/file-structure oriented",
        "output_style": "an instruction referencing the Replit project layout",
        "must_include": (
            "mention the relevant file or folder in the project",
            "tell it to use Replit's Secrets tool for any credential fix",
        ),
        "avoid": ("assuming a build step Replit's runtime doesn't have",),
    },
    "Generic": {
        "tone": "plain, platform-agnostic, non-technical",
        "output_style": "clear step-by-step instructions with no platform-specific vocabulary",
        "must_include": ("state the fix in plain language a non-developer can follow",),
        "avoid": ("naming a specific tool, IDE, or platform feature",),
    },
}


def render_builder_guidance(builder: str) -> str:
    """Render one builder's profile into the guidance line the system prompt embeds.

    Falls back to the Generic profile for any name outside BUILDER_PROFILES —
    callers are expected to have already normalized the builder (see
    app.guardrails.normalize_builder), but this keeps the function safe on its own.
    """
    profile = BUILDER_PROFILES.get(builder, BUILDER_PROFILES["Generic"])
    must_include = "; ".join(profile["must_include"])
    avoid = "; ".join(profile["avoid"])
    return (
        f"Tone: {profile['tone']}. Output style: {profile['output_style']}. "
        f"Must include: {must_include}. Avoid: {avoid}."
    )


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
    "Tailor the prompt specifically for that builder using this guidance:\n"
    "{builder_guidance}\n\n"
    "The prompt you write must:\n"
    "- Address the AI builder directly (second person), as an instruction.\n"
    "- State the security problem in plain, concrete language.\n"
    "- Say exactly what to change, and where, based on the finding.\n"
    "- Tell it to apply the fix safely without breaking existing functionality.\n"
    "- Be fully self-contained, so it works pasted on its own.\n\n"
    "Safety and accuracy rules:\n"
    "- Never invent specific files, routes, packages, frameworks, or APIs that "
    "the finding did not mention.\n"
    "- If the exact file or component isn't known, tell it to inspect the "
    "relevant route, page, or component instead of guessing a path.\n"
    "- Never suggest disabling, weakening, or bypassing a security control "
    "(e.g. turning off HTTPS, disabling a CSP, ignoring certificate errors) as "
    "a way to make the problem go away.\n"
    "- Never suggest hiding, suppressing, or ignoring the finding instead of "
    "fixing it.\n"
    "- Preserve existing UI and behavior; only change what the fix requires.\n"
    "- Always end with a concrete step to verify the fix worked.\n\n"
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
