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

from guardian_core import Guardrails  # post-model veto layer

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
3. If a retrieved chunk relates to the question even partially, USE it — quote its claims rather than declining. Decline only when NO chunk relates. When you decline, first state what the chunks DO cover, then say the specific asked detail is not covered. Do not invent facts.
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
    else:
        sys_prompt += (
            "\n\nIMPORTANT scope rule: questions about moving bitcoin the user "
            "ALREADY HOLDS (withdrawing from an exchange to their own wallet, "
            "transferring to self-custody) ARE in scope — answer them fully "
            "from the corpus. Refuse only live-data requests (current prices, "
            "buying recommendations, news) and anything outside Bitcoin custody. "
            "Mentioning the word 'exchange' does not make a question out of scope."
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
    Order: secret refusal (deterministic, no backend) -> scam verdict
    (rules, scam-check mode only) -> llama-server (auto) -> sail flex.
    Post-generation: guardrail scrub + veto. On a must_not_include hit the
    candidate is DISCARDED and replaced by the refusal template plus the
    retrieved entries' warnings verbatim - wrong advice is structurally
    unshippable regardless of backend.
    """
    g = Guardrails()
    # 1. Secrets: refuse deterministically, never call any backend (spec §3.2).
    if composed.detections.is_secret:
        return {
            "answer": (
                "STOP — this looks like secret wallet material (seed words or a "
                "private key). This app has refused to process it. Never type or "
                "paste seed words into ANY app, website, or chat — including this "
                "one. No legitimate party — wallet vendor, support agent, or "
                "'recovery expert' — will ever need your words. If someone asked "
                "you for them, it is a scam. Handle the phrase only on paper, "
                "offline, and move the text out of this screen."
            ),
            "backend": "deterministic_secret_refusal",
        }
    # 2. Scam verdict: deterministic rules, scam-check mode only (no false
    # verdicts on innocent questions like price checks).
    import scam_rules
    verdict = None
    if composed.mode == "scam-check":
        verdict = scam_rules.scam_verdict_answer(question)
    messages = build_messages(question, composed)
    if verdict is not None:
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
        candidate = verdict + "\n\n" + body.strip()
    elif backend == "sail":
        candidate = sail_answer(messages)
        backend = "sail_glm53"
    else:
        try:
            candidate = llama_answer(messages)
            backend = "local_qwen3_1.7b"
        except LlamaServerError:
            if backend == "llama":
                raise
            candidate = sail_answer(messages)
            backend = "sail_glm53"

    # 3. Post-model deterministic guardrails (the veto layer).
    candidate, injected = g.scrub(candidate, question)
    failures = g.check_constraints(candidate, composed.constraints)
    if failures["must_not_include"]:
        warn_lines = []
        seen = set()
        for ch in composed.retrieved:
            for w in ch.get("warnings", []) or []:
                if w not in seen:
                    seen.add(w)
                    warn_lines.append("• " + w)
        candidate = (
            g.refusal("out_of_corpus")["text"] if g.refusal("out_of_corpus")
            else "I cannot give a safe answer to this from my knowledge base."
        ) + "\n\nSafety rules I must repeat:\n" + "\n".join(warn_lines)
    # 4. Inheritance mode: panic-action default comes first, always.
    if composed.mode == "walkthrough" and not candidate.startswith("Do nothing yet"):
        if any(t in question.lower() for t in (
                "power on", "plug in", "turn on", "send", "move", "sell",
                "install", "wipe", "reset", "transfer", "exchange", "check the balance")):
            candidate = ("Do nothing yet. Nothing is urgent. Take your time.\n\n" + candidate)
    # 5. Walkthrough mode: retrieved entries' warnings ship verbatim, always.
    if composed.mode == "walkthrough":
        warn_lines = []
        seen = set()
        for ch in composed.retrieved:
            for w in ch.get("warnings", []) or []:
                if w not in seen and w not in candidate:
                    seen.add(w)
                    warn_lines.append("• " + w)
        if warn_lines:
            candidate += "\n\nSafety rules for this step:\n" + "\n".join(warn_lines)
    return {"answer": candidate, "backend": backend}


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
