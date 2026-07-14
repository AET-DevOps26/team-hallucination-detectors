import pytest

from app.guardrails import (
    MAX_PROMPT_LENGTH,
    normalize_builder,
    normalize_severity,
    validate_fix_prompt,
    validate_title,
)


@pytest.mark.parametrize(
    "raw,expected",
    [
        ("high", "High"),
        ("HIGH", "High"),
        ("  Critical  ", "Critical"),
        ("info", "Info"),
    ],
)
def test_normalize_severity_matches_case_insensitively(raw, expected):
    assert normalize_severity(raw) == expected


def test_normalize_severity_rejects_unknown_value():
    with pytest.raises(ValueError):
        normalize_severity("super-urgent")


@pytest.mark.parametrize(
    "raw,expected",
    [
        ("cursor", "Cursor"),
        ("  Lovable ", "Lovable"),
        ("V0", "v0"),
    ],
)
def test_normalize_builder_matches_case_insensitively(raw, expected):
    assert normalize_builder(raw) == expected


def test_normalize_builder_falls_back_to_generic_for_unknown_value():
    assert normalize_builder("SomeToolThatDoesNotExist") == "Generic"


def test_normalize_builder_falls_back_to_generic_for_empty_value():
    assert normalize_builder("") == "Generic"


def test_validate_title_rejects_empty():
    with pytest.raises(ValueError):
        validate_title("   ")


def test_validate_title_rejects_pure_symbols():
    with pytest.raises(ValueError):
        validate_title("!!!")


def test_validate_title_accepts_real_title():
    assert validate_title("  Missing security headers  ") == "Missing security headers"


def test_validate_fix_prompt_accepts_a_clean_prompt():
    prompt = (
        "Add the Content-Security-Policy header to your https://example.com/ "
        "site. After the change, verify the header appears in the response."
    )

    assert validate_fix_prompt(prompt, "https://example.com/") == []


def test_validate_fix_prompt_rejects_empty_prompt():
    assert validate_fix_prompt("   ", "https://example.com/") == ["prompt is empty"]


def test_validate_fix_prompt_rejects_over_max_length():
    prompt = ("Verify this. " * (MAX_PROMPT_LENGTH // 10))

    problems = validate_fix_prompt(prompt, "n/a")

    assert "prompt exceeds max length" in problems


def test_validate_fix_prompt_rejects_markdown_fences():
    prompt = "Verify this works.\n```js\nconsole.log('fix')\n```"

    problems = validate_fix_prompt(prompt, "n/a")

    assert "prompt contains markdown code fences" in problems


def test_validate_fix_prompt_rejects_missing_verification_step():
    prompt = "Add the missing security header to your site."

    problems = validate_fix_prompt(prompt, "n/a")

    assert "prompt has no verification step" in problems


def test_validate_fix_prompt_rejects_dangerous_instruction():
    prompt = "Just disable HTTPS for this route, then verify it loads."

    problems = validate_fix_prompt(prompt, "n/a")

    assert any("dangerous instruction" in reason for reason in problems)


def test_validate_fix_prompt_allows_dangerous_phrase_already_in_the_finding():
    prompt = "The site currently has HTTPS disabled; enable it and verify it loads over https."
    finding_context = "https is disabled on the login page"

    problems = validate_fix_prompt(prompt, "n/a", finding_context)

    assert not any("dangerous instruction" in reason for reason in problems)


def test_validate_fix_prompt_requires_target_reference_when_affected_is_known():
    prompt = "Add a Content-Security-Policy header somewhere. Then verify it works."

    problems = validate_fix_prompt(prompt, "https://example.com/checkout")

    assert "prompt does not reference the affected target" in problems


def test_validate_fix_prompt_skips_target_check_when_affected_is_unknown():
    prompt = "Add a Content-Security-Policy header to the affected page. Then verify it works."

    problems = validate_fix_prompt(prompt, "n/a")

    assert "prompt does not reference the affected target" not in problems
