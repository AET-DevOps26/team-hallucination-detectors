import pytest

from app.chains import (
    BUILDER_PROFILES,
    FIX_PROMPT_SYSTEM,
    ProviderNotConfigured,
    _model,
    _provider_config,
    build_chat_chain,
    build_fix_prompt_chain,
    render_builder_guidance,
)
from app.settings import settings


def test_provider_config_openai_reads_openai_settings(monkeypatch):
    monkeypatch.setattr(settings, "openai_api_key", "openai-key")
    monkeypatch.setattr(settings, "openai_base_url", "https://openai.example/v1")
    monkeypatch.setattr(settings, "model_name", "gpt-test")

    api_key, base_url, model = _provider_config("openai")

    assert (api_key, base_url, model) == ("openai-key", "https://openai.example/v1", "gpt-test")


def test_provider_config_logos_reads_logos_settings(monkeypatch):
    monkeypatch.setattr(settings, "logos_api_key", "logos-key")
    monkeypatch.setattr(settings, "logos_base_url", "https://logos.example/v1")
    monkeypatch.setattr(settings, "logos_model_name", "logos-model")

    api_key, base_url, model = _provider_config("logos")

    assert (api_key, base_url, model) == ("logos-key", "https://logos.example/v1", "logos-model")


def test_model_raises_provider_not_configured_when_key_missing():
    with pytest.raises(ProviderNotConfigured) as exc_info:
        _model("openai")

    assert exc_info.value.provider == "openai"


def test_model_raises_for_logos_when_key_missing():
    with pytest.raises(ProviderNotConfigured) as exc_info:
        _model("logos")

    assert exc_info.value.provider == "logos"


def test_model_builds_chat_openai_from_settings(monkeypatch):
    monkeypatch.setattr(settings, "openai_api_key", "openai-key")
    monkeypatch.setattr(settings, "openai_base_url", "https://openai.example/v1")
    monkeypatch.setattr(settings, "model_name", "gpt-test")

    model = _model("openai", temperature=0.5)

    assert model.model_name == "gpt-test"
    assert model.openai_api_base == "https://openai.example/v1"
    assert model.openai_api_key.get_secret_value() == "openai-key"
    assert model.temperature == 0.5


def test_build_chat_chain_wires_prompt_and_model(monkeypatch):
    monkeypatch.setattr(settings, "openai_api_key", "openai-key")

    chain = build_chat_chain("openai")

    assert "{message}" in chain.first.messages[1].prompt.template
    assert chain.last.temperature == 0.2


def test_build_chat_chain_propagates_missing_provider():
    with pytest.raises(ProviderNotConfigured):
        build_chat_chain("openai")


def test_build_fix_prompt_chain_wires_builder_placeholder_and_temperature(monkeypatch):
    monkeypatch.setattr(settings, "openai_api_key", "openai-key")

    chain = build_fix_prompt_chain("openai")

    system_template = chain.first.messages[0].prompt.template
    human_template = chain.first.messages[1].prompt.template
    assert "{builder}" in system_template
    assert "{builder_guidance}" in system_template
    assert "{title}" in human_template
    assert "{severity}" in human_template
    assert chain.last.temperature == 0.3


def test_build_fix_prompt_chain_propagates_missing_provider():
    with pytest.raises(ProviderNotConfigured):
        build_fix_prompt_chain("logos")


@pytest.mark.parametrize("builder", ["Lovable", "Cursor", "v0", "Bolt", "Replit", "Generic"])
def test_render_builder_guidance_covers_every_known_builder(builder):
    guidance = render_builder_guidance(builder)

    profile = BUILDER_PROFILES[builder]
    assert profile["tone"] in guidance
    assert profile["output_style"] in guidance
    for must_include in profile["must_include"]:
        assert must_include in guidance


def test_render_builder_guidance_falls_back_to_generic_for_unknown_builder():
    assert render_builder_guidance("SomeUnknownTool") == render_builder_guidance("Generic")


def test_fix_prompt_system_forbids_inventing_and_disabling_controls():
    assert "invent" in FIX_PROMPT_SYSTEM.lower()
    assert "disabling" in FIX_PROMPT_SYSTEM.lower() or "disable" in FIX_PROMPT_SYSTEM.lower()
    assert "verify" in FIX_PROMPT_SYSTEM.lower()
