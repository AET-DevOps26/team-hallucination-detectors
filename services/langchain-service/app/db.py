"""Async SQLAlchemy engine/session for the RAG knowledge base (app/retrieval.py).

langchain-service otherwise has no database of its own. This is scoped to its
own schema in the shared vibeshield Postgres, matching the schema-per-service
isolation the Java services already use (see api-service's application.yml).
"""

import logging

from pgvector.sqlalchemy import Vector
from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.types import String, Text

from app.settings import settings

logger = logging.getLogger(__name__)

SCHEMA_NAME = "langchain_service"
# Dimensionality of settings.embedding_model_name (text-embedding-3-small).
# Changing the embedding model means changing this and re-embedding everything.
EMBEDDING_DIMENSIONS = 1536


class Base(DeclarativeBase):
    pass


class KnowledgeChunk(Base):
    """One citable security write-up chunk, keyed to a scanner check type
    (see ScanOption in client/src/types/domain.ts) for the fix-prompt RAG step."""

    __tablename__ = "fix_prompt_knowledge"
    __table_args__ = {"schema": SCHEMA_NAME}

    id: Mapped[int] = mapped_column(primary_key=True)
    check_type: Mapped[str] = mapped_column(String(64), index=True)
    source: Mapped[str] = mapped_column(String(200))
    title: Mapped[str] = mapped_column(String(200), unique=True)
    content: Mapped[str] = mapped_column(Text)
    embedding: Mapped[list[float]] = mapped_column(Vector(EMBEDDING_DIMENSIONS))


_engine: AsyncEngine | None = None
_session_maker: async_sessionmaker[AsyncSession] | None = None


def _session_factory() -> async_sessionmaker[AsyncSession] | None:
    """None when DATABASE_URL isn't configured; callers treat that as "RAG is off"."""
    global _engine, _session_maker
    if not settings.database_url:
        return None
    if _session_maker is None:
        _engine = create_async_engine(settings.database_url)
        _session_maker = async_sessionmaker(_engine, expire_on_commit=False)
    return _session_maker


def get_session() -> AsyncSession:
    """Raises if DATABASE_URL isn't configured; callers must check settings.database_url first."""
    factory = _session_factory()
    if factory is None:
        raise RuntimeError("DATABASE_URL is not configured.")
    return factory()


async def ensure_schema() -> None:
    """Create the schema/extension/table if they don't exist yet.

    Safe to call on every startup: every statement here is idempotent. Logs
    and returns (rather than raising) on failure, so a misconfigured or
    unreachable database doesn't stop the rest of the service from starting —
    RAG is additive grounding, not a hard dependency.
    """
    factory = _session_factory()
    if factory is None:
        logger.info("DATABASE_URL not configured; skipping RAG schema setup.")
        return

    try:
        async with _engine.begin() as conn:
            await conn.exec_driver_sql(f"CREATE SCHEMA IF NOT EXISTS {SCHEMA_NAME}")
            await conn.exec_driver_sql("CREATE EXTENSION IF NOT EXISTS vector")
            await conn.run_sync(Base.metadata.create_all)
    except Exception:
        logger.warning("Could not set up the RAG schema; retrieval will stay disabled.", exc_info=True)
