"""scam_rules.py — deterministic scam adjudication for the answer harness.

Port of the Android ScamRules.kt red-flag rule set (spec: scam verdicts must
be decided by rules, not by the LLM; a 1.7B model hedges scam verdicts).
Same rules, same verdicts as the shipped app, so eval behavior and app
behavior stay aligned.
"""
import re

# (id, title, pattern, why)
RULES = [
    (
        "seed_request",
        "Asks for seed phrase / keys",
        re.compile(
            r"(send\s*(me|us)?\s*your?\s*(seed|recovery|words|private|mnemonic|xprv)|"
            r"share\s*your\s*(seed|recovery|words|private|mnemonic|xprv)|"
            r"enter\s*your\s*(seed|recovery|words|private|mnemonic)|"
            r"confirm\s*your\s*(seed|recovery|words|mnemonic)|"
            r"give\s*(me|us)\s*(your|the)\s*(seed|recovery|words|private|mnemonic|keys)|"
            r"ask(s|ed)?\s*(me|you|for)?\s*(my|your)\s*(seed|recovery|words|private\s*keys?|mnemonic)|"
            r"read\s*(out|me)\s*(your|the)\s*(seed|recovery|words)|"
            r"type\s*(your|the)\s*(seed|recovery|words|private))",
            re.IGNORECASE,
        ),
        "No legitimate party — vendor, support, or \"recovery expert\" — ever needs "
        "your seed phrase or keys. Anyone who asks is a scammer.",
    ),
    (
        "remote_software",
        "Remote-control software",
        re.compile(
            r"(anydesk|teamviewer|remote\s*desktop|\"support\s*tool\"|install\s*the\s*support|"
            r"screen\s*shar)",
            re.IGNORECASE,
        ),
        "Scammers push remote-control apps (AnyDesk, TeamViewer) to watch your "
        "screen and capture secrets or take over your device.",
    ),
    (
        "urgency",
        "Artificial urgency",
        re.compile(
            r"(urgent|urgently|deadline|immediately|right\s*now|expires?\s*today|"
            r"last\s*chance|act\s*now|funds?\s*(are|is)\s*at\s*risk|locked\s*out)",
            re.IGNORECASE,
        ),
        "Pressure to act fast is a control tactic. Nothing in Bitcoin is urgent "
        "for a custodian: waiting loses nothing; rushing loses everything.",
    ),
    (
        "pay_to_unlock",
        "\"Send a small amount first\"",
        re.compile(
            r"(send\s*a\s*small\s*(amount|fee)|unlock|test\s*transaction|verification\s*fee|"
            r"gas\s*fee|processing\s*fee|pay\s*(the|a)\s*fee)",
            re.IGNORECASE,
        ),
        "\"Send a small amount first to unlock/test/verify\" is a classic scam "
        "marker.",
    ),
    (
        "unsolicited",
        "They contacted you first",
        re.compile(
            r"(i\s*noticed|i\s*found\s*your|we\s*detected|unusual\s*activity\s*on\s*your|"
            r"i\s*reached\s*out|someone\s*gave\s*me\s*your|reaching\s*out\s*regarding)",
            re.IGNORECASE,
        ),
        "Any stranger who contacts YOU first about crypto is a scam by default. "
        "This app never contacts anyone and asks you for nothing.",
    ),
    (
        "guarantee",
        "Guaranteed results",
        re.compile(
            r"(100%|guarantee|guaranteed|risk\s*free|risk\s*-\s*free|refund\s*policy|"
            r"surely|always\s*succeed)",
            re.IGNORECASE,
        ),
        "No honest party guarantees recovery of lost keys — impossible by design. "
        "Guaranteed-recovery offers are scams.",
    ),
    (
        "fake_official",
        "Impersonates an official service",
        re.compile(
            r"(official\s*(support|team)|certified\s*(agent|partner)|ledger\s*support|"
            r"coinbase\s*support|blockchain\s*support|support\s*team\s*here|"
            r"customer\s*care\s*department)",
            re.IGNORECASE,
        ),
        "Real wallet vendors never initiate contact and never ask for keys; "
        "impersonating \"official support\" is the most common crypto scam pattern.",
    ),
    (
        "injection",
        "Prompt-injection style instructions",
        re.compile(
            r"(ignore\s*(all\s*)?(previous|prior)\s*instructions?|disregard\s*(the|your)\s*"
            r"(system|safety)|you\s*are\s*now|new\s*instructions?:|assistant\s*mode)",
            re.IGNORECASE,
        ),
        "The text tries to override the assistant's safety rules. In this app your "
        "message is treated as DATA, never as instructions — but the presence of "
        "injection language itself is a red flag for the message's origin.",
    ),
]

CLEAN_VERDICT = (
    "No red flags from the local rule set. That is NOT a guarantee the message "
    "is safe — the safest answer is still: stop contact, do nothing, and never "
    "share keys or seed words with anyone."
)


def scan(text: str) -> dict:
    """Return {"flagged": [...], "clean": bool, "verdict": str}.

    flagged items: {"id", "title", "why"}.
    """
    hits = [
        {"id": rid, "title": title, "why": why}
        for rid, title, pat, why in RULES
        if pat.search(text)
    ]
    if not hits:
        return {"flagged": [], "clean": True, "verdict": CLEAN_VERDICT}
    return {
        "flagged": hits,
        "clean": False,
        "verdict": (
            "%d red flag(s) detected. The safe answer is always: stop contact, "
            "do nothing, ask this app. You lose nothing by ignoring the message."
            % len(hits)
        ),
    }


def scam_verdict_answer(text: str) -> "str | None":
    """Deterministic scam answer, or None if no red flags.

    Builds the answer from rule explanations alone — no model involved. Used
    as a short-circuit in answer_step for scam-check questions: the verdict
    must never depend on a small model's confidence.
    """
    r = scan(text)
    if r["clean"]:
        return None
    parts = [
        "This shows classic scam red flags. Stop contact and do nothing further.",
        "",
        r["verdict"],
        "",
    ]
    for f in r["flagged"]:
        parts.append("• %s — %s" % (f["title"], f["why"]))
    parts.append("")
    parts.append(
        "Reminder: transactions are final and there is no hotline that can "
        "recover lost self-custodied bitcoin. Never type, photograph, or read "
        "out your seed phrase for anyone."
    )
    return "\n".join(parts)
