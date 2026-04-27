package com.niranjan.englisharticle.ui.state

import com.niranjan.englisharticle.domain.MeaningLookupMode

data class SelectedWord(
    val word: String,
    val sentence: String,
    val showSentence: Boolean
) {
    val lookupMode: MeaningLookupMode
        get() = if (showSentence) MeaningLookupMode.Sentence else MeaningLookupMode.Word
}
