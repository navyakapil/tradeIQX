"""TradeIQX RAG Service.

Connects the retriever, prompt builder, and LLM to answer trading questions.
"""

import sys
import logging
from pathlib import Path
from typing import Dict, Any, List

# Ensure project root is on sys.path
PROJECT_ROOT = Path(__file__).resolve().parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from rag.tradeiqx_retriever import (
    build_rag_context_with_scores,
    smart_search
)
from rag.prompting import (
    TRADEIQX_SYSTEM_INSTRUCTIONS,
    build_prompt
)
from llm import generate_llm_answer, is_llm_configured

logger = logging.getLogger(__name__)


def answer_question(question: str) -> Dict[str, Any]:
    """Retrieves relevant knowledge from TradeIQX KB and queries the LLM."""
    clean_q = question.strip()
    if not clean_q:
        raise ValueError("Question cannot be empty.")

    rag_result = build_rag_context_with_scores(clean_q)
    context = rag_result.get("context", "")
    sources = rag_result.get("sources", [])
    confidence = rag_result.get("confidence", 0.0)

    # If nothing relevant found above threshold
    if not context or rag_result.get("status") == "no_match" or confidence < 0.25:
        fallback_msg = (
            "I only help with trading, investing, and financial market concepts. "
            "Please ask me about stocks, orders, stop-losses, portfolio risk, or technical indicators!"
        )
        return {
            "answer": fallback_msg,
            "sources": [],
            "reply": fallback_msg
        }

    prompt = build_prompt(clean_q, context)

    # Call LLM if configured; otherwise use grounded summary from knowledge context
    if is_llm_configured():
        try:
            answer = generate_llm_answer(prompt, system_instruction=TRADEIQX_SYSTEM_INSTRUCTIONS)
        except Exception as exc:
            logger.warning(f"LLM call failed, falling back to retrieved knowledge: {exc}")
            answer = f"Based on TradeIQX Knowledge ({', '.join(sources)}):\n\n{context}"
    else:
        answer = f"Based on TradeIQX Knowledge ({', '.join(sources)}):\n\n{context}"

    return {
        "answer": answer,
        "sources": sources,
        "reply": answer
    }


def generate_quiz(topic: str = "Stock Market Basics", difficulty: str = "Beginner") -> Dict[str, Any]:
    """Generates financial quiz questions using retrieved knowledge chunks."""
    results = smart_search(topic, threshold=0.25, top_k=3)
    if not results:
        results = smart_search("Stock", threshold=0.2, top_k=3)

    questions = []
    for idx, item in enumerate(results):
        t = item.get("topic", "Trading")
        content = item.get("content", "")
        questions.append({
            "id": idx + 1,
            "question": f"Which concept relates to: '{content[:120]}...'?",
            "options": [t, "Gambling", "Speculation Only", "None of the above"],
            "correct_answer": t,
            "explanation": content
        })

    return {
        "topic": topic,
        "difficulty": difficulty,
        "questions": questions
    }
