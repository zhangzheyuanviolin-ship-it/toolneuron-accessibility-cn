package com.dark.tool_neuron.vlm

enum class VlmImageQuality(
    val value: String,
    val label: String,
    val maxLongEdge: Int?,
    val jpegQuality: Int
) {
    COMPACT("compact", "256 Compact", 256, 80),
    FAST("fast", "384 Fast", 384, 82),
    BALANCED("balanced", "512 Balanced", 512, 85),
    CLEAR("clear", "768 Clear", 768, 88),
    ORIGINAL("original", "Original", null, 92);

    companion object {
        fun from(value: String?): VlmImageQuality =
            entries.firstOrNull { it.value == value || it.name == value } ?: COMPACT
    }
}
