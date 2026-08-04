package com.dark.tool_neuron.repo

import com.dark.tool_neuron.models.data.HFModelRepository
import com.dark.tool_neuron.models.data.HuggingFaceModel

object UnsupportedModelFilter {
    fun isUnsupportedGemma4Repository(repo: HFModelRepository): Boolean {
        return listOf(repo.id, repo.name, repo.repoPath).any { containsUnsupportedGemma4Marker(it) }
    }

    fun isUnsupportedGemma4Model(model: HuggingFaceModel): Boolean {
        return listOf(
            model.id,
            model.name,
            model.description,
            model.fileUri,
            model.repositoryUrl
        ).any { containsUnsupportedGemma4Marker(it) } ||
                model.tags.any { containsUnsupportedGemma4Marker(it) }
    }

    private fun containsUnsupportedGemma4Marker(value: String): Boolean {
        val text = value.lowercase()
        val compact = text.filter { it.isLetterOrDigit() }

        return text.contains("gemma4") ||
                text.contains("gemma-4") ||
                text.contains("gemma_4") ||
                text.contains("gemma 4") ||
                text.contains("gemma.4") ||
                compact.contains("financegemmae4b")
    }
}
