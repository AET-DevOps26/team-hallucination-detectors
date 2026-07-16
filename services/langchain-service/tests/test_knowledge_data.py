"""Guards the corpus properties retrieval quality depends on.

The scanner's finding-producing check types and per-check finding variants are
defined in Java (services/scanner-service .../checks/), so these tests can't
import them — the sets below are the contract, kept in sync by hand. If a new
check type or finding variant lands in the scanner, add its entries to
app/knowledge_data.py and update these sets in the same PR.
"""

from collections import Counter

from app.knowledge_data import KNOWLEDGE_ENTRIES
from app.retrieval import MAX_CONTEXT_CHARS

# Every ScanCheck enum value that can emit findings — `crawl` is page
# discovery and never produces one, so it needs no grounding content.
FINDING_PRODUCING_CHECK_TYPES = {
    "https",
    "headers",
    "adminPaths",
    "secrets",
    "sensitiveFiles",
    "cookies",
    "cors",
}


def test_every_finding_producing_check_type_is_covered():
    covered = {entry["check_type"] for entry in KNOWLEDGE_ENTRIES}

    assert covered == FINDING_PRODUCING_CHECK_TYPES


def test_each_check_type_has_at_least_two_entries():
    """With fewer candidates than MAX_CHUNKS per check type, similarity search
    degenerates into a keyed lookup — the whole point of the per-variant corpus
    is that retrieval has real choices to make."""
    counts = Counter(entry["check_type"] for entry in KNOWLEDGE_ENTRIES)

    for check_type in FINDING_PRODUCING_CHECK_TYPES:
        assert counts[check_type] >= 2, f"{check_type} has only {counts[check_type]} entries"


def test_entries_have_all_required_fields_filled():
    for entry in KNOWLEDGE_ENTRIES:
        assert set(entry) == {"check_type", "source", "title", "content"}
        for field, value in entry.items():
            assert value.strip(), f"empty {field} in entry {entry['title']!r}"


def test_titles_are_unique():
    """embed_knowledge.py upserts by title; a duplicate would silently
    overwrite another entry instead of inserting."""
    titles = [entry["title"] for entry in KNOWLEDGE_ENTRIES]

    assert len(titles) == len(set(titles))


def test_every_rendered_entry_fits_the_context_budget():
    """render_retrieved_context always includes the first chunk whole; that's
    only safe while every individual entry fits MAX_CONTEXT_CHARS."""
    for entry in KNOWLEDGE_ENTRIES:
        line = f"- [{entry['source']}] {entry['content']}"
        assert len(line) <= MAX_CONTEXT_CHARS, f"entry too long: {entry['title']!r}"


def test_entries_cite_a_recognizable_authority():
    """Provenance is the credibility feature: each entry must cite OWASP, CWE,
    MDN, or the relevant vendor's own security docs."""
    authorities = ("OWASP", "CWE-", "MDN", "Stripe", "AWS", "Supabase", "WordPress")

    for entry in KNOWLEDGE_ENTRIES:
        assert any(marker in entry["source"] for marker in authorities), (
            f"unrecognized source authority: {entry['source']!r}"
        )
