from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from app.settings import settings


def _model(temperature: float = 0.2) -> ChatOpenAI:
    return ChatOpenAI(
        model=settings.model_name,
        api_key=settings.openai_api_key,
        temperature=temperature,
    )


def build_chat_chain():
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

    return prompt | _model()


# VibeShield's core GenAI capability: turn one security finding into a single,
# ready-to-paste prompt the user can hand straight to the AI builder that made
# their site. The user is non-technical and never reads or writes code, so the
# output must be the prompt itself — nothing else.
FIX_PROMPT_SYSTEM = (
    "You are VibeShield's fix-prompt generator. The user is a non-technical "
    '"vibecoder" who built their website with an AI builder (such as Lovable, '
    "Cursor, v0, Bolt, or Replit) and does not read or write code. Given one "
    "security finding from a scan, write a single prompt the user can paste "
    "directly into their AI builder to fix or verify the issue.\n\n"
    "The prompt you write must:\n"
    "- Address the AI builder directly (second person), as an instruction.\n"
    "- State the security problem in plain, concrete language.\n"
    "- Include the exact affected target.\n"
    "- State the required secure behavior.\n"
    "- Say exactly what to change, and where, based on the finding and guidance.\n"
    "- Tell it to apply the fix safely without breaking existing functionality.\n"
    "- Include verification steps the user can run after the change.\n"
    "- Include rollback guidance when the change can break existing behavior.\n"
    "- Be fully self-contained, so it works pasted on its own.\n\n"
    "If the requested mode is Verification only, do not ask for implementation "
    "changes. Ask only for checks that confirm whether the finding is fixed.\n\n"
    "Output ONLY the prompt text. No preamble, no sign-off, no commentary, and "
    "no surrounding markdown code fences."
)

FIX_PROMPT_HUMAN = (
    "Write the fix prompt for this finding:\n\n"
    "Selected AI builder: {builder}\n"
    "Prompt mode: {mode}\n"
    "Title: {title}\n"
    "Severity: {severity}\n"
    "Check that flagged it: {check}\n"
    "Affected URL, file, route, or endpoint: {affected}\n"
    "What happened: {summary}\n"
    "Potential impact: {impact}\n\n"
    "Deterministic remediation guidance:\n"
    "Issue type: {issue_type}\n"
    "Required secure behavior: {secure_behavior}\n"
    "Likely files, settings, or routes involved: {likely_targets}\n"
    "Implementation notes: {implementation_notes}\n"
    "Verification steps to include: {verification_steps}\n"
    "Rollback note: {rollback_note}\n"
    "Risk warning: {risk_note}\n"
    "Builder-specific guidance: {builder_guidance}\n"
    "Mode-specific guidance: {mode_guidance}\n"
    "Previous scan context: {rescan_guidance}"
)


def build_fix_prompt_chain():
    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", FIX_PROMPT_SYSTEM),
            ("human", FIX_PROMPT_HUMAN),
        ]
    )

    return prompt | _model(temperature=0.3)
