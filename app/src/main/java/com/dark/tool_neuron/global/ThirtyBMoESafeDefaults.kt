package com.dark.tool_neuron.global

import com.dark.tool_neuron.models.engine_schema.GgufLoadingParams

object ThirtyBMoESafeDefaults {
    private const val LARGE_MODEL_MB = 12 * 1024

    fun isLargeMoEModel(modelName: String, modelSizeMB: Int): Boolean {
        val normalized = modelName.uppercase()
        return modelSizeMB >= LARGE_MODEL_MB ||
            normalized.contains("30B") ||
            normalized.contains("24B") ||
            normalized.contains("A3B") ||
            normalized.contains("A2B") ||
            normalized.contains("MOE")
    }

    fun loadingParamsFor(
        base: GgufLoadingParams,
        modelName: String,
        modelSizeMB: Int
    ): GgufLoadingParams {
        if (!isLargeMoEModel(modelName, modelSizeMB)) return base

        return base.copy(
            threads = when {
                base.threads <= 0 -> 4
                else -> base.threads.coerceIn(2, 4)
            },
            ctxSize = base.ctxSize.coerceIn(2048, 4096),
            batchSize = base.batchSize.coerceIn(64, 128),
            useMmap = true,
            useMlock = false,
            flashAttn = false,
            cacheTypeK = 10,
            cacheTypeV = 10
        )
    }

    fun shouldSkipWarmUp(modelName: String, modelSizeMB: Int): Boolean =
        isLargeMoEModel(modelName, modelSizeMB)
}
