package com.custodyguardian.util

/**
 * Local regex scam red-flag rules — spec §11 v1.0 "Scam-checker mode".
 * NO LLM: purely mechanical pattern matching over pasted message text,
 * grounded in the corpus scam patterns (T08 / walkthrough scam-defense card).
 */
object ScamRules {

    data class RedFlag(
        val id: String,
        val title: String,
        val pattern: Regex,
        val why: String,
    )

    val rules: List<RedFlag> = listOf(
        RedFlag(
            "seed_request",
            "Asks for seed phrase / keys",
            Regex(
                "(seed\\s*phrase|recovery\\s*phrase|private\\s*key|24\\s*words|12\\s*words|" +
                    "your\\s*words|mnemonic|xprv)",
                RegexOption.IGNORE_CASE
            ),
            "No legitimate party — vendor, support, or \"recovery expert\" — ever needs your " +
                "seed phrase or keys. Anyone who asks is a scammer. (Spec §3 principle 2, T02.)"
        ),
        RedFlag(
            "remote_software",
            "Remote-control software",
            Regex(
                "(anydesk|teamviewer|remote\\s*desktop|\"support\\s*tool\"|install\\s*the\\s*support|" +
                    "screen\\s*shar)",
                RegexOption.IGNORE_CASE
            ),
            "Scammers push remote-control apps (AnyDesk, TeamViewer) to watch your screen and " +
                "capture secrets or take over your device. (Scam-defense card.)"
        ),
        RedFlag(
            "urgency",
            "Artificial urgency",
            Regex(
                "(urgent|urgently|deadline|immediately|right\\s*now|expires?\\s*today|" +
                    "last\\s*chance|act\\s*now|funds?\\s*(are|is)\\s*at\\s*risk|locked\\s*out)",
                RegexOption.IGNORE_CASE
            ),
            "Pressure to act fast is a control tactic. Nothing in Bitcoin is urgent for a " +
                "custodian: waiting loses nothing; rushing loses everything. (Rule #1.)"
        ),
        RedFlag(
            "pay_to_unlock",
            "\"Send a small amount first\"",
            Regex(
                "(send\\s*a\\s*small\\s*(amount|fee)|unlock|test\\s*transaction|verification\\s*fee|" +
                    "gas\\s*fee|processing\\s*fee|pay\\s*(the|a)\\s*fee)",
                RegexOption.IGNORE_CASE
            ),
            "\"Send a small amount first to unlock/test/verify\" is a classic scam marker. " +
                "(Scam-defense card.)"
        ),
        RedFlag(
            "unsolicited",
            "They contacted you first",
            Regex(
                "(i\\s*noticed|i\\s*found\\s*your|we\\s*detected|unusual\\s*activity\\s*on\\s*your|" +
                    "i\\s*reached\\s*out|someone\\s*gave\\s*me\\s*your|reaching\\s*out\\s*regarding)",
                RegexOption.IGNORE_CASE
            ),
            "Any stranger who contacts YOU first about crypto is a scam by default. " +
                "This app never contacts anyone and asks you for nothing. (Walkthrough, §5.)"
        ),
        RedFlag(
            "guarantee",
            "Guaranteed results",
            Regex(
                "(100%|guarantee|guaranteed|risk\\s*free|risk\\s*-\\s*free|refund\\s*policy|" +
                    "surely|always\\s*succeed)",
                RegexOption.IGNORE_CASE
            ),
            "No honest party guarantees recovery of lost keys — impossible by design. " +
                "Guaranteed-recovery offers are scams. (§5 principle: no customer support for lost keys.)"
        ),
        RedFlag(
            "fake_official",
            "Impersonates an official service",
            Regex(
                "(official\\s*(support|team)|certified\\s*(agent|partner)|ledger\\s*support|" +
                    "coinbase\\s*support|blockchain\\s*support|support\\s*team\\s*here|" +
                    "customer\\s*care\\s*department)",
                RegexOption.IGNORE_CASE
            ),
            "Real wallet vendors never initiate contact and never ask for keys; impersonating " +
                "\"official support\" is the most common crypto scam pattern. (T02 warnings.)"
        ),
        RedFlag(
            "injection",
            "Prompt-injection style instructions",
            Regex(
                "(ignore\\s*(all\\s*)?(previous|prior)\\s*instructions?|disregard\\s*(the|your)\\s*" +
                    "(system|safety)|you\\s*are\\s*now|new\\s*instructions?:|assistant\\s*mode)",
                RegexOption.IGNORE_CASE
            ),
            "The text tries to override the assistant's safety rules. In this app your message is " +
                "treated as DATA, never as instructions — but the presence of injection language " +
                "itself is a red flag for the message's origin."
        ),
    )

    data class ScanResult(
        val flagged: List<RedFlag>,
        val clean: Boolean,
        val verdict: String,
    )

    fun scan(text: String): ScanResult {
        val hits = rules.filter { it.pattern.containsMatchIn(text) }
        return if (hits.isEmpty()) {
            ScanResult(
                flagged = emptyList(),
                clean = true,
                verdict = "No red flags from the local rule set. That is NOT a guarantee the " +
                    "message is safe — the safest answer is still: stop contact, do nothing, " +
                    "and never share keys or seed words with anyone."
            )
        } else {
            ScanResult(
                flagged = hits,
                clean = false,
                verdict = "${hits.size} red flag(s) detected. The safe answer is always: stop " +
                    "contact, do nothing, ask this app. You lose nothing by ignoring the message."
            )
        }
    }
}
