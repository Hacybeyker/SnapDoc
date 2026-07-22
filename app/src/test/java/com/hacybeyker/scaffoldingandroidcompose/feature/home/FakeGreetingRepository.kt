package com.hacybeyker.scaffoldingandroidcompose.feature.home

import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.Greeting
import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.GreetingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake that honors the [GreetingRepository] contract (Liskov) so use case and ViewModel
 * tests observe real emissions instead of stubbed calls.
 */
class FakeGreetingRepository(initialName: String = "Android") : GreetingRepository {

    private val name = MutableStateFlow(initialName)

    val currentName: String get() = name.value

    override fun observeGreeting(): Flow<Greeting> = name.map { Greeting(name = it) }

    override suspend fun updateName(name: String) {
        this.name.value = name
    }
}
