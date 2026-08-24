package com.hacybeyker.snapdoc.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Carries the stored pages by path, which is the whole contract between the camera slice and the OCR
 * slice: neither imports the other's internals, they agree on where the images already live.
 */
@Serializable
data class DocumentText(val imagePaths: List<String>) : NavKey
