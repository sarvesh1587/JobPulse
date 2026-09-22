-- V2__seed_skills.sql
-- A starter skill vocabulary. Naive keyword-based job-skill extraction
-- (see JobUpsertService) can only tag skills that exist in this table —
-- proper NLP/embedding-based extraction is the Python AI service's job,
-- this is just enough to make the pipeline demonstrable end-to-end.

INSERT INTO skills (name, category) VALUES
    ('Java', 'Language'),
    ('Python', 'Language'),
    ('TypeScript', 'Language'),
    ('JavaScript', 'Language'),
    ('Spring Boot', 'Framework'),
    ('Spring', 'Framework'),
    ('React', 'Framework'),
    ('Node.js', 'Framework'),
    ('Express', 'Framework'),
    ('REST', 'API'),
    ('GraphQL', 'API'),
    ('MySQL', 'Database'),
    ('PostgreSQL', 'Database'),
    ('MongoDB', 'Database'),
    ('Redis', 'Database'),
    ('Docker', 'DevOps'),
    ('Kubernetes', 'DevOps'),
    ('Kafka', 'Messaging'),
    ('AWS', 'Cloud'),
    ('Azure', 'Cloud'),
    ('GCP', 'Cloud'),
    ('Git', 'Tooling'),
    ('JUnit', 'Testing'),
    ('CI/CD', 'DevOps'),
    ('Hibernate', 'Framework'),
    ('JPA', 'Framework')
ON CONFLICT (name) DO NOTHING;
