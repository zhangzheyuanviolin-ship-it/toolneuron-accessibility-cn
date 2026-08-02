package com.dark.tool_neuron.viewmodel

object ChatOutputPostProcessor {
    fun cleanupFinalText(text: String): String {
        val withoutDuplicateThinking = stripDuplicateAroundThinkingClose(text)
        val withoutDuplicateHalves = stripDuplicateAnswerHalves(withoutDuplicateThinking)
        return withoutDuplicateHalves.trim()
    }

    private fun stripDuplicateAroundThinkingClose(text: String): String {
        val marker = "</think>"
        val idx = text.indexOf(marker, ignoreCase = true)
        if (idx < 0) return text

        val before = text.substring(0, idx).trim()
        val after = text.substring(idx + marker.length).trim()
        if (before.length < 80 || after.length < 80) return text

        return if (similarityRatio(before, after) >= 0.88) after else text
    }

    private fun stripDuplicateAnswerHalves(text: String): String {
        if (text.length < 220) return text
        val middle = text.length / 2
        val searchRadius = minOf(300, text.length / 10)

        var best: Pair<Int, Double>? = null
        for (split in (middle - searchRadius)..(middle + searchRadius)) {
            if (split <= 80 || split >= text.length - 80) continue
            val left = text.substring(0, split).trim()
            val right = text.substring(split).trim()
            val score = similarityRatio(left, right)
            if (score >= 0.92 && (best == null || score > best.second)) {
                best = split to score
            }
        }

        val split = best?.first ?: return text
        return text.substring(0, split).trim()
    }

    private fun similarityRatio(a: String, b: String): Double {
        val aa = normalizeForSimilarity(a)
        val bb = normalizeForSimilarity(b)
        if (aa.length < 60 || bb.length < 60) return 0.0

        val maxLen = maxOf(aa.length, bb.length)
        val minLen = minOf(aa.length, bb.length)
        if (minLen.toDouble() / maxLen.toDouble() < 0.72) return 0.0

        val prefixLen = commonPrefixLength(aa, bb)
        if (prefixLen.toDouble() / minLen.toDouble() >= 0.86) return prefixLen.toDouble() / maxLen.toDouble()

        val tokenA = tokenSet(aa)
        val tokenB = tokenSet(bb)
        if (tokenA.isEmpty() || tokenB.isEmpty()) return 0.0
        val intersection = tokenA.count { it in tokenB }
        val union = tokenA.size + tokenB.size - intersection
        return intersection.toDouble() / union.toDouble()
    }

    private fun normalizeForSimilarity(value: String): String {
        return value
            .replace(Regex("<think>|</think>|\\[THINK]|\\[/THINK]|<reasoning>|</reasoning>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[\\p{Punct}\\s，。！？；：“”‘’、（）《》【】—…]+"), "")
            .lowercase()
            .trim()
    }

    private fun commonPrefixLength(a: String, b: String): Int {
        val limit = minOf(a.length, b.length)
        var i = 0
        while (i < limit && a[i] == b[i]) i++
        return i
    }

    private fun tokenSet(value: String): Set<String> {
        if (value.length <= 4) return emptySet()
        val result = LinkedHashSet<String>()
        var i = 0
        while (i + 4 <= value.length) {
            result.add(value.substring(i, i + 4))
            i += 2
        }
        return result
    }
}
