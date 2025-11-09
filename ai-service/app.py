import os, json, re, unicodedata, requests
from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional, List
from sentence_transformers import SentenceTransformer
import torch

OLLAMA=os.getenv("OLLAMA_BASE_URL","http://ollama:11434")
PRIMARY=os.getenv("PRIMARY_MODEL","llama3.1:8b-instruct-q4_K_M")
SECONDARY=os.getenv("SECONDARY_MODEL","qwen2.5:7b-instruct-q4_K_M")

app=FastAPI()

OPT = {
    "temperature": 0.0,
    "top_p": 1.0,
    "repeat_penalty": 1.15,
    "num_predict": 56,
    "seed": 42,
    "stop": ["\nRăspuns:", "\nAnswer:", "\nExplicație:", "\nExplanation:"]
}

def chat(model, messages, json_mode=False):
    data={"model":model,"messages":messages,"options":OPT}
    if json_mode: data["format"]="json"
    r=requests.post(f"{OLLAMA}/v1/chat/completions",json=data,timeout=120)
    r.raise_for_status()
    return r.json()["choices"][0]["message"]["content"].strip()

# --- Anti-halucinații: normalizare și validare ---

RO_STOP = set("""
și sau ori dar deci însă deoarece pentru ca că de la în din pe sub prin către spre cu fără precum într-un într-o este sunt a ai au un o ce cât cum unde când care cui
""".split())

BAN_STYLE = {"oculi","eteric","transcendent","mistic","inefabil","luminiscență","serafic","oniric"}

def normalize(s:str)->str:
    s = s.replace("\r","\n").strip()
    s = re.sub(r"\s+"," ",s)
    return s

def strip_diacritics(s:str)->str:
    return "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")

def tokens(s:str)->List[str]:
    s = normalize(s.lower())
    s = re.sub(r"[^a-zăâîșț0-9\- ]"," ",s, flags=re.IGNORECASE)
    return [t for t in s.split() if t]

def content_overlap(q:str, h:str)->int:
    qt = [t for t in tokens(q) if t not in RO_STOP]
    ht = [t for t in tokens(h) if t not in RO_STOP]
    return len(set(qt) & set(ht))

def has_banned_words(h:str)->bool:
    ht = set(tokens(strip_diacritics(h)))
    return any(w in ht for w in BAN_STYLE)

def no_new_numbers(q:str,h:str)->bool:
    qn=set(re.findall(r"\d+(?:[.,]\d+)?",q))
    hn=set(re.findall(r"\d+(?:[.,]\d+)?",h))
    return len(hn - qn) == 0

# --- Embedder pentru scor semantic ---
DEVICE = "cpu"
EMB = SentenceTransformer("sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2", device=DEVICE)
def sim(a:str,b:str)->float:
    va = EMB.encode([a], normalize_embeddings=True)
    vb = EMB.encode([b], normalize_embeddings=True)
    return float((va*vb).sum())

# --- Promptare strictă ---
SYS_HINT = (
    "Ești generator de indicii pentru quiz, română standard. Reguli stricte:\n"
    "- Stil factual, fără metafore, fără termeni rari.\n"
    "- O singură propoziție, maximum 18 cuvinte.\n"
    "- Nu dezvălui răspunsul. Nu introduce cifre sau nume noi față de întrebare.\n"
    "- Include cel puțin un cuvânt relevant din întrebare (excluzând conectori)."
)

FEW = [
    ("Care este complexitatea medie a quicksort?",
     "Gândește-te la partiționare recursivă în jurul pivotului și cost mediu pe nivel."),
    ("Ce afirmă teorema lui Pitagora?",
     "Amintește-ți relația dintre ipotenuză și catete, fără rezultat numeric."),
    ("În HTTP, ce indică 404?",
     "Leagă-l de absența resursei solicitate, nu de erori de server."),
]

def build_msgs(question:str, context:Optional[str]=None):
    msgs=[{"role":"system","content":SYS_HINT}]
    for q,h in FEW:
        msgs.append({"role":"user","content":"Întrebare: "+q})
        msgs.append({"role":"assistant","content":h})
    uc="Întrebare: "+question.strip()
    if context and context.strip():
        uc += "\nContext: " + context.strip()
    return msgs + [{"role":"user","content":uc}]

def hard_rule_hint(question:str)->Optional[str]:
    q=question.strip().lower()
    if q.startswith("ce culoare"):
        return "Gândește-te la cer într-o zi senină și la nuanța dominantă a bolții."
    if q.startswith("ce este "):
        return "Extrage trăsătura definitorie a termenului, fără exemple sau sinonime."
    if any(k in q for k in ["calculează","determină","află","cât este"]):
        return "Scrie formula corespunzătoare și identifică mărimile din enunț înainte de calcul."
    return None

def violates(question:str, hint:str)->bool:
    if len(hint.split())>18: return True
    if any(b in hint.lower() for b in ["răspunsul este","valoarea este","corect este","este =", "answer is","= "]): return True
    if has_banned_words(hint): return True
    if not no_new_numbers(question,hint): return True
    if content_overlap(question,hint) == 0: return True
    if sim(question,hint) < 0.42: return True
    return False

class HintIn(BaseModel):
    question:str
    context:Optional[str]=None

@app.post("/hint")
def hint(body:HintIn):
    hr = hard_rule_hint(body.question)
    if hr:
        return {"hint":hr}
    msgs=build_msgs(body.question, body.context)
    try:
        out=chat(PRIMARY,msgs)
    except:
        out=chat(SECONDARY,msgs)
    out = normalize(out)
    if violates(body.question,out):
        # Repară: impune cuvinte din întrebare și stil anti-metaforă
        qt = [t for t in tokens(body.question) if t not in RO_STOP]
        keep = ", ".join(qt[:4]) if qt else ""
        fix = [
            {"role":"system","content":SYS_HINT},
            {"role":"user","content":(
                "Rescrie indiciul respectând regulile. Evită metafore și termeni rari. "
                f"Inclus obligatoriu cel puțin unul dintre aceste cuvinte: {keep}\n"
                f"Întrebare: {body.question}\nIndiciu curent: {out}\n"
                "Returnează o singură propoziție, max 18 cuvinte."
            )}
        ]
        try:
            out = chat(PRIMARY,fix)
        except:
            out = chat(SECONDARY,fix)
        out = normalize(out)
        # fallback scurt dacă tot e slab
        if violates(body.question,out):
            out = hard_rule_hint(body.question) or "Concentrează-te pe conceptul cheie menționat în întrebare, fără a oferi răspunsul."
    return {"hint":out}

class GenIn(BaseModel):
    text:str
    max_questions:Optional[int]=None

@app.post("/generate-summary-quizzes")
def generate(body:GenIn):
    text=normalize(body.text)
    wc=len(text.split())
    n=body.max_questions or max(3,min(15,round(wc/700)))
    schema={"summary":"string","quizzes":[{"question":"string","options":["string","string","string","string"],"answer_index":0,"hint":"string"}]}
    instr=(f"Rezumat concis în 5–10 propoziții strict din text. Apoi creează {n} întrebări grilă cu 4 opțiuni fiecare. "
           "answer_index în [0..3]. Indicii scurte, fără răspunsuri. Fără informații externe. "
           "Stil factual, fără metafore. Returnează STRICT JSON conform schemei: ")+json.dumps(schema,ensure_ascii=False)
    p = instr + "\nText:\n" + text[:160000]
    for attempt in range(2):
        try:
            out=chat(PRIMARY,[{"role":"user","content":p}],json_mode=True)
        except:
            out=chat(SECONDARY,[{"role":"user","content":p}],json_mode=True)
        try:
            data=json.loads(out)
            if not isinstance(data,dict) or "summary" not in data or "quizzes" not in data:
                raise ValueError("schema")
            # micro-curățare hint-uri din quiz
            for q in data.get("quizzes",[]):
                q["hint"]=normalize(q.get("hint",""))
            return data
        except:
            fix=("Corectează în JSON valid exact conform schemei "+json.dumps(schema,ensure_ascii=False)+". Fără explicații:\n"+out)
            try:
                out2=chat(PRIMARY,[{"role":"user","content":fix}])
            except:
                out2=chat(SECONDARY,[{"role":"user","content":fix}])
            try:
                return json.loads(out2)
            except:
                continue
    return {"summary":"", "quizzes":[]}
