package com.hacybeyker.scaffoldingandroidcompose.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.scaffoldingandroidcompose.R
import com.hacybeyker.scaffoldingandroidcompose.core.ui.theme.ScaffoldingAndroidComposeTheme
import com.hacybeyker.scaffoldingandroidcompose.core.ui.theme.spacing
import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.Greeting

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
fun HomeContent(uiState: HomeUiState, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    when (uiState) {
        is HomeUiState.Loading -> HomeLoading(modifier = modifier)
        is HomeUiState.Content -> HomeGreeting(
            greeting = uiState.greeting,
            onSubmitName = { onIntent(HomeIntent.SubmitName(name = it)) },
            modifier = modifier
        )
    }
}

@Composable
private fun HomeLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HomeGreeting(greeting: Greeting, onSubmitName: (String) -> Unit, modifier: Modifier = Modifier) {
    var draftName by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.home_greeting, greeting.name),
            style = MaterialTheme.typography.headlineMedium
        )
        OutlinedTextField(
            value = draftName,
            onValueChange = { draftName = it },
            label = { Text(text = stringResource(R.string.home_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onSubmitName(draftName) }) {
            Text(text = stringResource(R.string.home_submit))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    ScaffoldingAndroidComposeTheme {
        HomeContent(
            uiState = HomeUiState.Content(greeting = Greeting(name = "Android")),
            onIntent = {}
        )
    }
}
