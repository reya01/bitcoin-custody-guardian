"""Tests for guardian_core.py — deterministic, stdlib unittest."""

import json
import os
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
            self.assertNotIn(24, [0])  # placeholder; real assert below
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


if __name__ == "__main__":
    unittest.main(verbosity=2)
