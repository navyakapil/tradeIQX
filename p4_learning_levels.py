"""
TradeIQX - P4 AI Intelligence Layer
User Skill Level Classification

This module determines a user's learning level based on
their quiz performance.

Levels:
    Beginner
    Advanced Beginner
    Intermediate
    Advanced
"""


# ---------------------------------------------------------
# LEVEL DEFINITIONS
# ---------------------------------------------------------

LEVELS = [
    "Beginner",
    "Advanced Beginner",
    "Intermediate",
    "Advanced"
]


# ---------------------------------------------------------
# DETERMINE USER LEVEL
# ---------------------------------------------------------

def determine_level(score, questions_answered):
    """
    Determine a user's skill level based on quiz accuracy.

    Parameters:
        score (int): Number of correctly answered questions.
        questions_answered (int): Total number of questions answered.

    Returns:
        str: User's current learning level.
    """

    if questions_answered <= 0:
        return "Beginner"

    # Prevent invalid scores
    if score < 0:
        score = 0

    if score > questions_answered:
        score = questions_answered

    accuracy = score / questions_answered

    if accuracy >= 0.85:
        return "Advanced"

    elif accuracy >= 0.70:
        return "Intermediate"

    elif accuracy >= 0.50:
        return "Advanced Beginner"

    else:
        return "Beginner"


# ---------------------------------------------------------
# LEVEL DESCRIPTIONS
# ---------------------------------------------------------

LEVEL_DESCRIPTIONS = {
    "Beginner":
        "Understands basic financial and trading concepts.",

    "Advanced Beginner":
        "Understands basic concepts and can apply them to simple examples.",

    "Intermediate":
        "Can apply financial concepts to moderately difficult scenarios.",

    "Advanced":
        "Can handle complex concepts and multi-step financial analysis."
}


# ---------------------------------------------------------
# TEST USER LEVEL
# ---------------------------------------------------------

def test_user_level(score, questions_answered):
    """
    Calculate and display a user's performance and level.

    Parameters:
        score (int): Number of correct answers.
        questions_answered (int): Total questions answered.

    Returns:
        str: User's assigned level.
    """

    if questions_answered <= 0:
        print("No questions have been answered yet.")
        return "Beginner"

    accuracy = (score / questions_answered) * 100

    level = determine_level(
        score,
        questions_answered
    )

    print("--------------------------------")
    print("       TRADEIQX USER LEVEL")
    print("--------------------------------")
    print("Questions answered:", questions_answered)
    print("Correct answers:", score)
    print("Accuracy:", round(accuracy, 1), "%")
    print("User level:", level)
    print("Description:", LEVEL_DESCRIPTIONS[level])
    print("--------------------------------")

    return level


# ---------------------------------------------------------
# EXAMPLE TESTS
# ---------------------------------------------------------

if __name__ == "__main__":

    print("\nTest 1:")
    test_user_level(3, 10)

    print("\nTest 2:")
    test_user_level(6, 10)

    print("\nTest 3:")
    test_user_level(8, 10)

    print("\nTest 4:")
    test_user_level(9, 10)
