def test_extract_skills_finds_known_terms(client):
    response = client.post(
        "/api/v1/skills/extract",
        json={"text": "We need a Java developer with Spring Boot and Docker experience."},
    )
    assert response.status_code == 200
    body = response.json()
    skill_names = {s["skill"] for s in body["skills"]}
    assert "Java" in skill_names
    assert "Spring Boot" in skill_names
    assert "Docker" in skill_names
    assert body["method"] in ("spacy-phrase-matcher", "regex-fallback")


def test_extract_skills_ignores_unknown_terms(client):
    response = client.post(
        "/api/v1/skills/extract",
        json={"text": "We need someone who is a great communicator and team player."},
    )
    assert response.status_code == 200
    assert response.json()["skills"] == []


def test_extract_skills_rejects_empty_text(client):
    response = client.post("/api/v1/skills/extract", json={"text": ""})
    assert response.status_code == 422  # pydantic min_length=1 validation
