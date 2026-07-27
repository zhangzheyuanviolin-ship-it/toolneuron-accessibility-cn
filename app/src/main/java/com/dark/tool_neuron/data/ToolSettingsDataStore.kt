package com.dark.tool_neuron.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.toolSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tool_settings")

enum class SearchProvider(val value: String, val label: String) {
    GOOGLE("google", "Google"),
    TAVILY("tavily", "Tavily"),
    EXA("exa", "Exa");

    companion object {
        fun from(value: String?): SearchProvider =
            entries.firstOrNull { it.value == value } ?: GOOGLE
    }
}

enum class ToolSearchTopic(val value: String, val label: String) {
    GENERAL("general", "General"),
    NEWS("news", "News"),
    FINANCE("finance", "Finance");

    companion object {
        fun from(value: String?): ToolSearchTopic =
            entries.firstOrNull { it.value == value } ?: GENERAL
    }
}

enum class ToolSearchDetail(val value: String, val label: String) {
    SUMMARY("summary", "Summary"),
    LIGHT("light", "Light"),
    STANDARD("standard", "Standard"),
    FULL("full", "Full");

    companion object {
        fun from(value: String?): ToolSearchDetail =
            entries.firstOrNull { it.value == value } ?: SUMMARY
    }
}

enum class TavilyDepthMode(val value: String, val label: String) {
    BASIC("basic", "Basic"),
    ADVANCED("advanced", "Advanced");

    companion object {
        fun from(value: String?): TavilyDepthMode =
            entries.firstOrNull { it.value == value } ?: BASIC
    }
}

enum class ExaSearchTypeMode(val value: String, val label: String) {
    AUTO("auto", "Auto"),
    INSTANT("instant", "Instant"),
    FAST("fast", "Fast"),
    DEEP("deep", "Deep");

    companion object {
        fun from(value: String?): ExaSearchTypeMode =
            entries.firstOrNull { it.value == value } ?: AUTO
    }
}

data class ToolSearchSettings(
    val provider: SearchProvider = SearchProvider.GOOGLE,
    val resultCount: Int = 3,
    val topic: ToolSearchTopic = ToolSearchTopic.GENERAL,
    val detail: ToolSearchDetail = ToolSearchDetail.SUMMARY,
    val tavilyDepth: TavilyDepthMode = TavilyDepthMode.BASIC,
    val exaSearchType: ExaSearchTypeMode = ExaSearchTypeMode.AUTO,
    val tavilyApiKey: String = "",
    val exaApiKey: String = ""
)

data class WorkspaceSettings(
    val treeUri: String = "",
    val displayName: String = ""
)

class ToolSettingsDataStore(private val context: Context) {
    companion object {
        private val TAVILY_API_KEY = stringPreferencesKey("tavily_api_key")
        private val EXA_API_KEY = stringPreferencesKey("exa_api_key")
        private val SEARCH_PROVIDER = stringPreferencesKey("search_provider")
        private val SEARCH_RESULT_COUNT = intPreferencesKey("search_result_count")
        private val SEARCH_TOPIC_MODE = stringPreferencesKey("search_topic_mode")
        private val SEARCH_DETAIL_MODE = stringPreferencesKey("search_detail_mode")
        private val TAVILY_DEPTH_MODE = stringPreferencesKey("tavily_depth_mode")
        private val EXA_SEARCH_TYPE_MODE = stringPreferencesKey("exa_search_type_mode")
        private val WORKSPACE_TREE_URI = stringPreferencesKey("workspace_tree_uri")
        private val WORKSPACE_DISPLAY_NAME = stringPreferencesKey("workspace_display_name")
        private val RESULT_COUNT_OPTIONS = listOf(1, 3, 5, 8)
    }

    val searchProvider: Flow<SearchProvider> = context.toolSettingsDataStore.data.map { prefs ->
        SearchProvider.from(prefs[SEARCH_PROVIDER])
    }

    val searchResultCount: Flow<Int> = context.toolSettingsDataStore.data.map { prefs ->
        clampResultCount(prefs[SEARCH_RESULT_COUNT] ?: 3)
    }

    val searchTopic: Flow<ToolSearchTopic> = context.toolSettingsDataStore.data.map { prefs ->
        ToolSearchTopic.from(prefs[SEARCH_TOPIC_MODE])
    }

    val searchDetail: Flow<ToolSearchDetail> = context.toolSettingsDataStore.data.map { prefs ->
        ToolSearchDetail.from(prefs[SEARCH_DETAIL_MODE])
    }

    val tavilyDepth: Flow<TavilyDepthMode> = context.toolSettingsDataStore.data.map { prefs ->
        TavilyDepthMode.from(prefs[TAVILY_DEPTH_MODE])
    }

    val exaSearchType: Flow<ExaSearchTypeMode> = context.toolSettingsDataStore.data.map { prefs ->
        ExaSearchTypeMode.from(prefs[EXA_SEARCH_TYPE_MODE])
    }

    val tavilyApiKey: Flow<String> = context.toolSettingsDataStore.data.map { prefs ->
        prefs[TAVILY_API_KEY] ?: ""
    }

    val exaApiKey: Flow<String> = context.toolSettingsDataStore.data.map { prefs ->
        prefs[EXA_API_KEY] ?: ""
    }

    val workspace: Flow<WorkspaceSettings> = context.toolSettingsDataStore.data.map { prefs ->
        WorkspaceSettings(
            treeUri = prefs[WORKSPACE_TREE_URI] ?: "",
            displayName = prefs[WORKSPACE_DISPLAY_NAME] ?: ""
        )
    }

    suspend fun searchSettingsSnapshot(): ToolSearchSettings {
        val prefs = context.toolSettingsDataStore.data.first()
        return ToolSearchSettings(
            provider = SearchProvider.from(prefs[SEARCH_PROVIDER]),
            resultCount = clampResultCount(prefs[SEARCH_RESULT_COUNT] ?: 3),
            topic = ToolSearchTopic.from(prefs[SEARCH_TOPIC_MODE]),
            detail = ToolSearchDetail.from(prefs[SEARCH_DETAIL_MODE]),
            tavilyDepth = TavilyDepthMode.from(prefs[TAVILY_DEPTH_MODE]),
            exaSearchType = ExaSearchTypeMode.from(prefs[EXA_SEARCH_TYPE_MODE]),
            tavilyApiKey = prefs[TAVILY_API_KEY] ?: "",
            exaApiKey = prefs[EXA_API_KEY] ?: ""
        )
    }

    suspend fun workspaceSnapshot(): WorkspaceSettings = workspace.first()

    suspend fun updateSearchProvider(provider: SearchProvider) {
        context.toolSettingsDataStore.edit { it[SEARCH_PROVIDER] = provider.value }
    }

    suspend fun updateSearchResultCount(count: Int) {
        context.toolSettingsDataStore.edit { it[SEARCH_RESULT_COUNT] = clampResultCount(count) }
    }

    suspend fun updateSearchTopic(topic: ToolSearchTopic) {
        context.toolSettingsDataStore.edit { it[SEARCH_TOPIC_MODE] = topic.value }
    }

    suspend fun updateSearchDetail(detail: ToolSearchDetail) {
        context.toolSettingsDataStore.edit { it[SEARCH_DETAIL_MODE] = detail.value }
    }

    suspend fun updateTavilyDepth(depth: TavilyDepthMode) {
        context.toolSettingsDataStore.edit { it[TAVILY_DEPTH_MODE] = depth.value }
    }

    suspend fun updateExaSearchType(type: ExaSearchTypeMode) {
        context.toolSettingsDataStore.edit { it[EXA_SEARCH_TYPE_MODE] = type.value }
    }

    suspend fun updateTavilyApiKey(key: String) {
        context.toolSettingsDataStore.edit { it[TAVILY_API_KEY] = key.trim() }
    }

    suspend fun updateExaApiKey(key: String) {
        context.toolSettingsDataStore.edit { it[EXA_API_KEY] = key.trim() }
    }

    suspend fun updateWorkspace(treeUri: String, displayName: String) {
        context.toolSettingsDataStore.edit {
            it[WORKSPACE_TREE_URI] = treeUri
            it[WORKSPACE_DISPLAY_NAME] = displayName
        }
    }

    suspend fun clearWorkspace() {
        context.toolSettingsDataStore.edit {
            it.remove(WORKSPACE_TREE_URI)
            it.remove(WORKSPACE_DISPLAY_NAME)
        }
    }

    private fun clampResultCount(count: Int): Int =
        RESULT_COUNT_OPTIONS.minByOrNull { kotlin.math.abs(it - count) } ?: 3
}
