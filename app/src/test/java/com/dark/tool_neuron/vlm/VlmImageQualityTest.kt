package com.dark.tool_neuron.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VlmImageQualityTest {
    @Test
    fun defaultQualityUsesCompact256Preset() {
        val quality = VlmImageQuality.from(null)

        assertEquals(VlmImageQuality.COMPACT, quality)
        assertEquals(256, quality.maxLongEdge)
        assertEquals(80, quality.jpegQuality)
    }

    @Test
    fun originalQualityDisablesLongEdgeResize() {
        assertNull(VlmImageQuality.ORIGINAL.maxLongEdge)
    }

    @Test
    fun invalidStoredValueFallsBackToCompact() {
        assertEquals(VlmImageQuality.COMPACT, VlmImageQuality.from("bad-value"))
    }
}
