package com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.usecase

import com.hacybeyker.scaffoldingandroidcompose.feature.home.FakeGreetingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateGreetingNameUseCaseTest {

    private val repository = FakeGreetingRepository(initialName = "Android")
    private val useCase = UpdateGreetingNameUseCase(repository)

    @Test
    fun `updates the name when input is valid`() = runTest {
        useCase("Hacybeyker")

        assertEquals("Hacybeyker", repository.currentName)
    }

    @Test
    fun `trims surrounding whitespace before saving`() = runTest {
        useCase("  Kotlin  ")

        assertEquals("Kotlin", repository.currentName)
    }

    @Test
    fun `keeps the previous name when input is blank`() = runTest {
        useCase("   ")

        assertEquals("Android", repository.currentName)
    }
}
