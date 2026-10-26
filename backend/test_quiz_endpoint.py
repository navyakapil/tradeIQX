import unittest
from unittest.mock import patch
import requests
from fastapi.testclient import TestClient

from main import app

client = TestClient(app)


class TestQuizEndpoint(unittest.TestCase):

    def test_quiz_route_registered(self):
        """Verify /api/quiz/generate is registered in FastAPI app routes."""
        route_paths = [getattr(route, "path", None) for route in app.routes]
        self.assertIn("/api/quiz/generate", route_paths)

    @patch("main.generate_quiz")
    def test_quiz_generate_success_contract(self, mock_generate_quiz):
        """Verify successful response matches the exact JSON contract expected by Android."""
        mock_payload = {
            "topic": "Stock Market Basics",
            "difficulty": "Beginner",
            "questions": [
                {
                    "question": "What is a stock?",
                    "options": [
                        "A loan to a bank",
                        "Fractional ownership in a company",
                        "A government tax document",
                        "A fixed deposit receipt"
                    ],
                    "correct_index": 1,
                    "explanation": "A stock represents fractional equity ownership in a company."
                }
            ],
            "total_questions": 1,
            "sources": ["Basic Financial Knowledge"],
            "mode": "ai_generated_rag_quiz"
        }
        mock_generate_quiz.return_value = mock_payload

        response = client.post(
            "/api/quiz/generate",
            json={
                "topic": "Stock Market Basics",
                "difficulty": "Beginner"
            }
        )

        self.assertEqual(response.status_code, 200)
        data = response.json()

        # Contract checks expected by AIService.kt and QuizScreen.kt
        self.assertEqual(data["topic"], "Stock Market Basics")
        self.assertEqual(data["difficulty"], "Beginner")
        self.assertIn("questions", data)
        self.assertIsInstance(data["questions"], list)
        self.assertGreater(len(data["questions"]), 0)

        first_q = data["questions"][0]
        self.assertIn("question", first_q)
        self.assertIsInstance(first_q["question"], str)
        self.assertIn("options", first_q)
        self.assertIsInstance(first_q["options"], list)
        self.assertEqual(len(first_q["options"]), 4)
        for opt in first_q["options"]:
            self.assertIsInstance(opt, str)
        self.assertIn("correct_index", first_q)
        self.assertIn(first_q["correct_index"], range(4))
        self.assertIn("explanation", first_q)
        self.assertIsInstance(first_q["explanation"], str)

        mock_generate_quiz.assert_called_once_with(
            topic="Stock Market Basics",
            difficulty="Beginner"
        )

    def test_quiz_generate_empty_topic_returns_400(self):
        """Verify empty or whitespace topic returns 400 Bad Request."""
        response = client.post(
            "/api/quiz/generate",
            json={
                "topic": "   ",
                "difficulty": "Beginner"
            }
        )
        self.assertEqual(response.status_code, 400)
        self.assertIn("topic", response.json().get("detail", "").lower())

    def test_quiz_generate_invalid_difficulty_returns_400(self):
        """Verify invalid difficulty level returns 400 Bad Request."""
        response = client.post(
            "/api/quiz/generate",
            json={
                "topic": "Stock Market Basics",
                "difficulty": "Impossible"
            }
        )
        self.assertEqual(response.status_code, 400)
        self.assertIn("difficulty", response.json().get("detail", "").lower())

    @patch("main.generate_quiz")
    def test_quiz_generate_ollama_failure_returns_503(self, mock_generate_quiz):
        """Verify upstream model connection failure returns 503 Service Unavailable."""
        mock_generate_quiz.side_effect = requests.RequestException("Ollama connection refused")

        response = client.post(
            "/api/quiz/generate",
            json={
                "topic": "Technical Analysis",
                "difficulty": "Intermediate"
            }
        )
        self.assertEqual(response.status_code, 503)
        self.assertIn("unavailable", response.json().get("detail", "").lower())

    @patch("main.generate_quiz")
    def test_quiz_generate_knowledge_missing_returns_503(self, mock_generate_quiz):
        """Verify missing knowledge file returns 503 Service Unavailable."""
        mock_generate_quiz.side_effect = FileNotFoundError("Knowledge file not found")

        response = client.post(
            "/api/quiz/generate",
            json={
                "topic": "Risk Management",
                "difficulty": "Advanced"
            }
        )
        self.assertEqual(response.status_code, 503)
        self.assertIn("knowledge base", response.json().get("detail", "").lower())


if __name__ == "__main__":
    unittest.main()
