package com.dark.tool_neuron.vlm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.max

data class VlmImagePayload(
    val bytes: ByteArray,
    val originalWidth: Int,
    val originalHeight: Int,
    val processedWidth: Int,
    val processedHeight: Int,
    val originalBytes: Int,
    val processedBytes: Int,
    val preprocessingMs: Long,
    val quality: VlmImageQuality
)

object VlmImagePreprocessor {
    fun preprocess(input: ByteArray, quality: VlmImageQuality): VlmImagePayload {
        val start = System.currentTimeMillis()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
        val originalWidth = bounds.outWidth.takeIf { it > 0 } ?: 0
        val originalHeight = bounds.outHeight.takeIf { it > 0 } ?: 0
        if (originalWidth <= 0 || originalHeight <= 0) {
            return VlmImagePayload(input, 0, 0, 0, 0, input.size, input.size, elapsed(start), quality)
        }

        val maxEdge = quality.maxLongEdge
        val scale = if (maxEdge == null) 1f else minOf(1f, maxEdge.toFloat() / max(originalWidth, originalHeight).toFloat())
        val targetWidth = max(1, (originalWidth * scale).toInt())
        val targetHeight = max(1, (originalHeight * scale).toInt())
        val decodeOptions = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSizeFor(originalWidth, originalHeight, targetWidth, targetHeight)
        }
        val decoded = BitmapFactory.decodeByteArray(input, 0, input.size, decodeOptions)
            ?: return VlmImagePayload(input, originalWidth, originalHeight, originalWidth, originalHeight, input.size, input.size, elapsed(start), quality)

        val scaled = if (decoded.width != targetWidth || decoded.height != targetHeight) {
            Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
        } else {
            decoded
        }

        val flattened = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.RGB_565)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(scaled, 0f, 0f, null)
        }

        val output = ByteArrayOutputStream()
        flattened.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, output)
        val bytes = output.toByteArray()

        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        flattened.recycle()

        return VlmImagePayload(
            bytes = bytes,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            processedWidth = targetWidth,
            processedHeight = targetHeight,
            originalBytes = input.size,
            processedBytes = bytes.size,
            preprocessingMs = elapsed(start),
            quality = quality
        )
    }

    private fun sampleSizeFor(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        var sample = 1
        val ratio = max(
            ceil(width.toFloat() / targetWidth.toFloat()).toInt(),
            ceil(height.toFloat() / targetHeight.toFloat()).toInt()
        )
        while (sample * 2 <= ratio) sample *= 2
        return sample
    }

    private fun elapsed(startMs: Long): Long = System.currentTimeMillis() - startMs
}
