package com.niranjan.englisharticle.domain

import org.json.JSONObject

data class ArticleSummary(
    val whatHappenedEnglish: String,
    val whatHappenedKannada: String,
    val gistEnglish: String,
    val gistKannada: String
) {
    fun isBlank(): Boolean {
        return whatHappenedEnglish.isBlank() &&
            whatHappenedKannada.isBlank() &&
            gistEnglish.isBlank() &&
            gistKannada.isBlank()
    }

    fun isNotBlank(): Boolean = !isBlank()

    companion object {
        val Empty = ArticleSummary(
            whatHappenedEnglish = "",
            whatHappenedKannada = "",
            gistEnglish = "",
            gistKannada = ""
        )

        fun fromJson(json: JSONObject): ArticleSummary = ArticleSummary(
            whatHappenedEnglish = json.optString("whatHappenedEnglish").trim(),
            whatHappenedKannada = json.optString("whatHappenedKannada").trim(),
            gistEnglish = json.optString("gistEnglish").trim(),
            gistKannada = json.optString("gistKannada").trim()
        )
    }
}
