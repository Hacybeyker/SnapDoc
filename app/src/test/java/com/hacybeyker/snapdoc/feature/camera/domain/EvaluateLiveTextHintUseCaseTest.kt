package com.hacybeyker.snapdoc.feature.camera.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EvaluateLiveTextHintUseCaseTest {

    private val useCase = EvaluateLiveTextHintUseCase()

    private fun readings(vararg blockCounts: Int) = blockCounts.map { LiveTextReading(it) }

    @Test
    fun `says nothing until enough frames have arrived`() {
        val hint = useCase(readings(2, 2), LiveTextHint.Searching)

        assertEquals(LiveTextHint.Searching, hint)
    }

    @Test
    fun `three frames that all see text report how much`() {
        val hint = useCase(readings(2, 3, 4), LiveTextHint.Searching)

        assertEquals(LiveTextHint.TextVisible(4), hint)
    }

    @Test
    fun `three empty frames report that nothing is readable`() {
        val hint = useCase(readings(0, 0, 0), LiveTextHint.TextVisible(3))

        assertEquals(LiveTextHint.NoTextVisible, hint)
    }

    @Test
    fun `a single empty frame does not wipe the hint`() {
        val hint = useCase(readings(3, 0, 2), LiveTextHint.TextVisible(3))

        assertEquals(LiveTextHint.TextVisible(3), hint)
    }

    @Test
    fun `a single frame with text does not announce a document yet`() {
        val hint = useCase(readings(0, 0, 2), LiveTextHint.NoTextVisible)

        assertEquals(LiveTextHint.NoTextVisible, hint)
    }

    @Test
    fun `only the newest frames count, older ones cannot hold the hint back`() {
        val hint = useCase(readings(0, 0, 0, 5, 6, 7), LiveTextHint.NoTextVisible)

        assertEquals(LiveTextHint.TextVisible(7), hint)
    }

    @Test
    fun `a steady document keeps reporting its latest block count`() {
        val hint = useCase(readings(4, 4, 4), LiveTextHint.TextVisible(4))

        assertEquals(LiveTextHint.TextVisible(4), hint)
    }
}
