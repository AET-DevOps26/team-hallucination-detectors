"""Retrieval step for the fix-prompt generator's grounding context.

Looks up a couple of short, cited security write-ups relevant to a finding by
embedding the finding's own text and running a pgvector similarity search
scoped to its check type. This is additive grounding, not a hard dependency:
if the DB or embedding provider isn't configured, or the query fails for any
reason, retrieval degrades to an empty context rather than failing the
fix-prompt request (see the /fix-prompt handler in app/main.py).

Shared by all three LLM providers (openai/logos/selfhosted) — retrieval is one
provider-agnostic step upstream of provider selection, not a per-provider concern.
"""

import logging

from langchain_openai import OpenAIEmbeddings
from sqlalchemy import select

from app.db import KnowledgeChunk, get_session
from app.settings import settings

logger = logging.getLogger(__name__)

MAX_CHUNKS = 2
# Keeps the injected context short: on the self-hosted CPU model, prefill time
# scales with input tokens, so this budget applies to every provider rather
# than excluding the slow one (see the RAG strategy memory for the reasoning).
MAX_CONTEXT_CHARS = 700

NO_CONTEXT = "(none)"


def _embeddings_client() -> OpenAIEmbeddings:
    return OpenAIEmbeddings(
        model=settings.embedding_model_name,
        api_key=settings.resolved_embedding_api_key,
        base_url=settings.embedding_base_url,
    )


async def embed_text(text: str) -> list[float]:
    return await _embeddings_client().aembed_query(text)


async def _query_similar(check: str, query_embedding: list[float], limit: int) -> list[KnowledgeChunk]:
    """Isolated from retrieve_context so tests can monkeypatch it instead of
    needing a live Postgres+pgvector instance."""
    async with get_session() as session:
        by_check = (
            select(KnowledgeChunk)
            .where(KnowledgeChunk.check_type == check)
            .order_by(KnowledgeChunk.embedding.cosine_distance(query_embedding))
            .limit(limit)
        )
        rows = list((await session.execute(by_check)).scalars().all())
        if rows:
            return rows

        # No entry for this exact check type (or check is unknown/blank): fall
        # back to a plain similarity search across everything instead of
        # returning nothing.
        any_check = (
            select(KnowledgeChunk)
            .order_by(KnowledgeChunk.embedding.cosine_distance(query_embedding))
            .limit(limit)
        )
        return list((await session.execute(any_check)).scalars().all())


def render_retrieved_context(chunks: list[KnowledgeChunk]) -> str:
    if not chunks:
        return NO_CONTEXT
    lines = [f"- [{chunk.source}] {chunk.content}" for chunk in chunks]
    return "\n".join(lines)[:MAX_CONTEXT_CHARS]


async def retrieve_context(check: str, title: str, summary: str) -> str:
    """Return a short, cited context block for the given finding, or the
    NO_CONTEXT sentinel if retrieval isn't configured, finds nothing, or fails."""
    if not settings.database_url or not settings.resolved_embedding_api_key:
        return NO_CONTEXT

    query = f"{check}: {title}. {summary}".strip()

    try:
        embedding = await embed_text(query)
        chunks = await _query_similar(check, embedding, MAX_CHUNKS)
    except Exception:
        logger.warning("RAG retrieval failed; continuing without grounding context.", exc_info=True)
        return NO_CONTEXT

    return render_retrieved_context(chunks)
