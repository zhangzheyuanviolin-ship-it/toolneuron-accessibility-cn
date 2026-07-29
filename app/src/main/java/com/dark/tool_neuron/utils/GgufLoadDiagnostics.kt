package com.dark.tool_neuron.utils

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.dark.gguf_lib.ErrorTracker
import com.dark.tool_neuron.models.engine_schema.GgufEngineSchema
import com.dark.tool_neuron.models.enums.PathType
import com.dark.tool_neuron.models.table_schema.Model
import com.dark.tool_neuron.models.table_schema.ModelConfig
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object GgufLoadDiagnostics {
    private const val QWEN36_27B_Q4_K_M_BYTES = 16_057_051_168L
    private const val DEFAULT_ALIGNMENT = 32L
    private val utf8: Charset = Charsets.UTF_8

    fun buildFailureReport(
        context: Context,
        model: Model,
        config: ModelConfig,
        loadVia: String,
        throwable: Throwable? = null,
        elapsedMs: Long? = null
    ): String {
        val lines = mutableListOf<String>()
        lines += "GGUF model load failed"
        lines += "Model: ${model.modelName}"
        lines += "Load path: $loadVia"
        elapsedMs?.let { lines += "Native load elapsed: ${it}ms" }
        throwable?.let { error ->
            lines += "Exception: ${error::class.java.name}: ${error.message ?: "(no message)"}"
        }
        lines += ""

        lines += "Current loading params:"
        val schema = GgufEngineSchema.fromJson(loadingJson = config.modelLoadingParams, inferenceJson = config.modelInferenceParams)
        lines += "ctxSize=${schema.loadingParams.ctxSize}, threads=${schema.loadingParams.threads}, flashAttn=${schema.loadingParams.flashAttn}"
        lines += "cacheTypeK=${schema.loadingParams.cacheTypeK}, cacheTypeV=${schema.loadingParams.cacheTypeV}, maxTokens=${schema.inferenceParams.maxTokens}"
        lines += ""

        appendMemoryInfo(context, lines)
        lines += ""
        appendNativeErrorTracker(lines)
        lines += ""

        if (model.pathType == PathType.CONTENT_URI) {
            appendContentUriInfo(context, model, lines)
        } else {
            appendFileInfo(model, lines)
        }

        lines += ""
        lines += "Local diagnosis:"
        lines += diagnose(lines)
        return lines.joinToString("\n")
    }

    private fun appendMemoryInfo(context: Context, lines: MutableList<String>) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(info)
        if (am == null) {
            lines += "Device memory: unavailable"
            return
        }
        lines += "Device memory:"
        lines += "avail=${formatBytes(info.availMem)}, threshold=${formatBytes(info.threshold)}, lowMemory=${info.lowMemory}"
    }

    private fun appendNativeErrorTracker(lines: MutableList<String>) {
        lines += "Native error tracker:"
        try {
            val json = ErrorTracker.getLastErrorJson()
            lines += if (json.isBlank() || json == "{}") {
                "lastError={}"
            } else {
                "lastError=$json"
            }
        } catch (e: Throwable) {
            lines += "lastErrorUnavailable=${e::class.java.simpleName}: ${e.message ?: "(no message)"}"
        }
    }

    private fun appendContentUriInfo(context: Context, model: Model, lines: MutableList<String>) {
        lines += "Model file:"
        lines += "uri=${model.modelPath}"
        lines += "dbFileSize=${formatNullableBytes(model.fileSize)}"
        try {
            val uri = Uri.parse(model.modelPath)
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) lines += "displayName=${cursor.getString(nameIndex)}"
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) lines += "providerSize=${formatBytes(cursor.getLong(sizeIndex))}"
                }
            }
            val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
            afd?.use {
                lines += "assetLength=${if (it.length >= 0) formatBytes(it.length) else "unknown"}"
            }
        } catch (e: Exception) {
            lines += "contentUriCheckError=${e::class.java.simpleName}: ${e.message ?: "(no message)"}"
        }
        lines += "GGUF header parse: skipped for content URI"
    }

    private fun appendFileInfo(model: Model, lines: MutableList<String>) {
        lines += "Model file:"
        val file = File(model.modelPath)
        lines += "path=${file.absolutePath}"
        lines += "exists=${file.exists()}, isFile=${file.isFile}, canRead=${file.canRead()}"
        lines += "actualSize=${if (file.exists()) formatBytes(file.length()) else "missing"}"
        lines += "dbFileSize=${formatNullableBytes(model.fileSize)}"
        if (file.exists()) {
            lines += "lastModified=${formatTime(file.lastModified())}"
        }
        knownExpectedSize(model)?.let { expected ->
            lines += "knownExpectedSize=${formatBytes(expected)}"
            if (file.exists() && file.length() != expected) {
                lines += "DEFINITE_SIZE_MISMATCH actual=${file.length()} expected=$expected"
            }
        }
        if (!file.exists() || !file.isFile || !file.canRead()) return

        try {
            val gguf = parseGguf(file)
            lines += ""
            lines += "GGUF header:"
            lines += "magic=${gguf.magic}, version=${gguf.version}, tensors=${gguf.tensorCount}, kv=${gguf.kvCount}"
            if (gguf.architecture != null) lines += "architecture=${gguf.architecture}"
            if (gguf.name != null) lines += "name=${gguf.name}"
            if (gguf.fileType != null) lines += "fileType=${gguf.fileType}"
            if (gguf.quantizationVersion != null) lines += "quantizationVersion=${gguf.quantizationVersion}"
            if (gguf.alignment != null) lines += "alignment=${gguf.alignment}"
            gguf.importantMetadata.forEach { (key, value) -> lines += "$key=$value" }
            lines += "tensorInfoEndOffset=${gguf.tensorInfoEndOffset}"
            lines += "dataStartOffset=${gguf.dataStartOffset}"
            if (gguf.minRequiredFileSize != null) {
                lines += "minRequiredByTensorTable=${formatBytes(gguf.minRequiredFileSize)}"
                if (file.length() < gguf.minRequiredFileSize) {
                    lines += "DEFINITE_TRUNCATED_GGUF actual=${file.length()} requiredAtLeast=${gguf.minRequiredFileSize}"
                }
            } else {
                lines += "minRequiredByTensorTable=unknown (${gguf.unknownTensorTypes} unknown tensor types)"
            }
            if (gguf.highestTensorEndOffset != null) {
                lines += "highestKnownTensorEndOffset=${gguf.highestTensorEndOffset}"
            }
        } catch (e: Exception) {
            lines += ""
            lines += "GGUF header parse error=${e::class.java.simpleName}: ${e.message ?: "(no message)"}"
        }
    }

    private fun diagnose(lines: List<String>): String {
        val joined = lines.joinToString("\n")
        return when {
            "DEFINITE_SIZE_MISMATCH" in joined ->
                "Model file size does not match the known Hub file size. The local GGUF is incomplete or different from the expected Q4_K_M file."
            "DEFINITE_TRUNCATED_GGUF" in joined ->
                "Tensor table requires more bytes than the local file contains. The local GGUF is truncated/incomplete."
            "GGUF header parse error=EOFException" in joined ->
                "GGUF parsing reached EOF early. The local file is very likely incomplete."
            "magic=GGUF" !in joined && "GGUF header parse: skipped" !in joined ->
                "The file could not be confirmed as a valid GGUF file from local header diagnostics."
            "architecture=gemma4" in joined ->
                "The current bundled native llama core appears not to include Gemma 4 support; this model likely needs a native core upgrade."
            "architecture=qwen35moe" in joined ->
                "The GGUF header is qwen35moe. If size and tensor checks are clean, the failure is inside native loading or its load-parameter compatibility, not a missing Qwen3.6 architecture tag."
            else ->
                "No definite local file corruption was detected by app-side diagnostics. Native loading returned failure without a Java exception."
        }
    }

    private fun knownExpectedSize(model: Model): Long? {
        val normalized = "${model.modelName} ${model.modelPath}".lowercase(Locale.US)
        return if (
            normalized.contains("qwen3.6") &&
            (normalized.contains("27b") || normalized.contains("35b-a3b")) &&
            normalized.contains("coder") &&
            normalized.contains("q4_k_m")
        ) {
            QWEN36_27B_Q4_K_M_BYTES
        } else {
            null
        }
    }

    private fun parseGguf(file: File): GgufInfo {
        RandomAccessFile(file, "r").use { raf ->
            val magic = String(ByteArray(4).also { raf.readFully(it) }, Charsets.US_ASCII)
            if (magic != "GGUF") {
                return GgufInfo(magic = magic, version = -1, tensorCount = -1, kvCount = -1)
            }

            val version = raf.readUInt32Le().toLong()
            val tensorCount = raf.readUInt64Le()
            val kvCount = raf.readUInt64Le()
            val metadata = linkedMapOf<String, String>()
            var alignment: Long? = null

            repeat(kvCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) {
                val key = raf.readGgufString()
                val type = raf.readUInt32Le()
                val value = raf.readGgufValue(type)
                if (isImportantKey(key)) metadata[key] = value.take(220)
                if (key == "general.alignment") alignment = value.toLongOrNull()
            }

            var minRequired: Long? = 0L
            var highestKnownEnd: Long? = null
            var unknownTypes = 0
            repeat(tensorCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) {
                val name = raf.readGgufString()
                val dimsCount = raf.readUInt32Le().coerceIn(0L, 8L).toInt()
                var elements = 1L
                repeat(dimsCount) {
                    elements = saturatingMultiply(elements, max(raf.readUInt64Le(), 1L))
                }
                val tensorType = raf.readUInt32Le()
                val offset = raf.readUInt64Le()
                tensorByteSize(tensorType, elements)?.let { tensorBytes ->
                    val end = offset + tensorBytes
                    highestKnownEnd = max(highestKnownEnd ?: 0L, end)
                    minRequired = max(minRequired ?: 0L, end)
                } ?: run {
                    unknownTypes += 1
                    minRequired = null
                }
                if (name.length > 512) throw IllegalStateException("unexpected long tensor name")
            }

            val tensorInfoEnd = raf.filePointer
            val dataStart = alignTo(tensorInfoEnd, alignment ?: DEFAULT_ALIGNMENT)
            val requiredWithDataStart = minRequired?.let { dataStart + it }
            return GgufInfo(
                magic = magic,
                version = version,
                tensorCount = tensorCount,
                kvCount = kvCount,
                architecture = metadata["general.architecture"],
                name = metadata["general.name"],
                fileType = metadata["general.file_type"],
                quantizationVersion = metadata["general.quantization_version"],
                alignment = alignment,
                importantMetadata = metadata.filterKeys {
                    it !in setOf("general.architecture", "general.name", "general.file_type", "general.quantization_version", "general.alignment")
                },
                tensorInfoEndOffset = tensorInfoEnd,
                dataStartOffset = dataStart,
                minRequiredFileSize = requiredWithDataStart,
                highestTensorEndOffset = highestKnownEnd,
                unknownTensorTypes = unknownTypes
            )
        }
    }

    private fun isImportantKey(key: String): Boolean {
        return key == "general.architecture" ||
            key == "general.name" ||
            key == "general.file_type" ||
            key == "general.quantization_version" ||
            key == "general.alignment" ||
            key == "tokenizer.ggml.model" ||
            key == "qwen35moe.expert_count" ||
            key == "qwen35moe.expert_used_count" ||
            key == "qwen35moe.block_count" ||
            key == "qwen35moe.context_length" ||
            key == "qwen35moe.embedding_length" ||
            key == "qwen35moe.feed_forward_length" ||
            key == "qwen3moe.expert_count" ||
            key == "qwen3moe.expert_used_count"
    }

    private fun RandomAccessFile.readGgufValue(type: Long): String {
        return when (type) {
            0L -> readUnsignedByte().toString()
            1L -> readByte().toString()
            2L -> readUInt16Le().toString()
            3L -> readInt16Le().toString()
            4L -> readUInt32Le().toString()
            5L -> readInt32Le().toString()
            6L -> Float.fromBits(readInt32Le()).toString()
            7L -> (readUnsignedByte() != 0).toString()
            8L -> readGgufString()
            9L -> readGgufArraySummary()
            10L -> readUInt64Le().toString()
            11L -> readInt64Le().toString()
            12L -> Double.fromBits(readInt64Le()).toString()
            else -> throw IllegalStateException("unknown GGUF metadata type $type at offset $filePointer")
        }
    }

    private fun RandomAccessFile.readGgufArraySummary(): String {
        val elementType = readUInt32Le()
        val count = readUInt64Le()
        val before = filePointer
        if (elementType == 8L) {
            repeat(count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) { readGgufString() }
        } else {
            val bytesPerElement = primitiveMetadataSize(elementType)
                ?: throw IllegalStateException("unknown GGUF array element type $elementType at offset $before")
            skipFully(saturatingMultiply(count, bytesPerElement))
        }
        return "array(type=$elementType,count=$count,bytes=${filePointer - before})"
    }

    private fun RandomAccessFile.readGgufString(): String {
        val length = readUInt64Le()
        if (length > 16L * 1024L * 1024L) {
            throw IllegalStateException("unreasonable GGUF string length $length at offset $filePointer")
        }
        val bytes = ByteArray(length.toInt())
        readFully(bytes)
        return String(bytes, utf8)
    }

    private fun primitiveMetadataSize(type: Long): Long? {
        return when (type) {
            0L, 1L, 7L -> 1L
            2L, 3L -> 2L
            4L, 5L, 6L -> 4L
            10L, 11L, 12L -> 8L
            else -> null
        }
    }

    private fun tensorByteSize(type: Long, elements: Long): Long? {
        val (blockSize, typeSize) = when (type) {
            0L -> 1L to 4L
            1L -> 1L to 2L
            2L -> 32L to 18L
            3L -> 32L to 20L
            6L -> 32L to 22L
            7L -> 32L to 24L
            8L -> 32L to 34L
            9L -> 32L to 40L
            10L -> 256L to 84L
            11L -> 256L to 110L
            12L -> 256L to 144L
            13L -> 256L to 176L
            14L -> 256L to 210L
            15L -> 256L to 292L
            16L -> 256L to 66L
            17L -> 256L to 74L
            18L -> 256L to 98L
            19L -> 256L to 50L
            20L -> 32L to 18L
            21L -> 256L to 110L
            22L -> 256L to 82L
            23L -> 256L to 58L
            24L -> 256L to 34L
            25L -> 256L to 50L
            26L -> 256L to 71L
            27L -> 256L to 80L
            28L -> 256L to 56L
            29L -> 256L to 148L
            30L -> 256L to 112L
            31L -> 256L to 24L
            32L -> 1L to 4L
            33L -> 1L to 2L
            34L -> 1L to 8L
            35L -> 256L to 192L
            36L -> 256L to 128L
            37L -> 256L to 128L
            38L -> 256L to 144L
            else -> return null
        }
        return saturatingMultiply((elements + blockSize - 1L) / blockSize, typeSize)
    }

    private fun RandomAccessFile.readUInt16Le(): Int {
        val b0 = readUnsignedByte()
        val b1 = readUnsignedByte()
        return b0 or (b1 shl 8)
    }

    private fun RandomAccessFile.readInt16Le(): Short = readUInt16Le().toShort()

    private fun RandomAccessFile.readUInt32Le(): Long {
        val b0 = readUnsignedByte().toLong()
        val b1 = readUnsignedByte().toLong()
        val b2 = readUnsignedByte().toLong()
        val b3 = readUnsignedByte().toLong()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun RandomAccessFile.readInt32Le(): Int = readUInt32Le().toInt()

    private fun RandomAccessFile.readUInt64Le(): Long {
        val b0 = readUnsignedByte().toLong()
        val b1 = readUnsignedByte().toLong()
        val b2 = readUnsignedByte().toLong()
        val b3 = readUnsignedByte().toLong()
        val b4 = readUnsignedByte().toLong()
        val b5 = readUnsignedByte().toLong()
        val b6 = readUnsignedByte().toLong()
        val b7 = readUnsignedByte().toLong()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or
            (b4 shl 32) or (b5 shl 40) or (b6 shl 48) or (b7 shl 56)
    }

    private fun RandomAccessFile.readInt64Le(): Long = readUInt64Le()

    private fun RandomAccessFile.skipFully(bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = skipBytes(remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            if (skipped <= 0) throw EOFException("unable to skip $remaining bytes at offset $filePointer")
            remaining -= skipped.toLong()
        }
    }

    private fun saturatingMultiply(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        if (a > Long.MAX_VALUE / b) return Long.MAX_VALUE
        return a * b
    }

    private fun alignTo(value: Long, alignment: Long): Long {
        if (alignment <= 0L) return value
        val remainder = value % alignment
        return if (remainder == 0L) value else value + alignment - remainder
    }

    private fun formatNullableBytes(value: Long?): String = value?.let { formatBytes(it) } ?: "unknown"

    private fun formatBytes(bytes: Long): String {
        val gib = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return "$bytes bytes (${String.format(Locale.US, "%.3f", gib)} GiB)"
    }

    private fun formatTime(timeMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timeMs))
    }

    private data class GgufInfo(
        val magic: String,
        val version: Long,
        val tensorCount: Long,
        val kvCount: Long,
        val architecture: String? = null,
        val name: String? = null,
        val fileType: String? = null,
        val quantizationVersion: String? = null,
        val alignment: Long? = null,
        val importantMetadata: Map<String, String> = emptyMap(),
        val tensorInfoEndOffset: Long? = null,
        val dataStartOffset: Long? = null,
        val minRequiredFileSize: Long? = null,
        val highestTensorEndOffset: Long? = null,
        val unknownTensorTypes: Int = 0
    )
}
