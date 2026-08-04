package com.dark.tool_neuron.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dark.tool_neuron.models.data.HFModelRepository
import com.dark.tool_neuron.models.data.ModelCategory
import com.dark.tool_neuron.models.data.ModelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.modelRepoDataStore: DataStore<Preferences> by preferencesDataStore(name = "model_repositories")

class ModelRepositoryDataStore(private val context: Context) {

    companion object {
        private val MODEL_REPOS_KEY = stringPreferencesKey("model_repositories")
        private val DELETED_DEFAULTS_KEY = stringPreferencesKey("deleted_default_repo_ids")

        private fun ggufRepository(
            id: String,
            name: String,
            repoPath: String,
            category: ModelCategory
        ) = HFModelRepository(
            id = id,
            name = name,
            repoPath = repoPath,
            modelType = ModelType.GGUF,
            isEnabled = true,
            category = category
        )

        private fun sdRepository(
            id: String,
            name: String,
            repoPath: String,
            category: ModelCategory
        ) = HFModelRepository(
            id = id,
            name = name,
            repoPath = repoPath,
            modelType = ModelType.SD,
            isEnabled = true,
            category = category
        )

        private fun isUnsupportedGemma4Repository(repo: HFModelRepository): Boolean {
            return repo.id.contains("gemma4", ignoreCase = true) ||
                    repo.name.contains("Gemma 4", ignoreCase = true) ||
                    repo.id.contains("financegemma-e4b", ignoreCase = true) ||
                    repo.name.contains("FinanceGemma E4B", ignoreCase = true) ||
                    repo.repoPath.contains("FinanceGemma-E4B", ignoreCase = true) ||
                    repo.repoPath.contains("gemma-4", ignoreCase = true)
        }

        val DEFAULT_REPOSITORIES = listOf(
            // === GENERAL ===
            ggufRepository("unsloth-qwen3_5-0_8b", "Qwen3.5 0.8B", "unsloth/Qwen3.5-0.8B-GGUF", ModelCategory.GENERAL),
            ggufRepository("unsloth-qwen3_5-2b", "Qwen3.5 2B", "unsloth/Qwen3.5-2B-GGUF", ModelCategory.GENERAL),
            ggufRepository("unsloth-qwen3_5-4b", "Qwen3.5 4B", "unsloth/Qwen3.5-4B-GGUF", ModelCategory.GENERAL),
            ggufRepository("unsloth-qwen3_5-9b", "Qwen3.5 9B", "unsloth/Qwen3.5-9B-GGUF", ModelCategory.GENERAL),
            ggufRepository("unsloth-qwen3_5-35b-a3b", "Qwen3.5 35B A3B MoE", "unsloth/Qwen3.5-35B-A3B-GGUF", ModelCategory.GENERAL),
            ggufRepository("unsloth-qwen3_5-35b-a3b-mtp", "Qwen3.5 35B A3B MTP", "unsloth/Qwen3.5-35B-A3B-MTP-GGUF", ModelCategory.GENERAL),
            ggufRepository("qwen3_6-35b-a3b-mtp", "Qwen3.6 35B A3B MTP", "saidonnet/Qwen3.6-35B-A3B-MTP-GGUF", ModelCategory.GENERAL),
            ggufRepository("unsloth-qwen3-30b-a3b-instruct-2507", "Qwen3 30B A3B Instruct 2507", "unsloth/Qwen3-30B-A3B-Instruct-2507-GGUF", ModelCategory.GENERAL),
            ggufRepository("bartowski-qwen3-30b-a3b-instruct-2507", "Qwen3 30B A3B Instruct Bartowski", "bartowski/Qwen_Qwen3-30B-A3B-Instruct-2507-GGUF", ModelCategory.GENERAL),
            ggufRepository("unsloth-qwen3-14b", "Qwen3 14B", "unsloth/Qwen3-14B-GGUF", ModelCategory.GENERAL),
            ggufRepository("unsloth-qwen3-8b", "Qwen3 8B", "unsloth/Qwen3-8B-GGUF", ModelCategory.GENERAL),
            ggufRepository("unsloth-qwen3-vl-4b-instruct", "Qwen3 VL 4B Instruct", "unsloth/Qwen3-VL-4B-Instruct-GGUF", ModelCategory.GENERAL),
            ggufRepository("lfm2-24b-a2b-bartowski", "LFM2 24B A2B MoE", "bartowski/LiquidAI_LFM2-24B-A2B-GGUF", ModelCategory.GENERAL),
            // === MEDICAL ===
            ggufRepository("medgemma-4b-it", "MedGemma 4B IT", "unsloth/medgemma-4b-it-GGUF", ModelCategory.MEDICAL),
            ggufRepository("medgemma-27b-text-it", "MedGemma 27B Text IT", "unsloth/medgemma-27b-text-it-GGUF", ModelCategory.MEDICAL),
            ggufRepository("medgemma-27b-it", "MedGemma 27B IT", "unsloth/medgemma-27b-it-GGUF", ModelCategory.MEDICAL),
            ggufRepository("bartowski-medgemma-27b-it", "MedGemma 27B IT Bartowski", "bartowski/google_medgemma-27b-it-GGUF", ModelCategory.MEDICAL),
            ggufRepository("qwen3_5-medical-gspo", "Qwen3.5 Medical GSPO", "mradermacher/Qwen3.5-Medical-GSPO-GGUF", ModelCategory.MEDICAL),
            ggufRepository("qwen3_5-medical-gspo-i1", "Qwen3.5 Medical GSPO i1", "mradermacher/Qwen3.5-Medical-GSPO-i1-GGUF", ModelCategory.MEDICAL),
            ggufRepository("gccl-medical-qwen3_5-4b", "GCCL Medical Qwen3.5 4B", "mradermacher/GCCL-Medical-LLM-Qwen3.5-4B-GGUF", ModelCategory.MEDICAL),
            ggufRepository("qwen3_5-9b-medical-v2", "Qwen3.5 9B Medical v2", "MateoM4/qwen3.5-9b-finetuned-medical-GGUF", ModelCategory.MEDICAL),
            ggufRepository("qwen3_5-0_8b-medical-id", "Qwen3.5 0.8B Medical ID", "AriesDjaenuri/qwen35-0.8b-medical-id-GGUF", ModelCategory.MEDICAL),
            ggufRepository("qwen3_5-2b-medical-id", "Qwen3.5 2B Medical ID", "AriesDjaenuri/qwen35-2b-medical-id-GGUF", ModelCategory.MEDICAL),
            ggufRepository("qwen3_5-4b-medical-id", "Qwen3.5 4B Medical ID", "AriesDjaenuri/qwen35-4b-medical-id-GGUF", ModelCategory.MEDICAL),
            // === RESEARCH ===
            ggufRepository("deepseek-r1-0528-qwen3-8b-lmstudio", "DeepSeek R1 0528 Qwen3 8B", "lmstudio-community/DeepSeek-R1-0528-Qwen3-8B-GGUF", ModelCategory.RESEARCH),
            ggufRepository("deepseek-r1-0528-qwen3-8b-unsloth", "DeepSeek R1 0528 Qwen3 8B Unsloth", "unsloth/DeepSeek-R1-0528-Qwen3-8B-GGUF", ModelCategory.RESEARCH),
            ggufRepository("deepseek-r1-0528-qwen3-8b-maziyar", "DeepSeek R1 0528 Qwen3 8B Maziyar", "MaziyarPanahi/DeepSeek-R1-0528-Qwen3-8B-GGUF", ModelCategory.RESEARCH),
            ggufRepository("qwen3_5-9b-claude-reasoning-v2", "Qwen3.5 9B Reasoning v2", "Jackrong/Qwen3.5-9B-Claude-4.6-Opus-Reasoning-Distilled-v2-GGUF", ModelCategory.RESEARCH),
            ggufRepository("qwen3_5-9b-deepseek-flash", "Qwen3.5 9B DeepSeek Flash", "Jackrong/Qwen3.5-9B-DeepSeek-V4-Flash-GGUF", ModelCategory.RESEARCH),
            ggufRepository("yui-math-python-qwen3_5-4b", "Yui Math Python Qwen3.5 4B", "naksyu/yui-math-python-qwen3.5-4b-v0.5d-fft-GGUF", ModelCategory.RESEARCH),
            ggufRepository("qwen3_5-2b-math", "Qwen3.5 2B Math", "hpham307/qwen3.5-2b-math-gguf", ModelCategory.RESEARCH),
            ggufRepository("qwen3_5-4b-math-quiz", "Qwen3.5 4B Math Quiz", "lzhang02/Qwen3.5-4B-Math-Quiz-GGUF", ModelCategory.RESEARCH),
            ggufRepository("qwen3_5-9b-data-science-tr", "Qwen3.5 9B Data Science TR", "murataksit34/Qwen3.5-9B-Data-Science-Insight-TR-16.2K-Q4_K_M-GGUF", ModelCategory.RESEARCH),
            ggufRepository("qwen3_5-9b-data-science", "Qwen3.5 9B Data Science", "murataksit34/Qwen3.5-9B-Data-Science-Insight-16.5K-Q4_K_M-GGUF", ModelCategory.RESEARCH),
            ggufRepository("qwen3_5-9b-researcher", "Qwen3.5 9B Researcher", "utareen/Qwen3.5-9B-Researcher-v1-GGUF", ModelCategory.RESEARCH),
            // === CODING ===
            ggufRepository("qwen3_5-9b-coder", "Qwen3.5 9B Coder", "mradermacher/Qwen3.5-9B-Coder-GGUF", ModelCategory.CODING),
            ggufRepository("qwen3_5-9b-coder-i1", "Qwen3.5 9B Coder i1", "mradermacher/Qwen3.5-9B-Coder-i1-GGUF", ModelCategory.CODING),
            ggufRepository("qwen3_5-4b-agentic-coder-v4", "Qwen3.5 4B Agentic Coder v4", "mradermacher/qwen3.5-4b-agentic-coder-v4-GGUF", ModelCategory.CODING),
            ggufRepository("qwen3_5-4b-agentic-coder-v4-i1", "Qwen3.5 4B Agentic Coder v4 i1", "mradermacher/qwen3.5-4b-agentic-coder-v4-i1-GGUF", ModelCategory.CODING),
            ggufRepository("qwen3_5-9b-sushi-coder", "Qwen3.5 9B Sushi Coder RL", "bigatuna/Qwen3.5-9b-Sushi-Coder-RL-GGUF", ModelCategory.CODING),
            ggufRepository("qwen3_6-27b-a3b-coder-mtp", "Qwen3.6 27B A3B Coder MTP", "ManniX-ITA/Qwen3.6-27B-A3B-Coder-MTP-GGUF", ModelCategory.CODING),
            ggufRepository("qwen3_6-27b-a3b-coder", "Qwen3.6 27B A3B Coder", "mradermacher/Qwen3.6-27B-A3B-Coder-GGUF", ModelCategory.CODING),
            ggufRepository("qwen3_6-27b-agentic-coder", "Qwen3.6 27B Agentic Coder", "jackasda211233/Qwen3.6-27B-AEON-RYS-Agentic-Coder-PatchCode-GGUF", ModelCategory.CODING),
            ggufRepository("qwen3_5-4b-python-coder", "Qwen3.5 4B Python Coder", "Abiray/Qwen3.5-4B-Python-Coder-GGUF", ModelCategory.CODING),
            ggufRepository("qwen3_5-9b-python-coder", "Qwen3.5 9B Python Coder", "lainlives/Qwen3.5-9B-Python-Coder-GGUF", ModelCategory.CODING),
            ggufRepository("qwen3_5-9b-claude-code", "Qwen3.5 9B Claude Code", "empero-ai/Qwen3.5-9B-Claude-Code-GGUF", ModelCategory.CODING),
            // === BUSINESS ===
            ggufRepository("businessgpt-qwen3_5-2b-v14", "BusinessGPT Qwen3.5 2B v14", "vXofi/businessgpt-v14-dpo-qwen3.5-2b-gguf", ModelCategory.BUSINESS),
            ggufRepository("businessgpt-qwen3_5-2b-v11", "BusinessGPT Qwen3.5 2B v11", "vXofi/businessgpt-v11-qwen3.5-2b-gguf", ModelCategory.BUSINESS),
            ggufRepository("businessgpt-qwen3_5-9b-v16", "BusinessGPT Qwen3.5 9B v16", "vXofi/businessgpt-v16-qwen3.5-9b-gguf", ModelCategory.BUSINESS),
            ggufRepository("qwen3_5-legal-q5", "Qwen3.5 Legal Q5", "Reytian/qwen3.5-legal-q5_k_m-gguf", ModelCategory.BUSINESS),
            ggufRepository("qwen3-4b-thinking-2507-business", "Qwen3 4B Thinking 2507 Business", "unsloth/Qwen3-4B-Thinking-2507-GGUF", ModelCategory.BUSINESS),
            ggufRepository("deepseek-r1-qwen3-8b-business", "DeepSeek R1 Qwen3 8B Business", "unsloth/DeepSeek-R1-0528-Qwen3-8B-GGUF", ModelCategory.BUSINESS),
            ggufRepository("legal-qwen3_5-9b", "Legal Qwen3.5 9B", "scottyjmp5/Legal-Qwen3.5-9B-Abliterated-GGUF", ModelCategory.BUSINESS),
            ggufRepository("legal-ai-qwen3_5-q8", "Legal AI Qwen3.5 Q8", "claspi2509/legal-AI-advanced-qwen3.5-q8-gguf", ModelCategory.BUSINESS),
            ggufRepository("qwen3-4b-sales-strategist", "Qwen3 4B Sales Strategist", "mradermacher/Qwen3-4B-Thinking-2507-Sales-Strategist-v1-GGUF", ModelCategory.BUSINESS),
            ggufRepository("qwen3-4b-sales-strategist-i1", "Qwen3 4B Sales Strategist i1", "mradermacher/Qwen3-4B-Thinking-2507-Sales-Strategist-v1-i1-GGUF", ModelCategory.BUSINESS),
            ggufRepository("accounting-qwen3_5-9b", "Accounting Qwen3.5 9B", "flarexio/accounting-qwen35-9b-gguf-smoke", ModelCategory.BUSINESS),
            // === UNCENSORED ===
            ggufRepository("qwen3_5-9b-defiant-fable-uncensored", "Qwen3.5 9B Defiant Fable Uncensored", "DavidAU/Qwen3.5-9B-The-Defiant-Fable-Uncensored-Heretic-NEO-IMATRIX-MAX-MTP-GGUF", ModelCategory.UNCENSORED),
            ggufRepository("qwen3_5-9b-ultra-uncensored", "Qwen3.5 9B Ultra Uncensored", "mradermacher/Qwen3.5-9B-ultra-uncensored-heretic-v2-GGUF", ModelCategory.UNCENSORED),
            ggufRepository("huihui-qwen3_5-9b-abliterated", "Huihui Qwen3.5 9B Abliterated", "mradermacher/Huihui-Qwen3.5-9B-abliterated-GGUF", ModelCategory.UNCENSORED),
            ggufRepository("qwen3_5-35b-a3b-abliterated", "Qwen3.5 35B A3B Abliterated", "HeYujie/Qwen3.5-35B-A3B-abliterated-GGUF", ModelCategory.UNCENSORED),
            ggufRepository("huihui-qwen3_5-35b-a3b-abliterated", "Huihui Qwen3.5 35B A3B Abliterated", "mradermacher/Huihui-Qwen3.5-35B-A3B-abliterated-GGUF", ModelCategory.UNCENSORED),
            ggufRepository("qwen3_6-35b-a3b-uncensored", "Qwen3.6 35B A3B Uncensored", "llmfan46/Qwen3.6-35B-A3B-uncensored-heretic-GGUF", ModelCategory.UNCENSORED),
            ggufRepository("qwen3_6-35b-a3b-uncensored-apex", "Qwen3.6 35B A3B Uncensored APEX", "SC117/Qwen3.6-35B-A3B-uncensored-heretic-Native-MTP-Preserved-APEX-GGUF", ModelCategory.UNCENSORED),
            ggufRepository("qwen3_5-9b-highiq-thinking-uncensored", "Qwen3.5 9B HighIQ Thinking Uncensored", "mradermacher/Qwen3.5-9B-Claude-4.6-HighIQ-THINKING-HERETIC-UNCENSORED-GGUF", ModelCategory.UNCENSORED),
            ggufRepository("qwen3_5-9b-deckard-uncensored", "Qwen3.5 9B Deckard Uncensored", "DavidAU/Qwen3.5-9B-Claude-4.6-Opus-Deckard-V4.2-Uncensored-Heretic-Thinking-GGUF", ModelCategory.UNCENSORED),
            ggufRepository("qwen3-30b-a3b-erotic-abliterated", "Qwen3 30B A3B Abliterated Erotic", "mradermacher/Qwen3-30B-A3B-abliterated-erotic-i1-GGUF", ModelCategory.UNCENSORED),
            ggufRepository("huihui-qwen3-coder-30b-a3b-abliterated", "Huihui Qwen3 Coder 30B A3B Abliterated", "mradermacher/Huihui-Qwen3-Coder-30B-A3B-Instruct-abliterated-i1-GGUF", ModelCategory.UNCENSORED),
            // === CYBERSECURITY ===
            ggufRepository("qwen3_5-0_8b-cyber", "Qwen3.5 0.8B Cyber", "reaperdoesntknow/Qwen3.5-0.8-Cyber-GGUF", ModelCategory.CYBERSECURITY),
            ggufRepository("qwen3_5-9b-cyber-v3", "Qwen3.5 9B Cyber v3", "mradermacher/Qwen3.5-9B-Uncensored-cyber-v3-GGUF", ModelCategory.CYBERSECURITY),
            ggufRepository("qwen3_5-9b-cyber-v2", "Qwen3.5 9B Cyber v2", "mradermacher/Qwen3.5-9B-Uncensored-cyber-v2-GGUF", ModelCategory.CYBERSECURITY),
            ggufRepository("ravenx-qwen3_6-35b-a3b", "RavenX Qwen3.6 35B A3B CyberAgent", "deadbydawn101/RavenX-CyberAgent-Qwen3.6-35B-A3B-Opus-4.7-OpenMythos-Pentester-BugHunter-RATH-GGUF", ModelCategory.CYBERSECURITY),
            ggufRepository("endy-qwen3_6-35b-a3b-cybersec", "Endy Qwen3.6 35B A3B CyberSec", "endystrike/Endy-Qwen3.6-CyberSec-35B-A3B-GGUF", ModelCategory.CYBERSECURITY),
            ggufRepository("titus-cybersecurity-q4", "Titus Cybersecurity LLM", "AlicanKiraz0/Titus-CybersecurityLLM-v1.0-Q4_K_M-No-MTP-GGUF", ModelCategory.CYBERSECURITY),
            ggufRepository("qwen3-4b-cybersecurity", "Qwen3 4B Cybersecurity", "sillykiwi/Qwen3-4B-Cybersecurity-Heretic-16bit-Q4_K_M-GGUF", ModelCategory.CYBERSECURITY),
            ggufRepository("lily-cybersecurity-7b", "Lily Cybersecurity 7B", "QuantFactory/Lily-Cybersecurity-7B-v0.2-GGUF", ModelCategory.CYBERSECURITY),
            ggufRepository("securityllm", "SecurityLLM", "QuantFactory/SecurityLLM-GGUF", ModelCategory.CYBERSECURITY),
            ggufRepository("pentesting-gpt", "Pentesting GPT", "mradermacher/Pentesting-GPT-v1.0-GGUF", ModelCategory.CYBERSECURITY),
            ggufRepository("seneca-cybersecurity", "Seneca Cybersecurity LLM", "AlicanKiraz0/Seneca-Cybersecurity-LLM-Q4_K_M-GGUF", ModelCategory.CYBERSECURITY),
            // === IMAGE GENERATION (SD) ===
            sdRepository("sd-qnn", "Stable Diffusion (NPU)", "xororz/sd-qnn", ModelCategory.GENERAL),
            sdRepository("sd-mnn", "Stable Diffusion (CPU)", "xororz/sd-mnn", ModelCategory.GENERAL),
            // === NSFW IMAGE GENERATION (SD) ===
            sdRepository("sd-mistoonanime-qnn", "MistoonAnime v3.0 (NPU)", "Mr-J-369/mistoonAnime_v30-SD1.5-qnn2.28", ModelCategory.UNCENSORED),
            sdRepository("sd-cyberrealistic-qnn", "CyberRealistic Classic (NPU)", "Mr-J-369/cyberrealistic-classic-SD1.5-qnn2.28", ModelCategory.UNCENSORED),
            sdRepository("sd-realhotspice-qnn", "RealHotSpice (NPU)", "Mr-J-369/RealHotSpice-SD1.5-qnn2.28", ModelCategory.UNCENSORED)
        )
    }

    val repositories: Flow<List<HFModelRepository>> =
        context.modelRepoDataStore.data.map { preferences ->
            val json = preferences[MODEL_REPOS_KEY]
            val deletedJson = preferences[DELETED_DEFAULTS_KEY]
            val deletedIds = deletedJson?.let {
                try { Json.decodeFromString<Set<String>>(it) } catch (_: Exception) { emptySet() }
            } ?: emptySet()

            if (json != null) {
                try {
                    val saved = Json.decodeFromString<List<HFModelRepository>>(json)
                        .filterNot { isUnsupportedGemma4Repository(it) }
                    val savedIds = saved.map { it.id }.toSet()
                    val newDefaults = DEFAULT_REPOSITORIES.filter {
                        it.id !in savedIds && it.id !in deletedIds
                    }
                    if (newDefaults.isNotEmpty()) saved + newDefaults else saved
                } catch (e: Exception) {
                    DEFAULT_REPOSITORIES
                }
            } else {
                DEFAULT_REPOSITORIES
            }
        }

    suspend fun saveRepositories(repos: List<HFModelRepository>) {
        context.modelRepoDataStore.edit { preferences ->
            preferences[MODEL_REPOS_KEY] = Json.encodeToString(repos)
        }
    }

    suspend fun addRepository(repo: HFModelRepository) {
        val current = repositories.first()
        saveRepositories(current + repo)
    }

    suspend fun removeRepository(repoId: String) {
        val current = repositories.first()
        saveRepositories(current.filterNot { it.id == repoId })
        if (DEFAULT_REPOSITORIES.any { it.id == repoId }) {
            context.modelRepoDataStore.edit { preferences ->
                val existing = preferences[DELETED_DEFAULTS_KEY]?.let {
                    try { Json.decodeFromString<Set<String>>(it) } catch (_: Exception) { emptySet() }
                } ?: emptySet()
                preferences[DELETED_DEFAULTS_KEY] = Json.encodeToString(existing + repoId)
            }
        }
    }

    suspend fun toggleRepository(repoId: String) {
        val current = repositories.first()
        saveRepositories(current.map {
            if (it.id == repoId) it.copy(isEnabled = !it.isEnabled)
            else it
        })
    }

    suspend fun updateRepository(repo: HFModelRepository) {
        val current = repositories.first()
        saveRepositories(current.map {
            if (it.id == repo.id) repo else it
        })
    }
}
