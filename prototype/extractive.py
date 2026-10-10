"""Extractive answers v1 (plan item 2.1): the model SELECTS, the corpus speaks.

The retrieval layer already finds the right entry ~85% of the time; free-form
paraphrase is where delivery breaks. Here the model performs only a selection
task — choose which retrieved CORPUS CHUNKS answer the question — and the
answer is rendered VERBATIM from the human-approved corpus, with a short
optional bridge.

Deterministic guarantees:
- Chunk texts come straight from the loaded approved corpus (guardian_core
  compose()); rendering copies them unmodified.
- Selection is validated: unknown IDs or empty selection -> deterministic
  fallback to the top chunk verbatim (never a free-form guess).
- Each rendered chunk carries its source entry id.
"""
import json
import re


def _norm(s: str) -> str:
    return " ".join(s.split()).lower()


_CORPUS_CACHE = {}


def _entry_by_id(eid: str):
    """Load the corpus entry for an entry_id like 'T02:slug' (cached)."""
    import os
    cdir = os.environ.get("GUARDIAN_CORPUS", "/opt/data/projects/custody-guardian/corpus")
    if cdir not in _CORPUS_CACHE:
        m = {}
        for fn in sorted(os.listdir(cdir)):
            if not fn.endswith(".json"):
                continue
            try:
                e = json.load(open(os.path.join(cdir, fn), encoding="utf-8"))
            except Exception:
                continue
            t = e.get("topic_ids", ["T00"])[0]
            slug = e.get("title", "").lower()
            import re as _re
            slug = _re.sub(r"[^a-z0-9]+", "-", slug).strip("-")
            m[f"{t}:{slug}"] = e
        _CORPUS_CACHE[cdir] = m
    return _CORPUS_CACHE[cdir].get(eid)


def build_menu(composed) -> list:
    """Menu of key_points from the retrieved entries (rank order), verbatim.

    Key points carry the safety imperatives ("never paste it into a website");
    raw chunks are explanatory prose and make weak selection targets.
    """
    menu = []
    seen_eids = []
    for ch in composed.retrieved:
        eid = ch.get("entry_id", "?")
        if eid in seen_eids:
            continue
        seen_eids.append(eid)
    for eid in seen_eids[:5]:
        entry = _entry_by_id(eid) or {}
        kps = entry.get("key_points") or []
        if not kps:
            plain = (entry.get("plain") or "").split("\n")
            kps = [p.strip() for p in plain if p.strip()][:4]
        for kp in kps:
            menu.append({"pid": f"P{len(menu)+1}", "text": kp, "entry_id": eid,
                         "title": entry.get("title", "")})
    return menu


def build_selection_prompt(question: str, menu: list) -> str:
    lines = [f'{m["pid"]}. ({m["entry_id"]}) {_norm(m["text"])[:400]}' for m in menu]
    return f"""You are the selection step of an offline Bitcoin self-custody assistant.
A question from a grieving beginner is shown below, followed by numbered passages
drawn verbatim from a human-reviewed corpus.

TASK: choose WHICH passages answer the question. Do not write the answer, do not
add information. Select as few as needed (1-5), keeping corpus order.

QUESTION: {question}

PASSAGES:
{chr(10).join(lines)}

Return ONLY compact JSON: {{"selected": ["P2", "P5"], "bridge": "<=2 short plain
sentences framing the selected passages. No new facts, no new numbers, no product
names not in the passages; if nothing to add, use empty string."}}"""


def parse_selection(raw: str, menu: list):
    """Parse model output; return (selected_pids, bridge) or (None, None)."""
    t = _strip_think(raw).strip()
    if t.startswith("```"):
        t = t.split("\n", 1)[1].rsplit("```", 1)[0]
    m = re.search(r"\{.*\}", t, re.S)
    if not m:
        return None, None
    try:
        obj = json.loads(m.group(0))
    except Exception:
        return None, None
    valid = {p["pid"] for p in menu}
    sel = [s for s in (obj.get("selected") or []) if s in valid]
    if not sel:
        return None, None
    bridge = str(obj.get("bridge") or "").strip()
    return sel, bridge


def render(sel: list, bridge: str, menu: list) -> str:
    by_pid = {p["pid"]: p for p in menu}
    ordered = [by_pid[s] for s in sel if s in by_pid]  # selection order
    parts = [f"- {p['text']} [{p['entry_id']}]" for p in ordered]
    out = "\n\n".join(parts)
    if bridge:
        out += "\n\n" + bridge
    entries = sorted({p["entry_id"] for p in ordered})
    out += "\n\nSource: " + ", ".join(entries) + " (human-reviewed corpus)"
    return out


def _strip_think(text: str) -> str:
    return re.sub(r"<think>.*?</think>", "", text, flags=re.S).strip()


def _fallback(composed) -> dict:
    """Deterministic fallback: top retrieved entry's key points verbatim."""
    eid = composed.retrieved[0].get("entry_id", "?") if composed.retrieved else "?"
    entry = _entry_by_id(eid) or {}
    kps = entry.get("key_points") or []
    if not kps:
        plain = (entry.get("plain") or "").split("\n")
        kps = [p.strip() for p in plain if p.strip()][:4]
    if not kps:
        return None
    return {
        "answer": (
            f"From the reviewed corpus [{eid}] "
            + entry.get("title", "") + ":\n\n"
            + "\n".join(f"- {kp}" for kp in kps)
            + "\n\n(Answered directly from the reviewed material.)"
        ),
        "backend": "extractive_fallback",
    }


def extractive_answer(question: str, composed, model_call) -> dict:
    """model_call(messages) -> str. Returns answer_step-shaped dict, or None."""
    menu = build_menu(composed)
    if not menu:
        return None
    messages = [
        {"role": "system", "content": "Return only the compact JSON object. No reasoning text."},
        {"role": "user", "content": build_selection_prompt(question, menu)},
    ]
    try:
        raw = model_call(messages)
    except Exception:
        return _fallback(composed)
    sel, bridge = parse_selection(raw, menu)
    if not sel:
        return _fallback(composed)
    # verbatim re-check: every rendered point must exist in its entry's approved text
    by_pid = {p["pid"]: p for p in menu}
    for s in sel:
        p = by_pid[s]
        entry = _entry_by_id(p["entry_id"]) or {}
        hay = _norm(json.dumps(entry, ensure_ascii=False))
        if _norm(p["text"]) not in hay:
            return _fallback(composed)
    answer = render(sel, bridge, menu)
    return {"answer": answer, "backend": "extractive_select", "selected": sel}
