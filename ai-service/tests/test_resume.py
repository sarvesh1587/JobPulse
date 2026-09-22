def test_parse_resume_detects_education_and_year(client):
    response = client.post(
        "/api/v1/resume/parse",
        json={"text": "B.Tech Computer Science, Class of 2027. Skills: Java, MySQL, Git."},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["likely_education_level"] == "B.Tech"
    assert body["likely_graduation_year"] == 2027
    skill_names = {s["skill"] for s in body["detected_skills"]}
    assert "Java" in skill_names
    assert "MySQL" in skill_names


def test_parse_resume_handles_no_matches_gracefully(client):
    response = client.post(
        "/api/v1/resume/parse",
        json={"text": "A short bio with nothing technical in it at all."},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["detected_skills"] == []
    assert body["likely_education_level"] is None
    assert body["likely_graduation_year"] is None
