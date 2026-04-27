package com.niranjan.englisharticle.data

import com.niranjan.englisharticle.domain.ArticleAiService
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
            systemMessage = "You are an exact article extraction engine. Return only valid JSON.",
            userMessage = """
                You are an exact article extraction engine.

                The user pasted raw text copied from a news website. It contains article text plus website noise.

                Your task:
                Extract the actual article using ONLY exact words from the pasted text.

                Strict rules:
                - Do NOT summarize
                - Do NOT rewrite
                - Do NOT paraphrase
                - Do NOT shorten paragraphs
                - Do NOT translate
                - Do NOT add new words
                - Preserve original wording exactly
                - Preserve paragraph order
                - Preserve direct quotes exactly
                - Remove only website noise such as navigation, sign in, subscribe, footer, copyright, tags, repeated image captions, follow us, ads, social links

                Return valid JSON only:
                {
                  "title": "",
                  "subtitle": "",
                  "author": "",
                  "publishedDate": "",
                  "cleanArticle": ""
                }

                Every sentence in cleanArticle must already exist in the pasted raw text.
                If the pasted text contains the full article, cleanArticle must contain the full extracted article, not a short version.

                Raw pasted article:
                $rawText
            """.trimIndent()
        )

        return CleanArticleResult.fromJson(JSONObject(content))
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
                The user double-tapped the word to understand the whole sentence.
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
