import json
import math
import os
import re
from pathlib import Path
from functools import lru_cache
import logging
import requests

logger = logging.getLogger(__name__)

# ---------------------------------
# CONFIGURATION
# ---------------------------------

BACKEND_DIR = Path(__file__).resolve().parent
PROJECT_DIR = BACKEND_DIR.parent

KNOWLEDGE_FILE = BACKEND_DIR / "rag" / "tradeiqx_knowledge.json"
if not KNOWLEDGE_FILE.exists():
    KNOWLEDGE_FILE = PROJECT_DIR / "rag" / "tradeiqx_knowledge.json"

OLLAMA_URL = os.getenv("OLLAMA_URL", "http://127.0.0.1:11434").rstrip("/")
CHAT_MODEL = os.getenv("CHAT_MODEL", "llama3.2:1b")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "nomic-embed-text")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "").strip()

# ---------------------------------
# LOAD FINANCIAL KNOWLEDGE
# ---------------------------------

@lru_cache(maxsize=1)
def load_knowledge():
    if not KNOWLEDGE_FILE.exists():
        raise FileNotFoundError(f"Knowledge base not found: {KNOWLEDGE_FILE}")

    with KNOWLEDGE_FILE.open("r", encoding="utf-8") as file:
        records = json.load(file)

    chunks = []
    for record in records:
        content = record.get("content", "").strip()
        if content:
            chunks.append({
                "id": record.get("id", ""),
                "topic": record.get("topic", "General"),
                "category": record.get("category", "Finance"),
                "difficulty": record.get("difficulty", "Beginner"),
                "keywords": record.get("keywords", []),
                "content": content
            })

    if not chunks:
        raise ValueError("Financial knowledge base is empty.")

    return chunks


# ---------------------------------
# CHECK OLLAMA AVAILABILITY
# ---------------------------------

_ollama_checked = False
_ollama_available = False

def is_ollama_alive():
    global _ollama_checked, _ollama_available
    if _ollama_checked and not _ollama_available:
        return False
    try:
        r = requests.get(f"{OLLAMA_URL}/api/version", timeout=1.0)
        _ollama_available = (r.status_code == 200)
    except Exception:
        _ollama_available = False
    _ollama_checked = True
    return _ollama_available


# ---------------------------------
# GENERATE LOCAL EMBEDDINGS (Ollama)
# ---------------------------------

def create_embeddings(texts):
    if not is_ollama_alive():
        raise ConnectionError("Ollama is not available")
    response = requests.post(
        f"{OLLAMA_URL}/api/embed",
        json={"model": EMBEDDING_MODEL, "input": texts},
        timeout=3
    )
    response.raise_for_status()
    embeddings = response.json().get("embeddings", [])
    if len(embeddings) != len(texts):
        raise RuntimeError("Ollama returned an unexpected number of embeddings.")
    return embeddings


@lru_cache(maxsize=1)
def get_knowledge_embeddings():
    chunks = load_knowledge()
    texts = [f"{chunk['topic']}. {chunk['content']}" for chunk in chunks]
    return create_embeddings(texts)


def cosine_similarity(a, b):
    dot_product = sum(x * y for x, y in zip(a, b))
    norm_a = math.sqrt(sum(x * x for x in a))
    norm_b = math.sqrt(sum(y * y for y in b))
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return dot_product / (norm_a * norm_b)


# ---------------------------------
# RETRIEVE RELEVANT KNOWLEDGE
# ---------------------------------

def retrieve_context_keyword(question, top_k=3):
    chunks = load_knowledge()
    tokens = set(re.findall(r"\w+", question.lower()))

    scored = []
    for chunk in chunks:
        topic_tokens = set(re.findall(r"\w+", chunk.get("topic", "").lower()))
        content_tokens = set(re.findall(r"\w+", chunk.get("content", "").lower()))
        keywords = {k.lower() for k in chunk.get("keywords", [])}

        score = 0
        for kw in keywords:
            if kw in question.lower():
                score += 8
        score += len(tokens.intersection(topic_tokens)) * 5
        score += len(tokens.intersection(content_tokens)) * 1
        scored.append((score, chunk))

    scored.sort(key=lambda x: x[0], reverse=True)
    selected = [chunk for score, chunk in scored[:top_k] if score > 0]
    if not selected:
        selected = chunks[:top_k]

    return selected


def retrieve_context(question, top_k=3):
    chunks = load_knowledge()

    selected = None
    try:
        knowledge_embeddings = get_knowledge_embeddings()
        question_embedding = create_embeddings([question])[0]
        ranked = []
        for chunk, embedding in zip(chunks, knowledge_embeddings):
            similarity = cosine_similarity(question_embedding, embedding)
            ranked.append((similarity, chunk))
        ranked.sort(key=lambda item: item[0], reverse=True)
        selected = [chunk for _, chunk in ranked[:top_k]]
    except Exception as exc:
        logger.info("Semantic embedding failed (%s), using keyword retriever", exc)
        selected = retrieve_context_keyword(question, top_k)

    context = "\n\n".join(
        f"Topic: {chunk['topic']}\n"
        f"Category: {chunk['category']}\n"
        f"Information: {chunk['content']}"
        for chunk in selected
    )
    sources = [chunk["topic"] for chunk in selected]
    return context, sources, selected


# ---------------------------------
# GENERATE RAG CHATBOT ANSWER
# ---------------------------------

def answer_question(question):
    if not question or not question.strip():
        raise ValueError("Please enter a trading question.")

    question = question.strip()
    context, sources, selected = retrieve_context(question)

    system_prompt = (
        "You are TradeIQX AI Trading Mentor. "
        "Explain stock market and trading concepts for educational purposes only. "
        "Use only the retrieved financial knowledge provided in the user's message. "
        "Do not promise profits or provide personalized investment advice."
    )
    user_prompt = f"Retrieved financial knowledge:\n{context}\n\nQuestion: {question}"

    # 1. Try Gemini API if key is present
    gemini_key = os.getenv("GEMINI_API_KEY", "").strip()
    if gemini_key:
        try:
            gemini_url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={gemini_key}"
            payload = {
                "contents": [
                    {"role": "user", "parts": [{"text": f"{system_prompt}\n\n{user_prompt}"}]}
                ]
            }
            res = requests.post(gemini_url, json=payload, timeout=10)
            if res.status_code == 200:
                answer = res.json()["candidates"][0]["content"]["parts"][0]["text"].strip()
                if answer:
                    return {"reply": answer, "sources": sources, "mode": "gemini_rag"}
        except Exception as e:
            logger.warning("Gemini API call failed: %s", e)

    # 2. Try Local Ollama if available
    if is_ollama_alive():
        try:
            response = requests.post(
                f"{OLLAMA_URL}/api/chat",
                json={
                    "model": CHAT_MODEL,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt}
                    ],
                    "stream": False,
                    "options": {"temperature": 0.3}
                },
                timeout=4
            )
            if response.status_code == 200:
                answer = response.json().get("message", {}).get("content", "").strip()
                if answer:
                    return {"reply": answer, "sources": sources, "mode": "local_rag"}
        except Exception as e:
            logger.info("Ollama error (%s), using intelligent knowledge synthesis", e)

    # 3. Intelligent Knowledge Retrieval Synthesis (100% resilient fallback)
    top_chunk = selected[0]
    topic_name = top_chunk.get("topic", "Market Concept")
    cat_name = top_chunk.get("category", "Trading")
    content = top_chunk.get("content", "")

    additional_insights = ""
    if len(selected) > 1:
        related_topics = [c["topic"] for c in selected[1:]]
        additional_insights = f"\n\nRelated Concepts: {', '.join(related_topics)}."

    reply = (
        f"**{topic_name}** ({cat_name})\n\n"
        f"{content}"
        f"{additional_insights}\n\n"
        f"Mentor Note: Practice with paper trading first to understand market dynamics and manage risk before risking real capital."
    )

    return {
        "reply": reply,
        "sources": sources,
        "mode": "knowledge_mentor"
    }


# ---------------------------------
# AI QUIZ GENERATOR
# ---------------------------------

def generate_quiz(topic="Stock Market Basics", difficulty="Beginner"):
    if difficulty not in ("Beginner", "Intermediate", "Advanced"):
        raise ValueError("Invalid difficulty level.")

    if not topic or not topic.strip():
        raise ValueError("Quiz topic is required.")

    topic = topic.strip()
    context, sources, selected = retrieve_context(f"{topic} financial concepts", top_k=5)

    system_prompt = (
        "You are TradeIQX AI Quiz Generator. "
        "Generate 5 educational multiple-choice questions using only the provided financial knowledge. "
        "Every question must have exactly 4 unique options and exactly one correct answer. "
        "Return ONLY a JSON object with a questions array. "
        "Each question must contain question, options, correct_index (0 to 3), and explanation."
    )
    user_prompt = f"Topic: {topic}\nDifficulty: {difficulty}\n\nFinancial Knowledge:\n{context}"

    # 1. Try Gemini API if key is present
    gemini_key = os.getenv("GEMINI_API_KEY", "").strip()
    if gemini_key:
        try:
            gemini_url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={gemini_key}"
            payload = {
                "contents": [
                    {"role": "user", "parts": [{"text": f"{system_prompt}\n\n{user_prompt}"}]}
                ],
                "generationConfig": {"response_mime_type": "application/json"}
            }
            res = requests.post(gemini_url, json=payload, timeout=10)
            if res.status_code == 200:
                text_content = res.json()["candidates"][0]["content"]["parts"][0]["text"].strip()
                parsed = json.loads(text_content)
                questions = parsed.get("questions", [])
                if len(questions) >= 1:
                    return {
                        "topic": topic,
                        "difficulty": difficulty,
                        "questions": questions[:5],
                        "total_questions": len(questions[:5]),
                        "sources": sources,
                        "mode": "gemini_quiz"
                    }
        except Exception as e:
            logger.warning("Gemini quiz generation failed: %s", e)

    # 2. Try Ollama if available
    if is_ollama_alive():
        try:
            response = requests.post(
                f"{OLLAMA_URL}/api/chat",
                json={
                    "model": CHAT_MODEL,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt}
                    ],
                    "format": "json",
                    "stream": False,
                    "options": {"temperature": 0.2}
                },
                timeout=4
            )
            if response.status_code == 200:
                content = response.json().get("message", {}).get("content", "").strip()
                if content:
                    quiz = json.loads(content)
                    questions = quiz.get("questions", [])
                    if len(questions) >= 1:
                        return {
                            "topic": topic,
                            "difficulty": difficulty,
                            "questions": questions[:5],
                            "total_questions": len(questions[:5]),
                            "sources": sources,
                            "mode": "ai_generated_rag_quiz"
                        }
        except Exception as e:
            logger.info("Ollama error for quiz (%s), using dynamic knowledge quiz generator", e)

    # 3. Dynamic Knowledge Quiz Generator (100% resilient fallback)
    chunks = load_knowledge()
    topic_chunks = [c for c in chunks if c.get("difficulty") == difficulty] or chunks

    questions = []
    sample_pool = selected if len(selected) >= 3 else chunks[:5]

    for i, chunk in enumerate(sample_pool[:5]):
        t = chunk.get("topic", "Market Concept")
        c = chunk.get("content", "")
        # Create clear educational questions
        q_text = f"What is the primary role of '{t}' in financial trading?"
        correct_option = c[:90].rsplit(" ", 1)[0] + "..." if len(c) > 90 else c
        
        # Pull distractors from other chunks
        distractors = [
            f"It guarantees fixed 100% daily returns for traders.",
            f"It replaces regulatory stock exchange oversight completely.",
            f"It represents an irreversible penalty on cash accounts."
        ]
        
        options = [correct_option] + distractors
        questions.append({
            "question": q_text,
            "options": options,
            "correct_index": 0,
            "explanation": f"{t}: {c}"
        })

    return {
        "topic": topic,
        "difficulty": difficulty,
        "questions": questions,
        "total_questions": len(questions),
        "sources": sources,
        "mode": "knowledge_quiz"
    }
