package com.niranjan.englisharticle.domain

import org.json.JSONArray
import org.json.JSONObject

/**
 * A phrase the AI reported: [text] is the span it copied from the article, [base] the
 * dictionary form ("bite the bullet"), [type] one of [PhraseType].
 */
data class PhraseCandidate(
    val text: String,
    val base: String = "",
    val type: PhraseType = PhraseType.Idiom
) {
    companion object {
        /**
         * Reads `{"phrases": [...]}`. Items are objects with text/base/type, or plain strings
         * (the older answer shape, still produced by some models). Items whose type the app
         * does not highlight (collocations, names) are dropped.
         */
        fun listFromJson(json: JSONObject): List<PhraseCandidate> {
            val array = json.optJSONArray("phrases") ?: return emptyList()
            return (0 until array.length()).mapNotNull { index -> array.candidateAt(index) }
        }

        private fun JSONArray.candidateAt(index: Int): PhraseCandidate? {
            val item = opt(index)
            if (item is String) {
                return item.trim().takeIf { it.isNotEmpty() }?.let { PhraseCandidate(text = it) }
            }
            val obj = item as? JSONObject ?: return null
            val type = PhraseType.fromLabel(obj.optString("type")) ?: return null
            val text = obj.optString("text").trim()
            val base = obj.optString("base").trim()
            if (text.isEmpty() && base.isEmpty()) return null
            return PhraseCandidate(text = text, base = base, type = type)
        }
    }
}

/** Phrase kinds the reader highlights (product decision: idioms, phrasal verbs, fixed expressions). */
enum class PhraseType(val label: String) {
    Idiom("idiom"),
    PhrasalVerb("phrasal_verb"),
    FixedExpression("fixed_expression");

    companion object {
        fun fromLabel(label: String): PhraseType? {
            val normalized = label.trim().lowercase().replace(' ', '_').replace('-', '_')
            if (normalized.isEmpty()) return Idiom
            return entries.firstOrNull { it.label == normalized }
        }
    }
}

/**
 * Finds each candidate in [article] and returns the article's own wording of every match
 * ("biting the bullet", not the AI's "bite the bullet"), deduplicated, longest first.
 *
 * The reader highlights by matching these strings word for word, so returning the article's
 * wording is what makes a highlight appear. The old filter kept a phrase only if the AI's
 * string occurred verbatim, which silently dropped dictionary forms ("pull someone's leg"),
 * other inflections ("bite" vs "biting"), curly-vs-straight apostrophes and line breaks.
 */
fun resolvePhraseSpans(article: String, candidates: List<PhraseCandidate>): List<String> {
    val tokens = article.phraseTokens()
    if (tokens.isEmpty()) return emptyList()

    val spans = linkedMapOf<String, String>()
    for (candidate in candidates) {
        val found = findMatches(tokens, candidate.text.patternWords(allowInflection = false))
            .ifEmpty { findMatches(tokens, candidate.text.patternWords(allowInflection = true)) }
            .ifEmpty { findMatches(tokens, candidate.base.patternWords(allowInflection = true)) }
        for (range in found) {
            val span = tokens.subList(range.first, range.last + 1)
            if (span.isProperName()) continue
            val text = span.joinToString(" ") { it.display }
            spans.putIfAbsent(text.lowercase(), text)
        }
    }
    return spans.values.sortedByDescending { it.split(' ').size }
}

/**
 * Normalizes a word for comparison: lowercase, curly quotes made straight, punctuation
 * trimmed from both ends. Inner apostrophes and hyphens stay ("don't", "long-term").
 * The reader uses the same function, so a span resolved here always highlights there.
 */
fun String.normalizedPhraseWord(): String {
    return lowercase()
        .replace('’', '\'')
        .replace('‘', '\'')
        .replace('ʼ', '\'')
        .trim { !it.isLetterOrDigit() }
}

// ── internals ──────────────────────────────────────────────────────────────

private class PhraseToken(
    /** The word as written, with edge punctuation removed (what gets stored and shown). */
    val display: String,
    val normalized: String,
    /** True when the token ends a sentence or clause, so a phrase cannot continue past it. */
    val endsClause: Boolean,
    /** True when a blank line follows, so a phrase cannot continue into the next paragraph. */
    val endsParagraph: Boolean,
    val isCapitalized: Boolean
)

private fun String.phraseTokens(): List<PhraseToken> {
    val matches = Regex("\\S+").findAll(this).toList()
    return matches.mapIndexedNotNull { index, match ->
        val raw = match.value
        val normalized = raw.normalizedPhraseWord()
        if (normalized.isEmpty()) return@mapIndexedNotNull null
        val nextStart = matches.getOrNull(index + 1)?.range?.first ?: length
        val gap = substring(match.range.last + 1, nextStart)
        val display = raw.replace('’', '\'').replace('‘', '\'').trim { !it.isLetterOrDigit() }
        PhraseToken(
            display = display,
            normalized = normalized,
            endsClause = raw.trimEnd('"', '\'', ')', '”', '’').lastOrNull() in CLAUSE_END,
            endsParagraph = gap.count { it == '\n' } >= 2,
            isCapitalized = display.firstOrNull()?.isUpperCase() == true
        )
    }
}

private val CLAUSE_END = setOf('.', '!', '?', ';', ':')

/** One element of a phrase pattern. */
private sealed interface PatternWord {
    /** A literal word, optionally matching its inflected forms. */
    data class Word(val word: String, val allowInflection: Boolean) : PatternWord

    /** "someone's", "one's": any possessive (my, his, the company's...). */
    data object Possessive : PatternWord

    /** "someone", "something": one to [maxWords] words. */
    data class Slot(val maxWords: Int) : PatternWord
}

private fun String.patternWords(allowInflection: Boolean): List<PatternWord> {
    val words = split(Regex("\\s+")).map { it.normalizedPhraseWord() }.filter { it.isNotEmpty() }
    if (words.size < 2) return emptyList()
    return words.map { word ->
        when (word) {
            in POSSESSIVE_PLACEHOLDERS -> PatternWord.Possessive
            in PERSON_PLACEHOLDERS -> PatternWord.Slot(maxWords = 3)
            in THING_PLACEHOLDERS -> PatternWord.Slot(maxWords = 5)
            else -> PatternWord.Word(word, allowInflection)
        }
    }
}

private val POSSESSIVE_PLACEHOLDERS = setOf("someone's", "somebody's", "one's", "sb's", "your", "their")
private val PERSON_PLACEHOLDERS = setOf("someone", "somebody", "sb")
private val THING_PLACEHOLDERS = setOf("something", "sth")
private val POSSESSIVE_WORDS = setOf("my", "your", "his", "her", "its", "our", "their", "one's")

/** All token ranges in [tokens] that match [pattern], without crossing a clause end. */
private fun findMatches(tokens: List<PhraseToken>, pattern: List<PatternWord>): List<IntRange> {
    if (pattern.size < 2 || pattern.none { it is PatternWord.Word }) return emptyList()
    return tokens.indices.mapNotNull { start ->
        matchFrom(tokens, start, pattern, 0)?.let { end -> start..end }
    }
}

/** Returns the index of the last token matched, or null. Backtracks over slot lengths. */
private fun matchFrom(
    tokens: List<PhraseToken>,
    tokenIndex: Int,
    pattern: List<PatternWord>,
    patternIndex: Int
): Int? {
    if (tokenIndex >= tokens.size) return null
    val element = pattern[patternIndex]
    val isLastElement = patternIndex == pattern.lastIndex

    fun continueAfter(lastUsed: Int): Int? {
        if (isLastElement) return lastUsed
        val token = tokens[lastUsed]
        if (token.endsClause || token.endsParagraph) return null
        return matchFrom(tokens, lastUsed + 1, pattern, patternIndex + 1)
    }

    return when (element) {
        is PatternWord.Word -> {
            if (!wordsMatch(element, tokens[tokenIndex].normalized)) null else continueAfter(tokenIndex)
        }
        PatternWord.Possessive -> {
            val word = tokens[tokenIndex].normalized
            val isPossessive = word in POSSESSIVE_WORDS || word.endsWith("'s") || word.endsWith("s'")
            if (!isPossessive) null else continueAfter(tokenIndex)
        }
        is PatternWord.Slot -> {
            for (length in 1..element.maxWords) {
                val last = tokenIndex + length - 1
                if (last >= tokens.size) break
                // A slot may not swallow a clause end before its last word.
                if ((tokenIndex until last).any { tokens[it].endsClause || tokens[it].endsParagraph }) break
                continueAfter(last)?.let { return it }
            }
            null
        }
    }
}

private fun wordsMatch(element: PatternWord.Word, actual: String): Boolean {
    val expected = element.word
    if (expected == actual) return true
    if (!element.allowInflection) return false
    // Short function words ("on", "in", "the") must match exactly, or "the" would match "then".
    if (expected.length < 3 && actual.length < 3) return false
    return sameLemma(expected, actual)
}

/** True when [a] and [b] are forms of the same word (bite/biting/bit, take/took/taken). */
internal fun sameLemma(a: String, b: String): Boolean {
    if (a == b) return true
    val lemmaA = IRREGULAR_FORMS[a] ?: a
    val lemmaB = IRREGULAR_FORMS[b] ?: b
    if (lemmaA == lemmaB) return true
    return b in regularForms(lemmaA) || a in regularForms(lemmaB)
}

private fun regularForms(word: String): Set<String> {
    if (word.length < 2) return emptySet()
    val forms = mutableSetOf(word + "s", word + "es", word + "ed", word + "ing", word + "d")
    if (word.endsWith("e")) {
        val stem = word.dropLast(1)
        forms += stem + "ing"
        forms += stem + "ed"
    }
    if (word.endsWith("y") && word.length > 2 && word[word.length - 2] !in VOWELS) {
        val stem = word.dropLast(1)
        forms += stem + "ies"
        forms += stem + "ied"
    }
    // Consonant doubling: stop -> stopped, cut -> cutting.
    val last = word.last()
    if (last !in VOWELS && last !in "wxy" && word.length >= 3 && word[word.length - 2] in VOWELS) {
        forms += word + last + "ing"
        forms += word + last + "ed"
    }
    return forms
}

private const val VOWELS = "aeiou"

/** Irregular verb forms mapped to their base form. Common verbs in idioms and phrasal verbs. */
private val IRREGULAR_FORMS: Map<String, String> = buildMap {
    fun verb(base: String, vararg forms: String) = forms.forEach { put(it, base) }
    verb("be", "is", "are", "was", "were", "been", "being", "am")
    verb("bite", "bit", "bitten")
    verb("break", "broke", "broken")
    verb("bring", "brought")
    verb("buy", "bought")
    verb("catch", "caught")
    verb("come", "came")
    verb("cut", "cuts", "cutting")
    verb("do", "did", "done", "does")
    verb("draw", "drew", "drawn")
    verb("drive", "drove", "driven")
    verb("eat", "ate", "eaten")
    verb("fall", "fell", "fallen")
    verb("feel", "felt")
    verb("fight", "fought")
    verb("find", "found")
    verb("get", "got", "gotten")
    verb("give", "gave", "given")
    verb("go", "went", "gone", "goes")
    verb("hang", "hung")
    verb("have", "had", "has")
    verb("hit", "hits", "hitting")
    verb("hold", "held")
    verb("keep", "kept")
    verb("know", "knew", "known")
    verb("lay", "laid")
    verb("lead", "led")
    verb("leave", "left")
    verb("let", "lets", "letting")
    verb("lose", "lost")
    verb("make", "made")
    verb("meet", "met")
    verb("pay", "paid")
    verb("put", "puts", "putting")
    verb("rise", "rose", "risen")
    verb("run", "ran")
    verb("say", "said")
    verb("see", "saw", "seen")
    verb("sell", "sold")
    verb("send", "sent")
    verb("set", "sets", "setting")
    verb("shake", "shook", "shaken")
    verb("shut", "shuts", "shutting")
    verb("sit", "sat")
    verb("speak", "spoke", "spoken")
    verb("spend", "spent")
    verb("stand", "stood")
    verb("stick", "stuck")
    verb("strike", "struck")
    verb("take", "took", "taken")
    verb("tear", "tore", "torn")
    verb("tell", "told")
    verb("think", "thought")
    verb("throw", "threw", "thrown")
    verb("turn", "turned")
    verb("wear", "wore", "worn")
    verb("win", "won")
}

/** Every word capitalized (e.g. "Wall Street", "Prime Minister"): a name, not a phrase. */
private fun List<PhraseToken>.isProperName(): Boolean = all { it.isCapitalized }
