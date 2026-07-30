#!/usr/bin/env python3
import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

PROTECTED_HASHES = {
    "app/src/main/java/com/dark/tool_neuron/engine/GGUFEngine.kt": "a64adfda12286f11bcb0b9718c5e9307181325b476d5e3e45f2c32a2504d3d66",
    "app/src/main/java/com/dark/tool_neuron/service/LLMService.kt": "18ee01877c4379a8c27e1ab8d7d4efff6f757f5e44eb70955dd86ac2bc5e0f59",
    "app/src/main/java/com/dark/tool_neuron/worker/LlmModelWorker.kt": "65800efc5b21272cd753d668ca69ef9d9cf152354871b29aee4168fbb8218792",
    "app/src/main/java/com/dark/tool_neuron/global/ThirtyBMoESafeDefaults.kt": "f45625d3fe0d8dcd89c2eac6be1a143cf958e58db54e33bae496571b3ee56016",
    "app/src/main/java/com/dark/tool_neuron/activity/ModelLoadingActivity.kt": "df52f04173882a1182185d69c5b5e00f4b4bb47d48254c7ba6576079a2d9de65",
    "app/src/main/java/com/dark/tool_neuron/viewmodel/LLMModelViewModel.kt": "2cb895842bad27b48fe17d51cc1dc67c8e7377deb5d41089950e871eeb3d04c3",
    "app/src/main/java/com/dark/tool_neuron/viewmodel/SettingsViewModel.kt": "68dc5f8ed6a467655dc2118b344d904f2817b796c2119dba41b94a8ebb114386",
}

REQUIRED_REPOS = [
    "unsloth/Qwen3.5-0.8B-GGUF",
    "unsloth/Qwen3.5-2B-GGUF",
    "unsloth/Qwen3.5-4B-GGUF",
    "unsloth/Qwen3.5-9B-GGUF",
    "unsloth/Qwen3.5-35B-A3B-GGUF",
    "saidonnet/Qwen3.6-35B-A3B-MTP-GGUF",
    "nypgd/mebi-gemma-4-e2b-assistant-gguf",
    "mradermacher/gemma-4-E4B-tamil-GGUF",
    "estread11/gemma-4-12b-mirozdanie-nvfp4-gguf",
    "AtomicChat/gemma-4-26B-A4B-it-GGUF",
]

CATEGORIES = [
    "GENERAL",
    "MEDICAL",
    "RESEARCH",
    "CODING",
    "UNCENSORED",
    "BUSINESS",
    "CYBERSECURITY",
]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def fail(message: str) -> None:
    print(message, file=sys.stderr)
    sys.exit(1)


def assert_contains(text: str, needle: str, label: str) -> None:
    if needle not in text:
        fail(f"{label}: missing {needle}")


def main() -> None:
    for rel_path, expected in PROTECTED_HASHES.items():
        actual = hashlib.sha256((ROOT / rel_path).read_bytes()).hexdigest()
        if actual != expected:
            fail(f"protected inference file changed: {rel_path}")

    gradle = read("app/build.gradle.kts")
    assert_contains(gradle, 'applicationId = "com.dark.tool_neuron.intelligencelabtest"', "parallel test package")
    assert_contains(gradle, 'versionCode = 42', "version code")
    assert_contains(gradle, 'versionName = "2.5.2-vlm-store-stable"', "version name")
    assert_contains(gradle, 'create("release")', "release signing")
    for env_name in [
        "INTELLIGENCE_LAB_KEYSTORE_PATH",
        "INTELLIGENCE_LAB_KEYSTORE_PASSWORD",
        "INTELLIGENCE_LAB_KEY_ALIAS",
        "INTELLIGENCE_LAB_KEY_PASSWORD",
    ]:
        assert_contains(gradle, env_name, "release signing env")

    assert_contains(read("app/src/main/AndroidManifest.xml"), 'android:name="${applicationId}.permission.BIND_LLM_SERVICE"', "application id scoped service permission")
    assert_contains(read("app/src/main/AndroidManifest.xml"), 'android:permission="${applicationId}.permission.BIND_LLM_SERVICE"', "application id scoped service binding")
    assert_contains(read("app/src/main/res/values/strings.xml"), "<string name=\"app_name\">智能实验室</string>", "english app name")
    assert_contains(read("app/src/main/res/values-zh-rCN/strings.xml"), "<string name=\"app_name\">智能实验室</string>", "chinese app name")

    aar_hash = hashlib.sha256((ROOT / "libs/gguf_lib-release.aar").read_bytes()).hexdigest()
    if aar_hash != "7a3415a1753f917b914d5c1527520b6ae801611650eedb7402c69f19f39f77bd":
        fail("gguf AAR hash does not match the 2.5.0-vlm-store baseline")

    workflow = read(".github/workflows/build-apk.yml")
    assert_contains(workflow, "INTELLIGENCE_LAB_KEYSTORE_BASE64", "workflow signing secret")
    assert_contains(workflow, ":app:assembleRelease", "release build")
    assert_contains(workflow, "outputs/apk/release", "release artifact")
    assert_contains(workflow, "IntelligenceLab-", "artifact name")

    repos = read("app/src/main/java/com/dark/tool_neuron/repo/ModelRepoDataStore.kt")
    for repo in REQUIRED_REPOS:
        assert_contains(repos, f'"{repo}"', "required latest repo")
    for category in CATEGORIES:
        full_blocks = len(re.findall(r"modelType = ModelType\.GGUF,\s*\n\s*isEnabled = true,\s*\n\s*category = ModelCategory\." + category, repos))
        helper_calls = len(re.findall(r"ggufRepository\([\s\S]*?ModelCategory\." + category + r"\s*\)", repos))
        count = full_blocks + helper_calls
        if count < 10:
            fail(f"category {category} has {count} enabled GGUF repositories, expected at least 10")

    download_service = read("app/src/main/java/com/dark/tool_neuron/service/ModelDownloadService.kt")
    assert_contains(download_service, '.header("Range"', "resumable download")
    assert_contains(download_service, "HTTP_PARTIAL", "partial response handling")
    assert_contains(download_service, "FileOutputStream(destFile, resumeFrom > 0 && isResuming)", "append resume writes")
    if re.search(r"if \\(tempDir\\.exists\\(\\)\\) \\{\\s*\\n\\s*tempDir\\.deleteRecursively\\(\\)", download_service):
        fail("download temp directory is still deleted before download")

    gguf_engine = read("app/src/main/java/com/dark/tool_neuron/engine/GGUFEngine.kt")
    for vlm_api in ["loadVlmProjector", "isVlmLoaded", "getVlmDefaultMarker", "generateVlmFlow"]:
        assert_contains(gguf_engine, vlm_api, "vlm api")
    for tool_api in ["enableToolCallingDirect", "setToolsJson", "GenerationEvent.ToolCall"]:
        assert_contains(gguf_engine, tool_api, "tool calling api")

    plugin_manager = read("app/src/main/java/com/dark/tool_neuron/plugins/PluginManager.kt")
    for needle in [
        "_toolsSyncedWithLoadedModel",
        "No enabled tools; skipping native tool clear during model load",
    ]:
        assert_contains(plugin_manager, needle, "30b-safe lazy tool sync")

    chat_vm = read("app/src/main/java/com/dark/tool_neuron/viewmodel/ChatViewModel.kt")
    for needle in [
        "compactToolResultForModel",
        "compactWebSearchResultForModel",
        "MAX_SEARCH_RESULT_CHARS_FOR_MODEL = 1400",
        "Search tool succeeded; skipping extra tool-decision rounds before summary",
    ]:
        assert_contains(chat_vm, needle, "compact web search tool result")

    metrics_ui = read("app/src/main/java/com/dark/tool_neuron/ui/screen/home/MessageMetrics.kt")
    for needle in [
        "formattedPrefillSpeed",
        "Prefill Speed",
        "metrics.tokensEvaluated / (metrics.timeToFirstTokenMs / 1000f)",
    ]:
        assert_contains(metrics_ui, needle, "prefill speed metric")

    tool_settings = read("app/src/main/java/com/dark/tool_neuron/data/ToolSettingsDataStore.kt")
    for key in [
        "TAVILY_API_KEY",
        "EXA_API_KEY",
        "SEARCH_PROVIDER",
        "SEARCH_RESULT_COUNT",
        "SEARCH_TOPIC_MODE",
        "SEARCH_DETAIL_MODE",
        "WORKSPACE_TREE_URI",
    ]:
        assert_contains(tool_settings, key, "tool settings datastore")

    web_plugin = read("app/src/main/java/com/dark/tool_neuron/plugins/WebSearchPlugin.kt")
    for needle in [
        "TavilySearchService",
        "ExaSearchService",
        'toolCall.getInt("num_results"',
        'toolCall.getInt("max_results"',
        '.stringParam("query"',
    ]:
        assert_contains(web_plugin, needle, "web search provider integration")
    if "numberParam(\"max_results\"" in web_plugin:
        fail("web_search should not ask local models to provide result count")

    tavily_service = read("app/src/main/java/com/dark/tool_neuron/plugins/services/TavilySearchService.kt")
    assert_contains(tavily_service, "https://api.tavily.com/search", "tavily endpoint")
    assert_contains(tavily_service, 'Authorization", "Bearer', "tavily auth")

    exa_service = read("app/src/main/java/com/dark/tool_neuron/plugins/services/ExaSearchService.kt")
    assert_contains(exa_service, "https://api.exa.ai/search", "exa endpoint")
    assert_contains(exa_service, '"x-api-key"', "exa auth")

    file_plugin = read("app/src/main/java/com/dark/tool_neuron/plugins/FileManagerPlugin.kt")
    for needle in [
        "DocumentFile.fromTreeUri",
        "WORKSPACE_PATH_HINT",
        "workspaceRoot",
        "read_text_file",
        "create_file",
    ]:
        assert_contains(file_plugin, needle, "authorized workspace file manager")

    tool_section = read("app/src/main/java/com/dark/tool_neuron/ui/screen/settings/ToolSettingsSection.kt")
    for needle in [
        "OpenDocumentTree",
        "Tavily API Key",
        "Exa API Key",
        "Search Engine",
        "Workspace Folder",
    ]:
        assert_contains(tool_section, needle, "tool settings UI")

    store_repo = read("app/src/main/java/com/dark/tool_neuron/repo/ModelStoreRepository.kt")
    for needle in [
        "VLM_PROJECTOR",
        "Image-to-Text",
        "Text-to-Text",
        "Reasoning",
        "Projector Available",
        "Experimental Load",
    ]:
        assert_contains(store_repo, needle, "vlm model store metadata")

    app_paths = read("app/src/main/java/com/dark/tool_neuron/global/AppPaths.kt")
    assert_contains(app_paths, "vlmProjectorFile", "projector storage path")

    bottom_bar = read("app/src/main/java/com/dark/tool_neuron/ui/screen/home/HomeBottomBar.kt")
    for needle in [
        "GetContent",
        "TakePicturePreview",
        "sendChatWithImages",
        "loadVlmProjector",
    ]:
        assert_contains(bottom_bar, needle, "image attachment entry")


if __name__ == "__main__":
    main()
