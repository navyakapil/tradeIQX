"""TradeIQX LLM Client Interface.

Manages connection to the LLM provider (Google GenAI or OpenAI)
using environment variables from backend/.env.
Never exposes or logs secret API keys.
"""

import os
from pathlib import Path
from typing import Optional
from dotenv import load_dotenv

# Load backend/.env
ENV_PATH = Path(__file__).resolve().parent / ".env"
load_dotenv(dotenv_path=ENV_PATH)

# Also check root .env as fallback
ROOT_ENV_PATH = Path(__file__).resolve().parent.parent / ".env"
if ROOT_ENV_PATH.exists():
    load_dotenv(dotenv_path=ROOT_ENV_PATH)


def get_llm_config():
    """Retrieves API key and model name securely from environment."""
    api_key = (
        os.environ.get("LLM_API_KEY")
        or os.environ.get("GEMINI_API_KEY")
        or os.environ.get("OPENAI_API_KEY")
    )
    model = (
        os.environ.get("LLM_MODEL")
        or ("gemini-2.5-flash" if not os.environ.get("OPENAI_API_KEY") else "gpt-4o-mini")
    )
    return api_key, model


def is_llm_configured() -> bool:
    """Returns True if a non-placeholder API key is set."""
    key, _ = get_llm_config()
    if not key:
        return False
    placeholder_tokens = ["your_", "change_me", "placeholder", "sk-proj-placeholder"]
    return not any(token in key.lower() for token in placeholder_tokens)


def generate_llm_answer(prompt: str, system_instruction: Optional[str] = None) -> str:
    """Generates an answer using the configured LLM provider.

    Raises RuntimeError with a sanitized error message if key is missing or invalid.
    """
    api_key, model = get_llm_config()

    if not api_key or not is_llm_configured():
        raise RuntimeError(
            "LLM API key is not configured. Please set LLM_API_KEY in backend/.env"
        )

    # Check whether using OpenAI or Gemini
    if api_key.startswith("sk-"):
        # OpenAI Provider
        try:
            from openai import OpenAI
            client = OpenAI(api_key=api_key)
            messages = []
            if system_instruction:
                messages.append({"role": "system", "content": system_instruction})
            messages.append({"role": "user", "content": prompt})

            completion = client.chat.completions.create(
                model=model if ("gpt" in model) else "gpt-4o-mini",
                messages=messages,
                temperature=0.2,
                max_tokens=800
            )
            return completion.choices[0].message.content.strip()
        except Exception as exc:
            raise RuntimeError("LLM service call failed. Please check network and provider limits.") from exc

    # Default Google GenAI Provider
    try:
        from google import genai
        client = genai.Client(api_key=api_key)
        config = {}
        if system_instruction:
            config["system_instruction"] = system_instruction

        response = client.models.generate_content(
            model=model,
            contents=prompt,
            config=config if config else None
        )
        if hasattr(response, "text") and response.text:
            return response.text.strip()
        raise RuntimeError("LLM returned an empty response.")
    except Exception as exc:
        raise RuntimeError("LLM service call failed. Please check network and provider limits.") from exc
