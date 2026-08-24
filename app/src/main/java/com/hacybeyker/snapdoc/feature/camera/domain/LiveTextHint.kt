package com.hacybeyker.snapdoc.feature.camera.domain

/** What the viewfinder tells the user about what it can currently read. */
sealed interface LiveTextHint {

    /** Too few frames have agreed so far to claim anything. */
    data object Searching : LiveTextHint

    data object NoTextVisible : LiveTextHint

    data class TextVisible(val blockCount: Int) : LiveTextHint
}
