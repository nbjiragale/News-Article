package com.niranjan.englisharticle.domain

data class WordToken(
    val text: String,
    val startIndex: Int,
    val paragraphIndex: Int
) {
    fun cleanWord(): String = text.trim().trim { !it.isLetterOrDigit() }
}

data class WordTokenGroup(
    val tokens: List<WordToken>,
    val isPhrase: Boolean
) {
    val text: String
        get() = tokens.joinToString(" ") { it.text }

    val startIndex: Int
        get() = tokens.firstOrNull()?.startIndex ?: 0

    fun cleanText(): String = tokens.joinToString(" ") { it.cleanWord() }.trim()
}

fun String.toWordTokens(): List<WordToken> {
    var paragraphIndex = 0
    var lastEnd = 0
    return Regex("\\S+").findAll(this).map { match ->
        if (substring(lastEnd, match.range.first).contains("\n\n")) {
            paragraphIndex += 1
        }
        lastEnd = match.range.last + 1
        WordToken(
            text = match.value,
            startIndex = match.range.first,
            paragraphIndex = paragraphIndex
        )
    }.toList()
}

fun List<WordToken>.toMeaningTokenGroups(priorityPhrases: List<String> = emptyList()): List<WordTokenGroup> {
    val normalizedPriorityPhrases = priorityPhrases
        .map { it.toPhraseWords() }
        .filter { it.size >= 2 }
        .distinct()
        .sortedByDescending { it.size }
    val groups = mutableListOf<WordTokenGroup>()
    var index = 0

    while (index < size) {
        val priorityPhraseSize = normalizedPriorityPhrases.firstOrNull { phraseWords ->
            matchesPhrase(startIndex = index, phraseWords = phraseWords)
        }?.size
        val phraseSize = priorityPhraseSize ?: (3 downTo 2).firstOrNull { wordCount ->
            canCreatePhrase(startIndex = index, wordCount = wordCount)
        }

        if (phraseSize == null) {
            groups += WordTokenGroup(tokens = listOf(this[index]), isPhrase = false)
            index += 1
        } else {
            groups += WordTokenGroup(tokens = subList(index, index + phraseSize), isPhrase = true)
            index += phraseSize
        }
    }

    return groups
}

fun String.findSentenceContaining(index: Int): String {
    if (isBlank()) return ""

    val safeIndex = index.coerceIn(indices)
    val start = lastIndexOfAny(charArrayOf('.', '!', '?', '\n'), startIndex = safeIndex)
        .let { if (it == -1) 0 else it + 1 }
    val end = indexOfAny(charArrayOf('.', '!', '?', '\n'), startIndex = safeIndex)
        .let { if (it == -1) length else it + 1 }

    return substring(start, end).trim()
}

fun String.isLikelyHeading(paragraphIndex: Int): Boolean {
    val trimmed = trim()
    if (trimmed.isBlank()) return false
    if (trimmed.length > 90) return false
    if (trimmed.count { it == '.' || it == '!' || it == '?' } > 0) return false

    val words = trimmed.split(Regex("\\s+"))
    return paragraphIndex == 0 || words.size <= 12
}

private fun List<WordToken>.canCreatePhrase(startIndex: Int, wordCount: Int): Boolean {
    if (startIndex + wordCount > size) return false
    return subList(startIndex, startIndex + wordCount).isLikelyMeaningPhrase()
}

private fun List<WordToken>.matchesPhrase(startIndex: Int, phraseWords: List<String>): Boolean {
    if (startIndex + phraseWords.size > size) return false
    if (subList(startIndex, startIndex + phraseWords.size - 1).any { it.endsPhraseBoundary() }) {
        return false
    }

    val tokenWords = subList(startIndex, startIndex + phraseWords.size)
        .map { it.cleanWord().toPhraseWord() }
    return tokenWords == phraseWords
}

private fun List<WordToken>.isLikelyMeaningPhrase(): Boolean {
    val words = map { it.cleanWord() }
    if (words.size !in 2..3) return false
    if (words.any { it.isBlank() || it.length < 2 || it.any { char -> char.isDigit() } }) return false
    if (dropLast(1).any { it.endsPhraseBoundary() }) return false

    val lowerWords = words.map { it.lowercase() }
    val normalized = lowerWords.joinToString(" ")
    if (normalized in knownPhrases) return true

    if (lowerWords.any { it in phraseStopWords }) return false
    if (all { it.isTitleLike() }) return true

    return when (words.size) {
        2 -> lowerWords[0].isPhraseModifier() && lowerWords[1].isPhraseHead()
        3 -> {
            val prefix = lowerWords.dropLast(1)
            val last = lowerWords.last()
            last.isPhraseHead() && prefix.all { it.isPhraseModifier() }
        }
        else -> false
    }
}

private fun WordToken.endsPhraseBoundary(): Boolean {
    return text.lastOrNull() in setOf('.', '!', '?', ':', ';')
}

private fun WordToken.isTitleLike(): Boolean {
    val word = cleanWord()
    return word.length > 1 && word.first().isUpperCase()
}

private fun String.isPhraseModifier(): Boolean {
    return this in phraseModifiers ||
        endsWith("al") ||
        endsWith("ic") ||
        endsWith("ive") ||
        endsWith("ous") ||
        endsWith("ary") ||
        endsWith("ent") ||
        endsWith("ant") ||
        endsWith("ed") ||
        endsWith("ing") ||
        endsWith("ful") ||
        endsWith("less") ||
        endsWith("able") ||
        endsWith("ible")
}

private fun String.isPhraseHead(): Boolean {
    return this in phraseHeads || (length > 4 && endsWith("s"))
}

private fun String.toPhraseWords(): List<String> {
    return Regex("[A-Za-z0-9']+")
        .findAll(this)
        .map { it.value.toPhraseWord() }
        .filter { it.isNotBlank() }
        .toList()
}

private fun String.toPhraseWord(): String {
    return lowercase().trim('\'')
}

private val knownPhrases = setOf(
    "as a result",
    "as well",
    "backed by",
    "because of",
    "carry through",
    "driven by",
    "due to",
    "even though",
    "in order",
    "instead of",
    "point to",
    "points to",
    "rather than",
    "so far",
    "such as"
)

private val phraseStopWords = setOf(
    "a",
    "an",
    "and",
    "are",
    "as",
    "at",
    "be",
    "been",
    "but",
    "by",
    "for",
    "from",
    "has",
    "have",
    "he",
    "her",
    "his",
    "in",
    "is",
    "it",
    "its",
    "of",
    "on",
    "or",
    "she",
    "that",
    "the",
    "their",
    "they",
    "this",
    "to",
    "was",
    "we",
    "were",
    "who",
    "will",
    "with"
)

private val phraseModifiers = setOf(
    "asian",
    "central",
    "chief",
    "downward",
    "economic",
    "equity",
    "european",
    "financial",
    "global",
    "growth",
    "institutional",
    "major",
    "market",
    "minor",
    "new",
    "overall",
    "positive",
    "primary",
    "public",
    "renewed",
    "similar",
    "strong",
    "tech",
    "technology",
    "upward",
    "wall",
    "street"
)

private val phraseHeads = setOf(
    "article",
    "business",
    "case",
    "catalyst",
    "company",
    "confidence",
    "conglomerate",
    "conglomerates",
    "correction",
    "data",
    "deal",
    "economies",
    "economy",
    "estimate",
    "estimates",
    "government",
    "growth",
    "industry",
    "indices",
    "inflation",
    "investor",
    "investors",
    "lead",
    "market",
    "markets",
    "momentum",
    "news",
    "performance",
    "plan",
    "policy",
    "portfolio",
    "quarter",
    "rally",
    "rate",
    "rates",
    "recovery",
    "report",
    "sector",
    "session",
    "share",
    "shares",
    "stock",
    "stocks",
    "technology",
    "tensions",
    "trajectory",
    "trend",
    "trends",
    "volatility"
)
