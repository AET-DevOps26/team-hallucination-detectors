"""Guardrails around the fix-prompt endpoint: normalizing/validating the
request before it reaches the LLM, and checking the LLM's output before it
reaches the user. Kept separate from the FastAPI wiring in main.py and from
prompt/chain construction in chains.py.
"""

import re

from app.chains import BUILDER_PROFILES

KNOWN_SEVERITIES = ("Critical", "High", "Medium", "Low", "Info")
DEFAULT_BUILDER = "Generic"

MAX_TITLE_LENGTH = 300
MAX_SHORT_FIELD_LENGTH = 300
MAX_LONG_FIELD_LENGTH = 2000
MAX_PROMPT_LENGTH = 4000

_VERIFICATION_MARKERS = (
    "verify",
    "confirm",
    "make sure",
    "check that",
    "double-check",
    "double check",
)

# Phrases that mean "weaken or remove a security control." A finding is
# allowed to mention its own broken state (e.g. a title of "CSP is disabled")
# without tripping this — see the `finding_context` param on validate_fix_prompt.
_DANGEROUS_PHRASES = (
    "disable csp",
    "disable content security policy",
    "turn off https",
    "disable https",
    "ignore certificate errors",
    "ignore ssl errors",
    "disable ssl verification",
    "disable tls verification",
    "turn off authentication",
    "disable authentication",
    "hardcode the credentials",
    "commit the secret",
)


class FixPromptGenerationFailed(Exception):
    """Raised when a generated fix prompt still fails validation after one retry."""

    def __init__(self, reasons: list[str]) -> None:
        self.reasons = reasons
        super().__init__(f"Fix prompt failed validation: {reasons}")


def normalize_severity(value: str) -> str:
    """Match a severity case-insensitively against the known set, or raise."""
    stripped = (value or "").strip()
    for known in KNOWN_SEVERITIES:
        if known.lower() == stripped.lower():
            return known
    raise ValueError(f"severity must be one of {KNOWN_SEVERITIES}")


def normalize_builder(value: str) -> str:
    """Match a builder case-insensitively against BUILDER_PROFILES, else Generic.

    Unlike severity, an unrecognized builder isn't rejected outright: the
    builder is a UI convenience, not safety-relevant, so degrading to Generic
    keeps the request working instead of hard-failing on a minor mismatch.
    """
    stripped = (value or "").strip()
    for known in BUILDER_PROFILES:
        if known.lower() == stripped.lower():
            return known
    return DEFAULT_BUILDER


def validate_title(value: str) -> str:
    """Reject empty or low-quality titles (e.g. pure symbols/whitespace)."""
    stripped = (value or "").strip()
    if len(stripped) < 3 or not re.search(r"[A-Za-z]", stripped):
        raise ValueError("title must be a real finding description, not empty or placeholder text.")
    return stripped


def _has_verification_step(prompt: str) -> bool:
    lowered = prompt.lower()
    return any(marker in lowered for marker in _VERIFICATION_MARKERS)


def _dangerous_phrase(prompt: str) -> str | None:
    lowered = prompt.lower()
    for phrase in _DANGEROUS_PHRASES:
        if phrase in lowered:
            return phrase
    return None


def _references_target(prompt: str, affected: str) -> bool:
    lowered_prompt = prompt.lower()
    if not affected or affected == "n/a":
        # Nothing concrete to reference; the system prompt already tells the
        # model to say the target is unknown rather than invent one.
        return True
    tokens = [t for t in re.split(r"[/\s:?#&=.]+", affected.lower()) if len(t) >= 4]
    if not tokens:
        return True
    return any(token in lowered_prompt for token in tokens)


def validate_fix_prompt(prompt: str, affected: str, finding_context: str = "") -> list[str]:
    """Check a generated fix prompt; return the list of problems (empty = passes).

    `finding_context` is the lowercased title/check/summary/impact of the
    finding itself, so a dangerous-sounding phrase that only restates the
    finding's own broken state isn't flagged as the model suggesting it.
    """
    reasons: list[str] = []

    if not prompt.strip():
        reasons.append("prompt is empty")
        return reasons

    if len(prompt) > MAX_PROMPT_LENGTH:
        reasons.append("prompt exceeds max length")

    if "```" in prompt:
        reasons.append("prompt contains markdown code fences")

    if not _has_verification_step(prompt):
        reasons.append("prompt has no verification step")

    dangerous = _dangerous_phrase(prompt)
    if dangerous and dangerous not in finding_context.lower():
        reasons.append(f"prompt contains a dangerous instruction ('{dangerous}')")

    if not _references_target(prompt, affected):
        reasons.append("prompt does not reference the affected target")

    return reasons
