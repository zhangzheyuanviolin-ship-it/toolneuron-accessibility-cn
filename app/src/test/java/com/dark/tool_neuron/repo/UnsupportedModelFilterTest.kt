package com.dark.tool_neuron.repo

import com.dark.tool_neuron.models.data.HFModelRepository
import com.dark.tool_neuron.models.data.HuggingFaceModel
import com.dark.tool_neuron.models.data.ModelCategory
import com.dark.tool_neuron.models.data.ModelType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnsupportedModelFilterTest {
    @Test
    fun rejectsGemma4RepositoryNameVariants() {
        assertTrue(
            UnsupportedModelFilter.isUnsupportedGemma4Repository(
                repo("gemma4-2b", "Gemma4 2B", "author/gemma4-2b-GGUF")
            )
        )
        assertTrue(
            UnsupportedModelFilter.isUnsupportedGemma4Repository(
                repo("gemma-4-26b", "Gemma 4 26B A4B", "author/gemma-4-26B-A4B-it-GGUF")
            )
        )
        assertTrue(
            UnsupportedModelFilter.isUnsupportedGemma4Repository(
                repo("financegemma-e4b", "FinanceGemma E4B", "mradermacher/FinanceGemma-E4B-GGUF")
            )
        )
    }

    @Test
    fun rejectsGemma4ModelsFromStaleCache() {
        val staleModel = HuggingFaceModel(
            id = "cached-gemma4-q4",
            name = "Gemma4 Community Mod - Q4_K_M",
            description = "stale cached model",
            fileUri = "author/gemma4-community/resolve/main/model.gguf",
            approximateSize = "2 GB",
            modelType = ModelType.GGUF,
            isZip = false,
            tags = listOf("GGUF", "gemma4"),
            repositoryUrl = "author/gemma4-community"
        )

        assertTrue(UnsupportedModelFilter.isUnsupportedGemma4Model(staleModel))
    }

    @Test
    fun keepsSupportedQwenAndMedGemmaRepositories() {
        assertFalse(
            UnsupportedModelFilter.isUnsupportedGemma4Repository(
                repo("qwen3-vl-4b", "Qwen3 VL 4B", "unsloth/Qwen3-VL-4B-Instruct-GGUF")
            )
        )
        assertFalse(
            UnsupportedModelFilter.isUnsupportedGemma4Repository(
                repo("medgemma-4b-it", "MedGemma 4B IT", "unsloth/medgemma-4b-it-GGUF")
            )
        )
    }

    private fun repo(id: String, name: String, path: String): HFModelRepository {
        return HFModelRepository(
            id = id,
            name = name,
            repoPath = path,
            modelType = ModelType.GGUF,
            isEnabled = true,
            category = ModelCategory.GENERAL
        )
    }
}
