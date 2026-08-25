package com.hacybeyker.snapdoc.feature.library.domain

import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

/** Where a scanned page lands on the sheet, in the page's own coordinate space. */
data class PageFit(val left: Int, val top: Int, val width: Int, val height: Int)

/**
 * Places a scan on an A4 sheet: scaled to fit inside the margins, keeping its aspect ratio, centred.
 *
 * A receipt is tall and narrow and will leave white space on either side — that is correct, not a
 * bug to design around. The alternative, sizing every page to its own image, produces a PDF whose
 * sheets are all different shapes and which prints unpredictably; a document people expect to print
 * or email should be A4.
 *
 * This is the whole of the PDF work that can be tested without a device, which is why it is a use
 * case of its own rather than arithmetic buried in the exporter's draw loop.
 */
class FitImageToPageUseCase @Inject constructor() {

    operator fun invoke(
        imageWidth: Int,
        imageHeight: Int,
        page: PageSize = PageSize.A4,
        margin: Int = MARGIN
    ): PageFit {
        require(imageWidth > 0 && imageHeight > 0) { "An image with no size cannot be placed on a page" }
        val availableWidth = page.width - margin * 2
        val availableHeight = page.height - margin * 2
        // The smaller ratio is the binding one: it is what keeps the other side inside the margins.
        val scale = min(
            availableWidth.toDouble() / imageWidth,
            availableHeight.toDouble() / imageHeight
        )
        val width = (imageWidth * scale).roundToInt().coerceAtLeast(1)
        val height = (imageHeight * scale).roundToInt().coerceAtLeast(1)
        return PageFit(
            left = (page.width - width) / 2,
            top = (page.height - height) / 2,
            width = width,
            height = height
        )
    }

    private companion object {
        const val MARGIN = 24
    }
}

/** PDF sizes are in points (1/72 inch), which is why A4 is 595 x 842 and not a pixel count. */
data class PageSize(val width: Int, val height: Int) {
    companion object {
        val A4 = PageSize(width = 595, height = 842)
    }
}
