package com.dark.tool_neuron.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

object ContentUriLocalPathResolver {
    fun resolveReadableFile(context: Context, uri: Uri): File? {
        if (uri.scheme == "file") {
            return uri.path?.let(::File)?.takeIfReadableModelFile()
        }
        if (uri.scheme != "content") return null

        val candidates = linkedSetOf<String>()
        runCatching {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                candidates += pathsFromDocumentId(DocumentsContract.getDocumentId(uri))
            }
        }
        uri.lastPathSegment?.let { candidates += pathsFromDocumentId(it) }
        uri.pathSegments.lastOrNull()?.let { candidates += pathsFromDocumentId(it) }

        return candidates
            .asSequence()
            .map(::File)
            .mapNotNull { it.takeIfReadableModelFile() }
            .firstOrNull()
    }

    private fun pathsFromDocumentId(documentId: String): List<String> {
        val id = Uri.decode(documentId)
        val paths = mutableListOf<String>()

        fun addExternalStoragePath(suffix: String) {
            val clean = suffix.trimStart('/')
            if (suffix.startsWith("/storage/")) {
                paths += suffix
            } else if (clean.isNotBlank()) {
                paths += "/storage/emulated/0/$clean"
            }
        }

        when {
            id.startsWith("raw:") -> paths += id.removePrefix("raw:")
            id.startsWith("primary:") -> addExternalStoragePath(id.removePrefix("primary:"))
            id.startsWith("home:") -> {
                val clean = id.removePrefix("home:").trimStart('/')
                paths += "/storage/emulated/0/Documents/$clean"
            }
            id.startsWith("/storage/") -> paths += id
        }

        return paths
    }

    private fun File.takeIfReadableModelFile(): File? {
        val file = absoluteFile
        return file.takeIf {
            it.exists() && it.isFile && it.canRead() && it.length() > 0L
        }
    }
}
