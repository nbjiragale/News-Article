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

/**
 * Groups a paragraph's tokens for display: each AI-detected phrase (and each built-in fixed
 * expression) becomes one tappable group; every other word stays on its own.
 *
 * There used to be a guesser here that also grouped title-case pairs and finance word pairs
 * ("Wall Street", "Prime Minister", "strong growth"). Those were styled exactly like real
 * idioms, so the page filled with false "phrases" and the real ones did not stand out.
 */
fun List<WordToken>.toMeaningTokenGroups(priorityPhrases: List<String> = emptyList()): List<WordTokenGroup> {
    val phrases = (priorityPhrases + KNOWN_FIXED_EXPRESSIONS)
        .map { it.toPhraseWords() }
        .filter { it.size >= 2 }
        .distinct()
        .sortedByDescending { it.size }
    val normalizedTokens = map { it.text.normalizedPhraseWord() }
    val groups = mutableListOf<WordTokenGroup>()
    var index = 0

    while (index < size) {
        val phraseSize = phrases.firstOrNull { phraseWords ->
            matchesPhrase(normalizedTokens, startIndex = index, phraseWords = phraseWords)
        }?.size

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
 * The text around the tapped position that a word lookup needs: the tapped sentence plus
 * the sentence before and after it, never leaving the paragraph (a line of the body), and
 * at most [maxWords] words.
 *
 * Lookups used to send the whole article on every tap. Picking the right sense of a word
 * needs only its surroundings, so this cuts each request from the article's full length to
 * a few hundred tokens. When the cap bites, the neighbouring sentences are trimmed from
 * their far ends first; a run-on "sentence" longer than the cap (unpunctuated captions) is
 * cut to a window centred on the tapped word.
 */
fun String.contextAround(index: Int, maxWords: Int = MEANING_CONTEXT_MAX_WORDS): String {
    if (isBlank()) return ""
    val safeIndex = index.coerceIn(indices)
    val paragraphStart = lastIndexOf('\n', safeIndex - 1).let { if (it == -1) 0 else it + 1 }
    val paragraphEnd = indexOf('\n', safeIndex).let { if (it == -1) length else it }
    val paragraph = substring(paragraphStart, paragraphEnd)
    val offsetInParagraph = safeIndex - paragraphStart

    // Sentences with their positions in the paragraph.
    val sentences = paragraph.splitIntoSentences()
    val ranges = mutableListOf<IntRange>()
    var cursor = 0
    for (sentence in sentences) {
        val start = paragraph.indexOf(sentence, cursor).takeIf { it >= 0 } ?: cursor
        ranges += start until start + sentence.length
        cursor = start + sentence.length
    }
    if (sentences.isEmpty()) return paragraph.trim().wordWindowAround(offsetInParagraph, maxWords)
    val tapped = ranges.indexOfFirst { offsetInParagraph <= it.last }.let { if (it == -1) ranges.lastIndex else it }

    val current = sentences[tapped]
    val currentWords = current.words()
    if (currentWords.size >= maxWords) {
        return current.wordWindowAround(offsetInParagraph - ranges[tapped].first, maxWords)
    }

    val budget = maxWords - currentWords.size
    val previousWords = sentences.getOrNull(tapped - 1)?.words().orEmpty()
    val nextWords = sentences.getOrNull(tapped + 1)?.words().orEmpty()
    // Share the remaining budget, giving any unused share to the other side.
    var previousTake = minOf(previousWords.size, budget / 2)
    val nextTake = minOf(nextWords.size, budget - previousTake)
    previousTake = minOf(previousWords.size, budget - nextTake)

    return listOf(
        previousWords.takeLast(previousTake).joinToString(" "),
        current,
        nextWords.take(nextTake).joinToString(" ")
    ).filter { it.isNotBlank() }.joinToString(" ")
}

/** Upper bound on the nearby text sent with a word lookup. */
const val MEANING_CONTEXT_MAX_WORDS = 120

private fun String.words(): List<String> = trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

/** [maxWords] words of this text centred on the word at [offset]. */
private fun String.wordWindowAround(offset: Int, maxWords: Int): String {
    val matches = Regex("\\S+").findAll(this).toList()
    if (matches.size <= maxWords) return trim()
    val tappedWord = matches.indexOfFirst { offset <= it.range.last }.let { if (it == -1) matches.lastIndex else it }
    val start = (tappedWord - maxWords / 2).coerceIn(0, matches.size - maxWords)
    return matches.subList(start, start + maxWords).joinToString(" ") { it.value }
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

/**
 * True when [phraseWords] match the tokens starting at [startIndex], compared with the same
 * normalization the phrase resolver uses (case, curly quotes, edge punctuation), and no word
 * before the last ends a sentence.
 */
private fun List<WordToken>.matchesPhrase(
    normalizedTokens: List<String>,
    startIndex: Int,
    phraseWords: List<String>
): Boolean {
    if (startIndex + phraseWords.size > size) return false
    if (subList(startIndex, startIndex + phraseWords.size - 1).any { it.endsPhraseBoundary() }) {
        return false
    }
    return phraseWords.indices.all { offset -> normalizedTokens[startIndex + offset] == phraseWords[offset] }
}

private fun WordToken.endsPhraseBoundary(): Boolean {
    return text.trimEnd('"', '\'', ')', '\u201D', '\u2019').lastOrNull() in setOf('.', '!', '?', ':', ';')
}

private fun String.toPhraseWords(): List<String> {
    return split(Regex("\\s+"))
        .map { it.normalizedPhraseWord() }
        .filter { it.isNotEmpty() }
}

/**
 * Fixed expressions common in news that are highlighted even when the AI misses them or
 * fails. Only multi-word expressions whose meaning a learner cannot get word by word.
 */
private val KNOWN_FIXED_EXPRESSIONS = listOf(
    "according to",
    "as a result",
    "as well as",
    "because of",
    "due to",
    "even though",
    "in order to",
    "in spite of",
    "instead of",
    "on the other hand",
    "rather than",
    "so far",
    "such as"
)
