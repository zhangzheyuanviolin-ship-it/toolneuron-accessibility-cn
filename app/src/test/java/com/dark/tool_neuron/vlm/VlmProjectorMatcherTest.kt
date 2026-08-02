package com.dark.tool_neuron.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class VlmProjectorMatcherTest {
    @Test
    fun findsProjectorForSameRepositoryAndModelFamily() {
        val projectors = listOf(
            File("/models/other-mmproj.gguf"),
            File("/models/qwen3-vl-4b-mmproj.gguf")
        )

        val match = VlmProjectorMatcher.bestMatch(
            modelId = "qwen3-vl-4b-q4_k_m",
            modelName = "Qwen3 VL 4B - Q4_K_M",
            repositoryUrl = "Qwen/Qwen3-VL-4B-GGUF",
            projectors = projectors
        )

        assertEquals("qwen3-vl-4b-mmproj.gguf", match?.name)
    }

    @Test
    fun returnsNullWhenNoReasonableProjectorExists() {
        val match = VlmProjectorMatcher.bestMatch(
            modelId = "qwen3-30b-a3b",
            modelName = "Qwen3 30B A3B",
            repositoryUrl = "Qwen/Qwen3-30B-A3B-GGUF",
            projectors = listOf(File("/models/gemma-mmproj.gguf"))
        )

        assertNull(match)
    }
}
