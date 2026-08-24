package com.hacybeyker.snapdoc.feature.camera.data

import android.content.Context
import androidx.core.net.toUri
import com.hacybeyker.snapdoc.core.coroutines.IoDispatcher
import com.hacybeyker.snapdoc.feature.camera.domain.ScannedPageReader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ContentResolverPageReader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ScannedPageReader {

    override suspend fun readPageBytes(uri: String): ByteArray = withContext(ioDispatcher) {
        context.contentResolver.openInputStream(uri.toUri())?.use { it.readBytes() }
            ?: throw FileNotFoundException("The scanner page is no longer available: $uri")
    }
}
