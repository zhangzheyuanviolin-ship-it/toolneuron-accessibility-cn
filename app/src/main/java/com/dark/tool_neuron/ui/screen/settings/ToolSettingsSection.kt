package com.dark.tool_neuron.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.data.ExaSearchTypeMode
import com.dark.tool_neuron.data.SearchProvider
import com.dark.tool_neuron.data.TavilyDepthMode
import com.dark.tool_neuron.data.ToolSearchDetail
import com.dark.tool_neuron.data.ToolSearchTopic
import com.dark.tool_neuron.data.ToolSettingsDataStore
import com.dark.tool_neuron.global.Standards
import com.dark.tool_neuron.ui.components.ActionButton
import com.dark.tool_neuron.ui.components.ActionTextButton
import com.dark.tool_neuron.ui.components.ActionToggleGroup
import com.dark.tool_neuron.ui.components.CaptionText
import com.dark.tool_neuron.ui.components.SectionDivider
import com.dark.tool_neuron.ui.components.SectionHeader
import com.dark.tool_neuron.ui.components.StandardCard
import com.dark.tool_neuron.ui.icons.TnIcons
import kotlinx.coroutines.launch

internal fun LazyListScope.toolSettingsEntrySection(onToolSettingsClick: () -> Unit) {
    item { Spacer(Modifier.height(Standards.SpacingSm)) }
    item { SectionDivider() }
    item { SectionHeader(title = "Tools") }
    item {
        StandardCard(
            title = "Tool Settings",
            description = "Search engines, API keys, model context results, and workspace folder",
            icon = TnIcons.Tool,
            onClick = onToolSettingsClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolSettingsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Tool Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    ActionButton(
                        onClickListener = onNavigateBack,
                        icon = TnIcons.ArrowLeft,
                        contentDescription = "Back"
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = Standards.SpacingMd),
            verticalArrangement = Arrangement.spacedBy(Standards.SpacingSm)
        ) {
            toolSettingsSection()
            item { Spacer(Modifier.height(Standards.SpacingXl)) }
        }
    }
}

internal fun LazyListScope.toolSettingsSection() {
    item { Spacer(Modifier.height(Standards.SpacingSm)) }
    item { SectionHeader(title = "Tool Configuration") }

    item {
        val context = androidx.compose.ui.platform.LocalContext.current
        val store = remember(context) { ToolSettingsDataStore(context.applicationContext) }
        val scope = rememberCoroutineScope()
        val provider by store.searchProvider.collectAsStateWithLifecycle(initialValue = SearchProvider.GOOGLE)
        val resultCount by store.searchResultCount.collectAsStateWithLifecycle(initialValue = 3)
        val modelResultCount by store.searchModelResultCount.collectAsStateWithLifecycle(initialValue = 3)
        val topic by store.searchTopic.collectAsStateWithLifecycle(initialValue = ToolSearchTopic.GENERAL)
        val detail by store.searchDetail.collectAsStateWithLifecycle(initialValue = ToolSearchDetail.SUMMARY)
        val tavilyDepth by store.tavilyDepth.collectAsStateWithLifecycle(initialValue = TavilyDepthMode.BASIC)
        val exaSearchType by store.exaSearchType.collectAsStateWithLifecycle(initialValue = ExaSearchTypeMode.AUTO)
        val tavilyKey by store.tavilyApiKey.collectAsStateWithLifecycle(initialValue = "")
        val exaKey by store.exaApiKey.collectAsStateWithLifecycle(initialValue = "")
        val workspace by store.workspace.collectAsStateWithLifecycle(initialValue = com.dark.tool_neuron.data.WorkspaceSettings())
        val workspaceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                persistWorkspacePermission(context, uri)
                scope.launch {
                    store.updateWorkspace(uri.toString(), displayNameForTreeUri(context, uri))
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Standards.SpacingSm)) {
            StandardCard(title = "Search Engine", icon = TnIcons.Search) {
                Column(verticalArrangement = Arrangement.spacedBy(Standards.SpacingSm)) {
                    ActionToggleGroup(
                        items = SearchProvider.entries.toList(),
                        selectedItem = provider,
                        onItemSelected = { scope.launch { store.updateSearchProvider(it) } },
                        itemLabel = { it.label }
                    )
                    CaptionText(text = "Result Count")
                    ActionToggleGroup(
                        items = listOf(1, 3, 5, 8),
                        selectedItem = resultCount,
                        onItemSelected = { scope.launch { store.updateSearchResultCount(it) } },
                        itemLabel = { it.toString() }
                    )
                    CaptionText(text = "Results Sent to Model")
                    ActionToggleGroup(
                        items = listOf(1, 3, 5, 8),
                        selectedItem = modelResultCount,
                        onItemSelected = { scope.launch { store.updateSearchModelResultCount(it) } },
                        itemLabel = { it.toString() }
                    )
                    CaptionText(text = "Topic")
                    ActionToggleGroup(
                        items = ToolSearchTopic.entries.toList(),
                        selectedItem = topic,
                        onItemSelected = { scope.launch { store.updateSearchTopic(it) } },
                        itemLabel = { it.label }
                    )
                    CaptionText(text = "Detail")
                    ActionToggleGroup(
                        items = ToolSearchDetail.entries.toList(),
                        selectedItem = detail,
                        onItemSelected = { scope.launch { store.updateSearchDetail(it) } },
                        itemLabel = { it.label }
                    )
                    if (provider == SearchProvider.TAVILY) {
                        CaptionText(text = "Tavily Depth")
                        ActionToggleGroup(
                            items = TavilyDepthMode.entries.toList(),
                            selectedItem = tavilyDepth,
                            onItemSelected = { scope.launch { store.updateTavilyDepth(it) } },
                            itemLabel = { it.label }
                        )
                    }
                    if (provider == SearchProvider.EXA) {
                        CaptionText(text = "Exa Type")
                        ActionToggleGroup(
                            items = ExaSearchTypeMode.entries.toList(),
                            selectedItem = exaSearchType,
                            onItemSelected = { scope.launch { store.updateExaSearchType(it) } },
                            itemLabel = { it.label }
                        )
                    }
                }
            }

            StandardCard(title = "Search API Keys", icon = TnIcons.Search) {
                Column(verticalArrangement = Arrangement.spacedBy(Standards.SpacingSm)) {
                    OutlinedTextField(
                        value = tavilyKey,
                        onValueChange = { scope.launch { store.updateTavilyApiKey(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Tavily API Key") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = exaKey,
                        onValueChange = { scope.launch { store.updateExaApiKey(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Exa API Key") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }

            StandardCard(title = "Workspace Folder", icon = TnIcons.FolderOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(Standards.SpacingSm)) {
                    Text(
                        text = workspace.displayName.ifBlank { "No folder selected" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Standards.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionTextButton(
                            onClickListener = { workspaceLauncher.launch(null) },
                            icon = TnIcons.FolderOpen,
                            text = "Choose",
                            modifier = Modifier.weight(1f)
                        )
                        ActionTextButton(
                            onClickListener = { scope.launch { store.clearWorkspace() } },
                            icon = TnIcons.Trash,
                            text = "Clear",
                            modifier = Modifier.weight(1f),
                            enabled = workspace.treeUri.isNotBlank()
                        )
                    }
                }
            }
        }
    }
}

private fun persistWorkspacePermission(context: Context, uri: Uri) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    context.contentResolver.takePersistableUriPermission(uri, flags)
}

private fun displayNameForTreeUri(context: Context, uri: Uri): String {
    return runCatching {
        val documentId = DocumentsContract.getTreeDocumentId(uri)
        documentId.substringAfterLast(':').ifBlank { uri.lastPathSegment.orEmpty() }
    }.getOrDefault(uri.lastPathSegment.orEmpty()).ifBlank { "Workspace Folder" }
}
