package com.dark.tool_neuron.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSearchResultCompactorTest {
    @Test
    fun limitsModelVisibleResultsToConfiguredCount() {
        val resultJson = """
            {
              "query": "test query",
              "provider": "exa",
              "totalResults": 5,
              "results": [
                {"title":"one","url":"https://example.com/1","snippet":"first"},
                {"title":"two","url":"https://example.com/2","snippet":"second"},
                {"title":"three","url":"https://example.com/3","snippet":"third"},
                {"title":"four","url":"https://example.com/4","snippet":"fourth"},
                {"title":"five","url":"https://example.com/5","snippet":"fifth"}
              ]
            }
        """.trimIndent()

        val compacted = WebSearchResultCompactor.compact(resultJson, modelResultCount = 5)

        assertTrue(compacted.contains("5. five"))
        assertFalse(compacted.contains("additional results were omitted"))
    }

    @Test
    fun clampsModelVisibleResultsToSupportedOptions() {
        val resultJson = """
            {
              "query": "test query",
              "provider": "exa",
              "totalResults": 8,
              "results": [
                {"title":"one","snippet":"first"},
                {"title":"two","snippet":"second"},
                {"title":"three","snippet":"third"},
                {"title":"four","snippet":"fourth"},
                {"title":"five","snippet":"fifth"},
                {"title":"six","snippet":"sixth"},
                {"title":"seven","snippet":"seventh"},
                {"title":"eight","snippet":"eighth"}
              ]
            }
        """.trimIndent()

        val compacted = WebSearchResultCompactor.compact(resultJson, modelResultCount = 99)

        assertTrue(compacted.contains("8. eight"))
    }
}
