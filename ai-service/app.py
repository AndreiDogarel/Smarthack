import os, json, re, unicodedata, requests
from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional, List
from sentence_transformers import SentenceTransformer
import torch

OLLAMA = os.getenv("OLLAMA_BASE_URL", "http://ollama:11434")
PRIMARY = os.getenv("PRIMARY_MODEL", "llama3.1:8b-instruct-q4_K_M")
SECONDARY = os.getenv("SECONDARY_MODEL", "qwen2.5:7b-instruct-q4_K_M")

app = FastAPI()

OPT = {
    "temperature": 0.0,
    "top_p": 1.0,
    "repeat_penalty": 1.15,
    "num_predict": 56,
    "seed": 42,
    "stop": ["\nRăspuns:", "\nAnswer:", "\nExplicație:", "\nExplanation:"]
}

def chat(model, messages, json_mode=False):
    data = {"model": model, "messages": messages, "options": OPT}
    if json_mode:
        data["format"] = "json"
    r = requests.post(f"{OLLAMA}/v1/chat/completions", json=data, timeout=120)
    r.raise_for_status()
    return r.json()["choices"][0]["message"]["content"].strip()

# --- Anti-halucinații și preprocesare ---
RO_STOP = set("""
și sau ori dar deci însă deoarece pentru ca că de la în din pe sub prin către spre cu fără precum într-un într-o este sunt a ai au un o ce cât cum unde când care cui
""".split())

DEVICE = "cpu"
EMB = SentenceTransformer("sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2", device=DEVICE)
def normalize(s: str) -> str:
    s = s.replace("\r", "\n").strip()
    s = re.sub(r"\s+", " ", s)
    return s

# ===== /hint -> RĂSPUNS DIRECT CONCIS =====

OPT_ANSWER = {
    "temperature": 0.1,
    "top_p": 1.0,
    "repeat_penalty": 1.1,
    "num_predict": 80,
    "seed": 42,
    "stop": ["\nExplicație:", "\nExplanation:", "\nChain of thought:", "\nGândire:"]
}

SYS_ANSWER = (
    "Răspunde direct și concis la întrebare, în limba întrebării. "
    "Dacă este întrebare cu opțiuni, returnează varianta corectă și un motiv scurt. "
    "Dacă este calcul, dă rezultatul final (include unitățile când e cazul). "
    "Dacă e definiție/concept, oferă definiția în 1–2 propoziții. "
    "Evită metafore și divagații; nu include pașii interni de raționament."
)

FEW_QA = [
    ("Ce afirmă teorema lui Pitagora?", "Într-un triunghi dreptunghic, pătratul ipotenuzei este suma pătratelor catetelor."),
    ("În HTTP ce înseamnă codul 404?", "Resursa solicitată nu a fost găsită pe server."),
    ("Care este complexitatea medie a quicksort?", "Θ(n log n)."),
    ("Cât este aria unui cerc de rază r?", "π·r^2."),
    ("Capitala Franței?", "Paris.")
]

def build_answer_msgs(question: str, context: Optional[str] = None):
    msgs = [{"role": "system", "content": SYS_ANSWER}]
    for q, a in FEW_QA:
        msgs.append({"role": "user", "content": q})
        msgs.append({"role": "assistant", "content": a})
    uc = question.strip()
    if context and context.strip():
        uc += "\nContext: " + context.strip()
    msgs.append({"role": "user", "content": uc})
    return msgs

class HintIn(BaseModel):
    question: str
    context: Optional[str] = None

@app.post("/hint")
def hint(body: HintIn):
    data = {"model": PRIMARY, "messages": build_answer_msgs(body.question, body.context), "options": OPT_ANSWER}
    try:
        r = requests.post(f"{OLLAMA}/v1/chat/completions", json=data, timeout=120)
        r.raise_for_status()
        out = r.json()["choices"][0]["message"]["content"].strip()
    except Exception:
        data["model"] = SECONDARY
        r = requests.post(f"{OLLAMA}/v1/chat/completions", json=data, timeout=120)
        r.raise_for_status()
        out = r.json()["choices"][0]["message"]["content"].strip()
    out = re.sub(r"\s+", " ", out).strip()
    if len(out.split()) > 50:
        out = " ".join(out.split()[:50]).rstrip(",.;:") + "..."
    return {"answer": out}

# ===== GENERARE REZUMAT + QUIZ =====

class GenIn(BaseModel):
    text: str
    max_questions: Optional[int] = None

@app.post("/generate-summary-quizzes")
def generate(body: GenIn):
    text = normalize(body.text)
    wc = len(text.split())
    n = body.max_questions or max(3, min(15, round(wc / 700)))
    schema = {"summary": "string", "quizzes": [{"question": "string", "options": ["string", "string", "string", "string"], "answer_index": 0, "hint": "string"}]}
    instr = (
        f"Rezumat concis în 5–10 propoziții strict din text. Apoi creează {n} întrebări grilă cu 4 opțiuni fiecare. "
        "answer_index în [0..3]. Indicii scurte, fără răspunsuri. Fără informații externe. "
        "Stil factual, fără metafore. Returnează STRICT JSON conform schemei: "
    ) + json.dumps(schema, ensure_ascii=False)
    p = instr + "\nText:\n" + text[:160000]
    for attempt in range(2):
        try:
            out = chat(PRIMARY, [{"role": "user", "content": p}], json_mode=True)
        except:
            out = chat(SECONDARY, [{"role": "user", "content": p}], json_mode=True)
        try:
            data = json.loads(out)
            if not isinstance(data, dict) or "summary" not in data or "quizzes" not in data:
                raise ValueError("schema")
            for q in data.get("quizzes", []):
                q["hint"] = normalize(q.get("hint", ""))
            return data
        except:
            fix = "Corectează în JSON valid exact conform schemei " + json.dumps(schema, ensure_ascii=False) + ". Fără explicații:\n" + out
            try:
                out2 = chat(PRIMARY, [{"role": "user", "content": fix}])
            except:
                out2 = chat(SECONDARY, [{"role": "user", "content": fix}])
            try:
                return json.loads(out2)
            except:
                continue
    return {"summary": "", "quizzes": []}
