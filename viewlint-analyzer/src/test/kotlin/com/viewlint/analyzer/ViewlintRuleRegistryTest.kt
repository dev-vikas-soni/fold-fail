package com.viewlint.analyzer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewlintRuleRegistryTest {

    @Test
    fun `defaultRules returns all 10 expected rules in order`() {
        val rules = ViewlintRuleRegistry.defaultRules()
        assertEquals(10, rules.size)

        val ruleIds = rules.map { it.id }
        val expectedIds = listOf(
            "FF001",
            "FF002",
            "FF003",
            "FF004",
            "FF005",
            "FF006",
            "FF007",
            "FF008",
            "FF009",
            "FF010"
        )

        assertEquals(expectedIds, ruleIds)
    }

    @Test
    fun `every registered rule has a unique ID and non-empty description`() {
        val rules = ViewlintRuleRegistry.defaultRules()
        val ids = mutableSetOf<String>()

        for (rule in rules) {
            assertTrue("Duplicate rule ID detected: ${rule.id}", ids.add(rule.id))
            assertNotNull(rule.description)
            assertTrue("Description for ${rule.id} should not be blank", rule.description.isNotBlank())
        }
    }
}
