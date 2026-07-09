import pytest

from app.db import KnowledgeChunk
from app.retrieval import (
    MAX_CONTEXT_CHARS,
    NO_CONTEXT,
    render_retrieved_context,
    retrieve_context,
)
from app.settings import settings


def _chunk(source: str, content: str) -> KnowledgeChunk:
    return KnowledgeChunk(check_type="headers", source=source, title=source, content=content)


def test_render_retrieved_context_returns_sentinel_for_no_chunks():
    assert render_retrieved_context([]) == NO_CONTEXT


def test_render_retrieved_context_formats_source_and_content():
    chunks = [_chunk("OWASP A05:2021", "Do the thing.")]

    assert render_retrieved_context(chunks) == "- [OWASP A05:2021] Do the thing."


def test_render_retrieved_context_joins_multiple_chunks():
    chunks = [_chunk("Source A", "First."), _chunk("Source B", "Second.")]

    rendered = render_retrieved_context(chunks)

    assert "- [Source A] First." in rendered
    assert "- [Source B] Second." in rendered


def test_render_retrieved_context_caps_length():
    chunks = [_chunk("Source", "x" * (MAX_CONTEXT_CHARS * 2))]

    assert len(render_retrieved_context(chunks)) == MAX_CONTEXT_CHARS


@pytest.mark.anyio
async def test_retrieve_context_is_disabled_when_database_url_unset(monkeypatch):
    monkeypatch.setattr(settings, "database_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "key")
    monkeypatch.setattr(settings, "openai_api_key", "key")

    result = await retrieve_context("headers", "Missing CSP", "No CSP header found.")

    assert result == NO_CONTEXT


@pytest.mark.anyio
async def test_retrieve_context_is_disabled_when_no_embedding_key_configured(monkeypatch):
    monkeypatch.setattr(settings, "database_url", "postgresql+asyncpg://u:p@host/db")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "openai_api_key", "")

    result = await retrieve_context("headers", "Missing CSP", "No CSP header found.")

    assert result == NO_CONTEXT


@pytest.mark.anyio
async def test_retrieve_context_embeds_and_renders_query_results(monkeypatch):
    monkeypatch.setattr(settings, "database_url", "postgresql+asyncpg://u:p@host/db")
    monkeypatch.setattr(settings, "embedding_api_key", "key")

    async def fake_embed_text(text):
        return [0.1, 0.2]

    async def fake_query_similar(check, embedding, limit):
        assert check == "headers"
        assert embedding == [0.1, 0.2]
        return [_chunk("OWASP A05:2021", "Add a CSP header.")]

    monkeypatch.setattr("app.retrieval.embed_text", fake_embed_text)
    monkeypatch.setattr("app.retrieval._query_similar", fake_query_similar)

    result = await retrieve_context("headers", "Missing CSP", "No CSP header found.")

    assert result == "- [OWASP A05:2021] Add a CSP header."


@pytest.mark.anyio
async def test_retrieve_context_degrades_to_no_context_on_failure(monkeypatch):
    monkeypatch.setattr(settings, "database_url", "postgresql+asyncpg://u:p@host/db")
    monkeypatch.setattr(settings, "embedding_api_key", "key")

    async def failing_embed_text(text):
        raise RuntimeError("embedding provider unreachable")

    monkeypatch.setattr("app.retrieval.embed_text", failing_embed_text)

    result = await retrieve_context("headers", "Missing CSP", "No CSP header found.")

    assert result == NO_CONTEXT
