package com.hacybeyker.snapdoc.feature.camera.ui

sealed interface CameraPreviewIntent {
    data object ViewfinderReady : CameraPreviewIntent
    data object CameraUnavailable : CameraPreviewIntent
    data object CapturePhoto : CameraPreviewIntent
    data class PhotoCaptured(val jpegBytes: ByteArray, val capturedAtEpochMillis: Long) : CameraPreviewIntent {
        // ByteArray compares by reference, which would make two different captures look equal.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PhotoCaptured) return false
            return jpegBytes.contentEquals(other.jpegBytes) &&
                capturedAtEpochMillis == other.capturedAtEpochMillis
        }

        override fun hashCode(): Int = 31 * jpegBytes.contentHashCode() + capturedAtEpochMillis.hashCode()
    }
    data object CaptureFailed : CameraPreviewIntent

    data object ScanDocument : CameraPreviewIntent

    /** The guided scanner returns file locations, not bytes; the pages are imported from them. */
    data class PagesScanned(val pageUris: List<String>, val scannedAtEpochMillis: Long) : CameraPreviewIntent
    data object ScanDismissed : CameraPreviewIntent
    data object ScanFailed : CameraPreviewIntent
}
