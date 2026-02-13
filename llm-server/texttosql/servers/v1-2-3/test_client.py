"""
import requests

BASE = "http://localhost:8000"

# Test 1: Single-turn
r1 = requests.post(f"{BASE}/predict", json={
    "question": "Show all students",
    "schema": "Student: id (number) [PK], name (text), age (number)"
})
print("Single:", r1.json()['sql'])

# Test 2: Dialog turn 2
r2 = requests.post(f"{BASE}/predict", json={
    "question": "Filter by age > 20",
    "schema": "Student: id (number) [PK], name (text), age (number)",
    "conversation_history": [
        {"question": "Show all students", "sql": "SELECT * FROM Student"}
    ]
})
print("Dialog 2:", r2.json()['sql'])

# Test 3: Dialog turn 3 (uses last 2 turns)
r3 = requests.post(f"{BASE}/predict", json={
    "question": "Order by name",
    "conversation_history": [
        {"question": "Show all students", "sql": "SELECT * FROM Student"},
        {"question": "Filter by age > 20", "sql": "SELECT * FROM Student WHERE age > 20"}
    ]
})
print("Dialog 3:", r3.json()['sql'])
"""