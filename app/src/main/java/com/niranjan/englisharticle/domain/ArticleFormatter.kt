package com.niranjan.englisharticle.domain

object ArticleFormatter {
    private val unwantedLinePhrases = listOf(
        "advertisement",
        "sponsored",
        "subscribe",
        "subscription",
        "sign in",
        "sign up",
        "log in",
        "login",
        "cookie",
        "cookies",
        "privacy policy",
        "terms of use",
        "skip to content",
        "skip to main content",
        "read more",
        "also read",
        "related stories",
        "related articles",
        "recommended",
        "recommended for you",
        "you may also like",
        "you might also like",
        "more from",
        "most read",
        "trending",
        "top stories",
        "top news",
        "latest updates",
        "latest news",
        "breaking news",
        "follow us",
        "follow for",
        "share this",
        "share article",
        "share on",
        "newsletter",
        "download app",
        "open app",
        "install app",
        "google play",
        "app store",
        "listen to article",
        "watch live",
        "live tv",
        "allow notifications",
        "enable notifications",
        "continue reading",
        "story continues below",
        "unlock",
        "premium",
        "already a subscriber",
        "comments",
        "post a comment",
        "view all",
        "web stories",
        "for more news",
        "click here",
        "click to read",
        "read full story",
        "next story",
        "previous story",
        "from our partners",
        "promoted",
        "around the web",
        "explore more",
        "in this section",
        "more stories",
        "related",
        "recommended stories",
        "latest articles",
        "daily briefing",
        "morning briefing",
        "evening briefing",
        "join our",
        "follow the latest",
        "stay updated",
        "get latest",
        "save article",
        "bookmark",
        "gift article",
        "copy link",
        "whatsapp",
        "telegram",
        "facebook",
        "twitter",
        "x.com",
        "instagram",
        "linkedin",
        "youtube",
        "first published",
        "last modified",
        "min read",
        "listen to this",
        "audio available",
        "text size",
        "report a correction",
        "report an error",
        "get our newsletter",
        "sign up for",
        "more on this topic",
        "see all",
        "show more",
        "story first appeared",
        "all rights reserved",
        "reproduction prohibited",
        "file photo",
        "photo:",
        "image:",
        "getty images",
        "reuters photo",
        "afp photo",
        "ap photo",
        "read the full story",
        "full coverage",
        "share your views",
        "post your comment",
        "view comments",
        "switch to",
        "switch edition",
        "share via",
        "daily digest",
        "morning digest",
        "evening digest",
        "end of article",
        "copyright"
    )

    private val unwantedShortLines = setOf(
        "home",
        "menu",
        "search",
        "news",
        "india",
        "world",
        "business",
        "sports",
        "technology",
        "entertainment",
        "lifestyle",
        "opinion",
        "photos",
        "videos",
        "explained",
        "latest",
        "live",
        "english",
        "read",
        "share",
        "print",
        "economy",
        "politics",
        "tech",
        "science",
        "health",
        "education",
        "environment",
        "cricket",
        "football",
        "auto",
        "culture",
        "travel",
        "food",
        "fashion",
        "astrology",
        "weather",
        "elections",
        "podcast",
        "gallery",
        "quiz",
        "poll",
        "audio",
        "watch",
        "listen",
        "topics",
        "tags",
        "submit",
        "ians",
        "pti",
        "advertisement"
    )

    fun postClean(text: String): String {
        if (text.isBlank()) return text

        val seenLines = mutableSetOf<String>()
        return text
            .decodeCommonHtmlEntities()
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { line ->
                if (line.isBlank()) return@filter true
                val lower = line.lowercase()
                if (lower in unwantedShortLines) return@filter false
                if (unwantedLinePhrases.any { phrase -> lower == phrase || lower.contains(phrase) }) return@filter false
                if (line.containsUrl() && line.split(Regex("\\s+")).size <= 3) return@filter false
                if (line.isMetadataOnly()) return@filter false
                if (line.isMostlySymbols()) return@filter false
                seenLines.add(lower)
            }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    fun format(rawArticle: String): String {
        val seenLines = mutableSetOf<String>()
        val usefulLines = rawArticle
            .decodeCommonHtmlEntities()
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\t", " ")
            .lines()
            .map { it.cleanLine() }
            .filter { it.isMeaningfulArticleLine() }
            .filter { seenLines.add(it.lowercase()) }
            .trimToMainArticle()

        if (usefulLines.isEmpty()) return rawArticle.trim()

        return usefulLines.toArticleParagraphs()
            .joinToString("\n\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun String.isMeaningfulArticleLine(): Boolean {
        if (isBlank()) return false

        val lowerLine = lowercase()
        if (unwantedLinePhrases.any { lowerLine == it || lowerLine.contains(it) }) return false
        if (lowerLine in unwantedShortLines) return false
        if (length <= 2) return false
        if (matches(Regex("^[|.\\-_=*]+$"))) return false
        if (matches(Regex("^\\u00A9.*"))) return false
        if (matches(Regex("^\\d+\\s*/\\s*\\d+$"))) return false
        if (containsUrl()) return false
        if (isMostlySymbols()) return false
        if (isMetadataOnly()) return false
        if (isNavigationCluster()) return false

        return true
    }

    private fun List<String>.trimToMainArticle(): List<String> {
        if (isEmpty()) return this

        val firstArticleLine = indexOfFirst { it.looksLikeArticleStart() }
            .let { if (it == -1) 0 else it }
        val withoutTopClutter = drop(firstArticleLine)
        val firstFooterLine = withoutTopClutter.indexOfFirstFooterLine()

        return if (firstFooterLine == -1) {
            withoutTopClutter
        } else {
            withoutTopClutter.take(firstFooterLine)
        }
    }

    private fun List<String>.toArticleParagraphs(): List<String> {
        val paragraphs = mutableListOf<String>()
        val currentParagraph = StringBuilder()

        fun flushParagraph() {
            if (currentParagraph.isNotBlank()) {
                paragraphs += currentParagraph.toString().trim()
                currentParagraph.clear()
            }
        }

        forEachIndexed { index, line ->
            if (line.isLikelyStandaloneHeading(index)) {
                flushParagraph()
                paragraphs += line
                return@forEachIndexed
            }

            if (currentParagraph.isNotEmpty()) {
                currentParagraph.append(' ')
            }
            currentParagraph.append(line)

            if (line.endsWithSentencePunctuation()) {
                flushParagraph()
            }
        }

        flushParagraph()
        return paragraphs
    }

    private fun String.isLikelyStandaloneHeading(index: Int): Boolean {
        if (index == 0) return true
        if (length > 90) return false
        if (endsWithSentencePunctuation()) return false

        val wordCount = split(Regex("\\s+")).size
        return wordCount <= 12
    }

    private fun String.looksLikeArticleStart(): Boolean {
        val words = split(Regex("\\s+")).size
        val hasSentencePunctuation = contains(".") || contains("?") || contains("!")
        return length >= 35 || words >= 6 || hasSentencePunctuation
    }

    private fun List<String>.indexOfFirstFooterLine(): Int {
        var bodySentenceCount = 0

        forEachIndexed { index, line ->
            if (line.endsWithSentencePunctuation()) {
                bodySentenceCount += 1
            }

            val lowerLine = line.lowercase()
            val isFooter = bodySentenceCount >= 2 && (
                lowerLine.contains("more from") ||
                    lowerLine.contains("related") ||
                    lowerLine.contains("recommended") ||
                    lowerLine.contains("top stories") ||
                    lowerLine.contains("latest news") ||
                    lowerLine.contains("follow us") ||
                    lowerLine.contains("share this") ||
                    lowerLine.contains("newsletter")
                )

            if (isFooter) return index
        }

        return -1
    }

    private fun String.endsWithSentencePunctuation(): Boolean {
        return endsWith(".") || endsWith("?") || endsWith("!") || endsWith("\"") || endsWith("'")
    }

    private fun String.cleanLine(): String {
        return trim()
            .replace(Regex("\\[(advertisement|ad)\\]", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\((advertisement|ad)\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '|')
            .trim()
    }

    private fun String.decodeCommonHtmlEntities(): String {
        return replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun String.containsUrl(): Boolean {
        val lowerLine = lowercase()
        return lowerLine.contains("http://") ||
            lowerLine.contains("https://") ||
            lowerLine.contains("www.") ||
            lowerLine.matches(Regex(".*\\.(com|in|org|net)(/.*)?"))
    }

    private fun String.isMostlySymbols(): Boolean {
        val lettersAndDigits = count { it.isLetterOrDigit() }
        return lettersAndDigits < length / 3
    }

    private fun String.isMetadataOnly(): Boolean {
        val lowerLine = lowercase()
        if (lowerLine.startsWith("published ")) return true
        if (lowerLine.startsWith("published:")) return true
        if (lowerLine.startsWith("updated")) return true
        if (lowerLine.startsWith("last updated")) return true
        if (lowerLine.startsWith("edited by")) return true
        if (lowerLine.startsWith("written by")) return true
        if (lowerLine.startsWith("reported by")) return true
        if (lowerLine.startsWith("by ") && split(Regex("\\s+")).size <= 8) return true
        if (lowerLine.startsWith("with inputs from")) return true
        if (lowerLine.startsWith("source:")) return true
        if (lowerLine.startsWith("agency:")) return true
        if (lowerLine.startsWith("location:")) return true
        if (lowerLine.startsWith("photo credit")) return true
        if (lowerLine.startsWith("image credit")) return true
        if (lowerLine.startsWith("credit:")) return true
        if (lowerLine.startsWith("tags:")) return true
        if (lowerLine.startsWith("topics:")) return true
        if (matches(Regex("^\\d+\\s+min\\s+read$", RegexOption.IGNORE_CASE))) return true
        if (matches(Regex("^[a-z ]+,\\s+[a-z]+\\s+\\d{1,2},\\s+\\d{4}.*$", RegexOption.IGNORE_CASE))) return true
        if (matches(Regex("^\\d+\\s+(share|comment|like|view)s?$", RegexOption.IGNORE_CASE))) return true

        return false
    }

    private fun String.isNavigationCluster(): Boolean {
        val words = lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size !in 2..8) return false

        val navigationWordCount = words.count { it in unwantedShortLines }
        return navigationWordCount >= 2 && navigationWordCount >= words.size / 2
    }
}
