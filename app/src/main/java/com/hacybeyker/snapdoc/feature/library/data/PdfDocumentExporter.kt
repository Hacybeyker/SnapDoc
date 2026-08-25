package com.hacybeyker.snapdoc.feature.library.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.hacybeyker.snapdoc.core.coroutines.IoDispatcher
import com.hacybeyker.snapdoc.feature.library.domain.FitImageToPageUseCase
import com.hacybeyker.snapdoc.feature.library.domain.PageSize
import com.hacybeyker.snapdoc.feature.library.domain.PdfExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Builds the PDF with the framework's own `PdfDocument` — no dependency, and it is what the platform
 * uses for printing, so the output behaves the way a print preview expects.
 *
 * Exports go to `cacheDir`: a PDF is a throwaway made for one share, and putting it in cache lets
 * Android reclaim the space instead of quietly growing the app's storage with every send.
 */
class PdfDocumentExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val fitImageToPageUseCase: FitImageToPageUseCase
) : PdfExporter {

    override suspend fun export(fileName: String, imagePaths: List<String>): String = withContext(ioDispatcher) {
        val directory = File(context.cacheDir, EXPORTS_DIRECTORY).apply { mkdirs() }
        val file = File(directory, fileName)
        val pdf = PdfDocument()
        try {
            imagePaths.forEachIndexed { index, path -> pdf.addPage(path, pageNumber = index + 1) }
            file.outputStream().use { pdf.writeTo(it) }
        } finally {
            // Always closed: an unclosed PdfDocument holds every page's native memory.
            pdf.close()
        }
        file.absolutePath
    }

    private fun PdfDocument.addPage(imagePath: String, pageNumber: Int) {
        val bitmap = decodeScaled(imagePath)
            ?: throw FileNotFoundException("The page is no longer on disk: $imagePath")
        try {
            val fit = fitImageToPageUseCase(bitmap.width, bitmap.height)
            val page = startPage(PdfDocument.PageInfo.Builder(PAGE.width, PAGE.height, pageNumber).create())
            page.canvas.drawBitmap(
                bitmap,
                null,
                Rect(fit.left, fit.top, fit.left + fit.width, fit.top + fit.height),
                null
            )
            finishPage(page)
        } finally {
            // One page's bitmap at a time: a ten-page scan held whole would be hundreds of megabytes.
            bitmap.recycle()
        }
    }

    /**
     * Reads the dimensions first and decodes at a fraction of the original. A 12 MP scan is roughly
     * 48 MB once expanded to ARGB_8888, and the page it is drawn onto is 595 points wide — decoding
     * it whole would risk an OutOfMemoryError to produce pixels that get thrown away on the next line.
     */
    private fun decodeScaled(imagePath: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        return BitmapFactory.decodeFile(imagePath, options)
    }

    /** Halves until both sides are within the target; inSampleSize is only honoured in powers of two. */
    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= TARGET_LONGEST_SIDE || height / (sampleSize * 2) >= TARGET_LONGEST_SIDE) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private companion object {
        const val EXPORTS_DIRECTORY = "exports"
        val PAGE = PageSize.A4

        /**
         * Roughly 200 dpi across an A4 sheet — enough that printed text stays crisp, without carrying
         * the full sensor resolution into a file meant to be emailed.
         */
        const val TARGET_LONGEST_SIDE = 1654
    }
}
