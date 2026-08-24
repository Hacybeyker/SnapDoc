package com.hacybeyker.snapdoc.feature.camera.domain

/**
 * The scanner hands back locations of files it owns, not bytes. The URI is a plain String so this
 * contract stays free of Android types; resolving it is the data layer's job.
 */
interface ScannedPageReader {
    suspend fun readPageBytes(uri: String): ByteArray
}
