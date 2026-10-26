import json
from pathlib import Path
from typing import List, Dict, Any, Optional

from sklearn.metrics.pairwise import cosine_similarity

_model = None
_knowledge_base = None
_embeddings = None

KB_PATH = Path(__file__).resolve().parent / "tradeiqx_knowledge.json"


def get_knowledge_base() -> List[Dict[str, Any]]:
    global _knowledge_base
    if _knowledge_base is None:
        if not KB_PATH.exists():
            raise FileNotFoundError(f"TradeIQX knowledge base not found at {KB_PATH}")
        with open(KB_PATH, "r", encoding="utf-8") as file:
            _knowledge_base = json.load(file)
    return _knowledge_base


def get_model():
    global _model
    if _model is None:
        from sentence_transformers import SentenceTransformer
        _model = SentenceTransformer("all-MiniLM-L6-v2")
    return _model


def get_embeddings():
    global _embeddings
    if _embeddings is None:
        model = get_model()
        kb = get_knowledge_base()
        texts = [item["content"] for item in kb]
        _embeddings = model.encode(texts, show_progress_bar=False)
    return _embeddings


def semantic_search(query: str, top_k: int = 3) -> List[Dict[str, Any]]:
    model = get_model()
    embeddings = get_embeddings()
    kb = get_knowledge_base()

    query_embedding = model.encode([query])
    similarities = cosine_similarity(query_embedding, embeddings)[0]
    top_indices = similarities.argsort()[::-1][:top_k]

    results = []
    for index in top_indices:
        result = kb[index].copy()
        result["similarity"] = float(similarities[index])
        results.append(result)

    return results


def smart_search(
    query: str,
    threshold: float = 0.35,
    top_k: int = 3
) -> List[Dict[str, Any]]:
    results = semantic_search(query, top_k=top_k)
    return [result for result in results if result["similarity"] >= threshold]


def personalized_search(
    query: str,
    difficulty: Optional[str] = None,
    threshold: float = 0.35,
    top_k: int = 3
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
    threshold: float = 0.35,
    top_k: int = 3
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
    threshold: float = 0.35,
    top_k: int = 3
) -> Dict[str, Any]:
    results = smart_search(query, threshold=threshold, top_k=top_k)
    if not results:
        return {
            "status": "no_match",
            "confidence": 0.0,
            "context": ""
        }

    best_score = float(results[0]["similarity"])
    context_parts = []
    for result in results:
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
        "context": "\n\n---\n\n".join(context_parts)
    }
