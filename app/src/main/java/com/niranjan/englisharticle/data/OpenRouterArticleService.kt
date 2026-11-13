package com.niranjan.englisharticle.data

import com.niranjan.englisharticle.domain.ArticleAiService
import com.niranjan.englisharticle.domain.ArticleFormatter
import com.niranjan.englisharticle.domain.ArticleSummary
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.MeaningLookupMode
import com.niranjan.englisharticle.domain.MeaningResult
import com.niranjan.englisharticle.domain.PhraseCandidate
import com.niranjan.englisharticle.domain.resolvePhraseSpans
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class OpenRouterArticleService(
    private val apiKey: String,
    private val model: String
) : ArticleAiService {
    override suspend fun cleanArticle(rawText: String): CleanArticleResult {
        val content = sendJsonChatRequest(
            systemMessage = """
                You are a precise article extraction engine.
                Your ONLY job is to extract the main article body from raw pasted website text.
                You MUST aggressively strip ALL non-article content.
                Return only valid JSON.
            """.trimIndent(),
            userMessage = """
                Extract the main news article from the raw text below.

                KEEP only:
                - The article headline (put in "title")
                - The article subtitle or deck (put in "subtitle")
                - Author name (put in "author")
                - Publication date (put in "publishedDate")
                - The complete article body paragraphs (put in "cleanArticle")

                REMOVE everything that is NOT part of the article body. This includes:
                - Navigation menus, breadcrumbs, section/category labels (Home, News, India, Sports, etc.)
                - Login, Sign in, Sign up, Register, Subscribe, Paywall prompts
                - Cookie consent banners, Privacy policy, Terms of use
                - Advertisement labels, Sponsored content markers, "Story continues below advertisement"
                - "Read more", "Also read", "Related stories", "Recommended for you", "More from" sections
                - Entire blocks of related article titles or teasers
                - Social media buttons, share counts, "Share on", "Follow us", Tweet, WhatsApp, Telegram links
                - Newsletter signup prompts, "Get our newsletter", "Sign up for alerts"
                - App download prompts ("Download app", "Open in app", Google Play, App Store)
                - Photo/image credits (Getty, Reuters, AFP, AP, PTI, File Photo, etc.) unless inline in a sentence
                - Standalone image captions that repeat the article text or describe photos
                - Comments section, reader comments, "Post a comment", "View comments"
                - Footer content (About us, Contact, Careers, Copyright, All rights reserved)
                - Metadata lines: "X min read", "Published:", "Updated:", "Last Modified:", timestamps
                - "Written by", "Reported by", "Edited by", "With inputs from" lines (put author in "author" field instead)
                - Video/audio player labels, "Watch", "Listen to this article"
                - Tags, topic labels, category badges
                - Any standalone URL or web address
                - Duplicate or repeated paragraphs
                - Breaking news tickers, live blog headers
                - "First Published", "Source:", agency tags on their own line

                Strict extraction rules:
                - Do NOT summarize, rewrite, paraphrase, shorten, or translate
                - Do NOT add any words not present in the original
                - Preserve original wording, paragraph order, and direct quotes exactly
                - cleanArticle must contain the COMPLETE article body, not a shortened version
                - Every sentence in cleanArticle must exist verbatim in the raw text
                - Separate paragraphs in cleanArticle with a BLANK LINE (two newlines, "\n\n")
                - If the raw text shows the body as one giant block, split it into natural paragraphs of 2-4 sentences each based on topic shifts and sentence flow
                - Never return the article body as a single paragraph

                Return valid JSON:
                {
                  "title": "",
                  "subtitle": "",
                  "author": "",
                  "publishedDate": "",
                  "cleanArticle": ""
                }

                Raw pasted text:
                $rawText
            """.trimIndent()
        )

        val result = CleanArticleResult.fromJson(parseModelJson(content))
        val postCleaned = ArticleFormatter.postClean(result.cleanArticle)
        return result.copy(cleanArticle = postCleaned)
    }

    override suspend fun formatTranscript(
        rawTranscript: String,
        videoTitle: String,
        videoAuthor: String
    ): CleanArticleResult {
        val titleHint = videoTitle.takeIf { it.isNotBlank() }.orEmpty()
        val authorHint = videoAuthor.takeIf { it.isNotBlank() }.orEmpty()

        val content = sendJsonChatRequest(
            systemMessage = """
                You convert raw YouTube transcripts into readable English articles for a Kannada-speaking learner.
                Return only valid JSON.
            """.trimIndent(),
            userMessage = """
                Convert the raw YouTube transcript below into a readable article.

                STRICT rules:
                - Do NOT summarize, shorten, paraphrase, or translate.
                - Keep every spoken sentence, in the original speaking order.
                - Preserve the speaker's words; only fix capitalization, punctuation, and obvious filler artifacts (uh, um, [Music], [Applause], speaker tags like "Speaker 1:") so it reads as prose.
                - Add sentence boundaries and paragraph breaks where natural. Use a BLANK LINE ("\n\n") between paragraphs of 2-4 sentences each based on topic shifts.
                - Do not invent facts, names, dates, or numbers. If the transcript is in another language, keep that language; do not translate.
                - Do not add headings, bullet lists, intro/outro lines, "in this video", or commentary.
                - Do not include timestamps.

                Metadata:
                - Use the video title as "title" if provided.
                - Use the channel/author name as "author" if provided.
                - Leave "subtitle" and "publishedDate" as empty strings.

                Return JSON in this exact shape:
                {
                  "title": "",
                  "subtitle": "",
                  "author": "",
                  "publishedDate": "",
                  "cleanArticle": ""
                }

                Video title: $titleHint
                Channel: $authorHint

                Raw transcript:
                $rawTranscript
            """.trimIndent()
        )

        val parsed = CleanArticleResult.fromJson(parseModelJson(content))
        val resolvedTitle = parsed.title.ifBlank { titleHint }
        val resolvedAuthor = parsed.author.ifBlank { authorHint }
        val postCleaned = ArticleFormatter.postClean(parsed.cleanArticle)
        return parsed.copy(
            title = resolvedTitle,
            author = resolvedAuthor,
            cleanArticle = postCleaned
        )
    }

    /**
     * Finds the idioms, phrasal verbs and fixed expressions a learner needs explained, and
     * returns them in the article's own wording (see [resolvePhraseSpans]).
     *
     * Long articles are sent in chunks of about [PHRASE_CHUNK_WORDS] words: in one long
     * request the model reports far fewer phrases. Chunks run in parallel, a few at a time;
     * a failed chunk is skipped, and only if every chunk fails is an error thrown.
     */
    override suspend fun extractIdiomaticPhrases(articleText: String): List<String> {
        if (articleText.isBlank()) return emptyList()

        val chunks = articleText.phraseChunks(PHRASE_CHUNK_WORDS)
        val limiter = Semaphore(MAX_PARALLEL_PHRASE_REQUESTS)
        val results = supervisorScope {
            chunks.map { chunk ->
                async {
                    limiter.withPermit {
                        try {
                            Result.success(requestPhraseCandidates(chunk))
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (error: Throwable) {
                            Log.w(TAG, "Phrase detection failed for one chunk", error)
                            Result.failure(error)
                        }
                    }
                }
            }.awaitAll()
        }

        if (results.all { it.isFailure }) {
            throw results.first().exceptionOrNull() ?: IllegalStateException("Phrase detection failed.")
        }
        val candidates = results.flatMap { it.getOrNull().orEmpty() }
        return resolvePhraseSpans(articleText, candidates)
    }

    private suspend fun requestPhraseCandidates(text: String): List<PhraseCandidate> {
        val content = sendJsonChatRequest(
            systemMessage = """
                You are an English teacher marking a news text for an intermediate learner whose first language is Kannada.
                Return only valid JSON.
            """.trimIndent(),
            userMessage = """
                Find every multi-word expression in the text whose meaning a learner could NOT work out from the individual words.

                Include these types:
                - "idiom": figurative expressions. Examples: bite the bullet, in hot water, a drop in the ocean, turn the corner.
                - "phrasal_verb": verb + particle with a special meaning. Examples: carry out, look into, set up, call off, step down, rule out, bring about.
                - "fixed_expression": fixed multi-word phrases. Examples: in the wake of, on the other hand, take into account, come under fire, in charge of, at stake.

                Never include:
                - Names of people, places, organisations, titles or events (Prime Minister, Wall Street, World Cup).
                - Ordinary noun phrases or adjective + noun pairs (economic growth, strong demand).
                - Literal phrasal verbs whose meaning is obvious (walk in, sit down, go back).
                - Single words.

                For each expression return:
                - "text": copied EXACTLY from the text, same words and same order, including any words inside it (for example "turned the offer down").
                - "base": the dictionary form (for example "turn down").
                - "type": one of "idiom", "phrasal_verb", "fixed_expression".

                Example:
                Text: Officials will look into the claims after the minister came under fire for biting the bullet on taxes.
                Output: {"phrases": [
                  {"text": "look into", "base": "look into", "type": "phrasal_verb"},
                  {"text": "came under fire", "base": "come under fire", "type": "fixed_expression"},
                  {"text": "biting the bullet", "base": "bite the bullet", "type": "idiom"}
                ]}

                Return JSON in exactly this shape: {"phrases": [ ... ]}
                Return {"phrases": []} if there are none. Do not add commentary.

                Text:
                $text
            """.trimIndent()
        )
        return PhraseCandidate.listFromJson(parseModelJson(content))
    }

    override suspend fun summarizeArticle(articleText: String): ArticleSummary {
        if (articleText.isBlank()) return ArticleSummary.Empty

        val content = sendJsonChatRequest(
            systemMessage = """
                You are a news editor and Kannada translator helping a Kannada-speaking learner understand English news articles.
                Return only valid JSON.

                Critical language rule:
                - whatHappenedEnglish and gistEnglish MUST be in plain, simple English (CEFR A2 level).
                - whatHappenedKannada and gistKannada MUST be in Kannada language using Kannada script only.
                - Never use Telugu, Malayalam, Tamil, Hindi, Devanagari, romanized Kannada, or transliteration in the Kannada fields.
            """.trimIndent(),
            userMessage = """
                Read the article below and produce a short two-part summary that helps the reader quickly understand the article.

                Part 1 — "What happened":
                - One or two simple sentences describing the concrete event or news in this article.
                - Focus on facts: who, what, when, where.

                Part 2 — "What this article is about":
                - One or two simple sentences describing the broader topic, theme, or angle of the article.
                - Explain why this story matters or what perspective it takes.

                Style rules:
                - Use very simple English words. Avoid jargon, idioms, and complex grammar.
                - Each English field should be one or two sentences only (max ~40 words combined).
                - Translate each English field faithfully into natural Kannada in Kannada script.
                - Do not copy sentences verbatim from the article; rewrite in your own simple words.
                - Do not add commentary, opinions, or extra notes.

                Return JSON exactly in this shape:
                {
                  "whatHappenedEnglish": "",
                  "whatHappenedKannada": "",
                  "gistEnglish": "",
                  "gistKannada": ""
                }

                Article:
                $articleText
            """.trimIndent()
        )

        return ArticleSummary.fromJson(parseModelJson(content))
    }

    override suspend fun fetchMeaning(
        articleText: String,
        sentence: String,
        word: String,
        lookupMode: MeaningLookupMode
    ): MeaningResult {
        val firstResult = requestMeaning(
            articleText = articleText,
            sentence = sentence,
            word = word,
            lookupMode = lookupMode,
            correction = null
        )
        if (firstResult.isValidFor(lookupMode, sentence)) return firstResult

        val correctedResult = requestMeaning(
            articleText = articleText,
            sentence = sentence,
            word = word,
            lookupMode = lookupMode,
            correction = """
                Your previous answer used the wrong language or script.
                Rewrite the Kannada fields in Kannada language using Kannada script only.
                Do not use Telugu, Malayalam, Tamil, Hindi, Devanagari, or transliteration.
                If this is a sentence lookup, exampleKannada must translate the entire tapped sentence,
                not only the tapped word.
            """.trimIndent()
        )
        if (!correctedResult.isValidFor(lookupMode, sentence)) {
            error("AI returned a non-Kannada meaning. Please retry.")
        }

        return correctedResult
    }

    private suspend fun requestMeaning(
        articleText: String,
        sentence: String,
        word: String,
        lookupMode: MeaningLookupMode,
        correction: String?
    ): MeaningResult {
        val content = sendJsonChatRequest(
            systemMessage = """
                You are an English teacher for Kannada speakers.
                Return only valid JSON.

                Critical language rule:
                - meaningKannada, explanationKannada, and exampleKannada MUST be in Kannada language using Kannada script.
                - Never use Telugu, Malayalam, Tamil, Hindi, Devanagari, romanized Kannada, or transliteration in those fields.
                - simpleEnglish, partOfSpeech, word, and exampleEnglish must stay in English.
            """.trimIndent(),
            userMessage = """
                ${correction?.let { "$it\n\n" } ?: ""}Context from the article (title, summary and the text around the tapped sentence):
                $articleText

                Tapped sentence:
                $sentence

                Tapped word or phrase:
                $word

                ${lookupMode.instructions()}

                Return JSON:
                {
                  "word": "",
                  "meaningKannada": "",
                  "simpleEnglish": "",
                  "partOfSpeech": "",
                  "explanationKannada": "",
                  "exampleEnglish": "",
                  "exampleKannada": ""
                }
            """.trimIndent()
        )

        return MeaningResult.fromJson(parseModelJson(content))
    }

    private fun MeaningLookupMode.instructions(): String {
        return when (this) {
            MeaningLookupMode.Word -> """
                Explain the meaning of the tapped word or phrase ONLY in this context.
                If the tapped text has multiple words, explain it as one phrase, not as separate words.
                exampleEnglish should be a short English sentence using the tapped word or phrase.
                exampleKannada should be the Kannada translation of that example sentence.
            """.trimIndent()

            MeaningLookupMode.Sentence -> """
                The user requested the meaning of the whole sentence containing this word.
                Translate the ENTIRE tapped sentence into natural Kannada.
                Do not translate only the tapped word.
                Put the original full tapped sentence in exampleEnglish.
                Put the full Kannada translation of the tapped sentence in exampleKannada.
                meaningKannada should still be the Kannada meaning of the tapped word.
                simpleEnglish should briefly define the tapped word in this sentence.
                explanationKannada should briefly explain the meaning of the full sentence in Kannada.
                partOfSpeech should be "Sentence context".
            """.trimIndent()
        }
    }

    private fun MeaningResult.isValidFor(
        lookupMode: MeaningLookupMode,
        sourceSentence: String
    ): Boolean {
        if (!hasValidKannadaFields()) return false
        if (lookupMode == MeaningLookupMode.Word) return true

        return hasUsableSentenceTranslation(sourceSentence)
    }

    private suspend fun sendJsonChatRequest(
        systemMessage: String,
        userMessage: String
    ): String {
        if (apiKey.isBlank()) {
            error("OpenRouter API key is missing.")
        }

        val payload = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", systemMessage)
                    )
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", userMessage)
                    )
            )
            .put(
                "response_format",
                JSONObject()
                    .put("type", "json_object")
            )
            .toString()

        return try {
            retryWithBackoff(isRetryable = Throwable::isSafeToRetryBilledRequest) {
                withContext(Dispatchers.IO) { executeChatRequest(payload) }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            // Surface a friendly message to the UI; keep the raw detail in logs only.
            throw IllegalStateException(friendlyMessage(error), error)
        }
    }

    private fun executeChatRequest(payload: String): String {
        val connection = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection)
            .apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 45_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("HTTP-Referer", "https://local.personal/english-article")
                setRequestProperty("X-Title", "English Article")
            }

        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (responseCode !in 200..299) {
                Log.w(TAG, "OpenRouter error $responseCode: ${body.take(300)}")
                if (responseCode == 429 || responseCode in 500..599) {
                    throw RetryableHttpException(responseCode, "OpenRouter HTTP $responseCode")
                }
                throw OpenRouterHttpException(responseCode)
            }

            return JSONObject(body)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
                .removeSurrounding("```json", "```")
                .removeSurrounding("```")
                .trim()
        } finally {
            connection.disconnect()
        }
    }

    private fun friendlyMessage(error: Throwable): String = when {
        error is RetryableHttpException && error.code == 429 ->
            "The service is busy right now. Please wait a moment and try again."
        error is RetryableHttpException ->
            "The article service is temporarily unavailable. Please try again."
        error is SocketTimeoutException ->
            "The article service took too long to answer. Please try again."
        error is IOException ->
            "Couldn't reach the article service. Please check your internet connection."
        error is OpenRouterHttpException && (error.code == 401 || error.code == 403) ->
            "The article service rejected the app's API key."
        error is OpenRouterHttpException ->
            "The article service returned an error (HTTP ${error.code}). Please try again."
        error is JSONException ->
            "The article service sent an answer the app could not read. Please try again."
        else -> error.message ?: "Something went wrong. Please try again."
    }

    /**
     * Parses the JSON the model wrote. Models sometimes return broken or non-JSON text; the raw
     * [JSONException] text ("Value <html> of type java.lang.String cannot be converted...")
     * used to be shown to the user as-is.
     */
    private fun parseModelJson(content: String): JSONObject = try {
        JSONObject(content)
    } catch (error: JSONException) {
        Log.w(TAG, "Model returned invalid JSON: ${content.take(200)}", error)
        throw IllegalStateException(
            "The AI sent an answer the app could not read. Please try again.",
            error
        )
    }

    private class OpenRouterHttpException(val code: Int) : RuntimeException("OpenRouter HTTP $code")

    private companion object {
        private const val TAG = "OpenRouterService"
        private const val PHRASE_CHUNK_WORDS = 1_500
        private const val MAX_PARALLEL_PHRASE_REQUESTS = 3
    }
}

/**
 * Splits text into chunks of about [maxWords] words, breaking only between paragraphs (or,
 * for a single huge paragraph, between sentences).
 */
internal fun String.phraseChunks(maxWords: Int): List<String> {
    val paragraphs = split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
    val units = paragraphs.flatMap { paragraph ->
        if (paragraph.wordCount() <= maxWords) listOf(paragraph)
        else paragraph.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
    }
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    var currentWords = 0
    for (unit in units) {
        val words = unit.wordCount()
        if (currentWords > 0 && currentWords + words > maxWords) {
            chunks += current.toString()
            current.clear()
            currentWords = 0
        }
        if (current.isNotEmpty()) current.append("\n\n")
        current.append(unit)
        currentWords += words
    }
    if (current.isNotEmpty()) chunks += current.toString()
    return chunks
}

private fun String.wordCount(): Int = split(Regex("\\s+")).count { it.isNotBlank() }
