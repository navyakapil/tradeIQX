import json
import os
from pathlib import Path
from typing import List, Dict, Any, Optional

import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

_model = None
_knowledge_base = None
_chunks = None
_embeddings = None

KB_PATH = Path(__file__).resolve().parent / "tradeiqx_knowledge.json"
CACHE_PATH = Path(__file__).resolve().parent / ".embeddings_cache.npy"

DEFAULT_TOP_K = int(os.environ.get("RAG_TOP_K", "3"))
DEFAULT_THRESHOLD = float(os.environ.get("RAG_THRESHOLD", "0.30"))


def get_knowledge_base() -> List[Dict[str, Any]]:
    global _knowledge_base
    if _knowledge_base is None:
        if not KB_PATH.exists():
            raise FileNotFoundError(f"TradeIQX knowledge base not found at {KB_PATH}")
        with open(KB_PATH, "r", encoding="utf-8") as file:
            _knowledge_base = json.load(file)
    return _knowledge_base


def chunk_text(text: str, max_chars: int = 600, overlap: int = 100) -> List[str]:
    """Splits text longer than max_chars with overlap at sentence or word boundaries."""
    if len(text) <= max_chars:
        return [text]

    chunks = []
    start = 0
    text_len = len(text)

    while start < text_len:
        end = start + max_chars
        if end >= text_len:
            chunk = text[start:].strip()
            if chunk:
                chunks.append(chunk)
            break

        split_pos = text.rfind(". ", start, end)
        if split_pos != -1 and split_pos >= start + (max_chars // 2):
            split_pos += 1
        else:
            split_pos = text.rfind(" ", start, end)
            if split_pos == -1 or split_pos <= start:
                split_pos = end

        chunk = text[start:split_pos].strip()
        if chunk:
            chunks.append(chunk)
        start = max(start + 1, split_pos - overlap)

    return chunks if chunks else [text]


def get_knowledge_chunks() -> List[Dict[str, Any]]:
    """Chunks knowledge base entries longer than ~600 chars with metadata preservation."""
    global _chunks
    if _chunks is None:
        kb = get_knowledge_base()
        chunks = []
        for item in kb:
            content = item.get("content", "").strip()
            topic = item.get("topic", "")
            category = item.get("category", "")
            keywords = item.get("keywords", [])
            text_chunks = chunk_text(content, max_chars=600, overlap=100)

            for idx, ch in enumerate(text_chunks):
                chunk_entry = item.copy()
                chunk_entry["content"] = ch
                chunk_entry["chunk_index"] = idx
                chunk_entry["total_chunks"] = len(text_chunks)
                kw_str = ", ".join(keywords) if isinstance(keywords, list) else str(keywords)
                chunk_entry["embed_text"] = f"Topic: {topic}. Category: {category}. Keywords: {kw_str}. Content: {ch}"
                chunks.append(chunk_entry)
        _chunks = chunks
    return _chunks


def get_model():
    global _model
    if _model is None:
        from sentence_transformers import SentenceTransformer
        _model = SentenceTransformer("all-MiniLM-L6-v2")
    return _model


def get_embeddings():
    """Returns knowledge base embeddings, loading from on-disk cache if available."""
    global _embeddings
    if _embeddings is None:
        chunks = get_knowledge_chunks()
        if CACHE_PATH.exists():
            try:
                cached = np.load(str(CACHE_PATH))
                if len(cached) == len(chunks):
                    _embeddings = cached
                    return _embeddings
            except Exception:
                pass

        model = get_model()
        texts = [chunk["embed_text"] for chunk in chunks]
        _embeddings = model.encode(texts, show_progress_bar=False)
        try:
            np.save(str(CACHE_PATH), _embeddings)
        except Exception:
            pass
    return _embeddings


def semantic_search(query: str, top_k: Optional[int] = None) -> List[Dict[str, Any]]:
    k = top_k if top_k is not None else DEFAULT_TOP_K
    model = get_model()
    embeddings = get_embeddings()
    chunks = get_knowledge_chunks()

    query_embedding = model.encode([query])
    similarities = cosine_similarity(query_embedding, embeddings)[0]
    top_indices = similarities.argsort()[::-1]

    results = []
    seen_topics = set()

    for index in top_indices:
        chunk = chunks[index]
        topic = chunk.get("topic", "")
        # Deduplicate by topic so results cover diverse relevant areas
        if topic in seen_topics:
            continue

        result = chunk.copy()
        result["similarity"] = float(similarities[index])
        results.append(result)
        seen_topics.add(topic)

        if len(results) >= k:
            break

    return results


def smart_search(
    query: str,
    threshold: Optional[float] = None,
    top_k: Optional[int] = None
) -> List[Dict[str, Any]]:
    min_score = threshold if threshold is not None else float(os.environ.get("RAG_THRESHOLD", str(DEFAULT_THRESHOLD)))
    k = top_k if top_k is not None else int(os.environ.get("RAG_TOP_K", str(DEFAULT_TOP_K)))

    # Fetch more candidates to ensure topic deduplication retains k quality matches
    candidate_k = max(k * 3, 10)
    results = semantic_search(query, top_k=candidate_k)
    filtered = [result for result in results if result["similarity"] >= min_score]
    return filtered[:k]


def personalized_search(
    query: str,
    difficulty: Optional[str] = None,
    threshold: Optional[float] = None,
    top_k: Optional[int] = None
) -> List[Dict[str, Any]]:
    results = smart_search(query, threshold=threshold, top_k=top_k)
    if difficulty:
        filtered_results = [
            result for result in results
            if result.get("difficulty", "").lower() == difficulty.lower()
        ]
        if filtered_results:
            results = filtered_results
    return results


def build_rag_context(
    query: str,
    threshold: Optional[float] = None,
    top_k: Optional[int] = None
) -> str:
    results = smart_search(query, threshold=threshold, top_k=top_k)
    if not results:
        return "No relevant TradeIQX knowledge was found."

    context_parts = []
    for result in results:
        context_parts.append(
            f"Topic: {result['topic']}\n"
            f"Category: {result.get('category', 'Finance')}\n"
            f"Difficulty: {result.get('difficulty', 'Beginner')}\n"
            f"Source: {result.get('source', 'TradeIQX Educational Knowledge Base')}\n"
            f"Similarity: {result['similarity']:.3f}\n"
            f"Content: {result['content']}"
        )

    return "\n\n---\n\n".join(context_parts)


def build_rag_context_with_scores(
    query: str,
    threshold: Optional[float] = None,
    top_k: Optional[int] = None
) -> Dict[str, Any]:
    results = smart_search(query, threshold=threshold, top_k=top_k)
    if not results:
        return {
            "status": "no_match",
            "confidence": 0.0,
            "context": "",
            "sources": []
        }

    best_score = float(results[0]["similarity"])
    context_parts = []
    sources = []

    for result in results:
        sources.append(result["topic"])
        context_parts.append(
            f"Topic: {result['topic']}\n"
            f"Category: {result.get('category', 'Finance')}\n"
            f"Difficulty: {result.get('difficulty', 'Beginner')}\n"
            f"Similarity: {result['similarity']:.3f}\n"
            f"Content: {result['content']}"
        )

    return {
        "status": "success",
        "confidence": best_score,
        "context": "\n\n---\n\n".join(context_parts),
        "sources": sources
    }
