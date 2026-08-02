package com.dark.tool_neuron.viewmodel

import org.json.JSONArray
import org.json.JSONObject

object WebSearchResultCompactor {
    private const val DEFAULT_RESULT_COUNT = 3
    private val RESULT_COUNT_OPTIONS = listOf(1, 3, 5, 8)

    fun compact(resultJson: String, modelResultCount: Int = DEFAULT_RESULT_COUNT): String {
        val count = clampResultCount(modelResultCount)
        return try {
            val json = JSONObject(resultJson)
            val results = json.optJSONArray("results") ?: JSONArray()
            val shownResults = minOf(results.length(), count)
            buildString {
                appendLine("web_search succeeded")
                appendLine("query: ${json.optString("query", "")}")
                appendLine("provider: ${json.optString("provider", "")}")
                val answer = compactWhitespace(json.optString("answer", ""))
                if (answer.isNotBlank()) appendLine("answer: ${answer.take(400)}")
                appendLine("results:")
                for (i in 0 until shownResults) {
                    val item = results.optJSONObject(i) ?: continue
                    val title = compactWhitespace(item.optString("title", "")).take(140)
                    val url = item.optString("url", "").take(240)
                    val snippet = compactWhitespace(
                        item.optString("snippet", "").ifBlank { item.optString("content", "") }
                    ).take(260)
                    appendLine("${i + 1}. $title")
                    if (url.isNotBlank()) appendLine("   url: $url")
                    if (snippet.isNotBlank()) appendLine("   snippet: $snippet")
                }
                val total = json.optInt("totalResults", results.length())
                if (total > shownResults) {
                    appendLine("context_note: ${total - shownResults} additional results were omitted for local model context safety.")
                }
            }.take(charBudgetFor(count))
        } catch (_: Exception) {
            resultJson.take(charBudgetFor(count))
        }
    }

    private fun clampResultCount(count: Int): Int =
        RESULT_COUNT_OPTIONS.minByOrNull { kotlin.math.abs(it - count) } ?: DEFAULT_RESULT_COUNT

    private fun charBudgetFor(count: Int): Int = when (count) {
        1 -> 900
        3 -> 1400
        5 -> 2400
        else -> 3800
    }

    private fun compactWhitespace(value: String): String =
        value.replace(Regex("\\s+"), " ").trim()
}
