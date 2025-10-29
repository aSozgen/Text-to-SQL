
"""
import requests
import json

BASE_URL = "http://localhost:8000"

# Example 1: Single-turn query
def test_single_turn():
    response = requests.post(
        f"{BASE_URL}/predict",
        json={
            "question": "Show all students",
            "schema": "Student: id (number) [PK], name (text), age (number)",
            "include_schema": True
        }
    )
    print("Single-turn:", response.json())

# Example 2: Dialog - Turn 2
def test_dialog_turn2():
    response = requests.post(
        f"{BASE_URL}/predict",
        json={
            "question": "Filter by age greater than 20",
            "schema": "Student: id (number) [PK], name (text), age (number)",
            "conversation_history": [
                {
                    "question": "Show all students",
                    "sql": "SELECT * FROM Student"
                }
            ],
            "include_schema": True
        }
    )
    print("Dialog Turn 2:", response.json())

# Example 3: Dialog - Turn 3
def test_dialog_turn3():
    response = requests.post(
        f"{BASE_URL}/predict",
        json={
            "question": "Order by name",
            "schema": "Student: id (number) [PK], name (text), age (number)",
            "conversation_history": [
                {
                    "question": "Show all students",
                    "sql": "SELECT * FROM Student"
                },
                {
                    "question": "Filter by age greater than 20",
                    "sql": "SELECT * FROM Student WHERE age > 20"
                }
            ],
            "include_schema": True
        }
    )
    print("Dialog Turn 3:", response.json())

if __name__ == "__main__":
    test_single_turn()
    test_dialog_turn2()
    test_dialog_turn3()
"""