"""Retrieval step for the fix-prompt generator's grounding context.

Looks up a couple of short, cited security write-ups relevant to a finding by
embedding the finding's own text and running a pgvector similarity search over
the whole knowledge base, with a score boost (not a hard filter) for chunks
tagged with the finding's check type. This is additive grounding, not a hard
dependency: if the DB or embedding provider isn't configured, or the query
fails for any reason, retrieval degrades to an empty context rather than
failing the fix-prompt request (see the /fix-prompt handler in app/main.py).

Shared by all three LLM providers (openai/logos/selfhosted) — retrieval is one
provider-agnostic step upstream of provider selection, not a per-provider concern.
"""

import logging
from collections import OrderedDict

from langchain_openai import OpenAIEmbeddings
from sqlalchemy import case, select

from app.db import KnowledgeChunk, get_session
from app.settings import settings

logger = logging.getLogger(__name__)

MAX_CHUNKS = 2
# Keeps the injected context short: on the self-hosted CPU model, prefill time
# scales with input tokens, so this budget applies to every provider rather
# than excluding the slow one (see the RAG strategy memory for the reasoning).
MAX_CONTEXT_CHARS = 1000

# Subtracted from a chunk's cosine distance when its check_type matches the
# finding's, so same-check chunks win ties but a clearly closer chunk from a
# related check (cookie Secure flag ↔ https, cors ↔ headers) can still be
# retrieved. Cosine distances between the curated chunks and real finding text
# cluster roughly around 0.3–0.6 for on-topic pairs and 0.7+ for unrelated
# ones, so 0.15 is a preference, not a partition — unlike the hard
# `WHERE check_type = ?` filter this replaced, which made the embedding
# irrelevant whenever a check type had no more chunks than MAX_CHUNKS.
CHECK_TYPE_BOOST = 0.15

NO_CONTEXT = "(none)"

# Finding titles/summaries come from a small closed set of scanner templates
# (see the checks in services/scanner-service), so identical queries recur
# constantly — across rescans of the same site and across users with the same
# finding. Caching the query embedding removes the per-request embedding API
# round-trip for every repeat. Keyed by the full embedding config so flipping
# models or endpoints never serves stale vectors.
_EMBEDDING_CACHE_MAX = 512
_embedding_cache: OrderedDict[tuple[str, str, str], list[float]] = OrderedDict()


def _embeddings_client() -> OpenAIEmbeddings:
    return OpenAIEmbeddings(
        model=settings.embedding_model_name,
        api_key=settings.resolved_embedding_api_key,
        base_url=settings.embedding_base_url,
    )


async def embed_text(text: str) -> list[float]:
    key = (settings.embedding_model_name, settings.embedding_base_url, text)
    cached = _embedding_cache.get(key)
    if cached is not None:
        _embedding_cache.move_to_end(key)
        return cached

    embedding = await _embeddings_client().aembed_query(text)

    _embedding_cache[key] = embedding
    if len(_embedding_cache) > _EMBEDDING_CACHE_MAX:
        _embedding_cache.popitem(last=False)
    return embedding


async def _query_similar(check: str, query_embedding: list[float], limit: int) -> list[KnowledgeChunk]:
    """Isolated from retrieve_context so tests can monkeypatch it instead of
    needing a live Postgres+pgvector instance."""
    distance = KnowledgeChunk.embedding.cosine_distance(query_embedding)
    check_boost = case((KnowledgeChunk.check_type == check, CHECK_TYPE_BOOST), else_=0.0)

    async with get_session() as session:
        stmt = select(KnowledgeChunk).order_by(distance - check_boost).limit(limit)
        return list((await session.execute(stmt)).scalars().all())


def render_retrieved_context(chunks: list[KnowledgeChunk]) -> str:
    """Render whole chunks until the budget is spent — never slice one
    mid-sentence, since a truncated security instruction can invert its own
    meaning. The first chunk is always included (hard-truncated only if it
    alone exceeds the budget, which the curated corpus never does)."""
    if not chunks:
        return NO_CONTEXT

    lines: list[str] = []
    used = 0
    for chunk in chunks:
        line = f"- [{chunk.source}] {chunk.content}"
        if lines and used + 1 + len(line) > MAX_CONTEXT_CHARS:
            break
        lines.append(line)
        used += len(line) + 1

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
