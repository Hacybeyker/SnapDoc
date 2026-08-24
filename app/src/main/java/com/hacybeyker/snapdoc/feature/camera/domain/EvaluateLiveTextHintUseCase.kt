package com.hacybeyker.snapdoc.feature.camera.domain

import javax.inject.Inject

/**
 * Turns the last few frames into what the viewfinder should say.
 *
 * Live OCR jitters: a document held perfectly still reads as 3 blocks, then 2, then 4, and as
 * nothing at all the moment a hand crosses it or the lens hunts for focus. Reacting to each frame
 * would make the hint strobe, so the recent frames have to agree unanimously before it changes;
 * while they disagree the previous hint stands. That debounce is the whole reason this is a use case
 * and not a `when` inside the ViewModel — and being a pure function of its inputs, it is testable
 * without a camera.
 */
class EvaluateLiveTextHintUseCase @Inject constructor() {

    /** [recentReadings] newest last; anything older than [AGREEING_FRAMES] is ignored. */
    operator fun invoke(recentReadings: List<LiveTextReading>, currentHint: LiveTextHint): LiveTextHint {
        if (recentReadings.size < AGREEING_FRAMES) return currentHint
        val window = recentReadings.takeLast(AGREEING_FRAMES)
        return when {
            window.none { it.hasText } -> LiveTextHint.NoTextVisible
            window.all { it.hasText } -> LiveTextHint.TextVisible(window.last().blockCount)
            else -> currentHint
        }
    }

    companion object {
        /**
         * Three frames is well under a second at the rate OCR can actually keep up with — long
         * enough for a wobble to settle, short enough that the hint still feels immediate.
         */
        const val AGREEING_FRAMES = 3
    }
}
