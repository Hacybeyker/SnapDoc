package com.hacybeyker.snapdoc.core.test

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Shared setup for the golden tests. They exist for the half of this app the JVM tests cannot reach:
 * a ViewModel test proves the state is right, and says nothing about a scrim that stops short of the
 * screen edge or a frame whose brackets end up under the controls — both of which shipped here once.
 *
 * `recordRoborazziDebug` writes the PNGs under `src/test/screenshots` (committed);
 * `verifyRoborazziDebug` fails when a pixel moves without someone re-recording it on purpose.
 *
 * The plain [Application] replaces the app's own: Hilt's is pointless here — a golden renders a
 * stateless composable with hand-built state, never an injected one — and starting it would only add
 * a graph to build.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = "w411dp-h891dp")
abstract class ScreenshotTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    /** Captures the whole screen: these are full-page composables, and their insets are part of them. */
    protected fun capture(darkTheme: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent { SnapDocTheme(darkTheme = darkTheme) { content() } }
        composeRule.onRoot().captureRoboImage()
    }
}
