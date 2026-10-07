"""guardian_core.py — deterministic front-half of Bitcoin Custody Guardian.

Five components, all pure-Python/stdlib so they can be ported to Kotlin:

1. SECRET_DETECTOR   — regex + heuristic detection of secrets in text
                       (BIP-39 word runs, WIF keys, BIP-32 extended keys,
                       raw hex private keys, SLIP-39 shares).
2. Router            — intent classification (learn / scam-check / verify /
                       directory / walkthrough) + topic routing to corpus
                       topic IDs T01..T12 by keyword scoring.
3. Retrieval         — BM25 index over corpus JSON entries (title, plain,
                       technical, claims), built at load time.
4. Guardrails        — must_include / must_not_include enforcement,
                       danger-phrase scrubbing with mandatory warning
                       injection, refusal templates.
5. AnswerRunner      — compose(question, corpus_dir) -> the full assembled
                       context. The LLM answer step is a STUB here: it
                       returns 'grounded answer required' plus the assembled
                       context, because the real answer step ships in Kotlin
                       later. No network, no LLM calls anywhere in this file.

No network calls. No randomness. Deterministic given the same inputs and
corpus files.
"""

from __future__ import annotations

import json
import math
import os
import re
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

# --------------------------------------------------------------------------
# Data paths
# --------------------------------------------------------------------------

_HERE = os.path.dirname(os.path.abspath(__file__))
WORDLIST_PATH = os.path.join(_HERE, "data", "bip39_wordlist.txt")


def _load_wordlist() -> Tuple[frozenset, Dict[str, int]]:
    """Load the bundled BIP-39 English wordlist (2048 words)."""
    with open(WORDLIST_PATH, "r", encoding="utf-8") as fh:
        words = [w.strip() for w in fh if w.strip()]
    if len(words) != 2048:
        raise ValueError(
            "bundled BIP-39 wordlist must contain exactly 2048 words, got %d"
            % len(words)
        )
    return frozenset(words), {w: i for i, w in enumerate(words)}


BIP39_WORDS, _BIP39_INDEX = _load_wordlist()

# ==========================================================================
# 1. SECRET_DETECTOR
# ==========================================================================

# WIF: mainnet uncompressed (5...), mainnet compressed (K.../L...),
# testnet (9.../c...). Base58, excludes 0, O, I, l.
_WIF_RE = re.compile(r"\b[59HKLC][1-9A-HJ-NP-Za-km-z]{50,51}\b")

# BIP-32 extended keys (xprv/xpub/yprv/ypub/zprv/zpub/vprv/vpub).
_XKEY_RE = re.compile(
    r"\b(?:x|y|z|v)(?:prv|pub)[1-9A-HJ-NP-Za-km-z]{90,120}\b"
)

# Raw 64-hex private key (32 bytes). 64 hex chars not adjacent to more hex.
_HEX_KEY_RE = re.compile(r"(?<![0-9a-fA-F])[0-9a-fA-F]{64}(?![0-9a-fA-F])")

# SLIP-39 share: e.g. "duckling envelope academic ... numeral" —
# mnemonic-style words where the LAST word carries the share index.
# Heuristic: >= 20 words from a wordlist-ish vocabulary on one line.
_SLIP39_HINT_RE = re.compile(
    r"\b(?:share\s*\d+\s*(?:of|/)\s*\d+|group\s+\d+\s*(?:of|/)\s*\d+)\b",
    re.IGNORECASE,
)

_WORD_RE = re.compile(r"[a-z]+")

# Word counts accepted as BIP-39/SLIP-39 mnemonic runs.
_MNEMONIC_LENGTHS = {12, 15, 18, 21, 24}

SEED_WARNING = (
    "WARNING: Never share your seed phrase with anyone. Anyone who has your "
    "12/24 words can take your Bitcoin. No legitimate wallet service, "
    "exchange, or support agent will ever ask for it."
)
WIF_WARNING = (
    "WARNING: This looks like a private key (WIF format). Anyone who has it "
    "can spend your Bitcoin directly. Never type it into a website or share it."
)
HEX_WARNING = (
    "WARNING: This looks like a raw 64-character hexadecimal private key. "
    "Anyone who has it can spend your Bitcoin. Never share or paste it."
)
XPRV_WARNING = (
    "WARNING: This looks like an extended private key (xprv/zprv family). It "
    "can generate every key in its wallet — treat it like a seed phrase."
)
SLIP39_WARNING = (
    "WARNING: This looks like a SLIP-39 share. Shares alone are not enough to "
    "recover a wallet, but combine enough shares and the wallet is exposed."
)
GENERIC_SECRET_WARNING = SEED_WARNING


@dataclass
class Detection:
    """One detected secret candidate."""

    kind: str  # 'bip39_mnemonic' | 'wif_key' | 'xprv_key' | 'hex_key' | 'slip39_share'
    confidence: float  # 0.0 .. 1.0
    warning: str
    detail: str
    span: Tuple[int, int]  # character span in the input text


@dataclass
class DetectionResult:
    detections: List[Detection] = field(default_factory=list)

    @property
    def is_secret(self) -> bool:
        return bool(self.detections)

    @property
    def highest_confidence(self) -> float:
        return max((d.confidence for d in self.detections), default=0.0)

    def warnings(self) -> List[str]:
        seen, out = set(), []
        for d in self.detections:
            if d.warning not in seen:
                seen.add(d.warning)
                out.append(d.warning)
        return out


def _detect_word_runs(
    text: str, window_lengths: Tuple[int, ...] = _MNEMONIC_LENGTHS,
    min_len: int = 3, max_len: int = 12, fuzz: int = 1
) -> List[Detection]:
    """Sliding-window detection of BIP-39 word sequences.

    A window of candidate length L is flagged if all L tokens are valid
    BIP-39 words, or all but `fuzz` of them are (fuzz for typos/OCR noise).
    Confidence scales with run length and exactness.
    """
    lowered = text.lower()
    tokens = [
        (m.group(0), m.start(), m.end())
        for m in re.finditer(r"[a-zA-Z']+", lowered)
    ]
    words = [t[0] for t in tokens]
    detections: List[Detection] = []
    for L in sorted(window_lengths, reverse=True):
        for start in range(0, len(words) - L + 1):
            window = words[start : start + L]
            exact = sum(1 for w in window if w in BIP39_WORDS)
            # also allow very close typo matches (edit distance <= fuzz)
            close = exact
            for w in window:
                if w not in BIP39_WORDS and _close_word(w, fuzz):
                    close += 1
            misses = L - close
            if misses > fuzz:
                continue
            conf = 0.55 + 0.4 * (close / L)
            if misses == 0:
                conf = min(1.0, conf + 0.05)
            # longer runs are more convincing
            if L >= 24:
                conf = min(1.0, conf + 0.02)
            first_tok = tokens[start]
            last_tok = tokens[start + L - 1]
            detections.append(
                Detection(
                    kind="bip39_mnemonic",
                    confidence=round(conf, 3),
                    warning=SEED_WARNING,
                    detail="%d-word BIP-39 run (%d/%d valid words%s)"
                    % (
                        L,
                        exact,
                        L,
                        "; possible typo tolerated" if misses else "",
                    ),
                    span=(first_tok[1], last_tok[2]),
                )
            )
    # keep only maximal non-overlapping detections per start (prefer longest)
    return _dedupe(detections)


def _close_word(w: str, fuzz: int) -> bool:
    """True if some bundled BIP-39 word is within edit distance `fuzz`."""
    if fuzz <= 0 or len(w) < 3:
        return False
    for target in BIP39_WORDS:
        if abs(len(target) - len(w)) > fuzz:
            continue
        if _edit_distance_leq(w, target, fuzz):
            return True
    return False


def _edit_distance_leq(a: str, b: str, k: int) -> bool:
    """Levenshtein early-exit check: distance(a,b) <= k."""
    if a == b:
        return True
    la, lb = len(a), len(b)
    if abs(la - lb) > k:
        return False
    prev = list(range(lb + 1))
    for i in range(1, la + 1):
        cur = [i] + [0] * lb
        best = cur[0]
        for j in range(1, lb + 1):
            cost = 0 if a[i - 1] == b[j - 1] else 1
            cur[j] = min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            best = min(best, cur[j])
        if best > k:
            return False
        prev = cur
    return prev[lb] <= k


def _dedupe(dets: List[Detection]) -> List[Detection]:
    """Collapse overlapping detections of the same kind, longest first."""
    dets = sorted(
        dets, key=lambda d: (-(d.span[1] - d.span[0]), -d.confidence, d.span)
    )
    kept: List[Detection] = []
    for d in dets:
        if any(
            _overlaps(d.span, k.span) and k.kind == d.kind for k in kept
        ):
            continue
        kept.append(d)
    return sorted(kept, key=lambda d: d.span)


def _overlaps(a: Tuple[int, int], b: Tuple[int, int]) -> bool:
    return a[0] < b[1] and b[0] < a[1]


class SecretDetector:
    """Detects secret material in free text. Regex + heuristics only."""

    def scan(self, text: str) -> DetectionResult:
        result = DetectionResult()
        if not text:
            return result

        # --- structured regex detectors ---------------------------------
        for m in _XKEY_RE.finditer(text):
            result.detections.append(
                Detection(
                    kind="xprv_key",
                    confidence=0.98,
                    warning=XPRV_WARNING,
                    detail="extended private key prefix (%s…)"
                    % m.group(0)[:4],
                    span=(m.start(), m.end()),
                )
            )
        for m in _WIF_RE.finditer(text):
            result.detections.append(
                Detection(
                    kind="wif_key",
                    confidence=0.95,
                    warning=WIF_WARNING,
                    detail="WIF private key (prefix %s)" % m.group(0)[0],
                    span=(m.start(), m.end()),
                )
            )
        for m in _HEX_KEY_RE.finditer(text):
            result.detections.append(
                Detection(
                    kind="hex_key",
                    confidence=0.85,
                    warning=HEX_WARNING,
                    detail="64-hex-character private key",
                    span=(m.start(), m.end()),
                )
            )

        # --- SLIP-39 share heuristics ------------------------------------
        if _SLIP39_HINT_RE.search(text):
            result.detections.append(
                Detection(
                    kind="slip39_share",
                    confidence=0.9,
                    warning=SLIP39_WARNING,
                    detail='explicit "share N of M" phrasing',
                    span=(0, len(text)),
                )
            )

        # --- BIP-39 word runs --------------------------------------------
        result.detections.extend(_detect_word_runs(text))

        result.detections = _dedupe(result.detections)
        return result


# ==========================================================================
# 2. ROUTER
# ==========================================================================

MODES = ("learn", "scam-check", "verify", "directory", "walkthrough")

_MODE_KEYWORDS: Dict[str, List[str]] = {
    "learn": [
        "what", "why", "how does", "explain", "understand", "learn",
        "difference", "mean", "meaning", "definition", "tell me about",
        "beginner", "introduction", "introduce", "concept",
    ],
    "scam-check": [
        "scam", "scammer", "phishing", "fraud", "fake", "suspicious",
        "legit", "legitimate", "asked for my", "someone asked", "should i give",
        "give out", "shared my", "is this safe", "red flag", "trick",
        "someone contacted", "dm", "cold contact", "support agent",
        "recovery agent", "recovery service", "recovery expert",
        "unlock", "verification fee", "double your bitcoin", "giveaway",
        "sextortion", "remote access", "anydesk", "teamviewer",
        "pig butchering", "romance", "investment opportunity",
    ],
    "verify": [
        "verify", "check", "validate", "confirm", "checksum", "test vector",
        "did i do this right", "double-check", "audit", "review my",
        "is my backup correct", "correct backup",
    ],
    "directory": [
        "list of", "which wallets", "what wallets", "options", "who makes",
        "vendors", "hardware wallet brands", "where can i buy", "compare",
        "products", "services", "tools", "overview of tools",
    ],
    "walkthrough": [
        "walkthrough", "step by step", "steps", "inherit", "inheritance",
        "executor", "heir", "legacy", "when i die", "after i die", "estate",
        "hand over", "successor", "loved ones", "family",
        "verification day", "day one", "first hours", "inherited",
        "my father", "my mother", "my husband", "my wife", "my spouse",
    ],
}

# Topic keyword scoring: T01..T12.
TOPIC_KEYWORDS: Dict[str, List[str]] = {
    "T01": [
        "wallet", "self-custody", "self custody", "custody", "keys", "hold",
        "your own bitcoin", "not your keys", "ownership", "control",
        "exchange account", "hotline", "refund", "insurance",
    ],
    "T02": [
        "seed phrase", "seed", "recovery phrase", "mnemonic", "bip-39",
        "bip39", "bip 39", "wordlist", "words", "12 words", "24 words",
        "backup phrase", "passphrase", "derivation", "m/44", "m/84",
        "entropy",
    ],
    "T03": [
        "private key", "public key", "address", "wif", "hex key",
        "spending key", "key format", "cold storage", "sparrow",
    ],
    "T04": [
        "hardware wallet", "signer", "device", "cold storage", "secure element",
        "pin", "firmware", "trezor", "ledger", "bitbox", "coldcard",
        "jade", "seedsigner", "krux", "bitkey",
    ],
    "T05": [
        "multisig", "multi-sig", "m of n", "quorum", "cosigner", "descriptor",
        "policy",
    ],
    "T06": [
        "inheritance", "inherit", "inherited", "heir", "estate", "day one",
        "first hours", "verification day", "inventory", "walkthrough",
        "died", "passed away", "spouse", "executor",
    ],
    "T07": [
        "scam", "phishing", "social engineering", "fraud", "fake support",
        "impersonation", "giveaway", "romance", "pressure", "urgency",
        "recovery agent", "recovery service", "double your", "sextortion",
        "pig butchering", "investment opportunity",
    ],
    "T08": [
        "red flag", "urgent", "deadline", "anydesk", "teamviewer",
        "remote control", "verify your wallet", "locked out",
        "support team", "dm", "suspicious message",
    ],
    "T09": [
        "privacy", "inventory privacy", "tell family", "anonymous", "doxx",
        "balance private",
    ],
    "T10": [
        "transaction", "signing", "broadcast", "fee", "confirmations",
        "send bitcoin", "receive bitcoin", "utxo", "satoshi", "mempool",
        "irreversible", "jargon", "what does.*mean",
    ],
    "T11": [
        "node", "full node", "pruned", "verification", "trustless",
        "run a node", "electrum server", "consensus",
    ],
    "T12": [
        "glossary", "directory", "terms", "definitions", "jargon",
        "vocabulary", "mistakes", "recovery stories",
    ],
}


class Router:
    """Intent (mode) + topic routing. Deterministic keyword scoring."""

    def classify_mode(self, question: str) -> str:
        q = " " + question.lower() + " "
        scores = {m: 0 for m in MODES}
        for mode, kws in _MODE_KEYWORDS.items():
            for kw in kws:
                if kw in q:
                    scores[mode] += 1
        # scam-check and walkthrough outrank learn on ties (safer defaults)
        priority = {"scam-check": 1, "walkthrough": 1}
        best = max(
            MODES,
            key=lambda m: (scores[m] + priority.get(m, 0), -MODES.index(m)),
        )
        return best if scores[best] > 0 else "learn"

    def classify_topics(self, question: str, top_k: int = 3) -> List[str]:
        q = " " + question.lower() + " "
        scores: Dict[str, int] = {}
        for topic, kws in TOPIC_KEYWORDS.items():
            s = 0
            for kw in kws:
                if kw in q:
                    s += len(kw.split()) + 1  # multiword matches weigh more
            if s:
                scores[topic] = s
        ranked = sorted(scores.items(), key=lambda kv: (-kv[1], kv[0]))
        return [t for t, _ in ranked[:top_k]] or ["T02"]


# ==========================================================================
# 3. RETRIEVAL — BM25
# ==========================================================================

_BM25_K1 = 1.5
_BM25_B = 0.75

_STOPWORDS = frozenset(
    "a an and are as at be but by for from has have how i in is it its of on "
    "or that the this to was what when where which who will with you your do "
    "does my me we our".split()
)

_TOKEN_RE = re.compile(r"[a-z0-9][a-z0-9\-]*")


def _tokenize(text: str) -> List[str]:
    return [t for t in _TOKEN_RE.findall(text.lower()) if t not in _STOPWORDS]


@dataclass
class Chunk:
    """One retrievable chunk of a corpus entry."""

    entry_id: str
    title: str
    section: str  # 'title' | 'plain' | 'technical' | 'claim' | 'warning'
    text: str
    chunk_id: str


class BM25Index:
    """In-memory BM25 index. Built at load; deterministic."""

    def __init__(self) -> None:
        self.chunks: List[Chunk] = []
        self._docs: List[List[str]] = []
        self._doc_tf: List[Dict[str, int]] = []
        self._df: Dict[str, int] = {}
        self._avgdl: float = 0.0

    def add_corpus_dir(self, corpus_dir: str) -> None:
        for fname in sorted(os.listdir(corpus_dir)):
            if not fname.endswith(".json"):
                continue
            with open(os.path.join(corpus_dir, fname), "r", encoding="utf-8") as fh:
                entry = json.load(fh)
            self.add_entry(entry)

    def add_entry(self, entry: dict) -> None:
        eid = entry.get("topic_ids", ["T00"])[0] + ":" + _slug(entry["title"])
        sections: List[Tuple[str, str]] = [("title", entry["title"])]
        for key in ("plain", "technical"):
            if entry.get(key):
                # split plain text into paragraph chunks for finer ranking
                if key == "plain":
                    paras = [p for p in entry[key].split("\n\n") if p.strip()]
                    for i, p in enumerate(paras):
                        sections.append(("%s.p%d" % (key, i), p))
                else:
                    sections.append((key, entry[key]))
        for i, c in enumerate(entry.get("claims", [])):
            sections.append(
                ("claim.%d" % i, c.get("claim", "") + " " + c.get("source", ""))
            )
        for i, w in enumerate(entry.get("warnings", [])):
            sections.append(("warning.%d" % i, w))
        for section, text in sections:
            toks = _tokenize(text)
            idx = len(self.chunks)
            self.chunks.append(
                Chunk(
                    entry_id=eid,
                    title=entry["title"],
                    section=section,
                    text=text,
                    chunk_id="%s#%s" % (eid, section),
                )
            )
            self._docs.append(toks)
            tf: Dict[str, int] = {}
            for t in toks:
                tf[t] = tf.get(t, 0) + 1
            self._doc_tf.append(tf)
        # update df
        for tf in self._doc_tf[-len(sections):]:
            for term in tf:
                self._df[term] = self._df.get(term, 0) + 1
        self._avgdl = sum(len(d) for d in self._docs) / max(1, len(self._docs))

    def search(self, query: str, top_k: int = 5) -> List[Tuple[Chunk, float]]:
        if not self.chunks:
            return []
        q_terms = _tokenize(query)
        if not q_terms:
            return []
        n = len(self._docs)
        scores: List[float] = [0.0] * n
        for doc_i in range(n):
            tf = self._doc_tf[doc_i]
            dl = len(self._docs[doc_i])
            s = 0.0
            for term in q_terms:
                f = tf.get(term)
                if not f:
                    continue
                idf = math.log(1.0 + (n - self._df.get(term, 0) + 0.5) / (self._df.get(term, 0) + 0.5))
                s += idf * (f * (_BM25_K1 + 1)) / (
                    f + _BM25_K1 * (1 - _BM25_B + _BM25_B * dl / max(1.0, self._avgdl))
                )
            scores[doc_i] = s
        ranked = sorted(
            range(n), key=lambda i: (-scores[i], self.chunks[i].chunk_id)
        )
        return [
            (self.chunks[i], round(scores[i], 4))
            for i in ranked[:top_k]
            if scores[i] > 0.0
        ]


def _slug(title: str) -> str:
    s = re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")
    return s[:40]


class Retriever:
    """BM25 retriever over a corpus directory. Index built at load."""

    def __init__(self, corpus_dir: str) -> None:
        self.index = BM25Index()
        self.index.add_corpus_dir(corpus_dir)

    def retrieve(self, query: str, top_k: int = 5,
                 prefer_topics: Optional[List[str]] = None) -> List[Dict]:
        scored = self.index.search(query, max(top_k * 12, 60))
        pref = set(prefer_topics or [])
        boosted = []
        for chunk, score in scored:
            boost = 0.0
            if pref:
                chunk_topics = chunk.entry_id.split(":")[0].split("+")
                overlap = len(pref & set(chunk_topics))
                if overlap:
                    boost = 2.0 + overlap  # topic-prior re-rank
            if chunk.section.startswith("claim"):
                boost += 0.8  # curated actionable guidance outranks background prose
            boosted.append((chunk, round(score + boost, 4)))

        # Entry-level aggregation: rank whole entries by their best chunk so
        # one matching claim pulls in its sibling claims/warnings. Title-token
        # overlap boosts entries whose title matches the question vocabulary,
        # which BM25 chunk scoring alone can miss.
        q_tokens = set(_tokenize(query))
        by_entry: Dict[str, List[Tuple[Chunk, float]]] = {}
        title_boost: Dict[str, float] = {}
        for chunk, score in boosted:
            by_entry.setdefault(chunk.entry_id, []).append((chunk, score))
            if chunk.entry_id not in title_boost and chunk.section == "title":
                ov = len(q_tokens & set(_tokenize(chunk.text)))
                title_boost[chunk.entry_id] = min(ov, 4) * 0.9
        entries = sorted(
            by_entry.items(),
            key=lambda kv: max(s for _, s in kv[1])
            + title_boost.get(kv[0], 0.0),
            reverse=True,
        )
        out: List[Dict] = []
        # Global-score fill with a per-entry cap: chunks enter strictly by
        # score, but one dominant entry cannot take more than cap slots, so
        # its remaining slots go to the next-best chunks.
        cap = max(3, top_k - 2)
        taken: Dict[str, int] = {}
        flat = sorted((cs for _, chunks in entries for cs in chunks),
                      key=lambda cs: -cs[1])
        for chunk, score in flat:
            if len(out) >= top_k:
                break
            if taken.get(chunk.entry_id, 0) >= cap:
                continue
            taken[chunk.entry_id] = taken.get(chunk.entry_id, 0) + 1
            out.append(
                {
                    "entry_id": chunk.entry_id,
                    "section": chunk.section,
                    "title": chunk.title,
                    "text": chunk.text,
                    "score": score,
                }
            )
        # Entry completion: every entry with >=2 chunks in the budget gets its
        # remaining claim chunks appended - a matched entry is presented in
        # full, not as scattered fragments. Hard cap: top_k + 4 total.
        if out:
            counts: Dict[str, int] = {}
            for c in out:
                counts[c["entry_id"]] = counts.get(c["entry_id"], 0) + 1
            for best_entry, _ in sorted(counts.items(), key=lambda kv: -kv[1]):
                if counts[best_entry] < 2 or len(out) >= top_k + 4:
                    continue
                extras_left = 2  # fair share: no single entry hoards completion
                in_out = {c["section"] for c in out if c["entry_id"] == best_entry}
                for chunk, score in flat:
                    if len(out) >= top_k + 4 or extras_left <= 0:
                        break
                    if chunk.entry_id != best_entry or chunk.section in in_out:
                        continue
                    in_out.add(chunk.section)
                    extras_left -= 1
                    out.append(
                        {
                            "entry_id": chunk.entry_id,
                            "section": chunk.section,
                            "title": chunk.title,
                            "text": chunk.text,
                            "score": score,
                        }
                    )
        return out


# ==========================================================================
# 4. GUARDRAILS
# ==========================================================================

# Danger phrases that, near seed topics, mandate an injected warning.
DANGER_PHRASES = [
    "reset the device",
    "reset device",
    "wipe the device",
    "factory reset",
    "restore without checking",
    "type your seed into",
    "enter your seed on",
    "share your seed",
    "share your phrase",
    "verify by phone",
    "remote support",
]

SEED_TOPIC_TERMS = [
    "seed", "mnemonic", "recovery phrase", "wordlist", "backup",
    "bip-39", "bip39", "bip 39", "words",
]

REFUSAL_OUT_OF_CORPUS = {
    "template": "out_of_corpus",
    "text": (
        "I can't answer that from the verified corpus. This app only answers "
        "questions covered by its reviewed curriculum (topics T01–T12). "
        "Try asking about wallets, seed phrases, backups, scams, or "
        "inheritance planning."
    ),
}

REFUSAL_OFFLINE = {
    "template": "offline_or_network",
    "text": (
        "I can't help with that here. Questions about buying, exchanging, "
        "prices, or anything requiring live internet data are out of scope "
        "for this offline, privacy-first app."
    ),
}


@dataclass
class Constraint:
    kind: str  # 'must_include' | 'must_not_include'
    phrase: str
    description: str


class Guardrails:
    """Post-answer checks + refusal templates."""

    @staticmethod
    def is_seed_related(text: str) -> bool:
        t = text.lower()
        return any(term in t for term in SEED_TOPIC_TERMS)

    @staticmethod
    def find_danger_phrases(text: str) -> List[str]:
        t = text.lower()
        return [p for p in DANGER_PHRASES if p in t]

    @staticmethod
    def mandatory_warning(danger_phrases: List[str]) -> str:
        return (
            "WARNING: Never reset or wipe a device that still holds your "
            "Bitcoin until your seed phrase backup has been fully verified. "
            "Never share your seed phrase with anyone — no real support "
            "agent will ever ask for it. If someone instructs you to do "
            "either, it is a scam. Stop and verify your backup offline first."
        )

    def scrub(
        self, answer: str, question: str = ""
    ) -> Tuple[str, List[str]]:
        """Return (scrubbed_answer, injected_warnings).

        If a danger phrase appears in an answer to a seed-related question,
        the mandatory warning is injected after the offending sentence.
        """
        injected: List[str] = []
        if not answer:
            return answer, injected
        danger = self.find_danger_phrases(answer)
        if danger and self.is_seed_related(answer + " " + question):
            warning = self.mandatory_warning(danger)
            # split into sentences, inject after the sentence containing the
            # first danger phrase
            sentences = re.split(r"(?<=[.!?])\s+", answer)
            out: List[str] = []
            placed = False
            for s in sentences:
                out.append(s)
                if not placed and any(p in s.lower() for p in danger):
                    out.append(warning)
                    injected.append(warning)
                    placed = True
            if not placed:
                out.append(warning)
                injected.append(warning)
            return " ".join(out), injected
        return answer, injected

    @staticmethod
    def check_constraints(
        answer: str, constraints: List[Constraint]
    ) -> Dict[str, List[str]]:
        """Verify an answer against must_include / must_not_include rules."""
        t = answer.lower()
        failures: Dict[str, List[str]] = {"must_include": [], "must_not_include": []}
        for c in constraints:
            present = c.phrase.lower() in t
            if c.kind == "must_include" and not present:
                failures["must_include"].append(c.phrase)
            elif c.kind == "must_not_include" and present:
                failures["must_not_include"].append(c.phrase)
        return failures

    @staticmethod
    def refusal(kind: str) -> Optional[Dict[str, str]]:
        if kind == "out_of_corpus":
            return dict(REFUSAL_OUT_OF_CORPUS)
        if kind == "offline_or_network":
            return dict(REFUSAL_OFFLINE)
        return None

    @staticmethod
    def detect_refusal_kind(question: str) -> Optional[str]:
        """Deterministic refusal routing for clearly out-of-scope questions.

        "Buy/where to buy" and price questions are refused, but withdrawal
        questions ("move my coins to a wallet") are self-custody topics the
        corpus answers — never refuse those.
        """
        q = question.lower()
        if any(t in q for t in ("withdraw", "move my", "move your", "transfer to",
                                "send to my wallet", "own wallet")):
            return None
        offline_terms = [
            "price", "exchange rate", "buy bitcoin", "where to buy",
            "stock", "news", "weather", "send an email", "latest",
            "current block", "mempool", "convert to dollars",
        ]
        if any(t in q for t in offline_terms):
            return "offline_or_network"
        return None


# ==========================================================================
# 5. ANSWER_RUNNER
# ==========================================================================

STUB_ANSWER = "grounded answer required"


@dataclass
class Composed:
    """Result of compose(): everything the (future, Kotlin) answer step needs."""

    mode: str
    topics: List[str]
    retrieved: List[Dict]
    constraints: List[Constraint]
    refusal: Optional[Dict[str, str]]
    detections: DetectionResult
    scrubbed: bool = False
    answer: Optional[str] = None  # stub — see module docstring

    def to_dict(self) -> Dict:
        return {
            "mode": self.mode,
            "topics": self.topics,
            "retrieved": self.retrieved,
            "constraints": [
                {"kind": c.kind, "phrase": c.phrase, "description": c.description}
                for c in self.constraints
            ],
            "refusal": self.refusal,
            "detections": [
                {
                    "kind": d.kind,
                    "confidence": d.confidence,
                    "detail": d.detail,
                    "warning": d.warning,
                }
                for d in self.detections.detections
            ],
            "scrubbed": self.scrubbed,
            "answer": self.answer,
        }


def compose(
    question: str,
    corpus_dir: str,
    candidate_answer: Optional[str] = None,
    top_k: int = 5,
) -> Composed:
    """The full deterministic front-half.

    Pipeline: detect secrets -> route mode/topics -> retrieve -> build
    constraints -> guardrail-scrub any candidate answer -> stub or refusal.

    The LLM answer step is a STUB: without a grounded candidate answer it
    returns STUB_ANSWER with the assembled context. The real answer
    generation ships in Kotlin later.
    """
    detector = SecretDetector()
    router = Router()
    guard = Guardrails()
    retriever = Retriever(corpus_dir)

    detections = detector.scan(question)
    mode = router.classify_mode(question)
    topics = router.classify_topics(question)
    retrieved = retriever.retrieve(question, top_k=top_k, prefer_topics=topics)

    refusal = None
    if guard.detect_refusal_kind(question):
        refusal = guard.refusal("offline_or_network")
    elif not retrieved:
        refusal = guard.refusal("out_of_corpus")

    constraints = _build_constraints(mode, topics, retrieved)

    answer = None
    scrubbed = False
    if candidate_answer:
        scrubbed_answer, injected = guard.scrub(candidate_answer, question)
        answer = scrubbed_answer
        scrubbed = bool(injected)
    else:
        answer = STUB_ANSWER

    return Composed(
        mode=mode,
        topics=topics,
        retrieved=retrieved,
        constraints=constraints,
        refusal=refusal,
        detections=detections,
        scrubbed=scrubbed,
        answer=answer,
    )


def _build_constraints(
    mode: str, topics: List[str], retrieved: List[Dict]
) -> List[Constraint]:
    """Deterministic constraints derived from mode + topics + retrieval."""
    cons: List[Constraint] = []
    seed_topics = {"T02", "T06", "T09"}
    if seed_topics & set(topics):
        cons.append(
            Constraint(
                "must_include",
                "never share your seed phrase",
                "Seed-topic answers must carry the never-share warning.",
            )
        )
    cons.append(
        Constraint(
            "must_not_include",
            "type your seed into",
            "Answers must never instruct pasting a seed into a website.",
        )
    )
    cons.append(
        Constraint(
            "must_not_include",
            "reset the device",
            "Answers must never instruct a device reset near seed topics.",
        )
    )
    if mode == "scam-check":
        cons.append(
            Constraint(
                "must_include",
                "scam",
                "Scam-check answers must name the threat explicitly.",
            )
        )
    if mode == "verify" or "T09" in topics:
        cons.append(
            Constraint(
                "must_include",
                "verify",
                "Verify-mode answers must mention verification.",
            )
        )
    return cons
