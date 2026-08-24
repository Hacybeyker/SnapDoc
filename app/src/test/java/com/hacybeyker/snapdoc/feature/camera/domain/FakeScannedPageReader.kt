package com.hacybeyker.snapdoc.feature.camera.domain

/**
 * [failOnUri] narrows [failure] to a single page, which is what makes a partial import reproducible:
 * with it null the reader fails on the first page and nothing ever reaches storage.
 */
class FakeScannedPageReader(private val failure: Throwable? = null, private val failOnUri: String? = null) :
    ScannedPageReader {

    val readUris = mutableListOf<String>()

    override suspend fun readPageBytes(uri: String): ByteArray {
        if (failure != null && (failOnUri == null || failOnUri == uri)) throw failure
        readUris += uri
        return uri.toByteArray()
    }
}
