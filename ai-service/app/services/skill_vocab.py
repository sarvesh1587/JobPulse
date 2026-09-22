import json
from functools import lru_cache
from pathlib import Path

DATA_PATH = Path(__file__).resolve().parent.parent / "data" / "skills.json"


@lru_cache
def load_skill_vocabulary() -> list[dict]:
    with open(DATA_PATH, encoding="utf-8") as f:
        return json.load(f)
