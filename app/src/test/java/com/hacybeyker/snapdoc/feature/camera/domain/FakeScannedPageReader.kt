package com.hacybeyker.snapdoc.feature.camera.domain

class FakeScannedPageReader(private val failure: Throwable? = null) : ScannedPageReader {

    val readUris = mutableListOf<String>()

    override suspend fun readPageBytes(uri: String): ByteArray {
        failure?.let { throw it }
        readUris += uri
        return uri.toByteArray()
    }
}
