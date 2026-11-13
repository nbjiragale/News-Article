package com.niranjan.englisharticle.ui.state

import com.niranjan.englisharticle.domain.MeaningLookupMode

data class SelectedWord(
    val word: String,
    val sentence: String,
    val showSentence: Boolean,
    /** The text around the tap (see contextAround), sent with the lookup instead of the article. */
    val context: String = ""
) {
    val lookupMode: MeaningLookupMode
        get() = if (showSentence) MeaningLookupMode.Sentence else MeaningLookupMode.Word
}
