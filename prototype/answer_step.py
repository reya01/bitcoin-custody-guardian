"""answer_step.py — the LLM answer step for Bitcoin Custody Guardian.

Given a compose() result (guardian_core.Composed) plus the original question,
generates a grounded answer via one of two backends:

1. local llama-server (llama.cpp, OpenAI-compatible endpoint on 127.0.0.1)
   running Qwen3-1.7B Q4_K_M GGUF — default;
2. Sail flex GLM-5.3 (opt/data/scripts/sail_flex.py call_flex) — fallback.

The system prompt embeds the retrieved corpus chunks, forces a citation
format ([T0x:title]), and carries the guardrail constraints. Deterministic
settings (temperature 0, fixed seed).
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import urllib.request
import urllib.error
from typing import Dict, List, Optional

_HERE = os.path.dirname(os.path.abspath(__file__))
SAIL_FLEX = "/opt/data/scripts/sail_flex.py"

LLAMA_SERVER_URL = os.environ.get(
    "GUARDIAN_LLAMA_URL", "http://127.0.0.1:9200"
)

SYSTEM_PREAMBLE = """You are the answer engine of Bitcoin Custody Guardian, an offline,
privacy-first app that teaches Bitcoin self-custody. You are a careful,
grounded assistant for a non-technical person who may have inherited bitcoin.

RULES (obey exactly):
1. ASSEMBLE, do not invent. Your job is to select and stitch together the CORPUS CHUNK guidance that fits the user's situation. Quote the corpus claims as close to verbatim as possible; smooth the joins for readability.
2. Cite each chunk you take material from like [T02:What is a seed phrase?] at the end of the sentence(s) it came from.
3. If the corpus chunks do not cover what is asked, say so plainly and give only safe general guidance; do not invent facts.
4. NEVER include secrets, seed words, or private keys in an answer. Never ask or advise the user to type seed words into any website, cloud service, or notes app. Never advise resetting or wiping a device that may hold bitcoin before the backup words are verified.
5. Never recommend a specific paid product, vendor, lawyer, or financial decision; never quote prices.
6. For scam situations: name the scam plainly and confidently (the corpus phrasing is authoritative) and tell the user to stop contact; do not hedge with 'may be'.
7. Be concrete and calm. Short sentences. Clear DO / DO NOT guidance.
8. End with a short 'Sources:' line listing the chunk citations you used."""

FALLBACK_NOTE = (
    "Answer from general Bitcoin-custody safety knowledge; no corpus chunks "
    "were available for this question."
)


def _chunks_block(retrieved: List[Dict]) -> str:
    if not retrieved:
        return "CORPUS CHUNKS: (none retrieved) " + FALLBACK_NOTE
    parts = []
    for i, ch in enumerate(retrieved, 1):
        cite = "[%s:%s]" % (ch["entry_id"].split(":")[0], ch["title"])
        parts.append(
            "CHUNK %d %s (section %s, score %.2f):\n%s"
            % (i, cite, ch["section"], ch["score"], ch["text"].strip())
        )
    return "\n\n".join(parts)


def _constraints_block(constraints: List) -> str:
    if not constraints:
        return ""
    lines = ["HARD CONSTRAINTS for this answer:"]
    for c in constraints:
        if c.kind == "must_include":
            lines.append("- MUST include (or clearly express): %s" % c.phrase)
        else:
            lines.append(
                "- MUST NOT contain this phrase or instruction: %s (%s)"
                % (c.phrase, c.description)
            )
    return "\n".join(lines)


def build_messages(question: str, composed) -> List[Dict[str, str]]:
    """Build the chat messages for the answer step from compose() output."""
    sys_prompt = SYSTEM_PREAMBLE + "\n\n" + _chunks_block(composed.retrieved)
    cons = _constraints_block(composed.constraints)
    if cons:
        sys_prompt += "\n\n" + cons
    if composed.refusal:
        sys_prompt += (
            "\n\nThis question is OUT OF SCOPE for the app (%s). Politely "
            "refuse in one or two sentences, explain why the app cannot help "
            "with it, and point to what the app CAN do. Do not answer the "
            "substantive question." % composed.refusal["template"]
        )
    user = "USER QUESTION: " + question + " /no_think"
    if composed.detections.is_secret:
        user += (
            "\n\n[SYSTEM: the question text contained secret material. Do not "
            "repeat it. Warn about it and answer generically.]"
        )
    return [
        {"role": "system", "content": sys_prompt},
        {"role": "user", "content": user},
    ]


# --------------------------------------------------------------------------
# Backends
# --------------------------------------------------------------------------

class LlamaServerError(RuntimeError):
    pass


def _strip_think(text: str) -> str:
    """Remove Qwen3 NICALL...NICALL blocks if present."""
    if "NICALL" in text:
        text = text.rsplit("NICALL", 1)[1]
    return text.strip()


def llama_answer(messages: List[Dict[str, str]], timeout: int = 1500) -> str:
    url = LLAMA_SERVER_URL.rstrip("/") + "/v1/chat/completions"
    payload = {
        "model": "qwen3",
        "messages": messages,
        "temperature": 0.0,
        "top_p": 0.8,
        "top_k": 20,
        "max_tokens": 1600,
    }
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except (urllib.error.URLError, TimeoutError, OSError) as e:
        raise LlamaServerError("llama-server unreachable: %s" % e)
    text = data["choices"][0]["message"]["content"]
    return _strip_think(text)


def sail_answer(messages: List[Dict[str, str]], timeout: int = 1200) -> str:
    """Fallback: Sail flex GLM-5.3 via the shared sail_flex module."""
    try:
        sys.path.insert(0, "/opt/data/scripts")
        from sail_flex import call_flex
    except Exception as e:
        raise LlamaServerError("sail_flex module unavailable: %s" % e)
    msgs = [
        {"role": m["role"], "content": m["content"]} for m in messages
    ]
    for attempt in range(2):
        try:
            out = call_flex("zai-org/GLM-5.3", msgs,
                            max_completion_tokens=16000, timeout_s=timeout)
            out = _strip_think(out)
            if out:
                return out
        except Exception as e:
            last_err = str(e)[-400:]
    raise LlamaServerError("sail_flex failed: %s" % last_err)


def answer_step(question: str, composed, backend: str = "auto") -> Dict:
    """Generate the answer for a composed context.

    Returns {"answer": str, "backend": str}.
    Order: deterministic scam rules -> llama-server (auto) -> sail flex;
    'llama'/'sail' force one. Scam red flags are adjudicated by rule, never
    by the model (a 1.7B hedges scam verdicts).
    """
    import scam_rules
    verdict = scam_rules.scam_verdict_answer(question)
    messages = build_messages(question, composed)
    if verdict is not None:
        # Deterministic verdict block is mandatory; the model only adds
        # scenario-specific guidance on top (verdict itself is never hedged).
        messages.append({
            "role": "system",
            "content": (
                "The deterministic rule engine has ALREADY flagged this message "
                "as a scam and produced a verdict block. That verdict is final: "
                "do not soften, hedge, or re-derive it. Add ONLY scenario-specific "
                "guidance grounded in the retrieved corpus chunks (what exactly to "
                "do next, step by step). Cite [Txx] where used."
            ),
        })
        try:
            body = llama_answer(messages) if backend in ("auto", "llama") else sail_answer(messages)
        except LlamaServerError:
            body = sail_answer(messages)
        return {"answer": verdict + "\n\n" + body.strip(),
                "backend": "scam_rules+" + ("local_qwen3_1.7b" if backend != "sail" else "sail_glm53")}
    if backend == "sail":
        return {"answer": sail_answer(messages), "backend": "sail_glm53"}
    try:
        return {"answer": llama_answer(messages), "backend": "local_qwen3_1.7b"}
    except LlamaServerError:
        if backend == "llama":
            raise
    return {"answer": sail_answer(messages), "backend": "sail_glm53"}


if __name__ == "__main__":
    import argparse
    from guardian_core import compose

    CORPUS = os.path.join(os.path.dirname(_HERE), "corpus")
    ap = argparse.ArgumentParser()
    ap.add_argument("question")
    ap.add_argument("--backend", default="auto", choices=["auto", "llama", "sail"])
    args = ap.parse_args()
    c = compose(args.question, CORPUS)
    out = answer_step(args.question, c, backend=args.backend)
    print(json.dumps(out, indent=2))
