package com.hacybeyker.snapdoc.feature.ocr.data

import android.content.Context
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hacybeyker.snapdoc.core.coroutines.IoDispatcher
import com.hacybeyker.snapdoc.feature.ocr.domain.DocumentTextRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Text Recognition v2 runs entirely on the device, which is the project's premise: the image never
 * leaves the phone and OCR works offline and for free.
 *
 * The client is a singleton because it owns the loaded model — building one per page would pay the
 * load cost again for every page of the same scan. It is deliberately never closed: it lives as long
 * as the process, and closing it would only strand the next call.
 */
@Singleton
class MlKitDocumentTextRecognizer @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DocumentTextRecognizer {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /**
     * `fromFilePath` is used rather than decoding the bitmap by hand because it applies the file's
     * EXIF rotation — a page read sideways recognizes as gibberish.
     */
    override suspend fun recognizeBlocks(imagePath: String): List<String> = withContext(ioDispatcher) {
        val image = InputImage.fromFilePath(context, File(imagePath).toUri())
        recognizer.process(image).await().textBlocks.map { it.text }
    }
}
