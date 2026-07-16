import pytest

from app.db import KnowledgeChunk
from app.retrieval import (
    MAX_CONTEXT_CHARS,
    NO_CONTEXT,
    _embedding_cache,
    embed_text,
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


def test_render_retrieved_context_drops_whole_chunk_over_budget_instead_of_slicing():
    """A second chunk that doesn't fully fit is dropped, not cut mid-sentence —
    a truncated security instruction can invert its own meaning."""
    first = _chunk("Source A", "x" * (MAX_CONTEXT_CHARS - 30))
    second = _chunk("Source B", "This whole sentence would be sliced apart.")

    rendered = render_retrieved_context([first, second])

    assert "Source A" in rendered
    assert "Source B" not in rendered


@pytest.mark.anyio
async def test_embed_text_caches_repeated_queries(monkeypatch):
    """Finding text comes from a closed set of scanner templates, so repeat
    queries must not pay the embedding API round-trip again."""
    _embedding_cache.clear()
    calls = {"count": 0}

    class FakeEmbeddings:
        async def aembed_query(self, text):
            calls["count"] += 1
            return [0.1, 0.2, 0.3]

    monkeypatch.setattr("app.retrieval._embeddings_client", lambda: FakeEmbeddings())

    first = await embed_text("headers: Missing CSP. No CSP header found.")
    second = await embed_text("headers: Missing CSP. No CSP header found.")
    await embed_text("a different finding")

    assert first == second == [0.1, 0.2, 0.3]
    assert calls["count"] == 2
    _embedding_cache.clear()


@pytest.mark.anyio
async def test_embed_text_cache_misses_when_embedding_config_changes(monkeypatch):
    """A cached vector from one embedding model must never be served for
    another — the key includes the full embedding config."""
    _embedding_cache.clear()
    calls = {"count": 0}

    class FakeEmbeddings:
        async def aembed_query(self, text):
            calls["count"] += 1
            return [0.1]

    monkeypatch.setattr("app.retrieval._embeddings_client", lambda: FakeEmbeddings())

    monkeypatch.setattr(settings, "embedding_model_name", "model-a")
    await embed_text("same text")
    monkeypatch.setattr(settings, "embedding_model_name", "model-b")
    await embed_text("same text")

    assert calls["count"] == 2
    _embedding_cache.clear()


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
