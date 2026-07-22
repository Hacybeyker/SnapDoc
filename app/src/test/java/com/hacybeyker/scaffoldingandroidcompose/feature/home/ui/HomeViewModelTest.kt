package com.hacybeyker.scaffoldingandroidcompose.feature.home.ui

import app.cash.turbine.test
import com.hacybeyker.scaffoldingandroidcompose.core.test.MainDispatcherRule
import com.hacybeyker.scaffoldingandroidcompose.feature.home.FakeGreetingRepository
import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.usecase.ObserveGreetingUseCase
import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.usecase.UpdateGreetingNameUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeGreetingRepository(initialName = "Android")

    private fun buildViewModel() = HomeViewModel(
        observeGreeting = ObserveGreetingUseCase(repository),
        updateGreetingName = UpdateGreetingNameUseCase(repository)
    )

    @Test
    fun `starts on loading and emits content with the initial greeting`() = runTest {
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val content = awaitItem() as HomeUiState.Content
            assertEquals("Android", content.greeting.name)
        }
    }

    @Test
    fun `submit name intent updates the greeting`() = runTest {
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals("Android", (awaitItem() as HomeUiState.Content).greeting.name)

            viewModel.onIntent(HomeIntent.SubmitName(name = "Compose"))

            assertEquals("Compose", (awaitItem() as HomeUiState.Content).greeting.name)
        }
    }
}
