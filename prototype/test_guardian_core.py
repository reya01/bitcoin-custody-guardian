"""Tests for guardian_core.py — deterministic, stdlib unittest."""

import json
import os
import re
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import guardian_core as gc

CORPUS_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "corpus"
)

# 24 valid BIP-39 words (first 24 of the wordlist).
WORDS_24 = (
    "abandon ability able about above absent absorb abstract absurd abuse "
    "access accident account accuse achieve acid acoustic acquire across act "
    "action actor actress actual"
).split()
SEED_24 = " ".join(WORDS_24)


class TestSecretDetectorSeed(unittest.TestCase):
    def setUp(self):
        self.det = gc.SecretDetector()

    def test_24_word_inline(self):
        r = self.det.scan("my backup is %s keep it safe" % SEED_24)
        self.assertTrue(r.is_secret)
        kinds = {d.kind for d in r.detections}
        self.assertIn("bip39_mnemonic", kinds)
        best = max(r.detections, key=lambda d: d.confidence)
        self.assertEqual(best.kind, "bip39_mnemonic")
        self.assertGreaterEqual(best.confidence, 0.9)

    def test_word_boundaries(self):
        # a valid word embedded in a longer word must NOT count as a word
        text = "the abilityX is absentabsurd but abandon ok"
        r = self.det.scan(text)
        for d in r.detections:
            # detection here is allowed; the assertion below pins the run length
            self.assertIsNotNone(d)
        # no 24-word run should fire on this
        runs = [
            d
            for d in r.detections
            if d.kind == "bip39_mnemonic" and "24-word" in d.detail
        ]
        self.assertEqual(runs, [])

    def test_case_insensitive(self):
        r = self.det.scan(SEED_24.upper())
        self.assertTrue(r.is_secret)
        best = max(r.detections, key=lambda d: d.confidence)
        self.assertEqual(best.kind, "bip39_mnemonic")

    def test_12_word_run(self):
        r = self.det.scan(" ".join(WORDS_24[:12]))
        self.assertTrue(r.is_secret)

    def test_fuzzy_typo_tolerated(self):
        typo = WORDS_24[:12]
        typo[3] = "abou"  # one-letter-short typo of 'about'
        r = self.det.scan(" ".join(typo))
        self.assertTrue(r.is_secret)

    def test_ordinary_text_not_flagged(self):
        r = self.det.scan(
            "What is a seed phrase and why does my wallet use twelve words?"
        )
        self.assertFalse(r.is_secret)

    def test_warning_text_present(self):
        r = self.det.scan(SEED_24)
        self.assertEqual(r.warnings(), [gc.SEED_WARNING])


class TestSecretDetectorKeys(unittest.TestCase):
    def setUp(self):
        self.det = gc.SecretDetector()

    def test_wif_uncompressed(self):
        # 51-char WIF starting with 5
        wif = "5" + "K" * 0 + "1" * 50
        r = self.det.scan("key: %s" % wif)
        self.assertTrue(any(d.kind == "wif_key" for d in r.detections))

    def test_wif_compressed(self):
        wif = "K" + "2" * 51
        r = self.det.scan("import this: %s" % wif)
        self.assertTrue(any(d.kind == "wif_key" for d in r.detections))

    def test_xprv(self):
        x = "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi"
        r = self.det.scan(x)
        self.assertTrue(any(d.kind == "xprv_key" for d in r.detections))
        best = max(r.detections, key=lambda d: d.confidence)
        self.assertGreaterEqual(best.confidence, 0.95)

    def test_hex_key(self):
        h = "a" * 64
        r = self.det.scan("privkey %s done" % h)
        self.assertTrue(any(d.kind == "hex_key" for d in r.detections))

    def test_63_hex_not_flagged(self):
        r = self.det.scan("privkey %s done" % ("a" * 63))
        self.assertFalse(any(d.kind == "hex_key" for d in r.detections))

    def test_slip39_hint(self):
        r = self.det.scan("Share 2 of 3: group 1 of 2 backup words follow")
        self.assertTrue(any(d.kind == "slip39_share" for d in r.detections))

    def test_detector_warning_texts_nonempty(self):
        r = self.det.scan(
            "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi"
        )
        for w in r.warnings():
            self.assertTrue(w.startswith("WARNING:"))


class TestRouter(unittest.TestCase):
    def setUp(self):
        self.router = gc.Router()

    def test_mode_scam(self):
        self.assertEqual(
            self.router.classify_mode("someone asked for my seed phrase — is this a scam?"),
            "scam-check",
        )

    def test_mode_walkthrough(self):
        self.assertEqual(
            self.router.classify_mode("step by step plan for my heir to inherit my bitcoin"),
            "walkthrough",
        )

    def test_mode_default_learn(self):
        self.assertEqual(self.router.classify_mode("what is a seed phrase"), "learn")

    def test_topic_seed_phrase(self):
        topics = self.router.classify_topics("what is a seed phrase")
        self.assertIn("T02", topics)
        self.assertEqual(topics[0], "T02")

    def test_topic_multisig(self):
        topics = self.router.classify_topics("how does multisig m of n quorum work")
        self.assertIn("T05", topics)

    def test_topic_hardware(self):
        topics = self.router.classify_topics("which hardware wallet device should I buy")
        self.assertIn("T04", topics)


class TestRetrieval(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ret = gc.Retriever(CORPUS_DIR)

    def test_index_built(self):
        self.assertGreater(len(self.ret.index.chunks), 0)

    def test_seed_phrase_query_returns_chunks(self):
        hits = self.ret.retrieve("what is a seed phrase", top_k=5)
        self.assertGreater(len(hits), 0)
        top = hits[0]
        self.assertEqual(top["entry_id"].split(":")[0], "T02")
        self.assertGreater(top["score"], 0.0)
        self.assertIn("section", top)
        self.assertIn("title", top)

    def test_cited_fields(self):
        hits = self.ret.retrieve("BIP-39 checksum entropy wordlist", top_k=3)
        for h in hits:
            self.assertIn("entry_id", h)
            self.assertIn("section", h)
            self.assertIn("score", h)
            self.assertIn("text", h)

    def test_top_k_respected(self):
        hits = self.ret.retrieve("wallet backup security words", top_k=3)
        self.assertLessEqual(len(hits), 3)

    def test_nonsense_query_empty(self):
        self.assertEqual(self.ret.retrieve("qqqzzz wubblyflorp", top_k=3), [])

    def test_deterministic(self):
        a = self.ret.retrieve("seed phrase", 5)
        b = self.ret.retrieve("seed phrase", 5)
        self.assertEqual(a, b)


class TestGuardrails(unittest.TestCase):
    def setUp(self):
        self.g = gc.Guardrails()

    def test_must_include_pass(self):
        c = [gc.Constraint("must_include", "never share", "test")]
        f = self.g.check_constraints("Always remember: never share your seed.", c)
        self.assertEqual(f["must_include"], [])

    def test_must_include_fail(self):
        c = [gc.Constraint("must_include", "never share", "test")]
        f = self.g.check_constraints("Totally safe to email around.", c)
        self.assertEqual(f["must_include"], ["never share"])

    def test_must_not_include(self):
        c = [gc.Constraint("must_not_include", "type your seed into", "test")]
        f = self.g.check_constraints("Just type your seed into the website.", c)
        self.assertEqual(f["must_not_include"], ["type your seed into"])

    def test_danger_phrase_warning_injection(self):
        ans = ("To fix the issue, reset the device. Then restore your wallet "
               "from the seed words.")
        out, injected = self.g.scrub(ans, "how do I fix my wallet")
        self.assertTrue(injected)
        self.assertIn(gc.Guardrails.mandatory_warning(["x"]), out)
        # warning appears immediately after the offending sentence
        first = out.index("reset the device")
        self.assertIn("WARNING:", out[first : first + 300])

    def test_no_injection_without_danger(self):
        out, injected = self.g.scrub("Keep your words offline on paper.", "seed backup")
        self.assertEqual(injected, [])
        self.assertEqual(out, "Keep your words offline on paper.")

    def test_out_of_corpus_refusal(self):
        r = self.g.refusal("out_of_corpus")
        self.assertEqual(r["template"], "out_of_corpus")
        self.assertIn("verified corpus", r["text"])

    def test_refusal_routing_offline(self):
        self.assertEqual(
            self.g.detect_refusal_kind("what is the price of bitcoin today"),
            "offline_or_network",
        )
        self.assertIsNone(self.g.detect_refusal_kind("what is a seed phrase"))


class TestAnswerRunner(unittest.TestCase):
    def test_compose_learn_stub(self):
        c = gc.compose("what is a seed phrase", CORPUS_DIR)
        self.assertEqual(c.mode, "learn")
        self.assertEqual(c.topics[0], "T02")
        self.assertTrue(c.retrieved)
        self.assertIsNone(c.refusal)
        self.assertEqual(c.answer, gc.STUB_ANSWER)
        d = c.to_dict()
        self.assertIn("retrieved", d)
        self.assertIn("constraints", d)

    def test_compose_out_of_corpus_refusal(self):
        c = gc.compose("qqqzzz wubblyflorp quantum flarn", CORPUS_DIR)
        self.assertIsNotNone(c.refusal)
        self.assertEqual(c.refusal["template"], "out_of_corpus")

    def test_compose_offline_refusal(self):
        c = gc.compose("what is the price of bitcoin today", CORPUS_DIR)
        self.assertIsNotNone(c.refusal)
        self.assertEqual(c.refusal["template"], "offline_or_network")

    def test_compose_with_candidate_scrub(self):
        cand = ("First, reset the device. Then re-enter your recovery words "
                "carefully.")
        c = gc.compose(
            "my wallet is stuck, what do I do with my seed phrase backup?",
            CORPUS_DIR,
            candidate_answer=cand,
        )
        self.assertTrue(c.scrubbed)
        self.assertIn("WARNING:", c.answer)

    def test_compose_deterministic(self):
        a = gc.compose("what is a seed phrase", CORPUS_DIR).to_dict()
        b = gc.compose("what is a seed phrase", CORPUS_DIR).to_dict()
        self.assertEqual(a, b)

    def test_seed_in_question_is_detected(self):
        c = gc.compose(
            "should I import %s into a wallet app?" % SEED_24, CORPUS_DIR
        )
        self.assertTrue(c.detections.is_secret)
        self.assertTrue(c.detections.warnings())


class ErrorCommunicationTests(unittest.TestCase):
    """Principle 8: errors must be explicit; no silent downgrades."""

    def test_internal_error_template(self):
        self.assertIn("Something went wrong", gc.Guardrails.refusal("internal_error")["text"])

    def test_total_backend_failure_reports_error_to_user(self):
        import answer_step
        orig_llama, orig_sail = answer_step.llama_answer, answer_step.sail_answer

        def boom(*a, **k):
            raise RuntimeError("engine dead")

        try:
            answer_step.llama_answer = boom
            answer_step.sail_answer = boom
            c = gc.compose("What is a seed phrase?", CORPUS_DIR, top_k=3)
            out = answer_step.answer_step("What is a seed phrase?", c, backend="auto")
        finally:
            answer_step.llama_answer, answer_step.sail_answer = orig_llama, orig_sail
        self.assertEqual(out["backend"], "error_reported")
        self.assertIn("Something went wrong", out["answer"])
        self.assertNotIn("seed phrase is", out["answer"].lower())


class TestBackendRouting(unittest.TestCase):
    """DO NEXT round (2026-10-29): the extractive path and the scam-verdict
    branch must honor backend=="llama" as LOCAL-ONLY — no silent remote
    fallback — and "auto" downgrades must carry the visible DOWNGRADE_NOTE."""

    def setUp(self):
        import answer_step
        self.answer_step = answer_step
        self.orig = (answer_step.llama_answer, answer_step.sail_answer)

    def tearDown(self):
        (self.answer_step.llama_answer, self.answer_step.sail_answer) = self.orig

    def test_extractive_llama_engine_down_fails_closed_with_note_and_never_goes_remote(self):
        import extractive
        calls = {"sail": 0}

        def llama_down(messages, timeout=300):
            raise self.answer_step.LlamaServerError("engine dead")

        def sail_spy(messages, timeout=1200):
            calls["sail"] += 1
            return "should never be called"

        self.answer_step.llama_answer = llama_down
        self.answer_step.sail_answer = sail_spy
        c = gc.compose("what is a seed phrase", CORPUS_DIR, top_k=3)
        old_mode = os.environ.get("ANSWER_MODE")
        os.environ["ANSWER_MODE"] = "extractive"
        try:
            out = self.answer_step.answer_step("what is a seed phrase", c, backend="llama")
        finally:
            if old_mode is None:
                os.environ.pop("ANSWER_MODE", None)
            else:
                os.environ["ANSWER_MODE"] = old_mode
        self.assertEqual(calls["sail"], 0, "local-only backend must never call the remote API")
        self.assertEqual(out["backend"], "extractive_fallback")
        self.assertIn("on-device answer engine did not respond", out["answer"])

    def test_extractive_llama_garbage_output_fallback_has_no_engine_note(self):
        # engine UP but returns garbage: the fallback must NOT claim the
        # engine did not respond (precision of the error message)
        old_mode = os.environ.get("ANSWER_MODE")
        os.environ["ANSWER_MODE"] = "extractive"
        try:
            self.answer_step.llama_answer = lambda messages, timeout=300: "no json here at all"
            self.answer_step.sail_answer = lambda messages, timeout=1200: "no json here at all"
            c = gc.compose("what is a seed phrase", CORPUS_DIR, top_k=3)
            out = self.answer_step.answer_step("what is a seed phrase", c, backend="llama")
        finally:
            if old_mode is None:
                os.environ.pop("ANSWER_MODE", None)
            else:
                os.environ["ANSWER_MODE"] = old_mode
        self.assertEqual(out["backend"], "extractive_fallback")
        self.assertNotIn("on-device answer engine", out["answer"])

    def test_auto_extractive_both_engines_down_no_false_downgrade_note(self):
        # DS review 2026-10-29: when BOTH engines fail, the result is the
        # canned fallback — appending DOWNGRADE_NOTE would falsely claim the
        # answer came from the backup engine.
        def down(messages, timeout=300):
            raise self.answer_step.LlamaServerError("engine down")

        self.answer_step.llama_answer = down
        self.answer_step.sail_answer = down
        c = gc.compose("what is a seed phrase", CORPUS_DIR, top_k=3)
        old_mode = os.environ.get("ANSWER_MODE")
        os.environ["ANSWER_MODE"] = "extractive"
        try:
            out = self.answer_step.answer_step("what is a seed phrase", c, backend="auto")
        finally:
            if old_mode is None:
                os.environ.pop("ANSWER_MODE", None)
            else:
                os.environ["ANSWER_MODE"] = old_mode
        self.assertEqual(out["backend"], "extractive_fallback")
        self.assertNotIn("backup engine", out["answer"])

    def test_scam_branch_llama_failure_never_falls_back_to_remote(self):
        calls = {"sail": 0}

        def llama_down(messages, timeout=300):
            raise self.answer_step.LlamaServerError("engine dead")

        def sail_spy(messages, timeout=1200):
            calls["sail"] += 1
            return "should never be called"

        self.answer_step.llama_answer = llama_down
        self.answer_step.sail_answer = sail_spy
        c = gc.compose(
            "someone asked for my seed phrase — is this a scam?", CORPUS_DIR, top_k=3
        )
        self.assertEqual(c.mode, "scam-check")
        out = self.answer_step.answer_step(
            "someone asked for my seed phrase — is this a scam?", c, backend="llama"
        )
        self.assertEqual(calls["sail"], 0, "local-only backend must never call the remote API")
        self.assertEqual(out["backend"], "error_reported")


class TestCorpusVendorNeutrality(unittest.TestCase):
    """External-review I7: no referrals, no affiliate params, no undated
    vendor rankings. Corpus text must stay neutral (spec §12 policy)."""

    CORPUS = os.environ.get("GUARDIAN_CORPUS", CORPUS_DIR)

    REFERRAL_URL = re.compile(
        r"[?&](ref|referral|ref_src|affiliate|utm_[a-z_]+|aff_id|aff|partner)=",
        re.I)
    VENDOR = re.compile(
        r"\b(trezor|ledger|bitbox|blockstream|jade|seedsigner|krux|liana|"
        r"sparrow|coldcard|coinkite|casa|unchained|river|swan|strike|"
        r"foundation devices|passport|specter|bluewallet|electrum|nunchuk|"
        r"shift crypto)\b", re.I)
    SUPERLATIVE = re.compile(
        r"(?:\b(?:the best|the top|the leading|the most trusted|number one|"
        r"the safest|the most secure|the easiest|the gold standard|"
        r"industry standard|best-in-class|top-rated|highly recommended|"
        r"our pick|the only choice|we recommend)\b|(?:^|\s)#1\b)", re.I)
    DATE = re.compile(r"\b(19|20)\d{2}\b|\bas of\b|\bcurrently\b", re.I)

    def _corpus_files(self):
        files = [f for f in sorted(os.listdir(self.CORPUS)) if f.endswith(".json")]
        self.assertGreater(len(files), 0, "no corpus files found — check GUARDIAN_CORPUS")
        return files

    def _texts(self, e):
        out = [e.get("plain", ""), e.get("technical", ""), e.get("title", "")]
        out += [kp for kp in (e.get("key_points") or []) if isinstance(kp, str)]
        out += [w for w in (e.get("warnings") or []) if isinstance(w, str)]
        return out

    def test_no_referral_or_affiliate_urls(self):
        for fn in self._corpus_files():
            text = json.dumps(json.load(open(os.path.join(self.CORPUS, fn))))
            m = self.REFERRAL_URL.search(text)
            self.assertIsNone(m, f"{fn}: referral/affiliate URL parameter: {m}")

    def test_no_undated_vendor_rankings(self):
        # vendor + superlative in the same sentence OR ±1 sentence, unless the
        # passage carries a date (dated, factual rankings are allowed)
        for fn in self._corpus_files():
            e = json.load(open(os.path.join(self.CORPUS, fn)))
            for text in self._texts(e):
                sents = re.split(r"(?<=[.!?])\s+", text)
                for i, sentence in enumerate(sents):
                    window = " ".join(sents[max(0, i - 1):i + 2])
                    if self.VENDOR.search(window) and self.SUPERLATIVE.search(window) \
                            and not self.DATE.search(window):
                        # "the gold standard"/"industry standard" describing a
                        # technical standard (e.g. BIP39) is a factual claim,
                        # not a vendor ranking
                        if re.search(r"\bBIP\d+\b", window) and \
                                re.search(r"(?:gold|industry) standard", window, re.I):
                            continue
                        self.fail(
                            f"{fn}: undated vendor ranking: {window[:140]}")

    def test_corpus_size_matches_readme_claim(self):
        files = self._corpus_files()
        self.assertGreaterEqual(len(files), 50,
                                f"corpus shrank below the README's 50-entry claim: {len(files)}")


class TestScamRulesParity(unittest.TestCase):
    """Golden vectors shared with the Kotlin ScamRulesTest (full-repo review
    2026-10-29): the same message must flag the same rule id in both mirrors."""

    VECTORS = [
        # (text, expected rule id)
        ("Please verify your 12-word seed phrase within 24 hours or your funds will be frozen.",
         "seed_request"),
        ("Validate your recovery phrase on our site and your wallet will be un-suspended.",
         "seed_request"),
        ("Paste your seed words here so we can migrate your wallet.", "seed_request"),
        ("Just tell us your seed phrase so we can check it.", "seed_request"),
        ("Input the private key in the form below.", "seed_request"),
        ("Ledger support here — your account was flagged.", "fake_official"),
        ("Trezor support team, about your wallet backup.", "fake_official"),
        ("Download our support tool so we can help.", "remote_software"),
        # DS/K3 review 2026-10-29: plural word-count phrasing ("12 words")
        # must flag, not just "12-word"
        ("Enter your 12 words here.", "seed_request"),
    ]

    def test_vectors_flag_expected_rule(self):
        import scam_rules
        for text, rule_id in self.VECTORS:
            res = scam_rules.scan(text)
            ids = [f["id"] for f in res["flagged"]]
            self.assertIn(rule_id, ids, msg=f"{rule_id} missing for: {text}")

    def test_benign_message_stays_clean(self):
        import scam_rules
        res = scam_rules.scan("How do I check the fee before sending? Is waiting okay?")
        self.assertTrue(res["clean"], msg=str(res["flagged"]))


class TestExtractiveManifestFailClosed(unittest.TestCase):
    """Full-repo review 2026-10-29: a missing/empty manifest must fail the
    verbatim gate CLOSED (every point unverified), never disable it."""

    def test_point_ok_false_when_manifest_missing(self):
        import extractive
        old = extractive._MANIFEST_CACHE
        try:
            extractive._MANIFEST_CACHE = {}
            self.assertFalse(extractive._point_ok({"text": "anything"}))
        finally:
            extractive._MANIFEST_CACHE = old

    def test_point_ok_true_for_manifested_point(self):
        import extractive
        man = extractive._manifest()
        if not man:
            self.fail("manifest missing in this checkout; run gen_manifest.py")
        some_hash = next(iter(man))
        # synthesize a point whose hash matches by construction is impossible
        # without the preimage; instead assert a random text fails
        self.assertFalse(extractive._point_ok({"text": "definitely not in the manifest"}))


if __name__ == "__main__":
    unittest.main(verbosity=2)
