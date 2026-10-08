package com.custodyguardian

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Item-1 schema upgrade tests: walkthrough JSON structure + verbatim safety
 * content, and corpus asset JSON carries the new fields. Reads the bundled
 * asset files directly from the repo working tree (plain JVM test, no
 * instrumentation needed); Python-side validation is the source of truth.
 */
class SchemaUpgradeTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun readAsset(path: String): String =
        javaClass.getResourceAsStream("/assets/$path")
            ?.bufferedReader()?.readText()
            ?: java.io.File("src/main/assets/$path").readText()

    @Test
    fun walkthrough_parses_with_seven_days() {
        val root = json.parseToJsonElement(readAsset("walkthrough.json")).jsonObject
        val days = root["days"]!!.jsonArray
        assertEquals(7, days.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), days.map { it.jsonObject["day"]!!.jsonPrimitive.int })
        assertTrue(root["rule_one"]!!.jsonPrimitive.content.contains("Nothing is urgent"))
    }

    @Test
    fun walkthrough_day1_donts_are_verbatim_safety_rules() {
        val root = json.parseToJsonElement(readAsset("walkthrough.json")).jsonObject
        val day1 = root["days"]!!.jsonArray.map { it.jsonObject }.first { it["day"]!!.jsonPrimitive.int == 1 }
        val donts = day1["donts"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(donts.any { it.contains("Do not photograph word lists") })
        assertTrue(donts.any { it.contains("reset", ignoreCase = true) })
        assertTrue(day1["dos"]!!.jsonArray.isNotEmpty())
    }

    @Test
    fun corpus_assets_carry_schema_upgrade_fields() {
        val dir = java.io.File("src/main/assets/corpus")
        val files = dir.listFiles { f -> f.name.endsWith(".json") }!!
        assertTrue(files.size >= 40)
        val withKeyPoints = files.count {
            json.parseToJsonElement(it.readText()).jsonObject["key_points"]?.jsonArray?.isNotEmpty() == true
        }
        assertTrue("expected nearly all entries to have key_points, got $withKeyPoints of ${files.size}",
            withKeyPoints >= files.size - 5)
    }
}
