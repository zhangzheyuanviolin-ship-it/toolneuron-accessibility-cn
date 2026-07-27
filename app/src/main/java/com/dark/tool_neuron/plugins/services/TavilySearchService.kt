package com.dark.tool_neuron.plugins.services

import com.dark.tool_neuron.data.ToolSearchSettings
import com.dark.tool_neuron.data.ToolSearchDetail
import com.dark.tool_neuron.plugins.ScrapedSearchResult
import com.dark.tool_neuron.plugins.WebSearchPipelineResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TavilySearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun search(query: String, settings: ToolSearchSettings): Result<WebSearchPipelineResult> =
        withContext(Dispatchers.IO) {
            if (settings.tavilyApiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Tavily API key is not configured"))
            }

            val startTime = System.currentTimeMillis()
            val body = JSONObject().apply {
                put("query", query)
                put("topic", settings.topic.value)
                put("search_depth", settings.tavilyDepth.value)
                put("max_results", settings.resultCount.coerceIn(1, 8))
                put("include_raw_content", settings.detail == ToolSearchDetail.FULL)
                put("include_answer", settings.detail != ToolSearchDetail.LIGHT)
                put("include_images", false)
                put("include_favicon", false)
                put("auto_parameters", false)
            }

            val request = Request.Builder()
                .url("https://api.tavily.com/search")
                .header("Authorization", "Bearer ${settings.tavilyApiKey}")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Tavily search failed: HTTP ${response.code} ${response.message}")
                        )
                    }

                    val json = JSONObject(responseBody)
                    val answer = json.optString("answer", "")
                    val resultsJson = json.optJSONArray("results")
                    val results = buildList {
                        if (resultsJson != null) {
                            for (i in 0 until resultsJson.length()) {
                                val item = resultsJson.optJSONObject(i) ?: continue
                                val content = listOf(
                                    item.optString("content", ""),
                                    item.optString("raw_content", ""),
                                    item.optString("published_date", "")
                                ).filter { it.isNotBlank() }.joinToString("\n").take(2200)
                                add(
                                    ScrapedSearchResult(
                                        title = item.optString("title", item.optString("url", "")),
                                        url = item.optString("url", ""),
                                        snippet = item.optString("content", "").take(500),
                                        content = content
                                    )
                                )
                            }
                        }
                    }

                    Result.success(
                        WebSearchPipelineResult(
                            query = query,
                            provider = "tavily",
                            answer = answer,
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
}
