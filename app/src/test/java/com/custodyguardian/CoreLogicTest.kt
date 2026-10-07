package com.custodyguardian

import com.custodyguardian.data.CorpusEntry
import com.custodyguardian.util.CorpusRouter
import com.custodyguardian.util.MemoryGuards
import com.custodyguardian.util.ScamRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Kotlin port of the deterministic-core tests (prototype/test_guardian_core.py).
 * Covers the Android-side pure logic: MemoryGuards seed detection, ScamRules
 * scanning, CorpusRouter routing/retrieval. Pure JVM — no emulator, no network.
 */
class CoreLogicTest {

    private val wordlist: Set<String> by lazy {
        File("src/main/assets/bip39_wordlist.txt")
            .readLines()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    // First 24 BIP-39 words.
    private val seed24 = ("abandon ability able about above absent absorb abstract absurd abuse " +
        "access accident account accuse achieve acid acoustic acquire across act " +
        "action actor actress actual").split(" ").joinToString(" ")

    // ---- MemoryGuards ----

    @Test
    fun seed24_inline_is_flagged() {
        val r = MemoryGuards.detectSeedPhrase("my backup is $seed24 keep it safe", wordlist)
        assertTrue(r is MemoryGuards.Result.SeedPhrase)
        val sp = r as MemoryGuards.Result.SeedPhrase
        assertTrue(sp.message.contains("scam"))
        assertTrue(sp.message.contains("Never type"))
    }

    @Test
    fun twelve_word_run_is_flagged() {
        val s12 = seed24.split(" ").take(12).joinToString(" ")
        val r = MemoryGuards.detectSeedPhrase("phrase: $s12", wordlist)
        assertTrue(r is MemoryGuards.Result.SeedPhrase)
    }

    @Test
    fun ordinary_text_is_safe() {
        val r = MemoryGuards.detectSeedPhrase(
            "Bitcoin is digital money that no bank controls; hardware wallets keep keys offline.",
            wordlist
        )
        assertEquals(MemoryGuards.Result.Safe, r)
    }

    @Test
    fun word_boundaries_respected() {
        // valid words embedded inside longer words must not accumulate a run
        val r = MemoryGuards.detectSeedPhrase("the abilityX is absentabsurd but abandon ok", wordlist)
        assertEquals(MemoryGuards.Result.Safe, r)
    }

    @Test
    fun empty_input_is_safe() {
        assertEquals(MemoryGuards.Result.Safe, MemoryGuards.detectSeedPhrase("", wordlist))
        assertEquals(MemoryGuards.Result.Safe, MemoryGuards.detectSeedPhrase("   ", wordlist))
    }

    // ---- ScamRules ----

    @Test
    fun scam_rules_flags_demand_for_seed() {
        val r = ScamRules.scan("Hi, I'm a recovery agent, I can get your bitcoin back, send me your seed phrase")
        assertFalse(r.clean)
        assertTrue(r.flagged.any { it.id == "seed_request" })
        assertTrue(r.verdict.contains("red flag"))
    }

    @Test
    fun scam_rules_mention_without_demand_is_not_flagged() {
        // Educational question mentioning the words must NOT be a red flag.
        val r = ScamRules.scan("What is the difference between a seed phrase and a private key?")
        assertTrue(r.clean)
        assertTrue(r.flagged.isEmpty())
    }

    @Test
    fun scam_rules_flags_remote_control_tools() {
        val r = ScamRules.scan("Please install AnyDesk so support can verify your wallet")
        assertFalse(r.clean)
    }

    // ---- CorpusRouter ----

    private val sampleCorpus: List<Pair<String, CorpusEntry>> by lazy { listOf(
        "t2" to CorpusEntry(
            title = "What Is a Seed Phrase",
            topic_ids = listOf("T02"),
            plain = "A seed phrase is a list of words that controls your wallet keys.",
        ),
        "t4" to CorpusEntry(
            title = "Choosing a Hardware Wallet",
            topic_ids = listOf("T04"),
            plain = "Hardware wallets like Ledger, Trezor, Jade and BitBox keep keys offline.",
        ),
        "t8" to CorpusEntry(
            title = "Scam Patterns",
            topic_ids = listOf("T08"),
            plain = "Recovery agent scams and urgent deadline pressure are common scams.",
        ),
    ) }

    @Test
    fun router_refuses_out_of_corpus_input() {
        val route = CorpusRouter.route(null, "zzz qqq vvv ??", sampleCorpus)
        assertTrue(route.isRefusal)
        assertTrue(route.note.contains("rather say"))
    }

    @Test
    fun router_routes_seed_question() {
        val route = CorpusRouter.route(null, "what is a seed phrase", sampleCorpus)
        assertFalse(route.isRefusal)
        assertTrue(route.retrieved.isNotEmpty())
        assertEquals("t2", route.retrieved.first().id)
    }

    @Test
    fun router_routes_scam_question() {
        val route = CorpusRouter.route(null, "is this urgent recovery agent message a scam", sampleCorpus)
        assertFalse(route.isRefusal)
        assertTrue(route.retrieved.any { it.id == "t8" })
    }

    @Test
    fun router_retrieval_is_deterministic() {
        val a = CorpusRouter.route(null, "which hardware wallet device should I buy", sampleCorpus)
        val b = CorpusRouter.route(null, "which hardware wallet device should I buy", sampleCorpus)
        assertEquals(a.detectedIntent, b.detectedIntent)
        assertEquals(a.retrieved.map { it.id }, b.retrieved.map { it.id })
        assertEquals(a.retrieved.map { it.score }, b.retrieved.map { it.score })
    }

    @Test
    fun router_caps_retrieved_at_three() {
        val big = (1..10).map { "e$it" to sampleCorpus[0].second }
        val route = CorpusRouter.route(null, "seed phrase backup", big)
        assertTrue(route.retrieved.size <= 3)
    }
}
