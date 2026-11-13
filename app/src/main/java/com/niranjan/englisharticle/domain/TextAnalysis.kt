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

/**
 * If [text] already contains paragraph breaks (`\n\n`), it is returned unchanged.
 * Otherwise the text is split into sentences and grouped roughly every
 * [sentencesPerParagraph] sentences, joined by a blank-line separator. This
 * keeps the article readable when the upstream cleaner returns one giant blob.
 */
fun String.ensureParagraphs(sentencesPerParagraph: Int = 3): String {
    val normalized = trim()
    if (normalized.isEmpty()) return normalized
    if (normalized.contains("\n\n")) return normalized

    val singleLine = normalized.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
    val sentences = singleLine.splitIntoSentences()

    val hasRunOnSentence = sentences.any { it.wordCount() > MAX_SENTENCE_WORDS }
    if (sentences.size <= sentencesPerParagraph && !hasRunOnSentence) return normalized

    val paragraphs = mutableListOf<String>()
    val pending = mutableListOf<String>()
    fun flush() {
        if (pending.isNotEmpty()) {
            paragraphs += pending.joinToString(" ")
            pending.clear()
        }
    }

    for (sentence in sentences) {
        val words = sentence.split(' ').filter { it.isNotEmpty() }
        if (words.size > MAX_SENTENCE_WORDS) {
            // Unpunctuated text (auto-generated YouTube captions) arrives as one endless
            // "sentence". Break it into short paragraphs so the reader is not one giant
            // block, and so a sentence lookup (which stops at a line break) sends one short
            // passage instead of the whole transcript.
            flush()
            words.chunked(RUN_ON_PARAGRAPH_WORDS).forEach { paragraphs += it.joinToString(" ") }
        } else {
            pending += sentence
            if (pending.size == sentencesPerParagraph) flush()
        }
    }
    flush()

    return paragraphs.joinToString("\n\n")
}

/** Longer than any normal sentence; a "sentence" past this has lost its punctuation. */
private const val MAX_SENTENCE_WORDS = 80

/** Paragraph size used when splitting unpunctuated text. */
private const val RUN_ON_PARAGRAPH_WORDS = 50

private fun String.wordCount(): Int = split(' ').count { it.isNotEmpty() }

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

    // Start: just after the last sentence end (or line break) before the tapped position.
    var start = 0
    var i = safeIndex - 1
    while (i >= 0) {
        val char = this[i]
        if (char == '\n') {
            start = i + 1
            break
        }
        if (char in SENTENCE_PUNCTUATION && isSentenceEndAt(i)) {
            start = endOfSentencePunctuation(i)
            break
        }
        i--
    }
    if (start > safeIndex) start = 0

    // End: the first sentence end (or line break) at or after the tapped position.
    var end = length
    var j = maxOf(safeIndex, start)
    while (j < length) {
        val char = this[j]
        if (char == '\n') {
            end = j
            break
        }
        if (char in SENTENCE_PUNCTUATION && isSentenceEndAt(j)) {
            end = endOfSentencePunctuation(j)
            break
        }
        j++
    }

    return substring(start, end).trim()
}

/**
 * Splits running text into sentences. A '.' ends a sentence only when whitespace (or the end)
 * follows and it does not close an abbreviation ("Mr.", "Dr.", "etc."), an initial or a dotted
 * abbreviation ("J.", "U.S.", "e.g."). Decimals ("3.5%") and web addresses never split, since
 * no whitespace follows their dot. The old split on every '.', '!' or '?' broke "Mr. Modi" and
 * "the U.S. economy" into fragments, so sentence lookups sent half-sentences.
 */
internal fun String.splitIntoSentences(): List<String> {
    val sentences = mutableListOf<String>()
    var start = 0
    var i = 0
    while (i < length) {
        if (this[i] in SENTENCE_PUNCTUATION && isSentenceEndAt(i)) {
            val end = endOfSentencePunctuation(i)
            substring(start, end).trim().takeIf { it.isNotEmpty() }?.let { sentences += it }
            start = end
            i = end
        } else {
            i++
        }
    }
    substring(start).trim().takeIf { it.isNotEmpty() }?.let { sentences += it }
    return sentences
}

/** True when the '.', '!' or '?' at [index] ends a sentence. */
private fun String.isSentenceEndAt(index: Int): Boolean {
    val after = endOfSentencePunctuation(index)
    if (after < length && !this[after].isWhitespace()) return false
    if (this[index] != '.') return true

    var wordStart = index
    while (wordStart > 0 && this[wordStart - 1].isLetter()) wordStart--
    val word = substring(wordStart, index)
    if (word.isEmpty()) return true
    // Part of a dotted abbreviation such as "U.S." or "e.g.".
    if (wordStart > 0 && this[wordStart - 1] == '.') return false
    // An initial such as "J." in "J. K. Rowling", or the "U." of "U.S.".
    if (word.length == 1 && word[0].isUpperCase()) return false
    return word.lowercase() !in ABBREVIATIONS
}

/** Index just past the punctuation run at [index] and any closing quotes or brackets. */
private fun String.endOfSentencePunctuation(index: Int): Int {
    var end = index + 1
    while (end < length && (this[end] in SENTENCE_PUNCTUATION || this[end] in SENTENCE_CLOSERS)) end++
    return end
}

private const val SENTENCE_PUNCTUATION = ".!?"
private const val SENTENCE_CLOSERS = "\"')]”’»"

private val ABBREVIATIONS = setOf(
    "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "st", "mt", "ft",
    "gen", "col", "lt", "sgt", "capt", "gov", "sen", "rep", "pres", "hon", "rev",
    "inc", "ltd", "corp", "bros", "vs", "etc", "approx", "dept",
    "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "sept", "oct", "nov", "dec",
    "rs", "fig", "vol", "ch", "pp"
)

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
