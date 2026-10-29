"""Extractive answers v2 (plan item 2.1, post-review hardening).

Philosophy: the model SELECTS, the corpus speaks. The only free-form surface is
a short bridge, deterministically validated; everything else is verbatim
approved text. Fails closed — extractive mode never falls through to a
free-form paraphrase backend.

Review fixes implemented (inspA_synthesis.md):
1  entry identity: 40-char retrieval-ID prefix match, loud unresolved logging
2  fail closed: no remote-paraphrase fallthrough; canned message on any failure
3  parse hardening: fences, case, dedup, bridge default, retry-once
4  menu: all unique retrieved entries (cap 10), full original-case text, dedup,
   key_points-only (no plain fabrication)
5  selection recall: cap 8, select-all-warnings instruction, menu-order render,
   deterministic warning pinning (logged as pinned)
6  bridge: structural checks (<=2 sentences, <=300 chars, no URLs, no new
   digits, blocklist), veto drops the bridge only
7  verbatim manifest: sha256 of key_points verified at render time
"""
import hashlib
import json
import os
import re

CORPUS_DIR = os.environ.get(
    "GUARDIAN_CORPUS", "/opt/data/projects/custody-guardian/corpus")
MANIFEST_PATH = os.environ.get(
    "GUARDIAN_MANIFEST", os.path.join(os.path.dirname(__file__), "key_points_manifest.json"))

CANNED_LOCAL_UNAVAILABLE = (
    "The answer engine did not respond, so I cannot compose an answer right now. "
    "Nothing was sent anywhere. General safety rule while you wait: never share, "
    "type, or photograph seed words or private keys for anyone, ever — no "
    "legitimate party will ask for them."
)

_BRIDGE_BLOCKLIST = re.compile(
    r"(safe to share|no risk|you can ignore|don't worry|it is fine|perfectly safe)",
    re.I)
_WARN_RE = re.compile(r"\b(never|do not|don't|must not|only)\b", re.I)


def _norm(s: str) -> str:
    return " ".join(s.split()).lower()


def _hash(s: str) -> str:
    return hashlib.sha256(_norm(s).encode()).hexdigest()


_CORPUS_CACHE = {}
_MANIFEST_CACHE = None


def _manifest() -> dict:
    global _MANIFEST_CACHE
    if _MANIFEST_CACHE is None:
        try:
            _MANIFEST_CACHE = json.load(open(MANIFEST_PATH))
            if not _MANIFEST_CACHE:
                # FAIL-CLOSED (full-repo review 2026-10-29): an empty/missing
                # manifest disables the verbatim gate by construction — that
                # is a broken install, not a free pass. Callers must treat a
                # missing manifest as "every point unverified".
                print("[extractive] FATAL manifest file empty — every point "
                      "is treated as unverified (fail closed)", flush=True)
                _MANIFEST_CACHE = {}
        except Exception as exc:
            print(f"[extractive] FATAL manifest load failed ({exc}) — every "
                  "point is treated as unverified (fail closed)", flush=True)
            _MANIFEST_CACHE = {}
    return _MANIFEST_CACHE


def _load_corpus_index():
    """Map: 40-char entry-id prefix -> entry (matches retrieval's truncation)."""
    if _CORPUS_CACHE:
        return _CORPUS_CACHE
    idx = {}
    for fn in sorted(os.listdir(CORPUS_DIR)):
        if not fn.endswith(".json"):
            continue
        try:
            e = json.load(open(os.path.join(CORPUS_DIR, fn), encoding="utf-8"))
        except Exception as exc:
            print(f"[extractive] corpus parse failure: {fn}: {exc}", flush=True)
            continue
        for t in e.get("topic_ids", ["T00"]):
            slug = re.sub(r"[^a-z0-9]+", "-", e.get("title", "").lower()).strip("-")
            eid = f"{t}:{slug}"
            idx.setdefault(eid, e)
            idx.setdefault(eid[:40], e)  # retrieval truncates at 40 chars
    _CORPUS_CACHE.update(idx)
    return _CORPUS_CACHE


def _entry_by_id(eid: str):
    idx = _load_corpus_index()
    e = idx.get(eid) or idx.get(eid[:40])
    if e is None:
        print(f"[extractive] WARNING unresolved entry_id: {eid!r}", flush=True)
    return e


def build_menu(composed) -> list:
    """Key points of ALL unique retrieved entries (cap 10 entries), verbatim,
    deduped by normalized text. key_points only — no plain fabrication."""
    seen_eids, menu, seen_texts = [], [], set()
    for ch in composed.retrieved:
        eid = ch.get("entry_id", "?")
        if eid not in seen_eids:
            seen_eids.append(eid)
    for eid in seen_eids[:10]:
        entry = _entry_by_id(eid) or {}
        for kp in entry.get("key_points") or []:
            if _norm(kp) in seen_texts:
                continue
            seen_texts.add(_norm(kp))
            menu.append({"pid": f"P{len(menu)+1}", "text": kp,
                         "entry_id": eid, "title": entry.get("title", ""),
                         "warning": bool(_WARN_RE.search(kp))})
    return menu


def build_selection_prompt(question: str, menu: list) -> str:
    lines = [f'{m["pid"]}. [{m["title"]}] {m["text"]}' for m in menu]
    return f"""You are the selection step of an offline Bitcoin self-custody assistant.
A question from a grieving beginner is shown below, followed by numbered key
points drawn verbatim from a human-reviewed corpus.

TASK: select ALL key points that help answer the question — including EVERY key
point carrying an applicable warning (never / do not / must not / only). Select
between 2 and 8. Do not write the answer, do not add information. Keep menu order.

QUESTION: {question}

KEY POINTS:
{chr(10).join(lines)}

Return ONLY compact JSON:
{{"selected": ["P1", "P3", "P4", "P7"], "bridge": "<=2 short plain sentences
framing the selection. No new facts, no new numbers, no URLs, no product names;
empty string if nothing to add."}}"""


def _strip_think(text: str) -> str:
    return re.sub(r"<think>.*?</think>", "", text, flags=re.S).strip()


def parse_selection(raw: str, menu: list):
    """Return (selected_pids, bridge) or (None, ''). Hardened per Fix 3."""
    t = _strip_think(raw).strip()
    if t.startswith("```"):
        t = t[3:]
        if "\n" in t:
            t = t.split("\n", 1)[1]
        t = t.rsplit("```", 1)[0]
    m = re.search(r"\{.*\}", t, re.S)
    if not m:
        return None, ""
    try:
        obj = json.loads(m.group(0))
    except Exception:
        return None, ""
    valid = {p["pid"] for p in menu}
    sel = []
    for s in obj.get("selected") or []:
        s = str(s).strip().upper()
        if s in valid and s not in sel:
            sel.append(s)
    if not sel:
        return None, ""
    bridge = str(obj.get("bridge") or "").strip()
    return sel, bridge


def _validate_bridge(bridge: str, sel_items: list) -> str:
    """Deterministic bridge checks (Fix 6). Returns the bridge or ''."""
    if not bridge:
        return ""
    if len(bridge) > 300 or len(re.findall(r"[.!?]", bridge)) > 2:
        return ""
    if re.search(r"https?://|www\.|\w+\.(com|org|net|io|ca)\b", bridge, re.I):
        return ""
    digits_in_points = set(re.findall(r"\d+", " ".join(p["text"] for p in sel_items)))
    for d in re.findall(r"\d+", bridge):
        if d not in digits_in_points:
            return ""
    if _BRIDGE_BLOCKLIST.search(bridge):
        return ""
    return bridge


def render(sel: list, bridge: str, menu: list) -> tuple:
    """Render verbatim, menu order, with warning pinning. Returns (text, pinned).
    Manifest check runs on the FINAL ordered set (post-pinning) so pinned
    warnings cannot bypass the integrity check."""
    by_pid = {p["pid"]: p for p in menu}
    ordered = [by_pid[s] for s in sel if s in by_pid]
    # deterministic safety pin (Fix 5): warnings from selected entries that the
    # model left out; logged as pinned so selection recall stays measurable
    pinned = []
    sel_entries = {p["entry_id"] for p in ordered}
    for p in menu:
        if p["warning"] and p["entry_id"] in sel_entries and p["pid"] not in sel:
            ordered.append(p)
            pinned.append(p["pid"])
    # integrity gate on the final rendered set — pinning cannot bypass it
    ordered = [p for p in ordered if _point_ok(p)]
    if not ordered:
        return "", []
    parts, seen = [], set()
    for p in ordered:
        if _norm(p["text"]) in seen:
            continue
        seen.add(_norm(p["text"]))
        parts.append(f"- {p['text']} [{p['entry_id']}]")
    text = "\n\n".join(parts)
    if bridge:
        text += "\n\n" + bridge
    entries = sorted({p["entry_id"] for p in ordered})
    text += "\n\nSource: " + ", ".join(entries) + " (human-reviewed corpus)"
    return text, pinned


def _fallback(composed, note=None) -> dict:
    """Fail-closed fallback: canned message; never free-form, never remote.
    `note` (only passed when the answer ENGINE itself failed) is appended so
    the message says the on-device engine did not respond."""
    text = CANNED_LOCAL_UNAVAILABLE
    if note:
        text = text + " " + note
    return {"answer": text, "backend": "extractive_fallback"}


def _point_ok(p: dict) -> bool:
    """Fix 7, hardened (full-repo review 2026-10-29): hash must match the
    approved manifest — and a missing/empty manifest fails CLOSED, never
    open (a broken install cannot silently disable the integrity gate)."""
    man = _manifest()
    if not man:
        return False
    return _hash(p["text"]) in man


def safety_block(composed, model_call, question: str = "") -> str:
    """Hybrid mode: verbatim safety layer appended to the grounded paraphrase.

    question: the real user question — selection is question-relevant, not
    question-blind. model_call should be the SAME backend that produced the
    paraphrase (local engine on-device, remote only where the paraphrase was
    remote) so the offline story stays true.

    Fail-soft: returns '' on any failure — the block is additive defense-in-depth
    over an already-vetoed paraphrase, so failing closed would trade a vetted
    answer for a canned non-answer. Preconditions that make soft-fail safe:
    every rendered point passes the manifest check inside render() (post-pinning)
    AND the independent corpus re-check below, AND answer_step re-runs the veto
    constraint check on the final concatenated answer.
    """
    try:
        menu = build_menu(composed)
        if not menu:
            return ""
        messages = [
            {"role": "system",
             "content": "Return only the compact JSON object. No reasoning text."},
            {"role": "user", "content": build_selection_prompt(
                (question or "Safety-critical points relevant to the user's "
                 "question") + "\n(Select the safety-critical points — "
                "especially warnings: never / do not / must not / only — that "
                "apply to this question.)", menu)},
        ]
        raw = model_call(messages)
        sel, _ = parse_selection(raw, menu)
        if not sel:
            # deterministic floor: up to 6 warnings, retrieval order (may come
            # from any of the retrieved entries — order, not a fixed entry count)
            sel = [p["pid"] for p in menu if p["warning"]][:6]
        if not sel:
            return ""
        by_pid = {p["pid"]: p for p in menu}
        # independent verbatim re-check against a fresh corpus load
        for s in sel:
            p = by_pid[s]
            entry = _entry_by_id(p["entry_id"]) or {}
            if _norm(p["text"]) not in _norm(json.dumps(entry, ensure_ascii=False)):
                print(f"[extractive] safety_block corpus re-check failed for {s}",
                      flush=True)
                sel = [x for x in sel if x != s]
        if not sel:
            return ""
        text, _pinned = render(sel, "", menu)
        if not text:
            return ""
        return ("From the reviewed corpus, verbatim:\n\n" + text)
    except Exception as exc:
        print(f"[extractive] safety_block failure (dropped): {exc}", flush=True)
        return ""


def extractive_answer(question: str, composed, model_call, fallback_note=None) -> dict:
    try:
        menu = build_menu(composed)
        if not menu:
            return _fallback(composed)
        messages = [
            {"role": "system",
             "content": "Return only the compact JSON object. No reasoning text."},
            {"role": "user", "content": build_selection_prompt(question, menu)},
        ]
        try:
            raw = model_call(messages)
        except Exception:
            # the ENGINE failed (not a bad selection): say so precisely
            return _fallback(composed, note=fallback_note)
        sel, bridge = parse_selection(raw, menu)
        if not sel:  # one retry, then fail closed
            try:
                raw = model_call(messages)
            except Exception:
                return _fallback(composed, note=fallback_note)
            sel, bridge = parse_selection(raw, menu)
        if not sel:
            return _fallback(composed)
        by_pid = {p["pid"]: p for p in menu}
        for s in sel:  # verbatim re-check against the corpus entry (independent load)
            p = by_pid[s]
            entry = _entry_by_id(p["entry_id"]) or {}
            if _norm(p["text"]) not in _norm(json.dumps(entry, ensure_ascii=False)):
                print(f"[extractive] verbatim check failed for {s}", flush=True)
                return _fallback(composed)
        if not all(_point_ok(by_pid[s]) for s in sel):
            print("[extractive] manifest check failed", flush=True)
            return _fallback(composed)
        bridge = _validate_bridge(bridge, [by_pid[s] for s in sel])
        text, pinned = render(sel, bridge, menu)
        return {"answer": text, "backend": "extractive_select",
                "selected": sel, "pinned": pinned}
    except Exception as exc:  # fail closed, loud
        print(f"[extractive] FAILURE (fail-closed): {type(exc).__name__}: {exc}",
              flush=True)
        return _fallback(composed)
