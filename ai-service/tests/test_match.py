def test_semantic_match_returns_score_in_range(client):
    response = client.post(
        "/api/v1/match/semantic",
        json={
            "resume_text": "Java Spring Boot MySQL REST API developer",
            "job_description": "Looking for a Java Backend Intern with Spring Boot and MySQL",
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert 0.0 <= body["similarity"] <= 1.0
    assert body["backend_used"] in ("transformer", "tfidf")


def test_semantic_match_unrelated_texts_score_lower(client):
    related = client.post(
        "/api/v1/match/semantic",
        json={
            "resume_text": "Java Spring Boot MySQL backend developer",
            "job_description": "Java Spring Boot backend engineer needed",
        },
    ).json()["similarity"]

    unrelated = client.post(
        "/api/v1/match/semantic",
        json={
            "resume_text": "Java Spring Boot MySQL backend developer",
            "job_description": "Looking for a pastry chef with cake decorating experience",
        },
    ).json()["similarity"]

    assert related > unrelated
