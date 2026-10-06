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
1. Ground EVERY factual statement in the CORPUS CHUNKS below. Cite each fact
   like [T02:What is a seed phrase?] using the topic id and title shown.
2. If the corpus chunks do not cover what is asked, say so plainly and give
   only safe general guidance; do not invent facts.
3. NEVER include secrets, seed words, or private keys in an answer. Never ask
   the user to type seed words into any website, cloud service, or notes app.
4. Never recommend a specific paid product, vendor, lawyer, or financial
   decision; never quote prices.
5. Be concrete and calm. Short sentences. Give the user clear DO / DO NOT
   guidance where the situation calls for it.
6. If the user's situation involves anyone contacting them offering help,
   treat it as a possible scam and say what makes it suspicious.
7. End with a short 'Sources:' line listing the chunk citations you used."""

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
        "max_tokens": 240,
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
    """Fallback: Sail flex GLM-5.3. Never prints .env values."""
    user_msgs = [
        m["content"] if m["role"] != "system" else "[SYSTEM]\n" + m["content"]
        for m in messages
    ]
    prompt = "\n\n".join(user_msgs)
    budget = 2048
    last_err = None
    for _ in range(3):
        proc = subprocess.run(
            [sys.executable, SAIL_FLEX, "call_flex"],
            input=prompt, capture_output=True, text=True, timeout=timeout,
        )
        out = proc.stdout.strip()
        if proc.returncode == 0 and out:
            return _strip_think(out)
        last_err = (proc.stderr or out or "empty")[-500:]
        if "max_output_tokens" in last_err:
            budget *= 2
            continue
        break
    raise LlamaServerError("sail_flex failed: %s" % last_err)


def answer_step(question: str, composed, backend: str = "auto") -> Dict:
    """Generate the answer for a composed context.

    Returns {"answer": str, "backend": str}.
    Order: llama-server (auto) -> sail flex; 'llama'/'sail' force one.
    """
    messages = build_messages(question, composed)
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
