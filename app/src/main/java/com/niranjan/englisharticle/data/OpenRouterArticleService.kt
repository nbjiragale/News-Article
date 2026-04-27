package com.niranjan.englisharticle.data

import com.niranjan.englisharticle.domain.ArticleAiService
import com.niranjan.englisharticle.domain.ArticleFormatter
import com.niranjan.englisharticle.domain.ArticleSummary
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.MeaningLookupMode
import com.niranjan.englisharticle.domain.MeaningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
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

        val result = CleanArticleResult.fromJson(JSONObject(content))
        val postCleaned = ArticleFormatter.postClean(result.cleanArticle)
        return result.copy(cleanArticle = postCleaned)
    }

    override suspend fun extractIdiomaticPhrases(articleText: String): List<String> {
        if (articleText.isBlank()) return emptyList()

        val content = sendJsonChatRequest(
            systemMessage = """
                Act as a linguistic expert.
                Return only valid JSON.
            """.trimIndent(),
            userMessage = """
                Extract all idiomatic phrases, colloquialisms, proverbs, and multi-word metaphors from the provided text.

                Guidelines:
                - Ignore literal descriptions; focus on figurative language.
                - Identify multi-word expressions that lose their meaning if split.
                - Return the exact phrase as it appears in the provided text whenever possible.
                - If a phrase is partially detected, return the complete phrase span from the text.
                - Do not return ordinary noun phrases or literal collocations.
                - Do not add commentary.

                Detection examples:
                Input: I decided to hit the hay early.
                Output: ["hit the hay"]
                Input: It's water under the bridge now.
                Output: ["water under the bridge"]
                Input: He was pulling my leg about the news.
                Output: ["pulling my leg"]
                Input: We need to cut to the chase.
                Output: ["cut to the chase"]

                Return JSON:
                {
                  "phrases": []
                }

                Text:
                $articleText
            """.trimIndent()
        )

        return JSONObject(content)
            .optJSONArray("phrases")
            ?.toStringList()
            ?.map { it.trim() }
            ?.filter { it.isValidDetectedPhrase(articleText) }
            ?.distinctBy { it.lowercase() }
            .orEmpty()
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

        return ArticleSummary.fromJson(JSONObject(content))
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
                ${correction?.let { "$it\n\n" } ?: ""}Article context:
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

        return MeaningResult.fromJson(JSONObject(content))
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
    ): String = withContext(Dispatchers.IO) {
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

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        connection.disconnect()

        if (responseCode !in 200..299) {
            error("OpenRouter error $responseCode: ${body.take(220)}")
        }

        JSONObject(body)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            .removeSurrounding("```json", "```")
            .removeSurrounding("```")
            .trim()
    }
}

private fun JSONArray.toStringList(): List<String> {
    return (0 until length()).mapNotNull { index ->
        optString(index).takeIf { it.isNotBlank() }
    }
}

private fun String.isValidDetectedPhrase(articleText: String): Boolean {
    val wordCount = trim().split(Regex("\\s+"))
        .count { token -> token.any { it.isLetter() } }
    return wordCount >= 2 && articleText.contains(this, ignoreCase = true)
}
