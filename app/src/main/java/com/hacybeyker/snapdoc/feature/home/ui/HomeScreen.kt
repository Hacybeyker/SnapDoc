package com.hacybeyker.snapdoc.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onScanClick: () -> Unit = {}, onLibraryClick: () -> Unit = {}) {
    HomeContent(modifier = modifier, onScanClick = onScanClick, onLibraryClick = onLibraryClick)
}

@Composable
private fun HomeContent(modifier: Modifier = Modifier, onScanClick: () -> Unit, onLibraryClick: () -> Unit) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onScanClick) {
                Text(text = stringResource(R.string.home_scan_action))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium
                )
                TextButton(onClick = onLibraryClick) {
                    Text(text = stringResource(R.string.home_library_action))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    SnapDocTheme {
        HomeContent(onScanClick = {}, onLibraryClick = {})
    }
}
