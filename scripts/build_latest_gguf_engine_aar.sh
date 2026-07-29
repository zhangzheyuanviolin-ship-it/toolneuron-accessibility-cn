#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
WORK_DIR="${RUNNER_TEMP:-/tmp}/intelligence-lab-latest-gguf-engine"
AI_REPO="${WORK_DIR}/Ai-Systems-New"
ANDROID_LLAMA="${WORK_DIR}/llama.cpp-android"
UPSTREAM_LLAMA="${WORK_DIR}/llama.cpp-upstream"
LLAMA_LINK="/home/home/dev/include/llama.cpp"

rm -rf "${WORK_DIR}"
mkdir -p "${WORK_DIR}"

git clone --depth 1 https://github.com/Siddhesh2377/Ai-Systems-New.git "${AI_REPO}"
git clone --depth 1 --branch re-write https://github.com/Siddhesh2377/llama.cpp-android.git "${ANDROID_LLAMA}"
git clone --depth 1 https://github.com/ggml-org/llama.cpp.git "${UPSTREAM_LLAMA}"

UPSTREAM_SHA="$(git -C "${UPSTREAM_LLAMA}" rev-parse HEAD)"
ANDROID_SHA="$(git -C "${ANDROID_LLAMA}" rev-parse HEAD)"
AI_SHA="$(git -C "${AI_REPO}" rev-parse HEAD)"

for path in CMakeLists.txt cmake common ggml include src vendor; do
    rm -rf "${ANDROID_LLAMA}/${path}"
    cp -a "${UPSTREAM_LLAMA}/${path}" "${ANDROID_LLAMA}/${path}"
done

if mkdir -p "$(dirname "${LLAMA_LINK}")" 2>/dev/null; then
    ln -sfn "${ANDROID_LLAMA}" "${LLAMA_LINK}"
elif command -v sudo >/dev/null 2>&1; then
    sudo mkdir -p "$(dirname "${LLAMA_LINK}")"
    sudo ln -sfn "${ANDROID_LLAMA}" "${LLAMA_LINK}"
else
    echo "Unable to create ${LLAMA_LINK}" >&2
    exit 1
fi

# GitHub's Android SDK currently provides newer CMake packages than the
# Android fork pins. Use the installed version selected by setup-android.
python3 - <<'PY' "${AI_REPO}/gguf_lib/build.gradle.kts"
from pathlib import Path
import re
import sys
path = Path(sys.argv[1])
text = path.read_text()
text = re.sub(r'version = "3\.[0-9.]+"', 'version = "3.31.6"', text)
path.write_text(text)
PY

python3 - <<'PY' "${AI_REPO}/gguf_lib/src/main/cpp/gguf_lib.cpp" "${ANDROID_LLAMA}/engine/rag-engine.cpp"
from pathlib import Path
import sys

gguf = Path(sys.argv[1])
text = gguf.read_text()
text = text.replace(
"""    auto mparams = llama_model_default_params();
    mparams.use_mmap  = (bool)useMmap;
    mparams.use_mlock = (bool)useMlock;
    g_state.use_mmap  = mparams.use_mmap;
    g_state.use_mlock = mparams.use_mlock;""",
"""    auto mparams = llama_model_default_params();
    if (useMmap && useMlock) {
        mparams.load_mode = LLAMA_LOAD_MODE_MMAP_MLOCK;
    } else if (useMlock) {
        mparams.load_mode = LLAMA_LOAD_MODE_MLOCK;
    } else if (useMmap) {
        mparams.load_mode = LLAMA_LOAD_MODE_MMAP;
    } else {
        mparams.load_mode = LLAMA_LOAD_MODE_NONE;
    }
    g_state.use_mmap  = (bool)useMmap;
    g_state.use_mlock = (bool)useMlock;""")
text = text.replace("    mparams.use_mmap = true;", "    mparams.load_mode = LLAMA_LOAD_MODE_MMAP;")
gguf.write_text(text)

rag = Path(sys.argv[2])
text = rag.read_text()
text = text.replace("    p.use_mmap        = true;", "    p.load_mode       = LLAMA_LOAD_MODE_MMAP;")
rag.write_text(text)
PY

python3 - <<'PY' "${AI_REPO}/gguf_lib/src/main/cpp/CMakeLists.txt"
from pathlib import Path
import sys
path = Path(sys.argv[1])
text = path.read_text()
text = text.replace("    common\n", "    llama-common\n")
path.write_text(text)
PY

mkdir -p "${AI_REPO}/gguf_lib/src/main/java/com/dark/gguf_lib/toolcalling"
cat > "${AI_REPO}/gguf_lib/src/main/java/com/dark/gguf_lib/toolcalling/ToolCallingCompat.kt" <<'EOF'
package com.dark.gguf_lib.toolcalling

import org.json.JSONArray
import org.json.JSONObject

enum class GrammarMode(val value: Int) {
    STRICT(0),
    LAZY(1)
}

data class ToolCallingConfig(
    val grammarMode: GrammarMode = GrammarMode.STRICT,
    val useTypedGrammar: Boolean = true,
    val maxRounds: Int = 3,
    val maxTokensPerTurn: Int = 512,
)

data class ToolCall(
    val name: String,
    val arguments: JSONObject = JSONObject(),
) {
    fun getString(key: String, default: String = ""): String = arguments.optString(key, default)
    fun getInt(key: String, default: Int = 0): Int = arguments.optInt(key, default)
    fun getDouble(key: String, default: Double = 0.0): Double = arguments.optDouble(key, default)
    fun getBoolean(key: String, default: Boolean = false): Boolean = arguments.optBoolean(key, default)
    fun has(key: String): Boolean = arguments.has(key)
}

class ToolDefinitionBuilder(
    val name: String,
    val description: String,
) {
    private val params = mutableListOf<Param>()

    fun stringParam(name: String, description: String, required: Boolean = false): ToolDefinitionBuilder =
        addParam(name, description, "string", required)

    fun numberParam(name: String, description: String, required: Boolean = false): ToolDefinitionBuilder =
        addParam(name, description, "number", required)

    fun integerParam(name: String, description: String, required: Boolean = false): ToolDefinitionBuilder =
        addParam(name, description, "integer", required)

    fun booleanParam(name: String, description: String, required: Boolean = false): ToolDefinitionBuilder =
        addParam(name, description, "boolean", required)

    fun enumParam(
        name: String,
        description: String,
        enumValues: List<String>,
        required: Boolean = false,
    ): ToolDefinitionBuilder {
        params += Param(name, description, "string", required, enumValues)
        return this
    }

    fun build(): ToolDefinition = ToolDefinition(name, description, params.toList())

    private fun addParam(name: String, description: String, type: String, required: Boolean): ToolDefinitionBuilder {
        params += Param(name, description, type, required, emptyList())
        return this
    }

    data class ToolDefinition(
        val name: String,
        val description: String,
        val parameters: List<Param>,
    ) {
        fun toOpenAIFormat(): JSONObject {
            val properties = JSONObject()
            val required = JSONArray()

            parameters.forEach { param ->
                properties.put(param.name, JSONObject().apply {
                    put("type", param.type)
                    put("description", param.description)
                    if (param.enumValues.isNotEmpty()) {
                        put("enum", JSONArray().apply {
                            param.enumValues.forEach { put(it) }
                        })
                    }
                })
                if (param.required) required.put(param.name)
            }

            return JSONObject().apply {
                put("name", name)
                put("description", description)
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", properties)
                    put("required", required)
                })
            }
        }
    }

    data class Param(
        val name: String,
        val description: String,
        val type: String,
        val required: Boolean,
        val enumValues: List<String> = emptyList(),
    )
}
EOF

cat > "${AI_REPO}/gguf_lib/src/main/java/com/dark/gguf_lib/LegacySdkCompat.kt" <<'EOF'
package com.dark.gguf_lib

data class ContextInfo(
    val total: Int = 0,
    val used: Int = 0,
    val remaining: Int = 0,
    val promptEstimate: Int = -1,
    val afterPrompt: Int = -1,
)

data class Personality(
    val name: String = "",
    val persona: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val repetitionPenalty: Float = 1.1f,
    val creativity: Float = 0.5f,
    val verbosity: Float = 0.5f,
    val formality: Float = 0.5f,
    val topK: Int = 40,
    val minP: Float = 0.05f,
)

enum class Mood {
    NEUTRAL,
    HAPPY,
    SAD,
    EXCITED,
    CALM,
    ANGRY,
    CURIOUS,
    CREATIVE,
    FOCUSED,
    CUSTOM
}

data class ControlVectorConfig(
    val path: String,
    val strength: Float = 1.0f,
)

class CharacterEngine(private val engine: GGMLEngine) {
    private var personality: Personality = Personality()
    private var mood: Mood = Mood.NEUTRAL
    private var customMood: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)
    private var uncensored: Boolean = false

    fun setPersonality(personality: Personality) {
        this.personality = personality
    }

    fun setMood(mood: Mood) {
        this.mood = mood
    }

    fun setCustomMood(tempMod: Float = 0f, topPMod: Float = 0f, repPenaltyMod: Float = 0f) {
        customMood = Triple(tempMod, topPMod, repPenaltyMod)
    }

    fun getContext(): String {
        return listOf(
            personality.name.takeIf { it.isNotBlank() }?.let { "Name: $it" },
            personality.persona.takeIf { it.isNotBlank() },
            "Mood: ${mood.name.lowercase()}",
            if (uncensored) "Uncensored mode enabled." else null,
        ).filterNotNull().joinToString("\n")
    }

    fun buildPrompt(userPrompt: String): String {
        val context = getContext()
        return if (context.isBlank()) userPrompt else "$context\n\n$userPrompt"
    }

    fun setUncensored(enabled: Boolean) {
        uncensored = enabled
    }

    fun isUncensored(): Boolean = uncensored

    fun calcVectors(text: String, progress: (Float) -> Unit = {}): FloatArray {
        progress(1f)
        return FloatArray(0)
    }

    fun applyVectors(vectors: FloatArray, scale: Float = 1.0f, startLayer: Int = 0, endLayer: Int = -1): Boolean = false

    fun clearVectors() = Unit

    fun loadControlVectors(configs: List<ControlVectorConfig>): Boolean = false

    fun clearControlVectors() = Unit

    fun setLogitBias(bias: Map<String, Float>) = Unit
}
EOF

python3 - <<'PY' "${AI_REPO}/gguf_lib/src/main/java/com/dark/gguf_lib/GGMLEngine.kt"
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text()
if "fun setToolsJson(toolsJson: String): Boolean" not in text:
    compat = r'''
    // Legacy ToolNeuron SDK compatibility. These methods preserve the host app
    // API while the actual generation path continues to use the latest native
    // llama.cpp-backed load/generate calls above.
    private var legacyToolsJson: String? = null

    fun setToolsJson(toolsJson: String): Boolean {
        legacyToolsJson = toolsJson
        return loaded
    }

    fun clearTools() {
        legacyToolsJson = null
    }

    fun isToolCallingSupported(): Boolean = loaded

    fun enableToolCalling(
        toolDefs: List<com.dark.gguf_lib.toolcalling.ToolDefinitionBuilder.ToolDefinition>,
        config: com.dark.gguf_lib.toolcalling.ToolCallingConfig,
    ): Boolean {
        legacyToolsJson = org.json.JSONArray().apply {
            toolDefs.forEach { put(it.toOpenAIFormat()) }
        }.toString()
        return loaded
    }

    fun setSpeculativeDecoding(enabled: Boolean, nDraft: Int = 4, ngramSize: Int = 4) = Unit

    fun loadControlVectors(vectorsJson: String): Boolean = false

    fun clearControlVector() = Unit

    fun getContextInfo(prompt: String? = null): ContextInfo {
        val stats = runCatching {
            getMemoryStatsJson()?.let { org.json.JSONObject(it) }
        }.getOrNull()
        val total = stats?.optInt("n_ctx", 0) ?: 0
        val used = stats?.optInt("n_used", 0) ?: 0
        val remaining = if (total > 0) (total - used).coerceAtLeast(0) else 0
        val promptEstimate = prompt?.let { (it.length / 3).coerceAtLeast(0) } ?: -1
        val afterPrompt = if (promptEstimate >= 0 && total > 0) (used + promptEstimate).coerceAtMost(total) else -1
        return ContextInfo(total, used, remaining, promptEstimate, afterPrompt)
    }

'''
    marker = "    companion object {"
    idx = text.rfind(marker)
    if idx < 0:
        raise SystemExit("Unable to find GGMLEngine companion marker")
    text = text[:idx] + compat + text[idx:]
    path.write_text(text)
PY

python3 - <<'PY' "${AI_REPO}/gguf_lib/src/main/java/com/dark/gguf_lib/models/GenerationEvent.kt"
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text()
if "data class ToolCall(" not in text:
    text = text.replace(
        "    data class Token(val text: String) : GenerationEvent()\n",
        "    data class Token(val text: String) : GenerationEvent()\n"
        "    data class ToolCall(val name: String, val argsJson: String) : GenerationEvent()\n",
    )
    path.write_text(text)
PY

mkdir -p "${AI_REPO}/gguf_lib/src/main/assets"
cat > "${AI_REPO}/gguf_lib/src/main/assets/intelligence_lab_engine_provenance.txt" <<EOF
ggml_org_llama_cpp=${UPSTREAM_SHA}
android_wrapper_fork=${ANDROID_SHA}
ai_systems_new=${AI_SHA}
build_time_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

chmod +x "${AI_REPO}/gradlew"
(
    cd "${AI_REPO}"
    ./gradlew --no-daemon --console=plain --stacktrace --max-workers=2 :gguf_lib:assembleRelease -x lint
)

AAR_SRC="$(find "${AI_REPO}/gguf_lib/build/outputs/aar" -name '*release.aar' | head -1)"
test -n "${AAR_SRC}"
test -s "${AAR_SRC}"
cp "${AAR_SRC}" "${ROOT_DIR}/libs/gguf_lib-release.aar"

mkdir -p "${ROOT_DIR}/out"
{
    echo "ggml_org_llama_cpp=${UPSTREAM_SHA}"
    echo "android_wrapper_fork=${ANDROID_SHA}"
    echo "ai_systems_new=${AI_SHA}"
    sha256sum "${ROOT_DIR}/libs/gguf_lib-release.aar"
} | tee "${ROOT_DIR}/out/latest-gguf-engine-provenance.txt"
