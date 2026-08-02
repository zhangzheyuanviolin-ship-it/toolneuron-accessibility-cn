package com.dark.tool_neuron.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VlmImageQualityTest {
    @Test
    fun defaultQualityUsesBalanced512Preset() {
        val quality = VlmImageQuality.from(null)

        assertEquals(VlmImageQuality.BALANCED, quality)
        assertEquals(512, quality.maxLongEdge)
        assertEquals(85, quality.jpegQuality)
    }

    @Test
    fun originalQualityDisablesLongEdgeResize() {
        assertNull(VlmImageQuality.ORIGINAL.maxLongEdge)
    }

    @Test
    fun invalidStoredValueFallsBackToBalanced() {
        assertEquals(VlmImageQuality.BALANCED, VlmImageQuality.from("bad-value"))
    }
}
