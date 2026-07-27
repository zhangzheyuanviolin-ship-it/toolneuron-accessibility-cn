package com.dark.tool_neuron.plugins.services

import com.dark.tool_neuron.data.ExaSearchTypeMode
import com.dark.tool_neuron.data.ToolSearchDetail
import com.dark.tool_neuron.data.ToolSearchSettings
import com.dark.tool_neuron.data.ToolSearchTopic
import com.dark.tool_neuron.plugins.ScrapedSearchResult
import com.dark.tool_neuron.plugins.WebSearchPipelineResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ExaSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun search(query: String, settings: ToolSearchSettings): Result<WebSearchPipelineResult> =
        withContext(Dispatchers.IO) {
            if (settings.exaApiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Exa API key is not configured"))
            }

            val startTime = System.currentTimeMillis()
            val body = JSONObject().apply {
                put("query", query)
                put("type", settings.exaSearchType.toExaType())
                put("numResults", settings.resultCount.coerceIn(1, 8))
                settings.topic.toExaCategory()?.let { put("category", it) }
                put("contents", JSONObject().apply {
                    when (settings.detail) {
                        ToolSearchDetail.SUMMARY -> put("summary", JSONObject().put("query", query))
                        ToolSearchDetail.LIGHT -> put("highlights", JSONObject().put("numSentences", 2))
                        ToolSearchDetail.STANDARD -> {
                            put("summary", JSONObject().put("query", query))
                            put("highlights", JSONObject().put("numSentences", 2))
                        }
                        ToolSearchDetail.FULL -> {
                            put("summary", JSONObject().put("query", query))
                            put("highlights", JSONObject().put("numSentences", 2))
                            put("text", JSONObject().put("maxCharacters", 1600))
                        }
                    }
                })
            }

            val request = Request.Builder()
                .url("https://api.exa.ai/search")
                .header("x-api-key", settings.exaApiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Exa search failed: HTTP ${response.code} ${response.message}")
                        )
                    }

                    val json = JSONObject(responseBody)
                    val resultsJson = json.optJSONArray("results")
                    val results = buildList {
                        if (resultsJson != null) {
                            for (i in 0 until resultsJson.length()) {
                                val item = resultsJson.optJSONObject(i) ?: continue
                                val summary = item.optString("summary", "")
                                val highlights = item.optJSONArray("highlights").toJoinedText()
                                val text = item.optString("text", "")
                                val content = listOf(summary, highlights, text, item.optString("publishedDate", ""))
                                    .filter { it.isNotBlank() }
                                    .joinToString("\n")
                                    .take(2200)
                                add(
                                    ScrapedSearchResult(
                                        title = item.optString("title", item.optString("url", "")),
                                        url = item.optString("url", ""),
                                        snippet = if (summary.isNotBlank()) summary.take(500) else highlights.take(500),
                                        content = content
                                    )
                                )
                            }
                        }
                    }

                    Result.success(
                        WebSearchPipelineResult(
                            query = query,
                            provider = "exa",
                            answer = "",
                            results = results,
                            totalResults = results.size,
                            searchTimeMs = System.currentTimeMillis() - startTime
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun ExaSearchTypeMode.toExaType(): String = when (this) {
        ExaSearchTypeMode.AUTO -> "auto"
        ExaSearchTypeMode.INSTANT -> "keyword"
        ExaSearchTypeMode.FAST -> "neural"
        ExaSearchTypeMode.DEEP -> "auto"
    }

    private fun ToolSearchTopic.toExaCategory(): String? = when (this) {
        ToolSearchTopic.NEWS -> "news"
        ToolSearchTopic.FINANCE -> "financial report"
        ToolSearchTopic.GENERAL -> null
    }

    private fun JSONArray?.toJoinedText(): String {
        if (this == null) return ""
        return buildList {
            for (i in 0 until length()) {
                optString(i, "").takeIf { it.isNotBlank() }?.let(::add)
            }
        }.joinToString("\n")
    }
}
