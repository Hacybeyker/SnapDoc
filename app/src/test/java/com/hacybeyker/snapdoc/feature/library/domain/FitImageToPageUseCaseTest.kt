package com.hacybeyker.snapdoc.feature.library.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FitImageToPageUseCaseTest {

    private val useCase = FitImageToPageUseCase()

    private val a4 = PageSize.A4
    private val margin = 24

    @Test
    fun `a tall receipt is limited by the page height, not its width`() {
        val fit = useCase(imageWidth = 600, imageHeight = 2000, page = a4, margin = margin)

        assertEquals(a4.height - margin * 2, fit.height)
        assertTrue(fit.width < a4.width - margin * 2)
    }

    @Test
    fun `a wide image is limited by the page width`() {
        val fit = useCase(imageWidth = 4000, imageHeight = 1000, page = a4, margin = margin)

        assertEquals(a4.width - margin * 2, fit.width)
        assertTrue(fit.height < a4.height - margin * 2)
    }

    @Test
    fun `the aspect ratio survives the fit`() {
        val fit = useCase(imageWidth = 3000, imageHeight = 4000, page = a4, margin = margin)

        assertEquals(3.0 / 4.0, fit.width.toDouble() / fit.height, 0.01)
    }

    /**
     * Within a point, because page coordinates are whole numbers: when the leftover space is odd it
     * cannot be split evenly, and one side legitimately carries the extra point.
     */
    @Test
    fun `the page is centred, so the margins match to within a point`() {
        val fit = useCase(imageWidth = 1000, imageHeight = 1000, page = a4, margin = margin)

        assertTrue(kotlin.math.abs((a4.width - fit.width - fit.left) - fit.left) <= 1)
        assertTrue(kotlin.math.abs((a4.height - fit.height - fit.top) - fit.top) <= 1)
    }

    @Test
    fun `nothing is ever drawn outside the margins`() {
        val fit = useCase(imageWidth = 9000, imageHeight = 200, page = a4, margin = margin)

        assertTrue(fit.left >= margin)
        assertTrue(fit.top >= margin)
        assertTrue(fit.left + fit.width <= a4.width - margin)
        assertTrue(fit.top + fit.height <= a4.height - margin)
    }

    @Test
    fun `an extreme ratio still leaves at least one pixel to draw`() {
        val fit = useCase(imageWidth = 20_000, imageHeight = 1, page = a4, margin = margin)

        assertTrue(fit.height >= 1)
    }

    @Test
    fun `an image with no size is rejected rather than dividing by zero`() {
        assertThrows(IllegalArgumentException::class.java) { useCase(imageWidth = 0, imageHeight = 100) }
        assertThrows(IllegalArgumentException::class.java) { useCase(imageWidth = 100, imageHeight = 0) }
    }
}
