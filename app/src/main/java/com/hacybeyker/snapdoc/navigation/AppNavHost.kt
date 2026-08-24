package com.hacybeyker.snapdoc.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hacybeyker.snapdoc.feature.camera.ui.CameraPermissionScreen
import com.hacybeyker.snapdoc.feature.home.ui.HomeScreen
import com.hacybeyker.snapdoc.feature.ocr.ui.DocumentTextScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Home)
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Home> { HomeScreen(onScanClick = { backStack.add(Camera) }) }
            entry<Camera> {
                CameraPermissionScreen(
                    onExtractText = { imagePaths -> backStack.add(DocumentText(imagePaths)) }
                )
            }
            entry<DocumentText> { key ->
                DocumentTextScreen(imagePaths = key.imagePaths, onBack = { backStack.removeLastOrNull() })
            }
        }
    )
}
