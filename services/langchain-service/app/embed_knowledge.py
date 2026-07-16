"""Embed app.knowledge_data.KNOWLEDGE_ENTRIES and upsert them into Postgres.

Run manually, once DATABASE_URL and an embedding provider key are configured
(the compose/k8s defaults already point at the shared vibeshield Postgres and
reuse OPENAI_API_KEY - see app/settings.py):

    cd services/langchain-service
    python -m app.embed_knowledge

Safe to re-run: entries are matched by title and updated in place, so editing
knowledge_data.py and re-running keeps the table in sync.
"""

import asyncio

from sqlalchemy import select

from app.db import KnowledgeChunk, ensure_schema, get_session
from app.knowledge_data import KNOWLEDGE_ENTRIES
from app.retrieval import embed_text


async def main() -> None:
    await ensure_schema()

    async with get_session() as session:
        for entry in KNOWLEDGE_ENTRIES:
            # Title + content, matching the shape of the retrieval query
            # ("check: title. summary" — see retrieve_context): the entry title
            # names the finding variant, so including it is what lets e.g. a
            # Referrer-Policy finding rank the Referrer-Policy chunk above the
            # four other `headers` chunks.
            embedding = await embed_text(f"{entry['title']}. {entry['content']}")

            existing = await session.execute(
                select(KnowledgeChunk).where(KnowledgeChunk.title == entry["title"])
            )
            chunk = existing.scalar_one_or_none()

            if chunk is None:
                session.add(KnowledgeChunk(**entry, embedding=embedding))
            else:
                chunk.check_type = entry["check_type"]
                chunk.source = entry["source"]
                chunk.content = entry["content"]
                chunk.embedding = embedding

            print(f"embedded: {entry['title']}")

        await session.commit()


if __name__ == "__main__":
    asyncio.run(main())
