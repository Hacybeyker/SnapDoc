package com.hacybeyker.snapdoc.feature.camera.data

import android.content.Context
import com.hacybeyker.snapdoc.core.coroutines.IoDispatcher
import com.hacybeyker.snapdoc.feature.camera.domain.CapturedPhoto
import com.hacybeyker.snapdoc.feature.camera.domain.PhotoStorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Scans go to the app's internal storage, not to the shared gallery: they are documents the user
 * scanned privately, and the project's whole premise is that nothing leaves the device. Internal
 * storage also needs no storage permission and is wiped when the app is uninstalled.
 */
class InternalStoragePhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PhotoStorageRepository {

    override suspend fun savePhoto(jpegBytes: ByteArray, fileName: String, capturedAtEpochMillis: Long): CapturedPhoto =
        withContext(ioDispatcher) {
            val directory = File(context.filesDir, SCANS_DIRECTORY).apply { mkdirs() }
            val file = File(directory, fileName)
            file.writeBytes(jpegBytes)
            CapturedPhoto(
                filePath = file.absolutePath,
                fileName = fileName,
                capturedAtEpochMillis = capturedAtEpochMillis
            )
        }

    private companion object {
        const val SCANS_DIRECTORY = "scans"
    }
}
