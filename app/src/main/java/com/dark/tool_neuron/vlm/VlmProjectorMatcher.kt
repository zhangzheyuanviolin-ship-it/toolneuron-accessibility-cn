package com.dark.tool_neuron.vlm

import java.io.File

object VlmProjectorMatcher {
    fun bestMatch(
        modelId: String,
        modelName: String,
        repositoryUrl: String = "",
        projectors: List<File>
    ): File? {
        if (projectors.isEmpty()) return null

        val modelTokens = tokensOf("$modelId $modelName $repositoryUrl")
        if (modelTokens.isEmpty()) return null

        return projectors
            .mapNotNull { projector ->
                val projectorTokens = tokensOf(projector.nameWithoutExtension)
                val shared = projectorTokens.count { it in modelTokens }
                val hasProjectorMarker = projector.name.contains("mmproj", ignoreCase = true) ||
                        projector.name.contains("projector", ignoreCase = true) ||
                        projector.name.contains("vision", ignoreCase = true)
                val score = shared + if (hasProjectorMarker) 2 else 0
                if (shared >= 2 && hasProjectorMarker) projector to score else null
            }
            .maxByOrNull { it.second }
            ?.first
    }

    fun installedProjectorIds(projectorDir: File): Set<String> =
        projectorDir.listFiles { file ->
            file.isFile && file.extension.equals("gguf", ignoreCase = true)
        }?.map { it.name.removeSuffix(".gguf") }?.toSet() ?: emptySet()

    private fun tokensOf(value: String): Set<String> =
        value.lowercase()
            .replace(".gguf", " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .split(' ')
            .map { it.trim() }
            .filter { it.length >= 2 && it !in ignoredTokens }
            .toSet()

    private val ignoredTokens = setOf(
        "gguf", "q2", "q3", "q4", "q5", "q6", "q8", "km", "ks", "kl",
        "q4k", "q5k", "q6k", "q8_0", "q4_k_m", "q4_k_s", "q6_k"
    )
}
